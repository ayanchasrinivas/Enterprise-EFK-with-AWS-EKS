package com.opsbrain.aianalysis.consumer;

import com.opsbrain.aianalysis.model.AnalysisResult;
import com.opsbrain.aianalysis.model.ContextBundle;
import com.opsbrain.aianalysis.service.AnalysisPublisher;
import com.opsbrain.aianalysis.service.BedrockAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextBundleConsumer {

    private final BedrockAnalysisService analysisService;
    private final AnalysisPublisher publisher;

    @KafkaListener(
            topics = "${opsbrain.kafka.topic.alert-context}",
            groupId = "ai-analysis",
            containerFactory = "kafkaListenerContainerFactory")
    public void onBundle(ContextBundle bundle) {
        log.info("Received context bundle {} for alert {}",
                bundle.getBundleId(), bundle.getAlert().getAlertId());

        AnalysisResult result = analysisService.analyze(bundle);   // never throws
        publisher.publish(result);
    }
}