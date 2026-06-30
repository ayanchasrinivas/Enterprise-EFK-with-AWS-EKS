package com.opsbrain.contextcollector.service;

import com.opsbrain.contextcollector.collector.*;
import com.opsbrain.contextcollector.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Runs all 5 collectors concurrently. Each has its own timeout and fallback,
 * so a single down/slow source degrades to an empty section rather than
 * failing the whole bundle. Failures are recorded in collectionErrors.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextAggregator {

    private final LogCollector logCollector;
    private final MetricCollector metricCollector;
    private final EventCollector eventCollector;
    private final DeploymentCollector deploymentCollector;
    private final DashboardCollector dashboardCollector;
    private final Executor collectorExecutor;

    @Value("${opsbrain.collect.per-collector-timeout-seconds:8}")
    private long perCollectorTimeout;

    public ContextBundle aggregate(NormalizedAlert alert) {
        List<String> errors = Collections.synchronizedList(new java.util.ArrayList<>());

        var logsF = async(() -> logCollector.collect(alert),
                LogContext.empty(), "elasticsearch", errors);
        var metricsF = async(() -> metricCollector.collect(alert),
                MetricContext.empty(), "prometheus", errors);
        var eventsF = async(() -> eventCollector.collect(alert),
                List.<KubernetesEvent>of(), "kubernetes", errors);
        var deployF = async(() -> deploymentCollector.collect(alert),
                DeploymentContext.empty(alert.getService()), "argocd", errors);
        var dashF = async(() -> dashboardCollector.collect(alert),
                List.<DashboardLink>of(), "grafana", errors);

        // Wait for all (each already bounded by its own timeout)
        CompletableFuture.allOf(logsF, metricsF, eventsF, deployF, dashF).join();

        ContextBundle bundle = ContextBundle.builder()
                .bundleId(UUID.randomUUID().toString())
                .alert(alert)
                .logs(logsF.join())
                .metrics(metricsF.join())
                .events(eventsF.join())
                .deployment(deployF.join())
                .dashboards(dashF.join())
                .collectedAt(Instant.now())
                .collectionErrors(errors)
                .build();

        log.info("Built context bundle {} for alert {} [{}] — errors={}",
                bundle.getBundleId(), alert.getAlertId(), alert.getAlertName(), errors);
        return bundle;
    }

    /** Wrap a collector call: run async, time it out, fall back + record errors. */
    private <T> CompletableFuture<T> async(Supplier<T> task, T fallback,
                                           String name, List<String> errors) {
        return CompletableFuture
                .supplyAsync(task, collectorExecutor)
                .completeOnTimeout(fallback, perCollectorTimeout, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    errors.add(name + ": " + rootMessage(ex));
                    log.warn("Collector '{}' failed: {}", name, rootMessage(ex));
                    return fallback;
                });
    }

    private String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getClass().getSimpleName() + ": " + c.getMessage();
    }
}