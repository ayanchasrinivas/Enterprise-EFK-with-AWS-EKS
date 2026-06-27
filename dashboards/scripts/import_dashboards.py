"""
Kibana Dashboard Provisioner.

Imports pre-built dashboards, index patterns, and saved searches
into Kibana using the Saved Objects API.

Usage:
    python import_dashboards.py \
        --kibana-url https://kibana.yourdomain.com \
        --username elastic \
        --password <password> \
        --dashboards-dir ../kibana

    python import_dashboards.py --list          # list available dashboards
    python import_dashboards.py --setup-ilm     # configure ILM policies only
    python import_dashboards.py --setup-all     # ILM + data streams + dashboards
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path
from typing import Optional

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


class KibanaProvisioner:

    def __init__(
        self,
        kibana_url: str,
        username: str,
        password: str,
        ca_cert: Optional[str] = None,
        verify_ssl: bool = True,
    ):
        self._base = kibana_url.rstrip("/")
        self._session = requests.Session()
        self._session.auth = (username, password)
        self._session.headers.update({
            "kbn-xsrf": "true",
            "Content-Type": "application/json",
        })
        if ca_cert:
            self._session.verify = ca_cert
        elif not verify_ssl:
            self._session.verify = False

        retry = Retry(total=5, backoff_factor=2, status_forcelist=[502, 503, 504])
        self._session.mount("https://", HTTPAdapter(max_retries=retry))
        self._session.mount("http://", HTTPAdapter(max_retries=retry))

    def wait_for_kibana(self, timeout: int = 300) -> bool:
        """Poll Kibana status until available or timeout."""
        print("Waiting for Kibana to be ready...")
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            try:
                resp = self._session.get(f"{self._base}/api/status", timeout=10)
                data = resp.json()
                level = data.get("status", {}).get("overall", {}).get("level", "")
                if level == "available":
                    print("✓ Kibana is ready")
                    return True
                print(f"  Kibana status: {level}, waiting…")
            except Exception as e:
                print(f"  Connection failed: {e}, retrying…")
            time.sleep(10)
        print("✗ Timed out waiting for Kibana")
        return False

    def import_saved_objects(self, ndjson_path: Path, overwrite: bool = True) -> dict:
        """Import saved objects from an NDJSON file."""
        with open(ndjson_path, "rb") as f:
            content = f.read()

        resp = self._session.post(
            f"{self._base}/api/saved_objects/_import",
            params={"overwrite": "true" if overwrite else "false"},
            files={"file": (ndjson_path.name, content, "application/ndjson")},
            headers={"kbn-xsrf": "true"},  # note: no Content-Type for multipart
        )
        resp.raise_for_status()
        return resp.json()

    def create_index_pattern(self, pattern: str, time_field: str = "@timestamp",
                              title: str = "") -> dict:
        """Create or update a Kibana data view (index pattern)."""
        payload = {
            "data_view": {
                "title": pattern,
                "timeFieldName": time_field,
                "name": title or pattern,
            }
        }
        resp = self._session.post(
            f"{self._base}/api/data_views/data_view",
            json=payload,
        )
        if resp.status_code == 409:
            print(f"  Data view already exists: {pattern}")
            return {}
        resp.raise_for_status()
        return resp.json()

    def setup_ilm_policy(self, es_url: str, username: str, password: str) -> None:
        """Create ILM policies on Elasticsearch directly."""
        es_session = requests.Session()
        es_session.auth = (username, password)
        es_session.verify = self._session.verify

        policies = {
            "logs-standard-ilm": {
                "policy": {
                    "phases": {
                        "hot": {
                            "min_age": "0ms",
                            "actions": {
                                "rollover": {
                                    "max_primary_shard_size": "50gb",
                                    "max_age": "1d",
                                },
                                "set_priority": {"priority": 100},
                            },
                        },
                        "warm": {
                            "min_age": "7d",
                            "actions": {
                                "shrink": {"number_of_shards": 1},
                                "forcemerge": {"max_num_segments": 1},
                                "set_priority": {"priority": 50},
                            },
                        },
                        "cold": {
                            "min_age": "30d",
                            "actions": {
                                "freeze": {},
                                "set_priority": {"priority": 0},
                            },
                        },
                        "delete": {
                            "min_age": "90d",
                            "actions": {"delete": {}},
                        },
                    }
                }
            },
            "metrics-standard-ilm": {
                "policy": {
                    "phases": {
                        "hot": {
                            "min_age": "0ms",
                            "actions": {
                                "rollover": {
                                    "max_primary_shard_size": "30gb",
                                    "max_age": "1d",
                                }
                            },
                        },
                        "warm": {"min_age": "3d", "actions": {"forcemerge": {"max_num_segments": 1}}},
                        "delete": {"min_age": "30d", "actions": {"delete": {}}},
                    }
                }
            },
        }

        for name, body in policies.items():
            resp = es_session.put(
                f"{es_url}/_ilm/policy/{name}",
                json=body,
                headers={"Content-Type": "application/json"},
            )
            if resp.ok:
                print(f"  ✓ ILM policy created: {name}")
            else:
                print(f"  ✗ ILM policy failed: {name} — {resp.text}")

    def setup_data_streams(self, es_url: str, username: str, password: str) -> None:
        """Create index templates for data streams."""
        es_session = requests.Session()
        es_session.auth = (username, password)
        es_session.verify = self._session.verify

        templates = [
            {
                "name": "logs-kubernetes",
                "body": {
                    "index_patterns": ["logs-kubernetes.*"],
                    "data_stream": {},
                    "priority": 200,
                    "template": {
                        "settings": {
                            "number_of_shards": 3,
                            "number_of_replicas": 1,
                            "index.lifecycle.name": "logs-standard-ilm",
                            "index.codec": "best_compression",
                            "index.refresh_interval": "5s",
                        },
                        "mappings": {
                            "dynamic_templates": [
                                {
                                    "strings_as_keyword": {
                                        "match_mapping_type": "string",
                                        "mapping": {
                                            "type": "keyword",
                                            "ignore_above": 1024,
                                            "fields": {
                                                "text": {"type": "text"}
                                            },
                                        },
                                    }
                                }
                            ],
                            "properties": {
                                "@timestamp": {"type": "date"},
                                "log": {"type": "keyword", "ignore_above": 8192,
                                        "fields": {"text": {"type": "text"}}},
                                "kubernetes": {
                                    "properties": {
                                        "namespace_name": {"type": "keyword"},
                                        "pod_name": {"type": "keyword"},
                                        "container_name": {"type": "keyword"},
                                        "node_name": {"type": "keyword"},
                                        "labels": {"type": "flattened"},
                                    }
                                },
                                "level": {"type": "keyword"},
                                "environment": {"type": "keyword"},
                                "cluster": {"type": "keyword"},
                            },
                        },
                    },
                },
            }
        ]

        for t in templates:
            resp = es_session.put(
                f"{es_url}/_index_template/{t['name']}",
                json=t["body"],
                headers={"Content-Type": "application/json"},
            )
            if resp.ok:
                print(f"  ✓ Index template created: {t['name']}")
            else:
                print(f"  ✗ Template failed: {t['name']} — {resp.text}")


def main():
    parser = argparse.ArgumentParser(description="Kibana Dashboard Provisioner")
    parser.add_argument("--kibana-url", default="https://kibana.yourdomain.com")
    parser.add_argument("--es-url", default="https://elasticsearch.yourdomain.com")
    parser.add_argument("--username", default="elastic")
    parser.add_argument("--password", required=True)
    parser.add_argument("--dashboards-dir", default="./dashboards/kibana")
    parser.add_argument("--ca-cert")
    parser.add_argument("--no-verify-ssl", action="store_true")
    parser.add_argument("--wait", action="store_true", help="Wait for Kibana readiness")
    parser.add_argument("--setup-ilm", action="store_true")
    parser.add_argument("--setup-data-streams", action="store_true")
    parser.add_argument("--setup-all", action="store_true")
    parser.add_argument("--list", action="store_true")
    args = parser.parse_args()

    dashboard_dir = Path(args.dashboards_dir)

    if args.list:
        files = list(dashboard_dir.glob("*.ndjson")) + list(dashboard_dir.glob("*.json"))
        print(f"Available dashboard files in {dashboard_dir}:")
        for f in files:
            print(f"  {f.name}")
        return

    provisioner = KibanaProvisioner(
        kibana_url=args.kibana_url,
        username=args.username,
        password=args.password,
        ca_cert=args.ca_cert,
        verify_ssl=not args.no_verify_ssl,
    )

    if args.wait:
        if not provisioner.wait_for_kibana():
            sys.exit(1)

    if args.setup_ilm or args.setup_all:
        print("Setting up ILM policies…")
        provisioner.setup_ilm_policy(args.es_url, args.username, args.password)

    if args.setup_data_streams or args.setup_all:
        print("Setting up data streams / index templates…")
        provisioner.setup_data_streams(args.es_url, args.username, args.password)

    # Create default data views
    print("Creating data views (index patterns)…")
    data_views = [
        ("logs-*", "@timestamp", "All Logs"),
        ("logs-kubernetes.*", "@timestamp", "Kubernetes Logs"),
        ("metrics-*", "@timestamp", "Metrics"),
        (".monitoring-*", "@timestamp", "Stack Monitoring"),
    ]
    for pattern, time_field, title in data_views:
        result = provisioner.create_index_pattern(pattern, time_field, title)
        if result:
            print(f"  ✓ Data view created: {title} ({pattern})")

    # Import NDJSON dashboard files
    ndjson_files = sorted(dashboard_dir.glob("*.ndjson"))
    if not ndjson_files:
        print(f"No .ndjson files found in {dashboard_dir}")
        print("Tip: export dashboards from Kibana → Stack Management → Saved Objects → Export")
    else:
        for f in ndjson_files:
            print(f"Importing {f.name}…")
            result = provisioner.import_saved_objects(f)
            success = result.get("successCount", 0)
            errors = result.get("errors", [])
            print(f"  ✓ {success} objects imported", end="")
            if errors:
                print(f", {len(errors)} errors:")
                for e in errors[:5]:
                    print(f"    - {e.get('id')}: {e.get('error', {}).get('message')}")
            else:
                print()

    print("\nProvisioning complete.")


if __name__ == "__main__":
    main()
