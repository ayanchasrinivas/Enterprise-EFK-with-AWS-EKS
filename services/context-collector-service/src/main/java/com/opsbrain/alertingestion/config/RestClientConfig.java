package com.opsbrain.contextcollector.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

/**
 * One RestClient per external system, each pre-authenticated.
 *
 * ES inside the cluster speaks HTTPS with a self-signed CA. For simplicity we
 * support skipping verification (es.insecure-skip-verify=true). In production
 * you'd mount the ES CA and trust it instead — same trade-off the rest of the
 * stack already makes.
 */
@Slf4j
@Configuration
public class RestClientConfig {

    @Value("${opsbrain.elasticsearch.url}")        private String esUrl;
    @Value("${opsbrain.elasticsearch.username}")   private String esUser;
    @Value("${opsbrain.elasticsearch.password}")   private String esPass;
    @Value("${opsbrain.elasticsearch.insecure-skip-verify:true}") private boolean esInsecure;

    @Value("${opsbrain.prometheus.url}")           private String prometheusUrl;
    @Value("${opsbrain.grafana.url}")              private String grafanaUrl;
    @Value("${opsbrain.grafana.token:}")           private String grafanaToken;
    @Value("${opsbrain.argocd.url}")               private String argocdUrl;
    @Value("${opsbrain.argocd.token:}")            private String argocdToken;
    @Value("${opsbrain.argocd.insecure-skip-verify:true}") private boolean argocdInsecure;

    @Bean
    RestClient esRestClient() {
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString((esUser + ":" + esPass).getBytes());
        return RestClient.builder()
                .baseUrl(esUrl)
                .requestFactory(factory(esInsecure))
                .defaultHeader(HttpHeaders.AUTHORIZATION, basic)
                .build();
    }

    @Bean
    RestClient prometheusRestClient() {
        return RestClient.builder()
                .baseUrl(prometheusUrl)
                .requestFactory(factory(false))
                .build();
    }

    @Bean
    RestClient grafanaRestClient() {
        RestClient.Builder b = RestClient.builder()
                .baseUrl(grafanaUrl)
                .requestFactory(factory(false));
        if (!grafanaToken.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + grafanaToken);
        }
        return b.build();
    }

    @Bean
    RestClient argocdRestClient() {
        RestClient.Builder b = RestClient.builder()
                .baseUrl(argocdUrl)
                .requestFactory(factory(argocdInsecure));
        if (!argocdToken.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + argocdToken);
        }
        return b.build();
    }

    /** JDK HttpClient with connect timeout; optionally trusts all certs. */
    private JdkClientHttpRequestFactory factory(boolean insecure) {
        HttpClient.Builder hc = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5));
        if (insecure) {
            hc.sslContext(trustAllSslContext());
        }
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(hc.build());
        f.setReadTimeout(Duration.ofSeconds(8));
        return f;
    }

    private SSLContext trustAllSslContext() {
        try {
            TrustManager[] trustAll = { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            return ctx;
        } catch (Exception e) {
            log.warn("Could not build trust-all SSLContext: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}