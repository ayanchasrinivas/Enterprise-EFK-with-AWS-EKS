package com.opsbrain.alertingestion.controller;

import com.opsbrain.alertingestion.dto.*;
import com.opsbrain.alertingestion.mapper.*;
import com.opsbrain.alertingestion.model.NormalizedAlert;
import com.opsbrain.alertingestion.service.AlertPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final PrometheusAlertMapper prometheusMapper;
    private final GrafanaAlertMapper grafanaMapper;
    private final GenericAlertMapper genericMapper;
    private final AlertPublisher publisher;

    /** Prometheus AlertManager webhook target. */
    @PostMapping("/prometheus")
    public ResponseEntity<Map<String, Object>> prometheus(@RequestBody PrometheusWebhook webhook) {
        List<NormalizedAlert> alerts = prometheusMapper.map(webhook);
        publisher.publish(alerts);
        return accepted(alerts.size());
    }

    /** Grafana unified alerting webhook target. */
    @PostMapping("/grafana")
    public ResponseEntity<Map<String, Object>> grafana(@RequestBody GrafanaWebhook webhook) {
        List<NormalizedAlert> alerts = grafanaMapper.map(webhook);
        publisher.publish(alerts);
        return accepted(alerts.size());
    }

    /** Generic endpoint for CloudWatch (via Lambda), K8s event exporters, scripts. */
    @PostMapping("/generic")
    public ResponseEntity<Map<String, Object>> generic(@Valid @RequestBody GenericAlertRequest req) {
        NormalizedAlert alert = genericMapper.map(req);
        publisher.publish(alert);
        return accepted(1);
    }

    private ResponseEntity<Map<String, Object>> accepted(int count) {
        // 202: we've accepted the alert for async processing via Kafka.
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("accepted", count, "status", "queued"));
    }
}