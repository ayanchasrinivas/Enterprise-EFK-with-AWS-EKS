package com.opsbrain.incident.entity;

import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.model.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String incidentId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(length = 200)
    private String affectedService;

    @Column(length = 500)
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String remediationSteps;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;

    private LocalDateTime resolvedAt;

    @Column(length = 100)
    private String resolvedBy;

    @Column(nullable = false)
    private Integer dedupCount;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IncidentAnalysis> analyses;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
        dedupCount = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }

    public void addAnalysis(IncidentAnalysis analysis) {
        if (analyses == null) {
            analyses = new java.util.ArrayList<>();
        }
        analyses.add(analysis);
        analysis.setIncident(this);
        dedupCount++;
    }
}
