package com.opsbrain.aianalysis.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.opsbrain.aianalysis.model.*;
import com.opsbrain.aianalysis.prompt.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Calls Claude on Bedrock with the context bundle and returns a structured
 * AnalysisResult. On any failure it returns a degraded result (never throws to
 * the consumer) so the incident still gets created and the engineer paged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockAnalysisService {

    private final AnthropicClient client;
    private final PromptBuilder promptBuilder;

    @Value("${opsbrain.bedrock.model-id}")
    private String modelId;                 // anthropic.claude-opus-4-8

    @Value("${opsbrain.bedrock.max-tokens:4096}")
    private long maxTokens;

    public AnalysisResult analyze(ContextBundle bundle) {
        NormalizedAlert alert = bundle.getAlert();
        try {
            String userPrompt = promptBuilder.buildUserPrompt(bundle);

            // Structured outputs: the SDK derives a JSON schema from AnalysisOutput
            // and constrains the model to return exactly that shape.
            // (If your SDK build requires it, add .addBeta("structured-outputs-2025-11-13").)
            StructuredMessageCreateParams<AnalysisOutput> params = MessageCreateParams.builder()
                    .model(modelId)
                    .maxTokens(maxTokens)
                    .system(PromptBuilder.SYSTEM_PROMPT)
                    .addUserMessage(userPrompt)
                    .outputConfig(AnalysisOutput.class)   // effort defaults to HIGH on Opus 4.8
                    .build();

            AnalysisOutput out = client.messages().create(params).content().stream()
                    .flatMap(cb -> cb.text().stream())
                    .map(typed -> typed.text())            // typed AnalysisOutput
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No structured output in response"));

            log.info("Analysis for alert {} [{}] — rootCause='{}' confidence={} deployRelated={}",
                    alert.getAlertId(), alert.getAlertName(),
                    out.rootCause(), out.confidence(), out.deploymentRelated());

            return toResult(bundle, out, null);

        } catch (Exception e) {
            log.error("Bedrock analysis failed for alert {} — emitting degraded result: {}",
                    alert.getAlertId(), e.getMessage(), e);
            return fallback(bundle, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private AnalysisResult toResult(ContextBundle b, AnalysisOutput out, String error) {
        NormalizedAlert a = b.getAlert();
        List<RemediationStep> steps = new ArrayList<>();
        if (out.remediationSteps() != null) {
            out.remediationSteps().forEach(s -> steps.add(RemediationStep.builder()
                    .action(s.action())
                    .command(s.command())
                    .risk(s.risk())
                    .rationale(s.rationale())
                    .build()));
        }

        return AnalysisResult.builder()
                .analysisId(UUID.randomUUID().toString())
                .bundleId(b.getBundleId())
                .alertId(a.getAlertId())
                .fingerprint(a.getFingerprint())
                .alertName(a.getAlertName())
                .service(a.getService())
                .namespace(a.getNamespace())
                .rootCause(out.rootCause())
                .confidence(out.confidence())
                .contributingFactors(out.contributingFactors())
                .remediationSteps(steps)
                .deploymentRelated(out.deploymentRelated())
                .summary(out.summary())
                .recommendedSeverity(Severity.fromRaw(out.recommendedSeverity()))
                .originalSeverity(a.getSeverity())
                .dashboards(b.getDashboards())
                .generatorUrl(a.getGeneratorUrl())
                .aiModel(modelId)
                .analyzedAt(Instant.now())
                .llmError(error)
                .build();
    }

    /** Degraded path — keep the deterministic deploy signal so it's still useful. */
    private AnalysisResult fallback(ContextBundle b, String error) {
        NormalizedAlert a = b.getAlert();
        boolean deployRelated = b.getDeployment() != null && b.getDeployment().isRecentlyDeployed();

        List<RemediationStep> steps = new ArrayList<>();
        if (deployRelated && b.getDeployment().getApplicationName() != null) {
            steps.add(RemediationStep.builder()
                    .action("Consider rolling back the recent deployment")
                    .command("argocd app rollback " + b.getDeployment().getApplicationName())
                    .risk("medium")
                    .rationale("A deploy landed near the alert time and may be the trigger")
                    .build());
        }
        steps.add(RemediationStep.builder()
                .action("Investigate manually — automated analysis was unavailable")
                .command("kubectl get events -n " + (a.getNamespace() == null ? "<namespace>" : a.getNamespace()))
                .risk("low")
                .rationale("AI analysis failed; on-call should triage from raw signals")
                .build());

        return AnalysisResult.builder()
                .analysisId(UUID.randomUUID().toString())
                .bundleId(b.getBundleId())
                .alertId(a.getAlertId())
                .fingerprint(a.getFingerprint())
                .alertName(a.getAlertName())
                .service(a.getService())
                .namespace(a.getNamespace())
                .rootCause("Automated analysis unavailable — manual investigation required")
                .confidence(0.0)
                .contributingFactors(List.of())
                .remediationSteps(steps)
                .deploymentRelated(deployRelated)
                .summary("OpsBrain could not reach the analysis model. "
                        + (deployRelated ? "A recent deployment is a likely suspect. " : "")
                        + "Paging on-call for manual triage.")
                .recommendedSeverity(a.getSeverity())
                .originalSeverity(a.getSeverity())
                .dashboards(b.getDashboards())
                .generatorUrl(a.getGeneratorUrl())
                .aiModel(modelId)
                .analyzedAt(Instant.now())
                .llmError(error)
                .build();
    }
}