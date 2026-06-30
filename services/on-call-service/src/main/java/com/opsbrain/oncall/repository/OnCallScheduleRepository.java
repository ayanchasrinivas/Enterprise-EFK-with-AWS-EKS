package com.opsbrain.oncall.repository;

import com.opsbrain.oncall.entity.OnCallSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnCallScheduleRepository extends JpaRepository<OnCallSchedule, Long> {

    Optional<OnCallSchedule> findByScheduleId(String scheduleId);

    List<OnCallSchedule> findByTeamId(Long teamId);

    List<OnCallSchedule> findByService(String service);

    List<OnCallSchedule> findByActive(Boolean active);

    @Query("SELECT s FROM OnCallSchedule s WHERE s.service = :service AND s.active = true")
    Optional<OnCallSchedule> findActiveScheduleByService(@Param("service") String service);
}
