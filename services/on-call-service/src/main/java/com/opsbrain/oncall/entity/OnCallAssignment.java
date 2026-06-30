package com.opsbrain.oncall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "on_call_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnCallAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private OnCallSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id", nullable = false)
    private TeamMember assignedTo;

    @Column(nullable = false, length = 50)
    private String severity;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = false)
    private Boolean acknowledged;

    private LocalDateTime acknowledgedAt;

    @Column(length = 100)
    private String acknowledgedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
