"""
Elasticsearch metrics collector.

Collects cluster health, node stats, index stats, shard allocation,
ILM policy status, and JVM/disk usage from the Elasticsearch API.
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Optional

from elasticsearch import Elasticsearch, ConnectionError, TransportError

from config import ES_CONFIG
from utils.logger import get_logger
from utils.metrics import METRIC_STORE

log = get_logger(__name__)


@dataclass
class ClusterHealthSnapshot:
    timestamp: datetime
    cluster_name: str
    status: str                   # green / yellow / red
    number_of_nodes: int
    number_of_data_nodes: int
    active_primary_shards: int
    active_shards: int
    relocating_shards: int
    initializing_shards: int
    unassigned_shards: int
    delayed_unassigned_shards: int
    number_of_pending_tasks: int
    active_shards_percent: float
    timed_out: bool


@dataclass
class NodeStats:
    node_id: str
    node_name: str
    roles: list[str]
    os_cpu_percent: float
    jvm_heap_used_percent: float
    jvm_heap_used_bytes: int
    jvm_heap_max_bytes: int
    disk_used_bytes: int
    disk_total_bytes: int
    disk_free_percent: float
    indexing_rate: float          # docs/sec (5-min average)
    search_rate: float            # queries/sec (5-min average)
    indexing_latency_ms: float
    search_latency_ms: float
    gc_old_collection_count: int
    gc_old_collection_time_ms: int
    thread_pool_write_rejected: int
    thread_pool_search_rejected: int
    open_file_descriptors: int
    network_rx_bytes: int
    network_tx_bytes: int


@dataclass
class IndexStats:
    index_name: str
    status: str
    health: str
    docs_count: int
    docs_deleted: int
    store_size_bytes: int
    primary_shards: int
    replica_shards: int
    indexing_rate: float
    search_rate: float
    merge_total: int
    refresh_total: int


@dataclass
class ILMPolicyStatus:
    policy_name: str
    indices_managed: int
    error_count: int
    errors: list[dict]


@dataclass
class ElasticsearchSnapshot:
    timestamp: datetime
    cluster_health: Optional[ClusterHealthSnapshot]
    nodes: list[NodeStats]
    indices: list[IndexStats]
    ilm_policies: list[ILMPolicyStatus]
    pending_tasks: int
    collection_duration_ms: float
    error: Optional[str] = None


class ElasticsearchCollector:
    """Collects all relevant metrics from an Elasticsearch cluster."""

    def __init__(self):
        self._client: Optional[Elasticsearch] = None
        self._prev_node_stats: dict[str, dict] = {}

    def _get_client(self) -> Elasticsearch:
        if self._client is None:
            kwargs: dict[str, Any] = {
                "hosts": ES_CONFIG.hosts,
                "http_auth": (ES_CONFIG.username, ES_CONFIG.password),
                "verify_certs": ES_CONFIG.verify_certs,
                "request_timeout": ES_CONFIG.timeout,
                "max_retries": ES_CONFIG.max_retries,
                "retry_on_timeout": ES_CONFIG.retry_on_timeout,
            }
            if ES_CONFIG.ca_certs:
                kwargs["ca_certs"] = ES_CONFIG.ca_certs
            self._client = Elasticsearch(**kwargs)
        return self._client

    def collect(self) -> ElasticsearchSnapshot:
        start = time.monotonic()
        ts = datetime.now(timezone.utc)

        try:
            client = self._get_client()
            health = self._collect_health(client)
            nodes = self._collect_nodes(client)
            indices = self._collect_indices(client)
            ilm = self._collect_ilm(client)
            pending = self._collect_pending_tasks(client)

            elapsed_ms = (time.monotonic() - start) * 1000
            snapshot = ElasticsearchSnapshot(
                timestamp=ts,
                cluster_health=health,
                nodes=nodes,
                indices=indices,
                ilm_policies=ilm,
                pending_tasks=pending,
                collection_duration_ms=elapsed_ms,
            )
            self._record_metrics(snapshot)
            log.info("ES collection complete", extra={"duration_ms": elapsed_ms})
            return snapshot

        except (ConnectionError, TransportError) as exc:
            elapsed_ms = (time.monotonic() - start) * 1000
            log.error("ES collection failed", extra={"error": str(exc)})
            self._client = None  # force reconnect on next attempt
            METRIC_STORE.record("es.collection.error", 1)
            return ElasticsearchSnapshot(
                timestamp=ts,
                cluster_health=None,
                nodes=[],
                indices=[],
                ilm_policies=[],
                pending_tasks=0,
                collection_duration_ms=elapsed_ms,
                error=str(exc),
            )

    def _collect_health(self, client: Elasticsearch) -> ClusterHealthSnapshot:
        raw = client.cluster.health(timeout="10s")
        return ClusterHealthSnapshot(
            timestamp=datetime.now(timezone.utc),
            cluster_name=raw["cluster_name"],
            status=raw["status"],
            number_of_nodes=raw["number_of_nodes"],
            number_of_data_nodes=raw["number_of_data_nodes"],
            active_primary_shards=raw["active_primary_shards"],
            active_shards=raw["active_shards"],
            relocating_shards=raw["relocating_shards"],
            initializing_shards=raw["initializing_shards"],
            unassigned_shards=raw["unassigned_shards"],
            delayed_unassigned_shards=raw["delayed_unassigned_shards"],
            number_of_pending_tasks=raw["number_of_pending_tasks"],
            active_shards_percent=raw["active_shards_percent_as_number"],
            timed_out=raw["timed_out"],
        )

    def _collect_nodes(self, client: Elasticsearch) -> list[NodeStats]:
        raw = client.nodes.stats(
            metric=["os", "jvm", "fs", "indices", "thread_pool", "process", "transport"],
            timeout="15s",
        )
        nodes_out = []
        for node_id, node in raw["nodes"].items():
            prev = self._prev_node_stats.get(node_id, {})

            # Derive rates from deltas
            idx_total = node["indices"]["indexing"]["index_total"]
            idx_time_ms = node["indices"]["indexing"]["index_time_in_millis"]
            prev_idx_total = prev.get("idx_total", idx_total)
            prev_idx_time = prev.get("idx_time_ms", idx_time_ms)
            prev_collection_time = prev.get("collection_time", time.monotonic())
            elapsed = time.monotonic() - prev_collection_time or 1

            idx_rate = max(0, (idx_total - prev_idx_total) / elapsed)
            idx_latency = (
                (idx_time_ms - prev_idx_time) / max(1, idx_total - prev_idx_total)
                if idx_total > prev_idx_total else 0.0
            )

            srch_total = node["indices"]["search"]["query_total"]
            srch_time_ms = node["indices"]["search"]["query_time_in_millis"]
            prev_srch_total = prev.get("srch_total", srch_total)
            prev_srch_time = prev.get("srch_time_ms", srch_time_ms)
            srch_rate = max(0, (srch_total - prev_srch_total) / elapsed)
            srch_latency = (
                (srch_time_ms - prev_srch_time) / max(1, srch_total - prev_srch_total)
                if srch_total > prev_srch_total else 0.0
            )

            fs_total = node["fs"]["total"]
            disk_free_pct = (
                (fs_total["free_in_bytes"] / fs_total["total_in_bytes"]) * 100
                if fs_total["total_in_bytes"] > 0 else 100.0
            )

            jvm = node["jvm"]
            heap_used_pct = jvm["mem"]["heap_used_percent"]

            stats = NodeStats(
                node_id=node_id,
                node_name=node["name"],
                roles=node.get("roles", []),
                os_cpu_percent=node["os"]["cpu"]["percent"],
                jvm_heap_used_percent=heap_used_pct,
                jvm_heap_used_bytes=jvm["mem"]["heap_used_in_bytes"],
                jvm_heap_max_bytes=jvm["mem"]["heap_max_in_bytes"],
                disk_used_bytes=fs_total["total_in_bytes"] - fs_total["free_in_bytes"],
                disk_total_bytes=fs_total["total_in_bytes"],
                disk_free_percent=disk_free_pct,
                indexing_rate=round(idx_rate, 2),
                search_rate=round(srch_rate, 2),
                indexing_latency_ms=round(idx_latency, 2),
                search_latency_ms=round(srch_latency, 2),
                gc_old_collection_count=jvm["gc"]["collectors"]["old"]["collection_count"],
                gc_old_collection_time_ms=jvm["gc"]["collectors"]["old"]["collection_time_in_millis"],
                thread_pool_write_rejected=node["thread_pool"]["write"]["rejected"],
                thread_pool_search_rejected=node["thread_pool"]["search"]["rejected"],
                open_file_descriptors=node["process"]["open_file_descriptors"],
                network_rx_bytes=node["transport"]["rx_size_in_bytes"],
                network_tx_bytes=node["transport"]["tx_size_in_bytes"],
            )
            nodes_out.append(stats)

            # Save for next delta computation
            self._prev_node_stats[node_id] = {
                "idx_total": idx_total,
                "idx_time_ms": idx_time_ms,
                "srch_total": srch_total,
                "srch_time_ms": srch_time_ms,
                "collection_time": time.monotonic(),
            }

        return nodes_out

    def _collect_indices(self, client: Elasticsearch) -> list[IndexStats]:
        cat_indices = client.cat.indices(
            h="index,status,health,docs.count,docs.deleted,store.size,pri,rep",
            bytes="b",
            format="json",
        )
        # Filter out system indices
        indices = [i for i in cat_indices if not i["index"].startswith(".")]

        # Get indexing/search rates from stats
        stats_raw = client.indices.stats(metric="indexing,search", index="_all")

        result = []
        for idx in indices:
            name = idx["index"]
            idx_stats = stats_raw["indices"].get(name, {}).get("primaries", {})
            result.append(IndexStats(
                index_name=name,
                status=idx.get("status", ""),
                health=idx.get("health", ""),
                docs_count=int(idx.get("docs.count") or 0),
                docs_deleted=int(idx.get("docs.deleted") or 0),
                store_size_bytes=int(idx.get("store.size") or 0),
                primary_shards=int(idx.get("pri") or 0),
                replica_shards=int(idx.get("rep") or 0),
                indexing_rate=idx_stats.get("indexing", {}).get("index_total", 0),
                search_rate=idx_stats.get("search", {}).get("query_total", 0),
                merge_total=idx_stats.get("merges", {}).get("total", 0),
                refresh_total=idx_stats.get("refresh", {}).get("total", 0),
            ))
        return result

    def _collect_ilm(self, client: Elasticsearch) -> list[ILMPolicyStatus]:
        try:
            status = client.ilm.explain_lifecycle(index="*")
            policy_stats: dict[str, ILMPolicyStatus] = {}

            for idx_name, info in status.get("indices", {}).items():
                policy = info.get("policy", "unknown")
                if policy not in policy_stats:
                    policy_stats[policy] = ILMPolicyStatus(
                        policy_name=policy,
                        indices_managed=0,
                        error_count=0,
                        errors=[],
                    )
                policy_stats[policy].indices_managed += 1
                if info.get("failed_step"):
                    policy_stats[policy].error_count += 1
                    policy_stats[policy].errors.append({
                        "index": idx_name,
                        "failed_step": info["failed_step"],
                        "step_info": info.get("step_info", {}),
                    })
            return list(policy_stats.values())
        except Exception as exc:
            log.warning("Could not collect ILM status", extra={"error": str(exc)})
            return []

    def _collect_pending_tasks(self, client: Elasticsearch) -> int:
        try:
            raw = client.cluster.pending_tasks()
            return len(raw.get("tasks", []))
        except Exception:
            return 0

    def _record_metrics(self, snap: ElasticsearchSnapshot) -> None:
        if snap.cluster_health:
            h = snap.cluster_health
            status_val = {"green": 0, "yellow": 1, "red": 2}.get(h.status, 3)
            METRIC_STORE.record("es.cluster.status", status_val)
            METRIC_STORE.record("es.cluster.nodes", h.number_of_nodes)
            METRIC_STORE.record("es.cluster.unassigned_shards", h.unassigned_shards)
            METRIC_STORE.record("es.cluster.active_shards_pct", h.active_shards_percent)
            METRIC_STORE.record("es.cluster.pending_tasks", h.number_of_pending_tasks)

        for node in snap.nodes:
            prefix = f"es.node.{node.node_name}"
            METRIC_STORE.record(f"{prefix}.jvm_heap_pct", node.jvm_heap_used_percent,
                                labels={"node": node.node_name})
            METRIC_STORE.record(f"{prefix}.cpu_pct", node.os_cpu_percent,
                                labels={"node": node.node_name})
            METRIC_STORE.record(f"{prefix}.disk_free_pct", node.disk_free_percent,
                                labels={"node": node.node_name})
            METRIC_STORE.record(f"{prefix}.indexing_rate", node.indexing_rate,
                                labels={"node": node.node_name})
            METRIC_STORE.record(f"{prefix}.search_rate", node.search_rate,
                                labels={"node": node.node_name})
            METRIC_STORE.record(f"{prefix}.indexing_latency_ms", node.indexing_latency_ms,
                                labels={"node": node.node_name})

        METRIC_STORE.record("es.collection.duration_ms", snap.collection_duration_ms)
