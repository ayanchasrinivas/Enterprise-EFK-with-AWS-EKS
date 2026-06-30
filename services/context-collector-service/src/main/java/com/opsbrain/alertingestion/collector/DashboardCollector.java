package com.opsbrain.contextcollector.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.opsbrain.contextcollector.model.DashboardLink;
import com.opsbrain.contextcollector.model.NormalizedAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches Grafana for dashboards matching the service name and returns deep
 * links. We don't scrape panel data (Prometheus already gives us metrics) —
 * these links are for the human engineer paged by the notification service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardCollector {

    private final RestClient grafanaRestClient;

    @Value("${opsbrain.grafana.url}")
    private String grafanaBaseUrl;

    public List<DashboardLink> collect(NormalizedAlert alert) {
        List<DashboardLink> links = new ArrayList<>();
        String term = alert.getService() != null ? alert.getService() : alert.getAlertName();
        if (term == null) return links;

        try {
            String uri = UriComponentsBuilder.fromPath("/api/search")
                    .queryParam("query", term)
                    .queryParam("limit", 5)
                    .build().toUriString();

            JsonNode resp = grafanaRestClient.get().uri(uri)
                    .retrieve().body(JsonNode.class);

            if (resp != null && resp.isArray()) {
                for (JsonNode d : resp) {
                    links.add(DashboardLink.builder()
                            .title(d.path("title").asText())
                            .url(grafanaBaseUrl + d.path("url").asText())
                            .build());
                }
            }
        } catch (Exception e) {
            log.debug("Grafana search failed: {}", e.getMessage());
        }
        return links;
    }
}