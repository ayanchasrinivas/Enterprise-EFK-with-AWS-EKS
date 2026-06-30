package com.opsbrain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisMessage {

    @JsonProperty("analysis_id")
    private String analysisId;

    @JsonProperty("alert_source")
    private String alertSource;

    @JsonProperty("alert_name")
    private String alertName;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("affected_service")
    private String affectedService;

    @JsonProperty("root_cause_analysis")
    private String rootCauseAnalysis;

    @JsonProperty("remediation_steps")
    private String remediationSteps;

    @JsonProperty("context_bundle")
    private Map<String, Object> contextBundle;

    @JsonProperty("timestamp")
    private Long timestamp;
}
