package com.opsbrain.contextcollector.collector;

import com.opsbrain.contextcollector.model.KubernetesEvent;
import com.opsbrain.contextcollector.model.NormalizedAlert;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lists recent K8s events in the alert's namespace and keeps the ones whose
 * involved object name matches the affected service (OOMKilling, BackOff,
 * FailedScheduling, Unhealthy...). These are often the smoking gun.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCollector {

    private final KubernetesClient client;

    public List<KubernetesEvent> collect(NormalizedAlert alert) {
        if (alert.getNamespace() == null) return List.of();

        List<Event> events = client.v1().events()
                .inNamespace(alert.getNamespace())
                .list().getItems();

        String svc = alert.getService();

        return events.stream()
                .filter(e -> svc == null || matchesService(e, svc))
                .sorted(Comparator.comparing(this::lastSeen).reversed())
                .limit(20)
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    private boolean matchesService(Event e, String svc) {
        String objName = e.getInvolvedObject() != null
                ? e.getInvolvedObject().getName() : null;
        return objName != null && objName.contains(svc);
    }

    private KubernetesEvent toModel(Event e) {
        return KubernetesEvent.builder()
                .type(e.getType())
                .reason(e.getReason())
                .message(e.getMessage())
                .involvedObject(e.getInvolvedObject() != null
                        ? e.getInvolvedObject().getKind() + "/" + e.getInvolvedObject().getName()
                        : null)
                .count(e.getCount() != null ? e.getCount() : 1)
                .lastSeen(lastSeen(e))
                .build();
    }

    private Instant lastSeen(Event e) {
        String ts = e.getLastTimestamp() != null ? e.getLastTimestamp() : e.getEventTime() != null
                ? e.getEventTime().getTime() : null;
        try { return ts == null ? Instant.EPOCH : Instant.parse(ts); }
        catch (Exception ex) { return Instant.EPOCH; }
    }
}