package com.opsbrain.contextcollector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** Bounded pool that runs the 5 collectors concurrently per alert. */
@Configuration
public class AsyncConfig {

    @Bean("collectorExecutor")
    public Executor collectorExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(10);
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("collector-");
        ex.initialize();
        return ex;
    }
}