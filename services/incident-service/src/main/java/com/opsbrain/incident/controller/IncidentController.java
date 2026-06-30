package com.opsbrain.incident.controller;

import com.opsbrain.incident.dto.CreateIncidentRequest;
import com.opsbrain.incident.dto.IncidentResponse;
import com.opsbrain.incident.dto.UpdateIncidentRequest;
import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Incidents", description = "Incident management APIs")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @Operation(summary = "Create a new incident")
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        log.info("Creating new incident: {}", request.getTitle());
        var incident = incidentService.createIncident(request);
        var response = incidentService.getIncidentById(incident.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all incidents with pagination")
    public ResponseEntity<Page<IncidentResponse>> getIncidents(Pageable pageable) {
        Page<IncidentResponse> incidents = incidentService.getIncidents(pageable);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/open")
    @Operation(summary = "Get open and acknowledged incidents")
    public ResponseEntity<Page<IncidentResponse>> getOpenIncidents(Pageable pageable) {
        Page<IncidentResponse> incidents = incidentService.getOpenIncidents(pageable);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get incidents by status")
    public ResponseEntity<Page<IncidentResponse>> getIncidentsByStatus(
            @PathVariable IncidentStatus status,
            Pageable pageable) {
        Page<IncidentResponse> incidents = incidentService.getIncidentsByStatus(status, pageable);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/service/{service}")
    @Operation(summary = "Get incidents by affected service")
    public ResponseEntity<Page<IncidentResponse>> getIncidentsByService(
            @PathVariable String service,
            Pageable pageable) {
        Page<IncidentResponse> incidents = incidentService.getIncidentsByService(service, pageable);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable Long id) {
        IncidentResponse incident = incidentService.getIncidentById(id);
        return ResponseEntity.ok(incident);
    }

    @GetMapping("/incident-id/{incidentId}")
    @Operation(summary = "Get incident by incident ID")
    public ResponseEntity<IncidentResponse> getIncidentByIncidentId(@PathVariable String incidentId) {
        IncidentResponse incident = incidentService.getIncidentByIncidentId(incidentId);
        return ResponseEntity.ok(incident);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update incident details")
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request) {
        log.info("Updating incident: {}", id);
        IncidentResponse incident = incidentService.updateIncident(id, request);
        return ResponseEntity.ok(incident);
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve an incident")
    public ResponseEntity<IncidentResponse> resolveIncident(
            @PathVariable Long id,
            @RequestParam String resolvedBy) {
        log.info("Resolving incident: {} by {}", id, resolvedBy);
        IncidentResponse incident = incidentService.resolveIncident(id, resolvedBy);
        return ResponseEntity.ok(incident);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Incident Service is running");
    }
}
