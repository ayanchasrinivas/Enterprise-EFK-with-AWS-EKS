package com.opsbrain.incident.service;

import com.opsbrain.incident.dto.CreateIncidentRequest;
import com.opsbrain.incident.dto.IncidentResponse;
import com.opsbrain.incident.entity.Incident;
import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.model.Severity;
import com.opsbrain.incident.repository.IncidentAnalysisRepository;
import com.opsbrain.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentAnalysisRepository analysisRepository;

    @Mock
    private IncidentDeduplicationService deduplicationService;

    @InjectMocks
    private IncidentService incidentService;

    private CreateIncidentRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateIncidentRequest();
        createRequest.setTitle("Test Incident");
        createRequest.setDescription("Test Description");
        createRequest.setSeverity("CRITICAL");
        createRequest.setAffectedService("test-service");
    }

    @Test
    void testCreateIncident_ShouldCreateAndReturnIncident() {
        Incident savedIncident = Incident.builder()
                .id(1L)
                .incidentId("test-id")
                .title(createRequest.getTitle())
                .description(createRequest.getDescription())
                .severity(Severity.CRITICAL)
                .status(IncidentStatus.OPEN)
                .build();

        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(savedIncident));

        Incident result = incidentService.createIncident(createRequest);

        assertNotNull(result);
        assertEquals("Test Incident", result.getTitle());
        assertEquals(IncidentStatus.OPEN, result.getStatus());

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertEquals("Test Incident", captor.getValue().getTitle());
    }

    @Test
    void testGetIncidentById_ShouldReturnIncident() {
        Incident incident = Incident.builder()
                .id(1L)
                .incidentId("test-id")
                .title("Test")
                .severity(Severity.HIGH)
                .status(IncidentStatus.OPEN)
                .build();

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        IncidentResponse response = incidentService.getIncidentById(1L);

        assertNotNull(response);
        assertEquals("Test", response.getTitle());
        assertEquals(Severity.HIGH, response.getSeverity());
    }

    @Test
    void testGetIncidentById_ShouldThrowWhenNotFound() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> incidentService.getIncidentById(999L));
    }
}
