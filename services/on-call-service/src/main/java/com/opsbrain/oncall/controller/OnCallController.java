package com.opsbrain.oncall.controller;

import com.opsbrain.oncall.dto.IncidentNotificationMessage;
import com.opsbrain.oncall.dto.OnCallResponse;
import com.opsbrain.oncall.service.OnCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/on-call")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "On-Call", description = "On-call management APIs")
public class OnCallController {

    private final OnCallService onCallService;

    @GetMapping("/current/{service}")
    @Operation(summary = "Get current on-call member for a service")
    public ResponseEntity<OnCallResponse> getCurrentOnCall(@PathVariable String service) {
        log.info("Getting current on-call for service: {}", service);
        OnCallResponse response = onCallService.getCurrentOnCall(service);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign an incident to on-call member")
    public ResponseEntity<IncidentNotificationMessage> assignIncident(
            @RequestParam String incidentId,
            @RequestParam String service,
            @RequestParam String severity,
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String rootCause) {
        log.info("Assigning incident {} to on-call for service: {}", incidentId, service);
        IncidentNotificationMessage message = onCallService.assignIncidentToOnCall(
                incidentId, service, severity, title, rootCause);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @PutMapping("/acknowledge")
    @Operation(summary = "Acknowledge an on-call assignment")
    public ResponseEntity<Void> acknowledgeAssignment(
            @RequestParam String incidentId,
            @RequestParam String acknowledgedBy) {
        log.info("Acknowledging assignment for incident: {}", incidentId);
        onCallService.acknowledgeAssignment(incidentId, acknowledgedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assignment/{incidentId}")
    @Operation(summary = "Get assignment for an incident")
    public ResponseEntity<?> getAssignment(@PathVariable String incidentId) {
        return ResponseEntity.ok(onCallService.getAssignmentByIncidentId(incidentId));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("On-Call Service is running");
    }
}
