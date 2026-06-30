package com.opsbrain.oncall.repository;

import com.opsbrain.oncall.entity.OnCallRotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnCallRotationRepository extends JpaRepository<OnCallRotation, Long> {

    List<OnCallRotation> findByScheduleId(Long scheduleId);

    @Query("SELECT r FROM OnCallRotation r WHERE r.schedule.id = :scheduleId AND r.startDate <= :date AND r.endDate >= :date")
    Optional<OnCallRotation> findCurrentRotation(@Param("scheduleId") Long scheduleId, @Param("date") LocalDate date);

    @Query("SELECT r FROM OnCallRotation r WHERE r.schedule.id = :scheduleId AND r.startDate >= :startDate ORDER BY r.startDate ASC")
    List<OnCallRotation> findUpcomingRotations(@Param("scheduleId") Long scheduleId, @Param("startDate") LocalDate startDate);

    List<OnCallRotation> findByTeamMemberId(Long teamMemberId);
}
