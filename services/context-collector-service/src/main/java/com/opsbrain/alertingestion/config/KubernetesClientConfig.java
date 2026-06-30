package com.opsbrain.contextcollector.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-cluster config is picked up automatically from the mounted ServiceAccount
 * token (/var/run/secrets/kubernetes.io/serviceaccount). No kubeconfig needed.
 */
@Configuration
public class KubernetesClientConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}