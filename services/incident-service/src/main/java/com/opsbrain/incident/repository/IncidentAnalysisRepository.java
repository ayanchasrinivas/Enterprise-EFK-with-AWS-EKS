package com.opsbrain.incident.repository;

import com.opsbrain.incident.entity.IncidentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysis, Long> {

    Optional<IncidentAnalysis> findByAnalysisId(String analysisId);

    List<IncidentAnalysis> findByIncidentId(Long incidentId);
}
