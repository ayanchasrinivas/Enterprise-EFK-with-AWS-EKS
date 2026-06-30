package com.opsbrain.postmortem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePostmortemRequest {

    @NotBlank(message = "Incident ID is required")
    private String incidentId;

    @NotBlank(message = "Title is required")
    private String title;

    private String summary;

    private String severity;

    private String affectedService;

    private String rootCause;

    private String remediationSteps;

    private LocalDateTime incidentStartTime;

    private LocalDateTime incidentEndTime;

    private String description;
}
