package com.opsbrain.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsbrain.notification.dto.IncidentNotificationMessage;
import com.opsbrain.notification.service.SlackNotificationService;
import com.opsbrain.notification.service.TeamsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentNotificationConsumer {

    private final SlackNotificationService slackNotificationService;
    private final TeamsNotificationService teamsNotificationService;
    private final ObjectMapper objectMapper;

    @Value("${notification.slack.enabled:true}")
    private Boolean slackEnabled;

    @Value("${notification.slack.channel:incidents}")
    private String slackChannel;

    @Value("${notification.teams.enabled:true}")
    private Boolean teamsEnabled;

    @Value("${notification.teams.channel:Incidents}")
    private String teamsChannel;

    @KafkaListener(topics = "incident-assigned", groupId = "notification-service-group")
    public void consumeIncidentNotification(String message) {
        try {
            log.debug("Received incident notification: {}", message);

            IncidentNotificationMessage incident = objectMapper.readValue(message, IncidentNotificationMessage.class);

            if (slackEnabled) {
                try {
                    slackNotificationService.sendToSlack(incident, slackChannel);
                } catch (Exception e) {
                    log.error("Failed to send Slack notification", e);
                }
            }

            if (teamsEnabled) {
                try {
                    teamsNotificationService.sendToTeams(incident, teamsChannel);
                } catch (Exception e) {
                    log.error("Failed to send Teams notification", e);
                }
            }

            log.info("Successfully processed incident notification: {}", incident.getIncidentId());

        } catch (Exception e) {
            log.error("Error processing incident notification message: {}", message, e);
        }
    }
}
