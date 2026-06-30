package com.opsbrain.postmortem.repository;

import com.opsbrain.postmortem.entity.Postmortem;
import com.opsbrain.postmortem.model.PostmortemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostmortemRepository extends JpaRepository<Postmortem, Long> {

    Optional<Postmortem> findByIncidentId(String incidentId);

    Optional<Postmortem> findByPostmortemId(String postmortemId);

    List<Postmortem> findByStatus(PostmortemStatus status);

    @Query("SELECT p FROM Postmortem p WHERE p.createdAt BETWEEN :startTime AND :endTime ORDER BY p.createdAt DESC")
    List<Postmortem> findByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT p FROM Postmortem p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Postmortem> findByStatusOrderByCreatedAtDesc(@Param("status") PostmortemStatus status);

    List<Postmortem> findByGeneratedBy(String generatedBy);
}
