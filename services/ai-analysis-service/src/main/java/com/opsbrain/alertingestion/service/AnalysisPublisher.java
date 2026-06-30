package com.opsbrain.aianalysis.service;

import com.opsbrain.aianalysis.model.AnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisPublisher {

    private final KafkaTemplate<String, AnalysisResult> analysisKafkaTemplate;

    @Value("${opsbrain.kafka.topic.incident-analysis}")
    private String topic;

    public void publish(AnalysisResult result) {
        // Key by fingerprint → all analyses for one incident stay ordered.
        analysisKafkaTemplate.send(topic, result.getFingerprint(), result)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish analysis {}: {}",
                            result.getAnalysisId(), ex.getMessage());
                    else log.info("Published analysis {} for alert {} to {}",
                            result.getAnalysisId(), result.getAlertId(), topic);
                });
    }
}