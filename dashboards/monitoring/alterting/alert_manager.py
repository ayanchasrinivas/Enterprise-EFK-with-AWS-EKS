"""
Alert manager: evaluates collected snapshots against thresholds
and fires notifications with cooldown deduplication.
"""
from __future__ import annotations

import threading
from dataclasses import dataclass, field
from datetime import datetime, timezone, timedelta
from enum import Enum
from typing import Optional

from config import ALERT_CONFIG
from utils.logger import get_logger
from utils.metrics import METRIC_STORE

log = get_logger(__name__)


class Severity(str, Enum):
    INFO = "info"
    WARNING = "warning"
    CRITICAL = "critical"


@dataclass
class Alert:
    alert_id: str
    severity: Severity
    component: str        # elasticsearch / kibana / logstash / fluentbit
    title: str
    message: str
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    resolved: bool = False
    resolved_at: Optional[datetime] = None
    metadata: dict = field(default_factory=dict)


class AlertManager:
    """
    Evaluates snapshots, manages active alerts, and dispatches notifications.
    Implements per-alert-type cooldown to prevent notification floods.
    """

    def __init__(self, notifiers: list = None):
        self._notifiers = notifiers or []
        self._active: dict[str, Alert] = {}      # alert_id → Alert
        self._last_fired: dict[str, datetime] = {}
        self._history: list[Alert] = []
        self._lock = threading.Lock()
        self._cooldown = timedelta(minutes=ALERT_CONFIG.alert_cooldown_minutes)

    def add_notifier(self, notifier) -> None:
        self._notifiers.append(notifier)

    def evaluate_elasticsearch(self, snapshot) -> list[Alert]:
        alerts = []
        if snapshot is None or snapshot.cluster_health is None:
            alerts.append(self._make_alert(
                "es.unreachable", Severity.CRITICAL, "elasticsearch",
                "Elasticsearch Unreachable",
                f"Cannot connect to Elasticsearch: {getattr(snapshot, 'error', 'unknown')}",
            ))
            self._dispatch(alerts)
            return alerts

        h = snapshot.cluster_health

        # Cluster status
        if h.status == "red":
            alerts.append(self._make_alert(
                "es.cluster.red", Severity.CRITICAL, "elasticsearch",
                "Elasticsearch Cluster RED",
                f"Cluster {h.cluster_name} is RED. "
                f"Unassigned shards: {h.unassigned_shards}. "
                f"Active shards: {h.active_shards_percent:.1f}%",
                metadata={"unassigned_shards": h.unassigned_shards,
                          "active_shards_pct": h.active_shards_percent},
            ))
        elif h.status == "yellow":
            alerts.append(self._make_alert(
                "es.cluster.yellow", Severity.WARNING, "elasticsearch",
                "Elasticsearch Cluster YELLOW",
                f"Cluster {h.cluster_name} is YELLOW. "
                f"Unassigned shards: {h.unassigned_shards}",
            ))

        # Unassigned shards
        if h.unassigned_shards >= ALERT_CONFIG.unassigned_shards_critical:
            alerts.append(self._make_alert(
                "es.unassigned_shards.critical", Severity.CRITICAL, "elasticsearch",
                "High Unassigned Shards",
                f"{h.unassigned_shards} unassigned shards (threshold: "
                f"{ALERT_CONFIG.unassigned_shards_critical})",
            ))

        # Per-node checks
        for node in snapshot.nodes:
            # JVM heap
            if node.jvm_heap_used_percent >= ALERT_CONFIG.jvm_heap_critical_pct:
                alerts.append(self._make_alert(
                    f"es.node.{node.node_name}.jvm_heap.critical",
                    Severity.CRITICAL, "elasticsearch",
                    f"Critical JVM Heap on {node.node_name}",
                    f"JVM heap at {node.jvm_heap_used_percent:.1f}% "
                    f"(threshold: {ALERT_CONFIG.jvm_heap_critical_pct}%)",
                    metadata={"node": node.node_name, "heap_pct": node.jvm_heap_used_percent},
                ))
            elif node.jvm_heap_used_percent >= ALERT_CONFIG.jvm_heap_warning_pct:
                alerts.append(self._make_alert(
                    f"es.node.{node.node_name}.jvm_heap.warning",
                    Severity.WARNING, "elasticsearch",
                    f"High JVM Heap on {node.node_name}",
                    f"JVM heap at {node.jvm_heap_used_percent:.1f}%",
                ))

            # Disk space
            disk_used_pct = 100 - node.disk_free_percent
            if disk_used_pct >= ALERT_CONFIG.disk_watermark_critical_pct:
                alerts.append(self._make_alert(
                    f"es.node.{node.node_name}.disk.critical",
                    Severity.CRITICAL, "elasticsearch",
                    f"Critical Disk Usage on {node.node_name}",
                    f"Disk {disk_used_pct:.1f}% used on {node.node_name} "
                    f"(threshold: {ALERT_CONFIG.disk_watermark_critical_pct}%)",
                    metadata={"node": node.node_name, "disk_used_pct": disk_used_pct},
                ))
            elif disk_used_pct >= ALERT_CONFIG.disk_watermark_warning_pct:
                alerts.append(self._make_alert(
                    f"es.node.{node.node_name}.disk.warning",
                    Severity.WARNING, "elasticsearch",
                    f"High Disk Usage on {node.node_name}",
                    f"Disk {disk_used_pct:.1f}% used on {node.node_name}",
                ))

            # Indexing latency
            if node.indexing_latency_ms >= ALERT_CONFIG.indexing_latency_crit_ms:
                alerts.append(self._make_alert(
                    f"es.node.{node.node_name}.index_latency.critical",
                    Severity.CRITICAL, "elasticsearch",
                    f"Critical Indexing Latency on {node.node_name}",
                    f"Indexing latency {node.indexing_latency_ms:.0f}ms "
                    f"(threshold: {ALERT_CONFIG.indexing_latency_crit_ms}ms)",
                ))

        # ILM errors
        for policy in snapshot.ilm_policies:
            if policy.error_count > 0:
                alerts.append(self._make_alert(
                    f"es.ilm.{policy.policy_name}.errors",
                    Severity.WARNING, "elasticsearch",
                    f"ILM Policy Errors: {policy.policy_name}",
                    f"{policy.error_count} indices stuck in ILM policy {policy.policy_name}",
                    metadata={"errors": policy.errors[:5]},  # first 5 errors
                ))

        self._dispatch(alerts)
        return alerts

    def evaluate_kibana(self, snapshot) -> list[Alert]:
        alerts = []
        if snapshot is None or snapshot.error:
            alerts.append(self._make_alert(
                "kibana.unreachable", Severity.CRITICAL, "kibana",
                "Kibana Unreachable",
                f"Cannot reach Kibana: {getattr(snapshot, 'error', 'unknown')}",
            ))
        elif snapshot.overall_status == "degraded":
            alerts.append(self._make_alert(
                "kibana.degraded", Severity.WARNING, "kibana",
                "Kibana Degraded",
                f"Kibana status is degraded. Response time: {snapshot.response_time_ms}ms",
            ))
        elif snapshot.overall_status == "unavailable":
            alerts.append(self._make_alert(
                "kibana.unavailable", Severity.CRITICAL, "kibana",
                "Kibana Unavailable",
                "Kibana reports unavailable status",
            ))

        if snapshot and snapshot.response_time_ms > 5000:
            alerts.append(self._make_alert(
                "kibana.slow_response", Severity.WARNING, "kibana",
                "Kibana Slow Response",
                f"Kibana response time: {snapshot.response_time_ms:.0f}ms (>5000ms)",
            ))

        self._dispatch(alerts)
        return alerts

    def evaluate_logstash(self, snapshot) -> list[Alert]:
        alerts = []
        if snapshot is None or snapshot.error:
            alerts.append(self._make_alert(
                "logstash.unreachable", Severity.CRITICAL, "logstash",
                "Logstash Unreachable",
                f"Cannot reach Logstash: {getattr(snapshot, 'error', 'unknown')}",
            ))
        else:
            if snapshot.jvm_heap_used_percent >= ALERT_CONFIG.jvm_heap_critical_pct:
                alerts.append(self._make_alert(
                    "logstash.jvm_heap.critical", Severity.CRITICAL, "logstash",
                    "Logstash Critical JVM Heap",
                    f"Logstash JVM heap at {snapshot.jvm_heap_used_percent:.1f}%",
                ))
            for p in snapshot.pipelines:
                if p.plugins_out_errors > 0:
                    alerts.append(self._make_alert(
                        f"logstash.pipeline.{p.pipeline_id}.output_errors",
                        Severity.WARNING, "logstash",
                        f"Logstash Pipeline Output Errors: {p.pipeline_id}",
                        f"Pipeline {p.pipeline_id} has {p.plugins_out_errors} output plugin errors",
                    ))
                if p.queue_events > 100_000:
                    alerts.append(self._make_alert(
                        f"logstash.pipeline.{p.pipeline_id}.queue_buildup",
                        Severity.WARNING, "logstash",
                        f"Logstash Queue Buildup: {p.pipeline_id}",
                        f"Pipeline {p.pipeline_id} queue has {p.queue_events:,} events",
                    ))

        self._dispatch(alerts)
        return alerts

    def evaluate_fluentbit(self, snapshot) -> list[Alert]:
        alerts = []
        if snapshot is None or snapshot.error:
            alerts.append(self._make_alert(
                "fluentbit.unreachable", Severity.CRITICAL, "fluentbit",
                "Fluent Bit Unreachable",
                f"Cannot collect Fluent Bit metrics: {getattr(snapshot, 'error', 'unknown')}",
            ))
        else:
            if snapshot.healthy_pods < snapshot.total_pods:
                unhealthy = snapshot.total_pods - snapshot.healthy_pods
                alerts.append(self._make_alert(
                    "fluentbit.pods.unhealthy", Severity.WARNING, "fluentbit",
                    f"Fluent Bit Pods Unhealthy",
                    f"{unhealthy}/{snapshot.total_pods} Fluent Bit pods are unhealthy",
                ))
            if snapshot.total_pods == 0:
                alerts.append(self._make_alert(
                    "fluentbit.pods.none", Severity.CRITICAL, "fluentbit",
                    "No Fluent Bit Pods Running",
                    "No Fluent Bit DaemonSet pods found — log collection may be stopped",
                ))
            if snapshot.total_error_rate > 10:
                alerts.append(self._make_alert(
                    "fluentbit.high_error_rate", Severity.WARNING, "fluentbit",
                    "Fluent Bit High Error Rate",
                    f"Output error rate: {snapshot.total_error_rate:.1f}/sec",
                ))

        self._dispatch(alerts)
        return alerts

    def resolve_alert(self, alert_id: str) -> None:
        with self._lock:
            if alert_id in self._active:
                alert = self._active.pop(alert_id)
                alert.resolved = True
                alert.resolved_at = datetime.now(timezone.utc)
                self._history.append(alert)
                log.info("Alert resolved", extra={"alert_id": alert_id})

    def get_active_alerts(self) -> list[Alert]:
        with self._lock:
            return list(self._active.values())

    def get_history(self, hours: int = 24) -> list[Alert]:
        cutoff = datetime.now(timezone.utc) - timedelta(hours=hours)
        return [a for a in self._history if a.timestamp >= cutoff]

    # ── internal helpers ──────────────────────────────────────────────

    def _make_alert(
        self,
        alert_id: str,
        severity: Severity,
        component: str,
        title: str,
        message: str,
        metadata: Optional[dict] = None,
    ) -> Alert:
        return Alert(
            alert_id=alert_id,
            severity=severity,
            component=component,
            title=title,
            message=message,
            metadata=metadata or {},
        )

    def _dispatch(self, alerts: list[Alert]) -> None:
        now = datetime.now(timezone.utc)
        for alert in alerts:
            with self._lock:
                last = self._last_fired.get(alert.alert_id)
                if last and (now - last) < self._cooldown:
                    continue  # still in cooldown
                self._active[alert.alert_id] = alert
                self._last_fired[alert.alert_id] = now
                self._history.append(alert)
                METRIC_STORE.record(
                    f"alert.fired.{alert.severity}",
                    1,
                    labels={"component": alert.component, "id": alert.alert_id},
                )

            log.warning(
                "Alert fired",
                extra={
                    "alert_id": alert.alert_id,
                    "severity": alert.severity,
                    "component": alert.component,
                    "title": alert.title,
                },
            )
            for notifier in self._notifiers:
                try:
                    notifier.send(alert)
                except Exception as exc:
                    log.error("Notifier failed", extra={"error": str(exc),
                                                        "notifier": type(notifier).__name__})
