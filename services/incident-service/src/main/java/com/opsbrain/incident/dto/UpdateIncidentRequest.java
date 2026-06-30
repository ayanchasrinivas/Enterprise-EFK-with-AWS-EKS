package com.opsbrain.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIncidentRequest {

    private String title;

    private String description;

    private String status;

    private String severity;

    private String affectedService;

    private String rootCause;

    private String remediationSteps;

    private String resolvedBy;
}
