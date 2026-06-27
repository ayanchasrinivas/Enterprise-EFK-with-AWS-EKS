"""
Kibana metrics collector.
Checks Kibana status API, space health, saved object counts, and active alerts.
"""
from __future__ import annotations

import time
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Optional

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from config import KIBANA_CONFIG
from utils.logger import get_logger
from utils.metrics import METRIC_STORE

log = get_logger(__name__)


@dataclass
class KibanaStatusSnapshot:
    timestamp: datetime
    overall_status: str           # available / degraded / unavailable
    version: str
    plugin_statuses: dict[str, str]
    active_alerts: int
    saved_objects_count: int
    spaces: list[str]
    response_time_ms: float
    error: Optional[str] = None


class KibanaCollector:

    def __init__(self):
        self._session = self._build_session()

    def _build_session(self) -> requests.Session:
        session = requests.Session()
        session.auth = (KIBANA_CONFIG.username, KIBANA_CONFIG.password)
        session.headers.update({
            "kbn-xsrf": "true",
            "Content-Type": "application/json",
        })
        retry = Retry(total=3, backoff_factor=1, status_forcelist=[502, 503, 504])
        adapter = HTTPAdapter(max_retries=retry)
        session.mount("https://", adapter)
        session.mount("http://", adapter)
        if not KIBANA_CONFIG.verify_ssl:
            session.verify = False
        elif KIBANA_CONFIG.ca_certs:
            session.verify = KIBANA_CONFIG.ca_certs
        return session

    def _get(self, path: str) -> tuple[Any, float]:
        url = f"{KIBANA_CONFIG.host.rstrip('/')}{path}"
        start = time.monotonic()
        resp = self._session.get(url, timeout=KIBANA_CONFIG.timeout)
        resp.raise_for_status()
        return resp.json(), (time.monotonic() - start) * 1000

    def collect(self) -> KibanaStatusSnapshot:
        ts = datetime.now(timezone.utc)
        try:
            status_data, response_ms = self._get("/api/status")
            overall = status_data.get("status", {}).get("overall", {}).get("level", "unknown")

            plugin_statuses = {
                p["id"]: p.get("level", "unknown")
                for p in status_data.get("status", {}).get("core", {}).values()
                if isinstance(p, dict)
            }

            version = status_data.get("version", {}).get("number", "unknown")

            # Spaces
            try:
                spaces_data, _ = self._get("/api/spaces/space")
                spaces = [s["id"] for s in spaces_data]
            except Exception:
                spaces = ["default"]

            # Active alerting rules (using Rules API)
            try:
                rules_data, _ = self._get("/api/alerting/rules/_find?per_page=1&filter=alert.attributes.enabled:true")
                active_alerts = rules_data.get("total", 0)
            except Exception:
                active_alerts = 0

            # Saved objects (sampled)
            try:
                so_data, _ = self._get("/api/saved_objects/_find?per_page=1")
                saved_objects_count = so_data.get("total", 0)
            except Exception:
                saved_objects_count = 0

            snap = KibanaStatusSnapshot(
                timestamp=ts,
                overall_status=overall,
                version=version,
                plugin_statuses=plugin_statuses,
                active_alerts=active_alerts,
                saved_objects_count=saved_objects_count,
                spaces=spaces,
                response_time_ms=round(response_ms, 2),
            )
            self._record_metrics(snap)
            log.info("Kibana collection complete",
                     extra={"status": overall, "response_ms": response_ms})
            return snap

        except Exception as exc:
            log.error("Kibana collection failed", extra={"error": str(exc)})
            METRIC_STORE.record("kibana.collection.error", 1)
            return KibanaStatusSnapshot(
                timestamp=ts,
                overall_status="unavailable",
                version="unknown",
                plugin_statuses={},
                active_alerts=0,
                saved_objects_count=0,
                spaces=[],
                response_time_ms=0,
                error=str(exc),
            )

    def _record_metrics(self, snap: KibanaStatusSnapshot) -> None:
        status_val = {"available": 0, "degraded": 1, "unavailable": 2}.get(
            snap.overall_status, 3
        )
        METRIC_STORE.record("kibana.status", status_val)
        METRIC_STORE.record("kibana.response_time_ms", snap.response_time_ms)
        METRIC_STORE.record("kibana.active_alerts", snap.active_alerts)
        METRIC_STORE.record("kibana.saved_objects", snap.saved_objects_count)
