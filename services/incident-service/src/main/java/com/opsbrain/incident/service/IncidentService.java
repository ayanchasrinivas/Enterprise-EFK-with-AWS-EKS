package com.opsbrain.incident.service;

import com.opsbrain.incident.dto.*;
import com.opsbrain.incident.entity.Incident;
import com.opsbrain.incident.entity.IncidentAnalysis;
import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.model.Severity;
import com.opsbrain.incident.repository.IncidentAnalysisRepository;
import com.opsbrain.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentAnalysisRepository analysisRepository;
    private final IncidentDeduplicationService deduplicationService;

    @Transactional
    public Incident createIncident(CreateIncidentRequest request) {
        String incidentId = UUID.randomUUID().toString();

        Incident incident = Incident.builder()
                .incidentId(incidentId)
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(Severity.valueOf(request.getSeverity().toUpperCase()))
                .status(IncidentStatus.OPEN)
                .affectedService(request.getAffectedService())
                .rootCause(request.getRootCause())
                .remediationSteps(request.getRemediationSteps())
                .build();

        incident = incidentRepository.save(incident);
        log.info("Created new incident: {} with ID: {}", incident.getTitle(), incident.getIncidentId());
        return incident;
    }

    @Transactional
    public Incident processAnalysis(AnalysisMessage analysis) {
        log.info("Processing analysis: {} for alert: {}", analysis.getAnalysisId(), analysis.getAlertName());

        Optional<Incident> existingIncident = deduplicationService.findMatchingIncident(analysis);

        Incident incident;
        if (existingIncident.isPresent()) {
            incident = existingIncident.get();
            log.info("Found matching incident: {}, deduplicating", incident.getIncidentId());
        } else {
            incident = Incident.builder()
                    .incidentId(UUID.randomUUID().toString())
                    .title(analysis.getAlertName())
                    .description(String.format("Alert from %s: %s", analysis.getAlertSource(), analysis.getAlertName()))
                    .severity(Severity.valueOf(analysis.getSeverity().toUpperCase()))
                    .status(IncidentStatus.OPEN)
                    .affectedService(analysis.getAffectedService())
                    .rootCause(analysis.getRootCauseAnalysis())
                    .remediationSteps(analysis.getRemediationSteps())
                    .build();
            log.info("Creating new incident from analysis: {}", analysis.getAnalysisId());
        }

        IncidentAnalysis incidentAnalysis = IncidentAnalysis.builder()
                .analysisId(analysis.getAnalysisId())
                .incident(incident)
                .analysisContent(convertContextBundleToString(analysis.getContextBundle()))
                .rootCauseAnalysis(analysis.getRootCauseAnalysis())
                .remediationSteps(analysis.getRemediationSteps())
                .sourceService("ai-analysis-service")
                .build();

        incident.addAnalysis(incidentAnalysis);

        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getIncidents(Pageable pageable) {
        return incidentRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getIncidentsByStatus(IncidentStatus status, Pageable pageable) {
        return incidentRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getIncidentsByService(String service, Pageable pageable) {
        return incidentRepository.findByAffectedService(service, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> getOpenIncidents(Pageable pageable) {
        return incidentRepository.findByStatusIn(
                Arrays.asList(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED),
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentByIncidentId(String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Incident not found with incidentId: " + incidentId));
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));

        if (request.getTitle() != null) {
            incident.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            incident.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            IncidentStatus newStatus = IncidentStatus.valueOf(request.getStatus().toUpperCase());
            incident.setStatus(newStatus);
            if (newStatus == IncidentStatus.RESOLVED && request.getResolvedBy() != null) {
                incident.setResolvedAt(LocalDateTime.now());
                incident.setResolvedBy(request.getResolvedBy());
            }
        }
        if (request.getSeverity() != null) {
            incident.setSeverity(Severity.valueOf(request.getSeverity().toUpperCase()));
        }
        if (request.getAffectedService() != null) {
            incident.setAffectedService(request.getAffectedService());
        }
        if (request.getRootCause() != null) {
            incident.setRootCause(request.getRootCause());
        }
        if (request.getRemediationSteps() != null) {
            incident.setRemediationSteps(request.getRemediationSteps());
        }

        incident = incidentRepository.save(incident);
        log.info("Updated incident: {}", incident.getIncidentId());
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long id, String resolvedBy) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with ID: " + id));

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(resolvedBy);

        incident = incidentRepository.save(incident);
        log.info("Resolved incident: {}", incident.getIncidentId());
        return toResponse(incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidentsSince(LocalDateTime since) {
        return incidentRepository.findRecentByStatus(IncidentStatus.OPEN, since)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private IncidentResponse toResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .incidentId(incident.getIncidentId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .affectedService(incident.getAffectedService())
                .rootCause(incident.getRootCause())
                .remediationSteps(incident.getRemediationSteps())
                .createdAt(incident.getCreatedAt())
                .lastUpdatedAt(incident.getLastUpdatedAt())
                .resolvedAt(incident.getResolvedAt())
                .resolvedBy(incident.getResolvedBy())
                .dedupCount(incident.getDedupCount())
                .build();
    }

    private String convertContextBundleToString(Map<String, Object> contextBundle) {
        if (contextBundle == null || contextBundle.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        contextBundle.forEach((key, value) -> {
            sb.append(key).append(": ").append(value).append("\n");
        });
        return sb.toString();
    }
}
