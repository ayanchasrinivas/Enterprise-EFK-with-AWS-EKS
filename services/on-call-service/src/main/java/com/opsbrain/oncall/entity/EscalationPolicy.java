package com.opsbrain.oncall.entity;

import com.opsbrain.oncall.model.EscalationLevel;
import com.opsbrain.oncall.model.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "escalation_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private OnCallSchedule schedule;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity minSeverity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EscalationLevel level;

    @Column(nullable = false, length = 100)
    private String targetTeamOrPerson;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
