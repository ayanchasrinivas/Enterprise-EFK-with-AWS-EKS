package com.opsbrain.aianalysis.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * The exact JSON shape we force the model to return via structured outputs.
 * The Anthropic SDK derives a JSON schema from this record and constrains the
 * response to match it — so there's no brittle text parsing. Field descriptions
 * become schema descriptions the model reads.
 */
public record AnalysisOutput(

        @JsonPropertyDescription("The single most likely root cause, stated in one concise sentence")
        String rootCause,

        @JsonPropertyDescription("Confidence in the root cause as a number from 0.0 to 1.0")
        double confidence,

        @JsonPropertyDescription("Other factors that contributed, if any; empty list if none")
        List<String> contributingFactors,

        @JsonPropertyDescription("Ordered, concrete steps to remediate the incident")
        List<RemediationStepOutput> remediationSteps,

        @JsonPropertyDescription("Recommended severity, one of: CRITICAL, HIGH, WARNING, INFO")
        String recommendedSeverity,

        @JsonPropertyDescription("One short paragraph for the on-call engineer being paged")
        String summary,

        @JsonPropertyDescription("True if a recent deployment is the most likely trigger")
        boolean deploymentRelated
) {
    public record RemediationStepOutput(
            @JsonPropertyDescription("The action to take")
            String action,
            @JsonPropertyDescription("The exact command to run, or null if not applicable")
            String command,
            @JsonPropertyDescription("Risk of performing this step: low, medium, or high")
            String risk,
            @JsonPropertyDescription("Why this step helps resolve the incident")
            String rationale
    ) {}
}