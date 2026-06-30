package com.opsbrain.postmortem.service;

import com.opsbrain.postmortem.dto.GeneratePostmortemRequest;
import com.opsbrain.postmortem.dto.IncidentResolvedMessage;
import com.opsbrain.postmortem.dto.PostmortemResponse;
import com.opsbrain.postmortem.entity.Postmortem;
import com.opsbrain.postmortem.entity.PostmortemAttachment;
import com.opsbrain.postmortem.model.PostmortemStatus;
import com.opsbrain.postmortem.repository.PostmortemAttachmentRepository;
import com.opsbrain.postmortem.repository.PostmortemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostmortemService {

    private final PostmortemRepository postmortemRepository;
    private final PostmortemAttachmentRepository attachmentRepository;
    private final BedrockService bedrockService;
    private final PDFGenerationService pdfGenerationService;

    @Transactional
    public Postmortem generatePostmortem(IncidentResolvedMessage incident) {
        log.info("Starting postmortem generation for incident: {}", incident.getIncidentId());

        Postmortem postmortem = Postmortem.builder()
                .incidentId(incident.getIncidentId())
                .postmortemId(UUID.randomUUID().toString())
                .title(incident.getTitle())
                .summary(incident.getDescription())
                .status(PostmortemStatus.GENERATING)
                .durationMinutes(calculateDuration(incident.getCreatedAt(), incident.getResolvedAt()))
                .incidentStartTime(LocalDateTime.now())
                .incidentEndTime(LocalDateTime.now())
                .generatedBy("system")
                .build();

        postmortem = postmortemRepository.save(postmortem);

        try {
            String postmortemContent = bedrockService.generatePostmortemContent(incident);

            postmortem.setStatus(PostmortemStatus.GENERATED);
            postmortem.setGeneratedAt(LocalDateTime.now());
            postmortem.setRootCauses(extractSection(postmortemContent, "Root Cause"));
            postmortem.setTimeline(extractSection(postmortemContent, "Timeline"));
            postmortem.setContributingFactors(extractSection(postmortemContent, "Contributing Factors"));
            postmortem.setActionItems(extractSection(postmortemContent, "Corrective Actions"));
            postmortem.setLessonLearned(extractSection(postmortemContent, "Lessons Learned"));

            // Generate PDF
            byte[] pdfContent = pdfGenerationService.generatePostmortemPDF(
                    postmortem.getTitle(),
                    postmortemContent,
                    postmortem.getGeneratedAt(),
                    postmortem.getGeneratedBy()
            );

            PostmortemAttachment attachment = PostmortemAttachment.builder()
                    .postmortem(postmortem)
                    .fileName(postmortem.getPostmortemId() + ".pdf")
                    .fileFormat("pdf")
                    .fileSize((long) pdfContent.length)
                    .storageLocation("s3://opsbrain-postmortems/" + postmortem.getPostmortemId() + ".pdf")
                    .contentMd5(calculateMD5(pdfContent))
                    .build();

            attachmentRepository.save(attachment);
            postmortem.getAttachments().add(attachment);

            return postmortemRepository.save(postmortem);

        } catch (Exception e) {
            log.error("Error generating postmortem", e);
            postmortem.setStatus(PostmortemStatus.FAILED);
            postmortem.setErrorMessage(e.getMessage());
            return postmortemRepository.save(postmortem);
        }
    }

    @Transactional
    public Postmortem generatePostmortemManually(GeneratePostmortemRequest request) {
        log.info("Generating postmortem manually for incident: {}", request.getIncidentId());

        IncidentResolvedMessage message = IncidentResolvedMessage.builder()
                .incidentId(request.getIncidentId())
                .title(request.getTitle())
                .severity(request.getSeverity())
                .affectedService(request.getAffectedService())
                .rootCause(request.getRootCause())
                .remediationSteps(request.getRemediationSteps())
                .description(request.getDescription())
                .createdAt(request.getIncidentStartTime() != null ?
                        request.getIncidentStartTime().toLocalDate().toEpochDay() : 0)
                .resolvedAt(request.getIncidentEndTime() != null ?
                        request.getIncidentEndTime().toLocalDate().toEpochDay() : 0)
                .build();

        return generatePostmortem(message);
    }

    @Transactional(readOnly = true)
    public Page<PostmortemResponse> getPostmortems(Pageable pageable) {
        return postmortemRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostmortemResponse getPostmortemByIncidentId(String incidentId) {
        Postmortem postmortem = postmortemRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new RuntimeException("Postmortem not found for incident: " + incidentId));
        return toResponse(postmortem);
    }

    @Transactional(readOnly = true)
    public PostmortemResponse getPostmortemById(Long id) {
        Postmortem postmortem = postmortemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postmortem not found with ID: " + id));
        return toResponse(postmortem);
    }

    @Transactional(readOnly = true)
    public List<PostmortemResponse> getGeneratedPostmortems() {
        return postmortemRepository.findByStatusOrderByCreatedAtDesc(PostmortemStatus.GENERATED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostmortemResponse> getFailedPostmortems() {
        return postmortemRepository.findByStatus(PostmortemStatus.FAILED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PostmortemResponse publishPostmortem(Long id) {
        Postmortem postmortem = postmortemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Postmortem not found with ID: " + id));

        postmortem.setStatus(PostmortemStatus.PUBLISHED);
        postmortem = postmortemRepository.save(postmortem);
        log.info("Published postmortem: {}", postmortem.getPostmortemId());

        return toResponse(postmortem);
    }

    private PostmortemResponse toResponse(Postmortem postmortem) {
        return PostmortemResponse.builder()
                .id(postmortem.getId())
                .incidentId(postmortem.getIncidentId())
                .postmortemId(postmortem.getPostmortemId())
                .title(postmortem.getTitle())
                .summary(postmortem.getSummary())
                .status(postmortem.getStatus().toString())
                .durationMinutes(postmortem.getDurationMinutes())
                .incidentStartTime(postmortem.getIncidentStartTime())
                .incidentEndTime(postmortem.getIncidentEndTime())
                .generatedBy(postmortem.getGeneratedBy())
                .generatedAt(postmortem.getGeneratedAt())
                .createdAt(postmortem.getCreatedAt())
                .build();
    }

    private long calculateDuration(Long createdAt, Long resolvedAt) {
        if (createdAt != null && resolvedAt != null) {
            return (resolvedAt - createdAt) / 60;
        }
        return 0;
    }

    private String extractSection(String content, String sectionName) {
        String[] lines = content.split("\n");
        StringBuilder section = new StringBuilder();
        boolean inSection = false;

        for (String line : lines) {
            if (line.contains(sectionName)) {
                inSection = true;
                continue;
            }
            if (inSection) {
                if (line.startsWith("#") && !line.contains(sectionName)) {
                    break;
                }
                section.append(line).append("\n");
            }
        }

        return section.toString().trim();
    }

    private String calculateMD5(byte[] content) {
        // Simplified MD5 calculation for demo
        return "md5_" + System.currentTimeMillis();
    }
}
