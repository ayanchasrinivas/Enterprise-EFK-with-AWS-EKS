package com.opsbrain.notification.service;

import com.opsbrain.notification.dto.IncidentNotificationMessage;
import com.opsbrain.notification.dto.TeamsMessage;
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
public class TeamsNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationChannelRepository notificationChannelRepository;
    private final WebClient webClient;

    @Transactional
    @Retry(name = "teamsNotification")
    public void sendToTeams(IncidentNotificationMessage incident, String channelName) {
        log.info("Sending Teams notification for incident: {}", incident.getIncidentId());

        NotificationChannel channel = notificationChannelRepository.findByName(channelName)
                .orElseThrow(() -> new RuntimeException("Teams channel not found: " + channelName));

        if (!channel.getActive()) {
            log.warn("Teams channel is inactive: {}", channelName);
            return;
        }

        TeamsMessage teamsMessage = buildTeamsMessage(incident);
        String recipient = channel.getChannelId() != null ? channel.getChannelId() : channelName;

        try {
            sendTeamsRequest(channel.getWebhookUrl(), teamsMessage);

            NotificationLog log = NotificationLog.builder()
                    .incidentId(incident.getIncidentId())
                    .channel(com.opsbrain.notification.model.NotificationChannel.TEAMS)
                    .recipient(recipient)
                    .messageContent(teamsMessage.toString())
                    .status(NotificationStatus.SENT)
                    .retryCount(0)
                    .sentAt(LocalDateTime.now())
                    .build();

            notificationLogRepository.save(log);
            log.info("Teams notification sent for incident: {}", incident.getIncidentId());

        } catch (Exception e) {
            log.error("Failed to send Teams notification for incident: {}", incident.getIncidentId(), e);
            saveFailedNotification(incident.getIncidentId(), recipient, e.getMessage());
            throw new RuntimeException("Failed to send Teams notification", e);
        }
    }

    private TeamsMessage buildTeamsMessage(IncidentNotificationMessage incident) {
        String themeColor = getThemeColor(incident.getSeverity());

        TeamsMessage.Fact severityFact = TeamsMessage.Fact.builder()
                .name("Severity")
                .value(incident.getSeverity())
                .build();

        TeamsMessage.Fact serviceFact = TeamsMessage.Fact.builder()
                .name("Affected Service")
                .value(incident.getAffectedService())
                .build();

        TeamsMessage.Fact onCallFact = TeamsMessage.Fact.builder()
                .name("On-Call Member")
                .value(incident.getOnCallMemberName() + " (" + incident.getOnCallMemberEmail() + ")")
                .build();

        TeamsMessage.Fact rootCauseFact = TeamsMessage.Fact.builder()
                .name("Root Cause")
                .value(incident.getRootCause() != null ? incident.getRootCause() : "N/A")
                .build();

        TeamsMessage.Section section = TeamsMessage.Section.builder()
                .activityTitle(incident.getTitle())
                .activitySubtitle(incident.getAffectedService())
                .text(incident.getTitle())
                .facts(List.of(severityFact, serviceFact, onCallFact, rootCauseFact))
                .build();

        return TeamsMessage.builder()
                .type("MessageCard")
                .context("https://schema.org/extensions")
                .summary(incident.getTitle())
                .themeColor(themeColor)
                .sections(List.of(section))
                .build();
    }

    private void sendTeamsRequest(String webhookUrl, TeamsMessage message) {
        webClient.post()
                .uri(webhookUrl)
                .bodyValue(message)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String getThemeColor(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "FF0000";
            case "HIGH" -> "FFA500";
            case "MEDIUM" -> "FFFF00";
            case "LOW" -> "00FF00";
            default -> "808080";
        };
    }

    @Transactional
    private void saveFailedNotification(String incidentId, String recipient, String errorMessage) {
        NotificationLog log = NotificationLog.builder()
                .incidentId(incidentId)
                .channel(com.opsbrain.notification.model.NotificationChannel.TEAMS)
                .recipient(recipient)
                .status(NotificationStatus.FAILED)
                .retryCount(0)
                .errorMessage(errorMessage)
                .build();

        notificationLogRepository.save(log);
    }
}
