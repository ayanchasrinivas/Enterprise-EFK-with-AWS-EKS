package com.opsbrain.contextcollector.service;

import com.opsbrain.contextcollector.model.ContextBundle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextPublisher {

    private final KafkaTemplate<String, ContextBundle> bundleKafkaTemplate;

    @Value("${opsbrain.kafka.topic.alert-context}")
    private String alertContextTopic;

    public void publish(ContextBundle bundle) {
        // Key by fingerprint → same incident's bundles stay ordered on one partition
        String key = bundle.getAlert().getFingerprint();
        bundleKafkaTemplate.send(alertContextTopic, key, bundle)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Failed to publish bundle {}: {}",
                            bundle.getBundleId(), ex.getMessage());
                    else log.info("Published context bundle {} to {}",
                            bundle.getBundleId(), alertContextTopic);
                });
    }
}