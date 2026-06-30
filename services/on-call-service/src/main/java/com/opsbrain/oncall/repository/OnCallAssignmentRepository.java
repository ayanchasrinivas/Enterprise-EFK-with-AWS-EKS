package com.opsbrain.oncall.repository;

import com.opsbrain.oncall.entity.OnCallAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnCallAssignmentRepository extends JpaRepository<OnCallAssignment, Long> {

    Optional<OnCallAssignment> findByIncidentId(String incidentId);

    List<OnCallAssignment> findByAssignedToId(Long teamMemberId);

    List<OnCallAssignment> findByAssignedToIdAndAcknowledgedFalse(Long teamMemberId);

    List<OnCallAssignment> findByAssignedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<OnCallAssignment> findByScheduleId(Long scheduleId);
}
