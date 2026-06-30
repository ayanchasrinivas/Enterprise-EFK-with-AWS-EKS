package com.opsbrain.incident.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsbrain.incident.dto.AnalysisMessage;
import com.opsbrain.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisConsumer {

    private final IncidentService incidentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "analysis-output", groupId = "incident-service-group")
    public void consumeAnalysis(String message) {
        try {
            log.debug("Received analysis message: {}", message);

            AnalysisMessage analysis = objectMapper.readValue(message, AnalysisMessage.class);

            incidentService.processAnalysis(analysis);

            log.info("Successfully processed analysis: {}", analysis.getAnalysisId());
        } catch (Exception e) {
            log.error("Error processing analysis message: {}", message, e);
        }
    }
}
