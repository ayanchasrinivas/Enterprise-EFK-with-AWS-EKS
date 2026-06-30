package com.opsbrain.postmortem.entity;

import com.opsbrain.postmortem.model.PostmortemStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "postmortems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Postmortem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String incidentId;

    @Column(nullable = false, length = 100)
    private String postmortemId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String timeline;

    @Column(columnDefinition = "TEXT")
    private String rootCauses;

    @Column(columnDefinition = "TEXT")
    private String contributingFactors;

    @Column(columnDefinition = "TEXT")
    private String actionItems;

    @Column(columnDefinition = "TEXT")
    private String lessonLearned;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PostmortemStatus status;

    @Column(nullable = false)
    private Long durationMinutes;

    @Column(nullable = false)
    private LocalDateTime incidentStartTime;

    @Column(nullable = false)
    private LocalDateTime incidentEndTime;

    @Column(length = 100)
    private String generatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime generatedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "postmortem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostmortemAttachment> attachments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
