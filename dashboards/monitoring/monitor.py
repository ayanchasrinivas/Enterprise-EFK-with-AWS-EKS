"""
ELK/EFK Stack Monitor — main entry point.

Runs collectors on a configurable interval, evaluates alerts,
and exposes metrics via a Prometheus endpoint and REST API.

Usage:
    python monitor.py [--once] [--no-alerts]

    --once        Run one collection cycle and exit (useful for cron/debugging)
    --no-alerts   Disable alert dispatching (collect + log only)
"""
from __future__ import annotations

import argparse
import signal
import sys
import threading
import time
from datetime import datetime, timezone

import schedule
from prometheus_client import (
    start_http_server as start_prometheus_server,
    Gauge,
    Counter,
    REGISTRY,
)
from rich.console import Console
from rich.table import Table
from rich.live import Live

from config import MONITOR_CONFIG, ALERT_CONFIG
from collectors.elasticsearch_collector import ElasticsearchCollector
from collectors.kibana_collector import KibanaCollector
from collectors.logstash_collector import LogstashCollector
from collectors.fluentbit_collector import FluentBitCollector
from alerting.alert_manager import AlertManager
from alerting.notifications import SlackNotifier, PagerDutyNotifier, EmailNotifier, SNSNotifier
from utils.logger import get_logger
from utils.metrics import METRIC_STORE

log = get_logger(__name__, level=MONITOR_CONFIG.log_level)
console = Console()

# ── Prometheus metrics ──────────────────────────────────────────────────────

ES_CLUSTER_STATUS = Gauge("elk_es_cluster_status", "Elasticsearch cluster status (0=green,1=yellow,2=red)")
ES_ACTIVE_SHARDS = Gauge("elk_es_active_shards", "Number of active shards")
ES_UNASSIGNED_SHARDS = Gauge("elk_es_unassigned_shards", "Number of unassigned shards")
ES_NODE_JVM_HEAP = Gauge("elk_es_node_jvm_heap_percent", "JVM heap %", ["node"])
ES_NODE_CPU = Gauge("elk_es_node_cpu_percent", "CPU %", ["node"])
ES_NODE_DISK_FREE = Gauge("elk_es_node_disk_free_percent", "Disk free %", ["node"])
ES_NODE_INDEX_RATE = Gauge("elk_es_node_indexing_rate", "Docs/sec indexed", ["node"])
KIBANA_STATUS = Gauge("elk_kibana_status", "Kibana status (0=available,1=degraded,2=unavailable)")
KIBANA_RESPONSE_MS = Gauge("elk_kibana_response_ms", "Kibana API response time ms")
LOGSTASH_HEAP = Gauge("elk_logstash_jvm_heap_percent", "Logstash JVM heap %")
LOGSTASH_EVENTS_IN = Counter("elk_logstash_events_in_total", "Total events ingested by Logstash")
FB_INPUT_RATE = Gauge("elk_fluentbit_input_rate", "Fluent Bit input records/sec")
FB_ERROR_RATE = Gauge("elk_fluentbit_error_rate", "Fluent Bit output errors/sec")
FB_HEALTHY_PODS = Gauge("elk_fluentbit_healthy_pods", "Healthy Fluent Bit pods")
COLLECTION_ERRORS = Counter("elk_monitor_collection_errors_total", "Collection errors", ["component"])

# ── State shared between collection thread and REST/dashboard ───────────────

_state: dict = {
    "es": None,
    "kibana": None,
    "logstash": None,
    "fluentbit": None,
    "last_collection": None,
    "alerts": [],
}
_state_lock = threading.Lock()


class StackMonitor:
    """Orchestrates all collectors and alert evaluation."""

    def __init__(self, enable_alerts: bool = True):
        self._es = ElasticsearchCollector()
        self._kb = KibanaCollector()
        self._ls = LogstashCollector()
        self._fb = FluentBitCollector()

        notifiers = []
        if enable_alerts:
            if ALERT_CONFIG.slack_webhook_url:
                notifiers.append(SlackNotifier())
            if ALERT_CONFIG.pagerduty_routing_key:
                notifiers.append(PagerDutyNotifier())
            if ALERT_CONFIG.email_from:
                notifiers.append(EmailNotifier())
            if ALERT_CONFIG.sns_topic_arn:
                notifiers.append(SNSNotifier())

        self._alert_mgr = AlertManager(notifiers)

    def collect_once(self) -> dict:
        """Run all collectors in parallel and return results dict."""
        results: dict = {}
        errors: dict = {}

        def run(name: str, fn):
            try:
                results[name] = fn()
            except Exception as exc:
                errors[name] = str(exc)
                COLLECTION_ERRORS.labels(component=name).inc()
                log.error("Collector raised exception",
                          extra={"component": name, "error": str(exc)})

        threads = [
            threading.Thread(target=run, args=("es", self._es.collect), daemon=True),
            threading.Thread(target=run, args=("kibana", self._kb.collect), daemon=True),
            threading.Thread(target=run, args=("logstash", self._ls.collect), daemon=True),
            threading.Thread(target=run, args=("fluentbit", self._fb.collect), daemon=True),
        ]
        for t in threads:
            t.start()
        for t in threads:
            t.join(timeout=60)

        # Evaluate alerts
        fired = []
        fired += self._alert_mgr.evaluate_elasticsearch(results.get("es"))
        fired += self._alert_mgr.evaluate_kibana(results.get("kibana"))
        fired += self._alert_mgr.evaluate_logstash(results.get("logstash"))
        fired += self._alert_mgr.evaluate_fluentbit(results.get("fluentbit"))

        # Update Prometheus gauges
        self._update_prometheus(results)

        # Update shared state
        with _state_lock:
            _state.update(results)
            _state["last_collection"] = datetime.now(timezone.utc).isoformat()
            _state["alerts"] = [
                {
                    "id": a.alert_id,
                    "severity": a.severity,
                    "component": a.component,
                    "title": a.title,
                    "message": a.message,
                    "timestamp": a.timestamp.isoformat(),
                }
                for a in self._alert_mgr.get_active_alerts()
            ]

        return results

    def _update_prometheus(self, results: dict) -> None:
        es = results.get("es")
        if es and es.cluster_health:
            h = es.cluster_health
            ES_CLUSTER_STATUS.set({"green": 0, "yellow": 1, "red": 2}.get(h.status, 3))
            ES_ACTIVE_SHARDS.set(h.active_shards)
            ES_UNASSIGNED_SHARDS.set(h.unassigned_shards)
        if es:
            for node in es.nodes:
                ES_NODE_JVM_HEAP.labels(node=node.node_name).set(node.jvm_heap_used_percent)
                ES_NODE_CPU.labels(node=node.node_name).set(node.os_cpu_percent)
                ES_NODE_DISK_FREE.labels(node=node.node_name).set(node.disk_free_percent)
                ES_NODE_INDEX_RATE.labels(node=node.node_name).set(node.indexing_rate)

        kb = results.get("kibana")
        if kb:
            KIBANA_STATUS.set({"available": 0, "degraded": 1, "unavailable": 2}.get(
                kb.overall_status, 3))
            KIBANA_RESPONSE_MS.set(kb.response_time_ms)

        ls = results.get("logstash")
        if ls:
            LOGSTASH_HEAP.set(ls.jvm_heap_used_percent)

        fb = results.get("fluentbit")
        if fb:
            FB_INPUT_RATE.set(fb.total_input_rate)
            FB_ERROR_RATE.set(fb.total_error_rate)
            FB_HEALTHY_PODS.set(fb.healthy_pods)

    def get_active_alerts(self):
        return self._alert_mgr.get_active_alerts()

    def get_alert_history(self, hours: int = 24):
        return self._alert_mgr.get_history(hours)


def print_status(results: dict) -> None:
    """Render a live Rich status table to the terminal."""
    table = Table(title="ELK/EFK Stack Status", show_header=True, header_style="bold cyan")
    table.add_column("Component", style="bold")
    table.add_column("Status")
    table.add_column("Details")

    es = results.get("es")
    if es and es.cluster_health:
        h = es.cluster_health
        color = {"green": "green", "yellow": "yellow", "red": "red"}.get(h.status, "white")
        table.add_row(
            "Elasticsearch",
            f"[{color}]{h.status.upper()}[/{color}]",
            f"Nodes: {h.number_of_nodes} | Shards: {h.active_shards} active, "
            f"{h.unassigned_shards} unassigned",
        )
        for node in es.nodes:
            heap_color = "red" if node.jvm_heap_used_percent > 90 else (
                "yellow" if node.jvm_heap_used_percent > 75 else "green"
            )
            table.add_row(
                f"  └ {node.node_name}",
                f"[{heap_color}]Heap: {node.jvm_heap_used_percent:.0f}%[/{heap_color}]",
                f"CPU: {node.os_cpu_percent:.0f}% | Disk free: {node.disk_free_percent:.0f}% | "
                f"Idx: {node.indexing_rate:.0f}/s | Srch: {node.search_rate:.0f}/s",
            )
    else:
        table.add_row("Elasticsearch", "[red]UNREACHABLE[/red]",
                      str(getattr(es, "error", "No data")))

    kb = results.get("kibana")
    if kb:
        color = {"available": "green", "degraded": "yellow", "unavailable": "red"}.get(
            kb.overall_status, "white")
        table.add_row(
            "Kibana",
            f"[{color}]{kb.overall_status.upper()}[/{color}]",
            f"v{kb.version} | Response: {kb.response_time_ms:.0f}ms | "
            f"Alerts: {kb.active_alerts} | Objects: {kb.saved_objects_count:,}",
        )

    ls = results.get("logstash")
    if ls:
        heap_color = "red" if ls.jvm_heap_used_percent > 90 else "green"
        table.add_row(
            "Logstash",
            f"[{heap_color}]{'UP' if not ls.error else 'DOWN'}[/{heap_color}]",
            f"Heap: {ls.jvm_heap_used_percent:.0f}% | "
            f"Events in: {ls.events_in_total:,} | Pipelines: {len(ls.pipelines)}",
        )

    fb = results.get("fluentbit")
    if fb:
        pod_color = "green" if fb.healthy_pods == fb.total_pods else "yellow"
        table.add_row(
            "Fluent Bit",
            f"[{pod_color}]{fb.healthy_pods}/{fb.total_pods} pods[/{pod_color}]",
            f"In: {fb.total_input_rate:.0f}/s | Out: {fb.total_output_rate:.0f}/s | "
            f"Errors: {fb.total_error_rate:.1f}/s",
        )

    console.print(table)


def run_loop(monitor: StackMonitor, interval: int) -> None:
    """Run collection on schedule until SIGTERM/SIGINT."""
    stop_event = threading.Event()

    def signal_handler(sig, frame):
        log.info("Shutdown signal received")
        stop_event.set()

    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)

    def job():
        log.info("Starting collection cycle")
        results = monitor.collect_once()
        print_status(results)

    schedule.every(interval).seconds.do(job)

    # Run immediately on startup
    job()

    while not stop_event.is_set():
        schedule.run_pending()
        stop_event.wait(timeout=1)

    log.info("Monitor stopped cleanly")


def main():
    parser = argparse.ArgumentParser(description="ELK/EFK Stack Monitor")
    parser.add_argument("--once", action="store_true",
                        help="Run one collection cycle and exit")
    parser.add_argument("--no-alerts", action="store_true",
                        help="Disable alert dispatching")
    parser.add_argument("--prometheus-port", type=int, default=9090,
                        help="Prometheus metrics port (default: 9090)")
    parser.add_argument("--interval", type=int,
                        default=MONITOR_CONFIG.collection_interval_seconds,
                        help="Collection interval in seconds")
    args = parser.parse_args()

    log.info("ELK/EFK Monitor starting",
             extra={"environment": MONITOR_CONFIG.environment,
                    "interval": args.interval})

    monitor = StackMonitor(enable_alerts=not args.no_alerts)

    # Start Prometheus metrics server
    if not args.once:
        start_prometheus_server(args.prometheus_port)
        log.info("Prometheus metrics server started",
                 extra={"port": args.prometheus_port})

        # Start Flask dashboard in background thread
        from dashboard.app import create_app
        app = create_app()
        dashboard_thread = threading.Thread(
            target=lambda: app.run(
                host=MONITOR_CONFIG.dashboard_host,
                port=MONITOR_CONFIG.dashboard_port,
                debug=False,
                use_reloader=False,
            ),
            daemon=True,
            name="dashboard",
        )
        dashboard_thread.start()
        log.info("Dashboard started",
                 extra={"host": MONITOR_CONFIG.dashboard_host,
                        "port": MONITOR_CONFIG.dashboard_port})

        run_loop(monitor, args.interval)
    else:
        results = monitor.collect_once()
        print_status(results)
        active = monitor.get_active_alerts()
        if active:
            console.print(f"\n[red]Active alerts: {len(active)}[/red]")
            for a in active:
                console.print(f"  [{a.severity}] {a.title}: {a.message}")
        sys.exit(0 if not any(
            a.severity == "critical" for a in active
        ) else 1)


if __name__ == "__main__":
    main()
