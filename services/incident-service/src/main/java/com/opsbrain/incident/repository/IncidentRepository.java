package com.opsbrain.incident.repository;

import com.opsbrain.incident.entity.Incident;
import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.model.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByIncidentId(String incidentId);

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findByAffectedService(String affectedService, Pageable pageable);

    Page<Incident> findBySeverity(Severity severity, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE i.status IN :statuses ORDER BY i.severity.priority ASC, i.createdAt DESC")
    Page<Incident> findByStatusIn(@Param("statuses") List<IncidentStatus> statuses, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE i.createdAt >= :startTime AND i.createdAt <= :endTime ORDER BY i.createdAt DESC")
    List<Incident> findByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT i FROM Incident i WHERE i.status = :status AND i.createdAt >= :since ORDER BY i.createdAt DESC")
    List<Incident> findRecentByStatus(@Param("status") IncidentStatus status, @Param("since") LocalDateTime since);
}
