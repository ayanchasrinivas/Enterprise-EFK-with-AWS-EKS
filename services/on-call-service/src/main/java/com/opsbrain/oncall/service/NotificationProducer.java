package com.opsbrain.oncall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsbrain.oncall.dto.IncidentNotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishIncidentNotification(IncidentNotificationMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("incident-assigned", message.getIncidentId(), payload);
            log.info("Published incident notification for: {}", message.getIncidentId());
        } catch (Exception e) {
            log.error("Error publishing incident notification", e);
        }
    }
}
