"""
Central configuration for the ELK/EFK stack monitoring service.
All values are loaded from environment variables with safe defaults.
"""
import os
from dataclasses import dataclass, field
from typing import Optional
from dotenv import load_dotenv

load_dotenv()


@dataclass
class ElasticsearchConfig:
    hosts: list[str] = field(default_factory=lambda: [
        os.getenv("ES_HOST", "https://elasticsearch.yourdomain.com")
    ])
    username: str = os.getenv("ES_USERNAME", "elastic")
    password: str = os.getenv("ES_PASSWORD", "")
    ca_certs: Optional[str] = os.getenv("ES_CA_CERT_PATH")
    verify_certs: bool = os.getenv("ES_VERIFY_CERTS", "true").lower() == "true"
    timeout: int = int(os.getenv("ES_TIMEOUT", "30"))
    max_retries: int = int(os.getenv("ES_MAX_RETRIES", "3"))
    retry_on_timeout: bool = True


@dataclass
class KibanaConfig:
    host: str = os.getenv("KIBANA_HOST", "https://kibana.yourdomain.com")
    username: str = os.getenv("KIBANA_USERNAME", "elastic")
    password: str = os.getenv("KIBANA_PASSWORD", "")
    ca_certs: Optional[str] = os.getenv("KIBANA_CA_CERT_PATH")
    verify_ssl: bool = os.getenv("KIBANA_VERIFY_SSL", "true").lower() == "true"
    timeout: int = int(os.getenv("KIBANA_TIMEOUT", "30"))


@dataclass
class LogstashConfig:
    host: str = os.getenv("LOGSTASH_HOST", "https://logstash.yourdomain.com")
    monitoring_port: int = int(os.getenv("LOGSTASH_MONITORING_PORT", "9600"))
    verify_ssl: bool = os.getenv("LOGSTASH_VERIFY_SSL", "true").lower() == "true"
    timeout: int = int(os.getenv("LOGSTASH_TIMEOUT", "15"))


@dataclass
class FluentBitConfig:
    # Fluent Bit exposes metrics on each node via DaemonSet
    namespace: str = os.getenv("FLUENT_BIT_NAMESPACE", "logging")
    label_selector: str = os.getenv("FLUENT_BIT_LABEL", "app.kubernetes.io/name=fluent-bit")
    metrics_port: int = int(os.getenv("FLUENT_BIT_METRICS_PORT", "2020"))
    # K8s API server for pod discovery
    kube_api: str = os.getenv("KUBE_API", "https://kubernetes.default.svc")
    kube_token_path: str = os.getenv(
        "KUBE_TOKEN_PATH",
        "/var/run/secrets/kubernetes.io/serviceaccount/token"
    )


@dataclass
class AlertConfig:
    slack_webhook_url: str = os.getenv("SLACK_WEBHOOK_URL", "")
    slack_channel: str = os.getenv("SLACK_CHANNEL", "#elk-alerts")
    pagerduty_routing_key: str = os.getenv("PAGERDUTY_ROUTING_KEY", "")
    email_smtp_host: str = os.getenv("SMTP_HOST", "smtp.gmail.com")
    email_smtp_port: int = int(os.getenv("SMTP_PORT", "587"))
    email_from: str = os.getenv("ALERT_EMAIL_FROM", "")
    email_to: list[str] = field(default_factory=lambda: os.getenv(
        "ALERT_EMAIL_TO", ""
    ).split(","))
    email_password: str = os.getenv("SMTP_PASSWORD", "")
    sns_topic_arn: str = os.getenv("SNS_TOPIC_ARN", "")

    # Thresholds
    cluster_health_red_critical: bool = True
    cluster_health_yellow_warning: bool = True
    disk_watermark_warning_pct: float = float(os.getenv("DISK_WARN_PCT", "80"))
    disk_watermark_critical_pct: float = float(os.getenv("DISK_CRIT_PCT", "90"))
    jvm_heap_warning_pct: float = float(os.getenv("JVM_WARN_PCT", "75"))
    jvm_heap_critical_pct: float = float(os.getenv("JVM_CRIT_PCT", "90"))
    unassigned_shards_warning: int = int(os.getenv("UNASSIGNED_SHARDS_WARN", "5"))
    unassigned_shards_critical: int = int(os.getenv("UNASSIGNED_SHARDS_CRIT", "10"))
    indexing_latency_warn_ms: float = float(os.getenv("INDEX_LATENCY_WARN_MS", "50"))
    indexing_latency_crit_ms: float = float(os.getenv("INDEX_LATENCY_CRIT_MS", "200"))
    search_latency_warn_ms: float = float(os.getenv("SEARCH_LATENCY_WARN_MS", "100"))
    search_latency_crit_ms: float = float(os.getenv("SEARCH_LATENCY_CRIT_MS", "500"))

    # Cooldown: don't re-alert within N minutes for the same issue
    alert_cooldown_minutes: int = int(os.getenv("ALERT_COOLDOWN_MINUTES", "15"))


@dataclass
class MonitorConfig:
    collection_interval_seconds: int = int(os.getenv("COLLECTION_INTERVAL", "60"))
    dashboard_port: int = int(os.getenv("DASHBOARD_PORT", "8080"))
    dashboard_host: str = os.getenv("DASHBOARD_HOST", "0.0.0.0")
    metrics_retention_hours: int = int(os.getenv("METRICS_RETENTION_HOURS", "24"))
    log_level: str = os.getenv("LOG_LEVEL", "INFO")
    environment: str = os.getenv("ENVIRONMENT", "production")


# Singleton config instances
ES_CONFIG = ElasticsearchConfig()
KIBANA_CONFIG = KibanaConfig()
LOGSTASH_CONFIG = LogstashConfig()
FLUENT_BIT_CONFIG = FluentBitConfig()
ALERT_CONFIG = AlertConfig()
MONITOR_CONFIG = MonitorConfig()
