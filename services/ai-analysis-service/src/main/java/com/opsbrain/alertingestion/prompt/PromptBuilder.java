package com.opsbrain.aianalysis.prompt;

import com.opsbrain.aianalysis.model.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Turns a ContextBundle into the prompt text the model reasons over.
 * The deployment-correlation flag is surfaced prominently because "a deploy
 * landed right before the alert" is the most common real root cause.
 */
@Component
public class PromptBuilder {

    public static final String SYSTEM_PROMPT = """
            You are a senior Site Reliability Engineer performing root-cause analysis
            for a production Kubernetes platform. You are given an alert plus the
            context gathered automatically at the moment it fired: recent logs,
            metrics, Kubernetes events, and deployment history.

            Rules:
            - Ground every conclusion in the evidence provided. Do not invent logs,
              metrics, or events that are not shown.
            - If a deployment landed shortly before the alert, treat it as the prime
              suspect and say so.
            - Prefer the single most likely root cause over a list of maybes.
            - Remediation steps must be concrete and ordered: safest, most likely
              fix first. Include exact kubectl/argocd commands where possible.
            - If the evidence is thin, say the confidence is low rather than guessing.
            - Be concise. The on-call engineer is being paged at 3am.
            """;

    public String buildUserPrompt(ContextBundle b) {
        NormalizedAlert a = b.getAlert();
        StringBuilder sb = new StringBuilder();

        sb.append("# ALERT\n");
        sb.append("Name: ").append(a.getAlertName()).append('\n');
        sb.append("Severity (as reported): ").append(a.getSeverity()).append('\n');
        sb.append("Summary: ").append(nz(a.getSummary())).append('\n');
        if (a.getDescription() != null) sb.append("Description: ").append(a.getDescription()).append('\n');
        sb.append("Service: ").append(nz(a.getService()))
          .append("   Namespace: ").append(nz(a.getNamespace())).append('\n');
        sb.append("Started: ").append(a.getStartsAt()).append('\n');

        // ── Deployment correlation — the headline signal ──
        DeploymentContext d = b.getDeployment();
        sb.append("\n# DEPLOYMENT HISTORY\n");
        if (d != null && d.getApplicationName() != null) {
            sb.append("ArgoCD app: ").append(d.getApplicationName()).append('\n');
            sb.append("Current revision: ").append(nz(d.getCurrentRevision())).append('\n');
            sb.append("Last deployed at: ").append(d.getLastDeployedAt()).append('\n');
            sb.append(">>> RECENT DEPLOY NEAR ALERT TIME: ")
              .append(d.isRecentlyDeployed() ? "YES — investigate this first" : "no")
              .append(" <<<\n");
            if (d.getHistory() != null) {
                d.getHistory().forEach(r ->
                        sb.append("  - ").append(r.getRevision())
                          .append(" @ ").append(r.getDeployedAt()).append('\n'));
            }
        } else {
            sb.append("(no deployment data available)\n");
        }

        // ── Metrics ──
        sb.append("\n# METRICS (current values)\n");
        MetricContext m = b.getMetrics();
        if (m != null && m.getValues() != null && !m.getValues().isEmpty()) {
            for (Map.Entry<String, Double> e : m.getValues().entrySet()) {
                sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
            }
        } else {
            sb.append("(no metrics available)\n");
        }

        // ── Kubernetes events ──
        sb.append("\n# KUBERNETES EVENTS\n");
        if (b.getEvents() != null && !b.getEvents().isEmpty()) {
            b.getEvents().forEach(e ->
                    sb.append("  [").append(e.getType()).append("] ")
                      .append(e.getReason()).append(" x").append(e.getCount())
                      .append(" — ").append(e.getMessage())
                      .append(" (").append(e.getInvolvedObject()).append(")\n"));
        } else {
            sb.append("(no events)\n");
        }

        // ── Logs ──
        sb.append("\n# RECENT LOGS\n");
        LogContext logs = b.getLogs();
        if (logs != null) {
            sb.append("Total matches in window: ").append(logs.getTotalMatches()).append('\n');
            if (logs.getLevelCounts() != null && !logs.getLevelCounts().isEmpty()) {
                sb.append("Level counts: ").append(logs.getLevelCounts()).append('\n');
            }
            if (logs.getRecentErrors() != null) {
                logs.getRecentErrors().forEach(le ->
                        sb.append("  ").append(le.getTimestamp())
                          .append(" [").append(le.getLevel()).append("] ")
                          .append(le.getPod() != null ? le.getPod() + ": " : "")
                          .append(trim(le.getMessage(), 300)).append('\n'));
            }
        } else {
            sb.append("(no logs available)\n");
        }

        // Honesty about gaps so the model can calibrate confidence
        if (b.getCollectionErrors() != null && !b.getCollectionErrors().isEmpty()) {
            sb.append("\n# DATA GAPS (these sources failed to collect)\n");
            b.getCollectionErrors().forEach(err -> sb.append("  - ").append(err).append('\n'));
        }

        sb.append("\nProduce your root-cause analysis now.");
        return sb.toString();
    }

    private static String nz(String s) { return s == null ? "(unknown)" : s; }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}