package com.opsbrain.aianalysis.model;

import lombok.Builder;
import lombok.Value;

/** One actionable remediation step produced by the LLM. */
@Value
@Builder
public class RemediationStep {
    String action;      // what to do
    String command;     // exact command, nullable
    String risk;        // low | medium | high
    String rationale;   // why it helps
}