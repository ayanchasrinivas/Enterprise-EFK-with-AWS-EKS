package com.opsbrain.oncall.repository;

import com.opsbrain.oncall.entity.EscalationPolicy;
import com.opsbrain.oncall.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationPolicyRepository extends JpaRepository<EscalationPolicy, Long> {

    List<EscalationPolicy> findByScheduleId(Long scheduleId);

    List<EscalationPolicy> findByScheduleIdAndActive(Long scheduleId, Boolean active);

    List<EscalationPolicy> findByScheduleIdAndMinSeverity(Long scheduleId, Severity severity);
}
