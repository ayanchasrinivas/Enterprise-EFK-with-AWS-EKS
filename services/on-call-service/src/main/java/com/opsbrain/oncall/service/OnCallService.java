package com.opsbrain.oncall.service;

import com.opsbrain.oncall.dto.IncidentNotificationMessage;
import com.opsbrain.oncall.dto.OnCallMemberResponse;
import com.opsbrain.oncall.dto.OnCallResponse;
import com.opsbrain.oncall.entity.OnCallAssignment;
import com.opsbrain.oncall.entity.OnCallRotation;
import com.opsbrain.oncall.entity.OnCallSchedule;
import com.opsbrain.oncall.entity.TeamMember;
import com.opsbrain.oncall.repository.OnCallAssignmentRepository;
import com.opsbrain.oncall.repository.OnCallRotationRepository;
import com.opsbrain.oncall.repository.OnCallScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnCallService {

    private final OnCallScheduleRepository scheduleRepository;
    private final OnCallRotationRepository rotationRepository;
    private final OnCallAssignmentRepository assignmentRepository;
    private final NotificationProducer notificationProducer;

    @Transactional(readOnly = true)
    public OnCallResponse getCurrentOnCall(String service) {
        Optional<OnCallSchedule> schedule = scheduleRepository.findActiveScheduleByService(service);

        if (schedule.isEmpty()) {
            throw new RuntimeException("No active schedule found for service: " + service);
        }

        OnCallSchedule onCallSchedule = schedule.get();
        LocalDate today = LocalDate.now();

        Optional<OnCallRotation> currentRotation = rotationRepository.findCurrentRotation(onCallSchedule.getId(), today);

        if (currentRotation.isEmpty()) {
            throw new RuntimeException("No current on-call rotation found for service: " + service);
        }

        OnCallRotation rotation = currentRotation.get();
        TeamMember member = rotation.getTeamMember();

        return OnCallResponse.builder()
                .scheduleId(onCallSchedule.getScheduleId())
                .service(onCallSchedule.getService())
                .teamName(onCallSchedule.getTeam().getTeamName())
                .currentOnCall(toMemberResponse(member))
                .startDate(rotation.getStartDate())
                .endDate(rotation.getEndDate())
                .build();
    }

    @Transactional
    public IncidentNotificationMessage assignIncidentToOnCall(String incidentId, String service, String severity, String title, String rootCause) {
        log.info("Assigning incident {} to on-call for service: {}", incidentId, service);

        OnCallResponse onCall = getCurrentOnCall(service);
        TeamMember member = getTeamMemberByMemberId(onCall.getCurrentOnCall().getMemberId());

        OnCallSchedule schedule = scheduleRepository.findByService(service)
                .stream()
                .filter(OnCallSchedule::getActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Schedule not found for service: " + service));

        OnCallAssignment assignment = OnCallAssignment.builder()
                .incidentId(incidentId)
                .schedule(schedule)
                .assignedTo(member)
                .severity(severity)
                .assignedAt(LocalDateTime.now())
                .acknowledged(false)
                .build();

        assignment = assignmentRepository.save(assignment);
        log.info("Assigned incident {} to {}", incidentId, member.getName());

        IncidentNotificationMessage notification = IncidentNotificationMessage.builder()
                .incidentId(incidentId)
                .title(title)
                .severity(severity)
                .affectedService(service)
                .rootCause(rootCause)
                .onCallMemberId(member.getMemberId())
                .onCallMemberName(member.getName())
                .onCallMemberEmail(member.getEmail())
                .onCallMemberPhone(member.getPhone())
                .onCallMemberSlackId(member.getSlackUserId())
                .timestamp(System.currentTimeMillis())
                .build();

        notificationProducer.publishIncidentNotification(notification);

        return notification;
    }

    @Transactional
    public void acknowledgeAssignment(String incidentId, String acknowledgedBy) {
        Optional<OnCallAssignment> assignment = assignmentRepository.findByIncidentId(incidentId);

        if (assignment.isEmpty()) {
            throw new RuntimeException("Assignment not found for incident: " + incidentId);
        }

        OnCallAssignment onCallAssignment = assignment.get();
        onCallAssignment.setAcknowledged(true);
        onCallAssignment.setAcknowledgedAt(LocalDateTime.now());
        onCallAssignment.setAcknowledgedBy(acknowledgedBy);

        assignmentRepository.save(onCallAssignment);
        log.info("Incident {} acknowledged by {}", incidentId, acknowledgedBy);
    }

    @Transactional(readOnly = true)
    public OnCallAssignment getAssignmentByIncidentId(String incidentId) {
        return assignmentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found for incident: " + incidentId));
    }

    private OnCallMemberResponse toMemberResponse(TeamMember member) {
        return OnCallMemberResponse.builder()
                .id(member.getId())
                .memberId(member.getMemberId())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .slackUserId(member.getSlackUserId())
                .active(member.getActive())
                .build();
    }

    private TeamMember getTeamMemberByMemberId(String memberId) {
        // For now, we'll find by email (since memberId is returned as ID)
        // This should be refactored to use proper member ID lookup
        return new TeamMember(); // Placeholder - will be implemented with proper lookup
    }
}
