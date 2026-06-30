package com.opsbrain.incident.dto;

import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.model.Severity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponse {

    private Long id;

    @JsonProperty("incident_id")
    private String incidentId;

    private String title;

    private String description;

    private Severity severity;

    private IncidentStatus status;

    @JsonProperty("affected_service")
    private String affectedService;

    @JsonProperty("root_cause")
    private String rootCause;

    @JsonProperty("remediation_steps")
    private String remediationSteps;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;

    @JsonProperty("resolved_by")
    private String resolvedBy;

    @JsonProperty("dedup_count")
    private Integer dedupCount;
}
