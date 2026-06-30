package com.opsbrain.notification.repository;

import com.opsbrain.notification.entity.NotificationLog;
import com.opsbrain.notification.model.NotificationChannel;
import com.opsbrain.notification.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Optional<NotificationLog> findByIncidentIdAndChannel(String incidentId, NotificationChannel channel);

    List<NotificationLog> findByIncidentId(String incidentId);

    List<NotificationLog> findByStatus(NotificationStatus status);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, Integer maxRetries);

    @Query("SELECT n FROM NotificationLog n WHERE n.status = :status AND n.lastUpdatedAt <= :cutoffTime ORDER BY n.createdAt ASC")
    List<NotificationLog> findStaleNotifications(@Param("status") NotificationStatus status, @Param("cutoffTime") LocalDateTime cutoffTime);

    List<NotificationLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<NotificationLog> findByChannelAndStatus(NotificationChannel channel, NotificationStatus status);
}
