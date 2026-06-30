package com.opsbrain.notification.repository;

import com.opsbrain.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateName(String templateName);

    List<NotificationTemplate> findByChannelAndActive(String channel, Boolean active);

    List<NotificationTemplate> findByActive(Boolean active);
}
