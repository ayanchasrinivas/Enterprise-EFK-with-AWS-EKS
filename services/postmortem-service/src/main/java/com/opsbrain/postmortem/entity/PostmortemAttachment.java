package com.opsbrain.postmortem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "postmortem_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostmortemAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postmortem_id", nullable = false)
    private Postmortem postmortem;

    @Column(nullable = false, length = 100)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String fileFormat;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 500)
    private String storageLocation;

    @Column(columnDefinition = "TEXT")
    private String contentMd5;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
