package com.opsbrain.alertingestion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * Fallback format for any source (CloudWatch via Lambda, custom scripts, K8s
 * event exporters). Caller pre-normalizes minimally; we fill the rest.
 */
@Data
public class GenericAlertRequest {

    @NotBlank(message = "alertName is required")
    private String alertName;

    private String severity;     // mapped via Severity.fromRaw
    private String status;       // mapped via AlertStatus.fromRaw
    private String summary;
    private String description;
    private String service;
    private String namespace;
    private String cluster;
    private String generatorUrl;
    private Map<String, String> labels;
    private Map<String, String> annotations;
}