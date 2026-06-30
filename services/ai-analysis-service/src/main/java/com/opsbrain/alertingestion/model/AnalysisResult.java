package com.opsbrain.aianalysis.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * The LLM's verdict on one alert. incident-service uses this to create the
 * incident; notification-service pages the on-call with summary + remediation.
 */
@Value
@Builder
public class AnalysisResult {

    String analysisId;
    String bundleId;

    // Identity carried through from the alert (for incident grouping)
    String alertId;
    String fingerprint;
    String alertName;
    String service;
    String namespace;

    // The analysis
    String rootCause;
    double confidence;                  // 0.0–1.0
    List<String> contributingFactors;
    List<RemediationStep> remediationSteps;
    boolean deploymentRelated;          // did a recent deploy likely cause this?
    String summary;                     // one-paragraph, on-call-facing

    Severity recommendedSeverity;       // LLM's assessment
    Severity originalSeverity;          // what the alert claimed

    // Pass-through context for the notification
    List<DashboardLink> dashboards;
    String generatorUrl;

    // Provenance
    String aiModel;
    Instant analyzedAt;
    String llmError;                    // null on success; set on degraded fallback
}