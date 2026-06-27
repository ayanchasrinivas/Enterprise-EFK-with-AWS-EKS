"""
Fluent Bit metrics collector.
Queries the Prometheus metrics endpoint on each Fluent Bit DaemonSet pod.
Uses the Kubernetes API to discover pod IPs dynamically.
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Optional
import re

import requests

from config import FLUENT_BIT_CONFIG
from utils.logger import get_logger
from utils.metrics import METRIC_STORE

log = get_logger(__name__)


@dataclass
class FluentBitPodStats:
    pod_name: str
    node_name: str
    pod_ip: str
    input_records_total: int
    input_bytes_total: int
    output_records_total: int
    output_bytes_total: int
    output_errors_total: int
    output_retries_total: int
    filter_records_total: int
    uptime_seconds: float
    response_time_ms: float
    error: Optional[str] = None


@dataclass
class FluentBitSnapshot:
    timestamp: datetime
    pods: list[FluentBitPodStats]
    total_input_rate: float       # records/sec across all pods
    total_output_rate: float
    total_error_rate: float
    healthy_pods: int
    total_pods: int
    error: Optional[str] = None


def _parse_prometheus_text(text: str) -> dict[str, float]:
    """
    Minimal Prometheus text format parser.
    Returns {metric_name_with_labels: value}.
    """
    result: dict[str, float] = {}
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("#") or not line:
            continue
        # metric_name{labels} value [timestamp]
        m = re.match(r'^([a-zA-Z_:][a-zA-Z0-9_:]*)(?:\{[^}]*\})?\s+([\d.eE+\-]+)', line)
        if m:
            name = m.group(1)
            try:
                val = float(m.group(2))
                # For repeated metric names, keep the last (or sum them)
                result[name] = result.get(name, 0) + val
            except ValueError:
                pass
    return result


class FluentBitCollector:
    """
    Discovers Fluent Bit pods via the Kubernetes API and scrapes
    the Prometheus metrics endpoint on each one.
    """

    def __init__(self):
        self._k8s_session = self._build_k8s_session()
        self._prev_totals: dict[str, dict] = {}

    def _build_k8s_session(self) -> requests.Session:
        session = requests.Session()
        try:
            with open(FLUENT_BIT_CONFIG.kube_token_path) as f:
                token = f.read().strip()
            session.headers["Authorization"] = f"Bearer {token}"
            session.verify = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"
        except FileNotFoundError:
            # Running outside the cluster — rely on kubeconfig
            log.warning("Kubernetes service account token not found; "
                        "Fluent Bit pod discovery may fail outside the cluster.")
        return session

    def _discover_pods(self) -> list[dict]:
        """Return list of {name, node, ip} for all Fluent Bit pods."""
        url = (
            f"{FLUENT_BIT_CONFIG.kube_api}/api/v1/namespaces"
            f"/{FLUENT_BIT_CONFIG.namespace}/pods"
            f"?labelSelector={FLUENT_BIT_CONFIG.label_selector}"
        )
        try:
            resp = self._k8s_session.get(url, timeout=10)
            resp.raise_for_status()
            pods = []
            for item in resp.json().get("items", []):
                pod_ip = item.get("status", {}).get("podIP")
                if pod_ip and item.get("status", {}).get("phase") == "Running":
                    pods.append({
                        "name": item["metadata"]["name"],
                        "node": item.get("spec", {}).get("nodeName", "unknown"),
                        "ip": pod_ip,
                    })
            return pods
        except Exception as exc:
            log.error("Pod discovery failed", extra={"error": str(exc)})
            return []

    def _scrape_pod(self, pod: dict) -> FluentBitPodStats:
        url = f"http://{pod['ip']}:{FLUENT_BIT_CONFIG.metrics_port}/api/v1/metrics/prometheus"
        start = time.monotonic()
        try:
            resp = requests.get(url, timeout=5)
            resp.raise_for_status()
            resp_ms = (time.monotonic() - start) * 1000
            m = _parse_prometheus_text(resp.text)

            return FluentBitPodStats(
                pod_name=pod["name"],
                node_name=pod["node"],
                pod_ip=pod["ip"],
                input_records_total=int(m.get("fluentbit_input_records_total", 0)),
                input_bytes_total=int(m.get("fluentbit_input_bytes_total", 0)),
                output_records_total=int(m.get("fluentbit_output_records_total", 0)),
                output_bytes_total=int(m.get("fluentbit_output_bytes_total", 0)),
                output_errors_total=int(m.get("fluentbit_output_errors_total", 0)),
                output_retries_total=int(m.get("fluentbit_output_retries_total", 0)),
                filter_records_total=int(m.get("fluentbit_filter_records_total", 0)),
                uptime_seconds=m.get("fluentbit_uptime", 0),
                response_time_ms=round(resp_ms, 2),
            )
        except Exception as exc:
            return FluentBitPodStats(
                pod_name=pod["name"],
                node_name=pod["node"],
                pod_ip=pod["ip"],
                input_records_total=0,
                input_bytes_total=0,
                output_records_total=0,
                output_bytes_total=0,
                output_errors_total=0,
                output_retries_total=0,
                filter_records_total=0,
                uptime_seconds=0,
                response_time_ms=0,
                error=str(exc),
            )

    def collect(self) -> FluentBitSnapshot:
        ts = datetime.now(timezone.utc)
        try:
            pods_info = self._discover_pods()
            if not pods_info:
                log.warning("No Fluent Bit pods found")
                return FluentBitSnapshot(
                    timestamp=ts, pods=[], total_input_rate=0,
                    total_output_rate=0, total_error_rate=0,
                    healthy_pods=0, total_pods=0,
                    error="No pods discovered",
                )

            pod_stats = [self._scrape_pod(p) for p in pods_info]
            healthy = [p for p in pod_stats if p.error is None]

            total_in = sum(p.input_records_total for p in healthy)
            total_out = sum(p.output_records_total for p in healthy)
            total_err = sum(p.output_errors_total for p in healthy)

            # Approximate per-second rates using stored previous totals
            prev = self._prev_totals
            elapsed = 60  # assume ~60s collection interval
            in_rate = max(0, (total_in - prev.get("in", total_in)) / elapsed)
            out_rate = max(0, (total_out - prev.get("out", total_out)) / elapsed)
            err_rate = max(0, (total_err - prev.get("err", total_err)) / elapsed)
            self._prev_totals = {"in": total_in, "out": total_out, "err": total_err}

            snap = FluentBitSnapshot(
                timestamp=ts,
                pods=pod_stats,
                total_input_rate=round(in_rate, 2),
                total_output_rate=round(out_rate, 2),
                total_error_rate=round(err_rate, 2),
                healthy_pods=len(healthy),
                total_pods=len(pod_stats),
            )
            self._record_metrics(snap)
            log.info("Fluent Bit collection complete",
                     extra={"healthy_pods": len(healthy), "total_pods": len(pod_stats)})
            return snap

        except Exception as exc:
            log.error("Fluent Bit collection failed", extra={"error": str(exc)})
            return FluentBitSnapshot(
                timestamp=ts, pods=[], total_input_rate=0,
                total_output_rate=0, total_error_rate=0,
                healthy_pods=0, total_pods=0, error=str(exc),
            )

    def _record_metrics(self, snap: FluentBitSnapshot) -> None:
        METRIC_STORE.record("fluentbit.input_rate", snap.total_input_rate)
        METRIC_STORE.record("fluentbit.output_rate", snap.total_output_rate)
        METRIC_STORE.record("fluentbit.error_rate", snap.total_error_rate)
        METRIC_STORE.record("fluentbit.healthy_pods", snap.healthy_pods)
        METRIC_STORE.record("fluentbit.total_pods", snap.total_pods)
