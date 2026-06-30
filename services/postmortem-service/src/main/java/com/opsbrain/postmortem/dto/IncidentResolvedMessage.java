package com.opsbrain.postmortem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResolvedMessage {

    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("affected_service")
    private String affectedService;

    @JsonProperty("root_cause")
    private String rootCause;

    @JsonProperty("remediation_steps")
    private String remediationSteps;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("resolved_at")
    private Long resolvedAt;

    @JsonProperty("resolved_by")
    private String resolvedBy;

    @JsonProperty("description")
    private String description;

    @JsonProperty("timestamp")
    private Long timestamp;
}
