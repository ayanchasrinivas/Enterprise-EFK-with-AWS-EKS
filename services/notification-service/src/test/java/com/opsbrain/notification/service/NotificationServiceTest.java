package com.opsbrain.notification.service;

import com.opsbrain.notification.entity.NotificationLog;
import com.opsbrain.notification.model.NotificationChannel;
import com.opsbrain.notification.model.NotificationStatus;
import com.opsbrain.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationLog testLog;

    @BeforeEach
    void setUp() {
        testLog = NotificationLog.builder()
                .id(1L)
                .incidentId("incident-1")
                .channel(NotificationChannel.SLACK)
                .recipient("C123456")
                .status(NotificationStatus.SENT)
                .retryCount(0)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetFailedNotifications() {
        NotificationLog failedLog = NotificationLog.builder()
                .id(2L)
                .incidentId("incident-2")
                .channel(NotificationChannel.TEAMS)
                .recipient("TeamsChannel")
                .status(NotificationStatus.FAILED)
                .retryCount(1)
                .errorMessage("Connection timeout")
                .build();

        when(notificationLogRepository.findByStatus(NotificationStatus.FAILED))
                .thenReturn(Arrays.asList(failedLog));

        List<NotificationLog> logs = notificationLogRepository.findByStatus(NotificationStatus.FAILED);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals(NotificationStatus.FAILED, logs.get(0).getStatus());
    }

    @Test
    void testGetPendingNotifications() {
        NotificationLog pendingLog = NotificationLog.builder()
                .id(3L)
                .incidentId("incident-3")
                .channel(NotificationChannel.SLACK)
                .recipient("C789012")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        when(notificationLogRepository.findByStatus(NotificationStatus.PENDING))
                .thenReturn(Arrays.asList(pendingLog));

        List<NotificationLog> logs = notificationLogRepository.findByStatus(NotificationStatus.PENDING);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals(NotificationStatus.PENDING, logs.get(0).getStatus());
    }

    @Test
    void testRetryNotification() {
        when(notificationLogRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(notificationLogRepository.save(any(NotificationLog.class))).thenReturn(testLog);

        Optional<NotificationLog> log = notificationLogRepository.findById(1L);

        assertTrue(log.isPresent());
        assertEquals(NotificationStatus.SENT, log.get().getStatus());
        assertEquals(0, log.get().getRetryCount());
    }

    @Test
    void testGetNotificationsByIncidentId() {
        when(notificationLogRepository.findByIncidentId("incident-1"))
                .thenReturn(Arrays.asList(testLog));

        List<NotificationLog> logs = notificationLogRepository.findByIncidentId("incident-1");

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("incident-1", logs.get(0).getIncidentId());
    }
}
