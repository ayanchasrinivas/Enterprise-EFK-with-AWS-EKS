package com.opsbrain.incident.service;

import com.opsbrain.incident.dto.AnalysisMessage;
import com.opsbrain.incident.entity.Incident;
import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.model.Severity;
import com.opsbrain.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentDeduplicationServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentDeduplicationService deduplicationService;

    private Incident testIncident;
    private AnalysisMessage testAnalysis;

    @BeforeEach
    void setUp() {
        testIncident = Incident.builder()
                .id(1L)
                .incidentId("test-incident-1")
                .title("Database Connection Timeout")
                .description("Test incident")
                .severity(Severity.CRITICAL)
                .status(IncidentStatus.OPEN)
                .affectedService("payment-service")
                .rootCause("Connection pool exhausted")
                .remediationSteps("Increase pool size")
                .createdAt(LocalDateTime.now())
                .build();

        testAnalysis = AnalysisMessage.builder()
                .analysisId("analysis-1")
                .alertName("Database Connection Timeout")
                .alertSource("prometheus")
                .severity("CRITICAL")
                .affectedService("payment-service")
                .rootCauseAnalysis("Connection pool exhausted")
                .remediationSteps("Increase pool size")
                .build();
    }

    @Test
    void testFindMatchingIncident_ShouldReturnMatchWhenSimilar() {
        when(incidentRepository.findRecentByStatus(eq(IncidentStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testIncident));

        Optional<Incident> result = deduplicationService.findMatchingIncident(testAnalysis);

        assertTrue(result.isPresent());
        assertEquals(testIncident.getIncidentId(), result.get().getIncidentId());
    }

    @Test
    void testFindMatchingIncident_ShouldReturnEmptyWhenNoMatch() {
        when(incidentRepository.findRecentByStatus(eq(IncidentStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        Optional<Incident> result = deduplicationService.findMatchingIncident(testAnalysis);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindMatchingIncident_ShouldNotMatchDifferentService() {
        testAnalysis.setAffectedService("other-service");

        when(incidentRepository.findRecentByStatus(eq(IncidentStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testIncident));

        Optional<Incident> result = deduplicationService.findMatchingIncident(testAnalysis);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindMatchingIncident_ShouldNotMatchLowerSeverity() {
        testAnalysis.setSeverity("LOW");

        when(incidentRepository.findRecentByStatus(eq(IncidentStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testIncident));

        Optional<Incident> result = deduplicationService.findMatchingIncident(testAnalysis);

        assertFalse(result.isPresent());
    }
}
