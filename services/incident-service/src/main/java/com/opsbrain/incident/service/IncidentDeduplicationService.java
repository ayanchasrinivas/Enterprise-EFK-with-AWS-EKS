package com.opsbrain.incident.service;

import com.opsbrain.incident.dto.AnalysisMessage;
import com.opsbrain.incident.entity.Incident;
import com.opsbrain.incident.model.IncidentStatus;
import com.opsbrain.incident.model.Severity;
import com.opsbrain.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentDeduplicationService {

    private final IncidentRepository incidentRepository;

    private static final long DEDUP_WINDOW_MINUTES = 60;

    @Transactional(readOnly = true)
    public Optional<Incident> findMatchingIncident(AnalysisMessage analysis) {
        log.debug("Searching for matching incident for analysis: {}", analysis.getAnalysisId());

        LocalDateTime since = LocalDateTime.now().minusMinutes(DEDUP_WINDOW_MINUTES);

        List<Incident> recentIncidents = incidentRepository.findRecentByStatus(
                IncidentStatus.OPEN,
                since
        );

        return recentIncidents.stream()
                .filter(incident -> isSimilar(incident, analysis))
                .findFirst()
                .peek(match -> log.info("Found matching incident: {} for analysis: {}",
                        match.getIncidentId(), analysis.getAnalysisId()));
    }

    private boolean isSimilar(Incident incident, AnalysisMessage analysis) {
        // Match by affected service and severity
        if (!isSameService(incident.getAffectedService(), analysis.getAffectedService())) {
            return false;
        }

        if (!isSameSeverity(incident.getSeverity(), Severity.valueOf(analysis.getSeverity().toUpperCase()))) {
            return false;
        }

        // Match by alert name similarity
        if (!isAlertNameSimilar(incident.getTitle(), analysis.getAlertName())) {
            return false;
        }

        return true;
    }

    private boolean isSameService(String service1, String service2) {
        if (service1 == null || service2 == null) {
            return true;
        }
        return service1.equalsIgnoreCase(service2);
    }

    private boolean isSameSeverity(Severity severity1, Severity severity2) {
        if (severity1 == null || severity2 == null) {
            return true;
        }
        return severity1.getPriority() <= severity2.getPriority() + 1;
    }

    private boolean isAlertNameSimilar(String title, String alertName) {
        if (title == null || alertName == null) {
            return false;
        }

        String normalizedTitle = normalizeString(title);
        String normalizedAlert = normalizeString(alertName);

        return normalizedTitle.contains(normalizedAlert) ||
               normalizedAlert.contains(normalizedTitle) ||
               calculateSimilarity(normalizedTitle, normalizedAlert) > 0.7;
    }

    private String normalizeString(String str) {
        return str.toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private double calculateSimilarity(String str1, String str2) {
        int maxLen = Math.max(str1.length(), str2.length());
        if (maxLen == 0) return 1.0;

        int distance = levenshteinDistance(str1, str2);
        return 1.0 - (double) distance / maxLen;
    }

    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[str1.length()][str2.length()];
    }
}
