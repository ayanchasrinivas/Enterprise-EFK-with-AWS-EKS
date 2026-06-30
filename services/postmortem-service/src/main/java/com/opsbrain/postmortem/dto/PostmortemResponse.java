package com.opsbrain.postmortem.dto;

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
public class PostmortemResponse {

    private Long id;

    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("postmortem_id")
    private String postmortemId;

    private String title;

    private String summary;

    private String status;

    @JsonProperty("duration_minutes")
    private Long durationMinutes;

    @JsonProperty("incident_start_time")
    private LocalDateTime incidentStartTime;

    @JsonProperty("incident_end_time")
    private LocalDateTime incidentEndTime;

    @JsonProperty("generated_by")
    private String generatedBy;

    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
