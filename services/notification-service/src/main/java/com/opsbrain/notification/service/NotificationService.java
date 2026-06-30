package com.opsbrain.notification.service;

import com.opsbrain.notification.dto.NotificationResponse;
import com.opsbrain.notification.entity.NotificationLog;
import com.opsbrain.notification.model.NotificationStatus;
import com.opsbrain.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        return notificationLogRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByIncidentId(String incidentId) {
        return notificationLogRepository.findByIncidentId(incidentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getFailedNotifications() {
        return notificationLogRepository.findByStatus(NotificationStatus.FAILED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getPendingNotifications() {
        return notificationLogRepository.findByStatus(NotificationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRetryableNotifications(Integer maxRetries) {
        return notificationLogRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, maxRetries)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void retryNotification(Long notificationId) {
        NotificationLog log = notificationLogRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + notificationId));

        log.setStatus(NotificationStatus.RETRYING);
        log.setRetryCount(log.getRetryCount() + 1);
        notificationLogRepository.save(log);

        log.info("Marked notification {} for retry (attempt {})", notificationId, log.getRetryCount());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getStaleNotifications(Integer minutesThreshold) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesThreshold);
        return notificationLogRepository.findStaleNotifications(NotificationStatus.PENDING, cutoffTime)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(NotificationLog log) {
        return NotificationResponse.builder()
                .id(log.getId())
                .incidentId(log.getIncidentId())
                .channel(log.getChannel().getValue())
                .recipient(log.getRecipient())
                .status(log.getStatus().toString())
                .retryCount(log.getRetryCount())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .sentAt(log.getSentAt())
                .build();
    }
}
