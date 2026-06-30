package com.opsbrain.postmortem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsbrain.postmortem.dto.IncidentResolvedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class BedrockService {

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String modelId;

    @Value("${aws.bedrock.max-tokens:2000}")
    private Integer maxTokens;

    public String generatePostmortemContent(IncidentResolvedMessage incident) {
        log.info("Generating postmortem using AWS Bedrock for incident: {}", incident.getIncidentId());

        String prompt = buildPostmortemPrompt(incident);

        try {
            String response = invokeBedrockModel(prompt);
            log.info("Successfully generated postmortem content for incident: {}", incident.getIncidentId());
            return response;
        } catch (Exception e) {
            log.error("Error generating postmortem using Bedrock", e);
            throw new RuntimeException("Failed to generate postmortem", e);
        }
    }

    private String invokeBedrockModel(String prompt) {
        try {
            String requestBody = buildClaudeRequest(prompt);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(software.amazon.awssdk.core.SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);

            String responseBody = new String(response.body().asByteArray(), StandardCharsets.UTF_8);
            return parseBedrockResponse(responseBody);

        } catch (Exception e) {
            log.error("Error invoking Bedrock model", e);
            throw new RuntimeException("Failed to invoke Bedrock model", e);
        }
    }

    private String buildClaudeRequest(String prompt) throws Exception {
        return objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("anthropic_version", "bedrock-2023-06-01")
                        .put("max_tokens", maxTokens)
                        .set("messages", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode()
                                        .put("role", "user")
                                        .put("content", prompt)))
        );
    }

    private String parseBedrockResponse(String responseBody) throws Exception {
        var jsonNode = objectMapper.readTree(responseBody);
        var content = jsonNode.get("content");
        if (content != null && content.isArray() && content.size() > 0) {
            return content.get(0).get("text").asText();
        }
        throw new RuntimeException("Invalid Bedrock response format");
    }

    private String buildPostmortemPrompt(IncidentResolvedMessage incident) {
        return String.format("""
                Generate a comprehensive incident postmortem based on the following details:

                Incident ID: %s
                Title: %s
                Severity: %s
                Affected Service: %s
                Duration: From the creation to resolution

                Root Cause: %s
                Remediation Steps: %s
                Description: %s

                Please generate a detailed postmortem that includes:
                1. Executive Summary
                2. Timeline of Events
                3. Root Cause Analysis
                4. Contributing Factors
                5. Preventive Actions
                6. Corrective Actions
                7. Lessons Learned

                Format the response in clear sections with headers.
                """,
                incident.getIncidentId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getAffectedService(),
                incident.getRootCause() != null ? incident.getRootCause() : "N/A",
                incident.getRemediationSteps() != null ? incident.getRemediationSteps() : "N/A",
                incident.getDescription() != null ? incident.getDescription() : "N/A"
        );
    }
}
