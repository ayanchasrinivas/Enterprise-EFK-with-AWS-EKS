"""
Logstash metrics collector.
Reads Logstash node stats and pipeline stats from the monitoring API (:9600).
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Optional

import requests

from config import LOGSTASH_CONFIG
from utils.logger import get_logger
from utils.metrics import METRIC_STORE

log = get_logger(__name__)


@dataclass
class PipelineStats:
    pipeline_id: str
    events_in: int
    events_out: int
    events_filtered: int
    duration_millis: int
    queue_events: int
    queue_size_bytes: int
    plugins_in_errors: int
    plugins_filter_errors: int
    plugins_out_errors: int
    reloads_successes: int
    reloads_failures: int


@dataclass
class LogstashSnapshot:
    timestamp: datetime
    version: str
    status: str
    jvm_heap_used_percent: float
    jvm_heap_used_bytes: int
    jvm_threads_count: int
    cpu_percent: float
    uptime_millis: int
    pipelines: list[PipelineStats]
    events_in_total: int
    events_out_total: int
    response_time_ms: float
    error: Optional[str] = None


class LogstashCollector:

    def __init__(self):
        self._base = f"{LOGSTASH_CONFIG.host.rstrip('/')}:{LOGSTASH_CONFIG.monitoring_port}"
        self._session = requests.Session()
        if not LOGSTASH_CONFIG.verify_ssl:
            self._session.verify = False

    def _get(self, path: str) -> tuple[dict, float]:
        url = f"{self._base}{path}"
        start = time.monotonic()
        resp = self._session.get(url, timeout=LOGSTASH_CONFIG.timeout)
        resp.raise_for_status()
        return resp.json(), (time.monotonic() - start) * 1000

    def collect(self) -> LogstashSnapshot:
        ts = datetime.now(timezone.utc)
        try:
            node_info, resp_ms = self._get("/_node/stats")
            pipelines_raw, _ = self._get("/_node/stats/pipelines")

            jvm = node_info.get("jvm", {})
            process = node_info.get("process", {})

            heap_used = jvm.get("mem", {}).get("heap_used_in_bytes", 0)
            heap_max = jvm.get("mem", {}).get("heap_max_in_bytes", 1)
            heap_pct = (heap_used / heap_max * 100) if heap_max else 0

            pipelines = []
            total_in = 0
            total_out = 0
            for pid, pdata in pipelines_raw.get("pipelines", {}).items():
                events = pdata.get("events", {})
                queue = pdata.get("queue", {})
                reloads = pdata.get("reloads", {})

                def count_plugin_errors(section: str) -> int:
                    return sum(
                        p.get("events", {}).get("out", 0) == 0
                        for p in pdata.get("plugins", {}).get(section, [])
                    )

                ev_in = events.get("in", 0)
                ev_out = events.get("out", 0)
                total_in += ev_in
                total_out += ev_out

                pipelines.append(PipelineStats(
                    pipeline_id=pid,
                    events_in=ev_in,
                    events_out=ev_out,
                    events_filtered=events.get("filtered", 0),
                    duration_millis=events.get("duration_in_millis", 0),
                    queue_events=queue.get("events", 0),
                    queue_size_bytes=queue.get("capacity", {}).get("used_in_bytes", 0),
                    plugins_in_errors=count_plugin_errors("inputs"),
                    plugins_filter_errors=count_plugin_errors("filters"),
                    plugins_out_errors=count_plugin_errors("outputs"),
                    reloads_successes=reloads.get("successes", 0),
                    reloads_failures=reloads.get("failures", 0),
                ))

            snap = LogstashSnapshot(
                timestamp=ts,
                version=node_info.get("version", "unknown"),
                status=node_info.get("status", "unknown"),
                jvm_heap_used_percent=round(heap_pct, 2),
                jvm_heap_used_bytes=heap_used,
                jvm_threads_count=jvm.get("threads", {}).get("count", 0),
                cpu_percent=process.get("cpu", {}).get("percent", 0),
                uptime_millis=node_info.get("jvm", {}).get("uptime_in_millis", 0),
                pipelines=pipelines,
                events_in_total=total_in,
                events_out_total=total_out,
                response_time_ms=round(resp_ms, 2),
            )
            self._record_metrics(snap)
            log.info("Logstash collection complete", extra={"pipelines": len(pipelines)})
            return snap

        except Exception as exc:
            log.error("Logstash collection failed", extra={"error": str(exc)})
            METRIC_STORE.record("logstash.collection.error", 1)
            return LogstashSnapshot(
                timestamp=ts,
                version="unknown",
                status="down",
                jvm_heap_used_percent=0,
                jvm_heap_used_bytes=0,
                jvm_threads_count=0,
                cpu_percent=0,
                uptime_millis=0,
                pipelines=[],
                events_in_total=0,
                events_out_total=0,
                response_time_ms=0,
                error=str(exc),
            )

    def _record_metrics(self, snap: LogstashSnapshot) -> None:
        METRIC_STORE.record("logstash.jvm_heap_pct", snap.jvm_heap_used_percent)
        METRIC_STORE.record("logstash.cpu_pct", snap.cpu_percent)
        METRIC_STORE.record("logstash.events_in_total", snap.events_in_total)
        METRIC_STORE.record("logstash.events_out_total", snap.events_out_total)
        METRIC_STORE.record("logstash.response_time_ms", snap.response_time_ms)
        for p in snap.pipelines:
            prefix = f"logstash.pipeline.{p.pipeline_id}"
            METRIC_STORE.record(f"{prefix}.queue_events", p.queue_events)
            METRIC_STORE.record(f"{prefix}.events_in", p.events_in)
            METRIC_STORE.record(f"{prefix}.events_out", p.events_out)
