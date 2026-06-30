package com.opsbrain.contextcollector.consumer;

import com.opsbrain.contextcollector.model.*;
import com.opsbrain.contextcollector.service.ContextAggregator;
import com.opsbrain.contextcollector.service.ContextPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertConsumer {

    private final ContextAggregator aggregator;
    private final ContextPublisher publisher;

    @KafkaListener(
            topics = "${opsbrain.kafka.topic.raw-alerts}",
            groupId = "context-collector",
            containerFactory = "kafkaListenerContainerFactory")
    public void onAlert(NormalizedAlert alert) {
        log.info("Received alert {} [{}] status={}",
                alert.getAlertId(), alert.getAlertName(), alert.getStatus());

        // RESOLVED alerts don't need heavy context collection — pass a light
        // bundle through so incident-service can close the matching incident.
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            publisher.publish(lightBundle(alert));
            return;
        }

        ContextBundle bundle = aggregator.aggregate(alert);
        publisher.publish(bundle);
    }

    private ContextBundle lightBundle(NormalizedAlert alert) {
        return ContextBundle.builder()
                .bundleId(UUID.randomUUID().toString())
                .alert(alert)
                .logs(LogContext.empty())
                .metrics(MetricContext.empty())
                .events(List.of())
                .deployment(DeploymentContext.empty(alert.getService()))
                .dashboards(List.of())
                .collectedAt(Instant.now())
                .collectionErrors(List.of())
                .build();
    }
}