package com.opsbrain.notification.service;

import com.opsbrain.notification.dto.IncidentNotificationMessage;
import com.opsbrain.notification.dto.SlackMessage;
import com.opsbrain.notification.entity.NotificationChannel;
import com.opsbrain.notification.entity.NotificationLog;
import com.opsbrain.notification.model.NotificationStatus;
import com.opsbrain.notification.repository.NotificationChannelRepository;
import com.opsbrain.notification.repository.NotificationLogRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlackNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationChannelRepository notificationChannelRepository;
    private final WebClient webClient;

    @Transactional
    @Retry(name = "slackNotification")
    public void sendToSlack(IncidentNotificationMessage incident, String channelName) {
        log.info("Sending Slack notification for incident: {}", incident.getIncidentId());

        NotificationChannel channel = notificationChannelRepository.findByName(channelName)
                .orElseThrow(() -> new RuntimeException("Slack channel not found: " + channelName));

        if (!channel.getActive()) {
            log.warn("Slack channel is inactive: {}", channelName);
            return;
        }

        SlackMessage slackMessage = buildSlackMessage(incident);
        String recipient = channel.getChannelId() != null ? channel.getChannelId() : channelName;

        try {
            sendSlackRequest(channel.getWebhookUrl(), slackMessage);

            NotificationLog log = NotificationLog.builder()
                    .incidentId(incident.getIncidentId())
                    .channel(com.opsbrain.notification.model.NotificationChannel.SLACK)
                    .recipient(recipient)
                    .messageContent(slackMessage.toString())
                    .status(NotificationStatus.SENT)
                    .retryCount(0)
                    .sentAt(LocalDateTime.now())
                    .build();

            notificationLogRepository.save(log);
            log.info("Slack notification sent for incident: {}", incident.getIncidentId());

        } catch (Exception e) {
            log.error("Failed to send Slack notification for incident: {}", incident.getIncidentId(), e);
            saveFailedNotification(incident.getIncidentId(), recipient, e.getMessage());
            throw new RuntimeException("Failed to send Slack notification", e);
        }
    }

    private SlackMessage buildSlackMessage(IncidentNotificationMessage incident) {
        String severityColor = getSeverityColor(incident.getSeverity());

        SlackMessage.Text titleText = SlackMessage.Text.builder()
                .type("mrkdwn")
                .text("*" + incident.getTitle() + "*")
                .build();

        SlackMessage.Block headerBlock = SlackMessage.Block.builder()
                .type("section")
                .text(titleText)
                .blockId("header")
                .build();

        SlackMessage.Field severityField = SlackMessage.Field.builder()
                .type("mrkdwn")
                .text("*Severity:*\n" + incident.getSeverity())
                .build();

        SlackMessage.Field serviceField = SlackMessage.Field.builder()
                .type("mrkdwn")
                .text("*Service:*\n" + incident.getAffectedService())
                .build();

        SlackMessage.Field onCallField = SlackMessage.Field.builder()
                .type("mrkdwn")
                .text("*On-Call:*\n" + incident.getOnCallMemberName())
                .build();

        SlackMessage.Field rootCauseField = SlackMessage.Field.builder()
                .type("mrkdwn")
                .text("*Root Cause:*\n" + (incident.getRootCause() != null ? incident.getRootCause() : "N/A"))
                .build();

        SlackMessage.Block fieldsBlock = SlackMessage.Block.builder()
                .type("section")
                .fields(List.of(severityField, serviceField, onCallField, rootCauseField))
                .blockId("fields")
                .build();

        return SlackMessage.builder()
                .text(incident.getTitle())
                .blocks(List.of(headerBlock, fieldsBlock))
                .build();
    }

    private void sendSlackRequest(String webhookUrl, SlackMessage message) {
        webClient.post()
                .uri(webhookUrl)
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String getSeverityColor(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "#FF0000";
            case "HIGH" -> "#FFA500";
            case "MEDIUM" -> "#FFFF00";
            case "LOW" -> "#00FF00";
            default -> "#808080";
        };
    }

    @Transactional
    private void saveFailedNotification(String incidentId, String recipient, String errorMessage) {
        NotificationLog log = NotificationLog.builder()
                .incidentId(incidentId)
                .channel(com.opsbrain.notification.model.NotificationChannel.SLACK)
                .recipient(recipient)
                .status(NotificationStatus.FAILED)
                .retryCount(0)
                .errorMessage(errorMessage)
                .build();

        notificationLogRepository.save(log);
    }
}
