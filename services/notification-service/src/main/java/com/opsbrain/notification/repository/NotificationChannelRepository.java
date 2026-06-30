package com.opsbrain.notification.repository;

import com.opsbrain.notification.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {

    Optional<NotificationChannel> findByName(String name);

    List<NotificationChannel> findByChannelTypeAndActive(String channelType, Boolean active);

    List<NotificationChannel> findByActive(Boolean active);
}
