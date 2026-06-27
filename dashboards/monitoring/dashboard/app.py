"""
Flask web dashboard for real-time ELK/EFK stack monitoring.
Provides REST API + auto-refreshing HTML dashboard.

Endpoints:
  GET /              → HTML dashboard
  GET /api/status    → JSON current state
  GET /api/metrics   → JSON all time-series metrics
  GET /api/alerts    → JSON active alerts
  GET /api/health    → liveness probe
"""
from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any

from flask import Flask, jsonify, render_template_string

from utils.metrics import METRIC_STORE

# Shared state is imported from monitor (populated by collection loop)
# Import lazily to avoid circular import
def _get_state() -> dict:
    try:
        import monitor
        import threading
        with monitor._state_lock:
            return dict(monitor._state)
    except Exception:
        return {}


DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ELK/EFK Stack Monitor</title>
  <script src="https://cdn.plot.ly/plotly-2.27.0.min.js"></script>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', sans-serif; background: #0f172a; color: #e2e8f0; }
    header { background: #1e293b; padding: 16px 24px; display: flex; align-items: center; gap: 16px; border-bottom: 1px solid #334155; }
    header h1 { font-size: 1.25rem; color: #38bdf8; }
    .badge { padding: 4px 12px; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }
    .badge-green { background: #166534; color: #86efac; }
    .badge-yellow { background: #713f12; color: #fde68a; }
    .badge-red { background: #7f1d1d; color: #fca5a5; }
    .badge-gray { background: #334155; color: #94a3b8; }
    #last-updated { font-size: 0.75rem; color: #64748b; margin-left: auto; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; padding: 24px; }
    .card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 20px; }
    .card-title { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.1em; color: #64748b; margin-bottom: 8px; }
    .card-value { font-size: 2rem; font-weight: 700; color: #f1f5f9; }
    .card-sub { font-size: 0.8rem; color: #64748b; margin-top: 4px; }
    .chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; padding: 0 24px 24px; }
    .chart-card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 16px; }
    .chart-card h3 { color: #94a3b8; font-size: 0.875rem; margin-bottom: 12px; }
    .alerts-section { padding: 0 24px 24px; }
    .alert-item { background: #1e293b; border-left: 4px solid #ef4444; border-radius: 0 8px 8px 0; padding: 12px 16px; margin-bottom: 8px; }
    .alert-item.warning { border-left-color: #f59e0b; }
    .alert-item.info { border-left-color: #3b82f6; }
    .alert-title { font-weight: 600; font-size: 0.9rem; }
    .alert-meta { font-size: 0.75rem; color: #64748b; margin-top: 4px; }
    .no-alerts { color: #22c55e; font-size: 0.9rem; }
    .node-table { width: 100%; border-collapse: collapse; font-size: 0.8rem; }
    .node-table th { color: #64748b; text-align: left; padding: 6px 8px; border-bottom: 1px solid #334155; }
    .node-table td { padding: 6px 8px; border-bottom: 1px solid #1e293b; }
    .progress { background: #334155; border-radius: 4px; height: 6px; margin-top: 2px; }
    .progress-bar { height: 6px; border-radius: 4px; transition: width 0.3s; }
  </style>
</head>
<body>
  <header>
    <h1>⚡ ELK/EFK Stack Monitor</h1>
    <span id="cluster-badge" class="badge badge-gray">Loading…</span>
    <span id="last-updated"></span>
  </header>

  <div class="grid" id="summary-cards">
    <div class="card">
      <div class="card-title">Cluster Status</div>
      <div class="card-value" id="cluster-status">—</div>
      <div class="card-sub" id="cluster-shards">—</div>
    </div>
    <div class="card">
      <div class="card-title">Active Nodes</div>
      <div class="card-value" id="node-count">—</div>
      <div class="card-sub">Elasticsearch nodes</div>
    </div>
    <div class="card">
      <div class="card-title">Indexing Rate</div>
      <div class="card-value" id="index-rate">—</div>
      <div class="card-sub">docs/sec (cluster total)</div>
    </div>
    <div class="card">
      <div class="card-title">Fluent Bit</div>
      <div class="card-value" id="fb-pods">—</div>
      <div class="card-sub" id="fb-rate">—</div>
    </div>
    <div class="card">
      <div class="card-title">Kibana</div>
      <div class="card-value" id="kibana-status">—</div>
      <div class="card-sub" id="kibana-rt">—</div>
    </div>
    <div class="card">
      <div class="card-title">Active Alerts</div>
      <div class="card-value" id="alert-count" style="color: #ef4444;">—</div>
      <div class="card-sub">firing right now</div>
    </div>
  </div>

  <div class="chart-row">
    <div class="chart-card">
      <h3>JVM Heap % by Node</h3>
      <div id="heap-chart" style="height:250px;"></div>
    </div>
    <div class="chart-card">
      <h3>Indexing Rate (docs/sec)</h3>
      <div id="index-chart" style="height:250px;"></div>
    </div>
  </div>

  <div style="padding: 0 24px 24px;">
    <div class="chart-card">
      <h3>Node Details</h3>
      <table class="node-table" id="node-table">
        <thead>
          <tr><th>Node</th><th>Roles</th><th>JVM Heap</th><th>CPU</th><th>Disk Free</th><th>Idx/s</th><th>Srch/s</th></tr>
        </thead>
        <tbody id="node-tbody"></tbody>
      </table>
    </div>
  </div>

  <div class="alerts-section">
    <div class="chart-card">
      <h3>Active Alerts</h3>
      <div id="alerts-list"><p class="no-alerts">No active alerts ✓</p></div>
    </div>
  </div>

  <script>
    const plotlyConfig = { displayModeBar: false, responsive: true };
    const darkLayout = {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#94a3b8', size: 11 },
      margin: { l: 50, r: 10, t: 10, b: 40 },
      xaxis: { gridcolor: '#1e293b' },
      yaxis: { gridcolor: '#334155', rangemode: 'tozero' },
    };

    let heapInitialized = false;
    let indexInitialized = false;

    async function fetchAndUpdate() {
      try {
        const [statusRes, metricsRes] = await Promise.all([
          fetch('/api/status'), fetch('/api/metrics')
        ]);
        const status = await statusRes.json();
        const metrics = await metricsRes.json();
        updateSummary(status);
        updateCharts(metrics);
        updateNodeTable(status.es_nodes || []);
        updateAlerts(status.alerts || []);
        document.getElementById('last-updated').textContent =
          'Updated: ' + new Date().toLocaleTimeString();
      } catch(e) {
        console.error('Fetch failed:', e);
      }
    }

    function updateSummary(s) {
      const h = s.cluster_health || {};
      const colors = {green: 'badge-green', yellow: 'badge-yellow', red: 'badge-red'};
      const badge = document.getElementById('cluster-badge');
      badge.textContent = (h.status || 'unknown').toUpperCase();
      badge.className = 'badge ' + (colors[h.status] || 'badge-gray');

      document.getElementById('cluster-status').textContent = (h.status || '—').toUpperCase();
      document.getElementById('cluster-shards').textContent =
        h.active_shards ? `${h.active_shards} active, ${h.unassigned_shards} unassigned` : '—';
      document.getElementById('node-count').textContent = h.number_of_nodes || '—';

      const totalIdx = (s.es_nodes || []).reduce((a, n) => a + (n.indexing_rate || 0), 0);
      document.getElementById('index-rate').textContent = totalIdx.toFixed(0);

      const fb = s.fluentbit || {};
      document.getElementById('fb-pods').textContent =
        fb.healthy_pods != null ? `${fb.healthy_pods}/${fb.total_pods}` : '—';
      document.getElementById('fb-rate').textContent =
        fb.total_input_rate != null ? `${fb.total_input_rate.toFixed(0)} records/sec` : '—';

      const kb = s.kibana || {};
      document.getElementById('kibana-status').textContent = kb.overall_status || '—';
      document.getElementById('kibana-rt').textContent =
        kb.response_time_ms != null ? `${kb.response_time_ms.toFixed(0)}ms response` : '—';

      const alertCount = (s.alerts || []).length;
      document.getElementById('alert-count').textContent = alertCount;
      document.getElementById('alert-count').style.color =
        alertCount > 0 ? '#ef4444' : '#22c55e';
    }

    function updateCharts(metrics) {
      // JVM Heap chart
      const heapKeys = Object.keys(metrics).filter(k => k.includes('.jvm_heap_pct'));
      const heapData = heapKeys.map(k => {
        const pts = metrics[k] || [];
        const node = k.split('.')[2] || k;
        return {
          x: pts.map(p => p.timestamp),
          y: pts.map(p => p.value),
          name: node, type: 'scatter', mode: 'lines',
        };
      });
      if (!heapInitialized && heapData.length) {
        Plotly.newPlot('heap-chart', heapData,
          { ...darkLayout, yaxis: { ...darkLayout.yaxis, range: [0, 100] } },
          plotlyConfig);
        heapInitialized = true;
      } else if (heapData.length) {
        Plotly.react('heap-chart', heapData,
          { ...darkLayout, yaxis: { ...darkLayout.yaxis, range: [0, 100] } });
      }

      // Indexing rate chart
      const idxKeys = Object.keys(metrics).filter(k => k.includes('.indexing_rate'));
      const idxData = idxKeys.map(k => {
        const pts = metrics[k] || [];
        const node = k.split('.')[2] || k;
        return {
          x: pts.map(p => p.timestamp),
          y: pts.map(p => p.value),
          name: node, type: 'scatter', mode: 'lines',
          fill: 'tozeroy', fillcolor: 'rgba(56, 189, 248, 0.1)',
        };
      });
      if (!indexInitialized && idxData.length) {
        Plotly.newPlot('index-chart', idxData, darkLayout, plotlyConfig);
        indexInitialized = true;
      } else if (idxData.length) {
        Plotly.react('index-chart', idxData, darkLayout);
      }
    }

    function updateNodeTable(nodes) {
      const tbody = document.getElementById('node-tbody');
      tbody.innerHTML = nodes.map(n => {
        const heapColor = n.jvm_heap_used_percent > 90 ? '#ef4444'
          : n.jvm_heap_used_percent > 75 ? '#f59e0b' : '#22c55e';
        return `<tr>
          <td>${n.node_name}</td>
          <td>${(n.roles || []).join(', ')}</td>
          <td>
            ${n.jvm_heap_used_percent?.toFixed(0)}%
            <div class="progress"><div class="progress-bar" style="width:${n.jvm_heap_used_percent}%;background:${heapColor};"></div></div>
          </td>
          <td>${n.os_cpu_percent?.toFixed(0)}%</td>
          <td>${n.disk_free_percent?.toFixed(0)}%</td>
          <td>${n.indexing_rate?.toFixed(0)}</td>
          <td>${n.search_rate?.toFixed(0)}</td>
        </tr>`;
      }).join('');
    }

    function updateAlerts(alerts) {
      const list = document.getElementById('alerts-list');
      if (!alerts.length) {
        list.innerHTML = '<p class="no-alerts">No active alerts ✓</p>';
        return;
      }
      list.innerHTML = alerts.map(a => `
        <div class="alert-item ${a.severity}">
          <div class="alert-title">${a.title}</div>
          <div class="alert-meta">${a.component} · ${a.severity.toUpperCase()} · ${new Date(a.timestamp).toLocaleTimeString()}</div>
          <div class="alert-meta" style="margin-top:4px;color:#94a3b8;">${a.message}</div>
        </div>
      `).join('');
    }

    fetchAndUpdate();
    setInterval(fetchAndUpdate, 30000);  // refresh every 30s
  </script>
</body>
</html>
"""


def create_app() -> Flask:
    app = Flask(__name__)

    @app.route("/")
    def index():
        return render_template_string(DASHBOARD_HTML)

    @app.route("/api/health")
    def health():
        return jsonify({"status": "ok", "timestamp": datetime.now(timezone.utc).isoformat()})

    @app.route("/api/status")
    def api_status():
        state = _get_state()
        result: dict[str, Any] = {
            "last_collection": state.get("last_collection"),
            "alerts": state.get("alerts", []),
        }

        es = state.get("es")
        if es and es.cluster_health:
            h = es.cluster_health
            result["cluster_health"] = {
                "status": h.status,
                "number_of_nodes": h.number_of_nodes,
                "active_shards": h.active_shards,
                "unassigned_shards": h.unassigned_shards,
                "active_shards_percent": h.active_shards_percent,
            }
            result["es_nodes"] = [
                {
                    "node_name": n.node_name,
                    "roles": n.roles,
                    "jvm_heap_used_percent": n.jvm_heap_used_percent,
                    "os_cpu_percent": n.os_cpu_percent,
                    "disk_free_percent": n.disk_free_percent,
                    "indexing_rate": n.indexing_rate,
                    "search_rate": n.search_rate,
                }
                for n in es.nodes
            ]

        kb = state.get("kibana")
        if kb:
            result["kibana"] = {
                "overall_status": kb.overall_status,
                "version": kb.version,
                "response_time_ms": kb.response_time_ms,
                "active_alerts": kb.active_alerts,
            }

        fb = state.get("fluentbit")
        if fb:
            result["fluentbit"] = {
                "healthy_pods": fb.healthy_pods,
                "total_pods": fb.total_pods,
                "total_input_rate": fb.total_input_rate,
                "total_output_rate": fb.total_output_rate,
                "total_error_rate": fb.total_error_rate,
            }

        ls = state.get("logstash")
        if ls:
            result["logstash"] = {
                "status": ls.status,
                "jvm_heap_used_percent": ls.jvm_heap_used_percent,
                "events_in_total": ls.events_in_total,
                "pipeline_count": len(ls.pipelines),
            }

        return jsonify(result)

    @app.route("/api/metrics")
    def api_metrics():
        return jsonify(METRIC_STORE.all_metrics())

    @app.route("/api/alerts")
    def api_alerts():
        state = _get_state()
        return jsonify(state.get("alerts", []))

    return app
