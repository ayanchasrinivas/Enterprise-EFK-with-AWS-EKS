package com.opsbrain.postmortem.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsbrain.postmortem.dto.IncidentResolvedMessage;
import com.opsbrain.postmortem.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentResolvedConsumer {

    private final PostmortemService postmortemService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "incident-resolved", groupId = "postmortem-service-group")
    public void consumeIncidentResolved(String message) {
        try {
            log.debug("Received incident resolved message: {}", message);

            IncidentResolvedMessage incident = objectMapper.readValue(message, IncidentResolvedMessage.class);

            postmortemService.generatePostmortem(incident);

            log.info("Successfully processed incident resolved message: {}", incident.getIncidentId());

        } catch (Exception e) {
            log.error("Error processing incident resolved message: {}", message, e);
        }
    }
}
