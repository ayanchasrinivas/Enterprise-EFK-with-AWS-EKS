package com.opsbrain.notification.controller;

import com.opsbrain.notification.dto.NotificationResponse;
import com.opsbrain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications with pagination")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/incident/{incidentId}")
    @Operation(summary = "Get notifications for an incident")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByIncidentId(@PathVariable String incidentId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByIncidentId(incidentId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/failed")
    @Operation(summary = "Get failed notifications")
    public ResponseEntity<List<NotificationResponse>> getFailedNotifications() {
        List<NotificationResponse> notifications = notificationService.getFailedNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending notifications")
    public ResponseEntity<List<NotificationResponse>> getPendingNotifications() {
        List<NotificationResponse> notifications = notificationService.getPendingNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/retryable")
    @Operation(summary = "Get notifications that can be retried")
    public ResponseEntity<List<NotificationResponse>> getRetryableNotifications(
            @RequestParam(defaultValue = "3") Integer maxRetries) {
        List<NotificationResponse> notifications = notificationService.getRetryableNotifications(maxRetries);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/retry")
    @Operation(summary = "Retry sending a failed notification")
    public ResponseEntity<Void> retryNotification(@PathVariable Long id) {
        log.info("Retrying notification: {}", id);
        notificationService.retryNotification(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stale")
    @Operation(summary = "Get stale notifications")
    public ResponseEntity<List<NotificationResponse>> getStaleNotifications(
            @RequestParam(defaultValue = "30") Integer minutesThreshold) {
        List<NotificationResponse> notifications = notificationService.getStaleNotifications(minutesThreshold);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
}
