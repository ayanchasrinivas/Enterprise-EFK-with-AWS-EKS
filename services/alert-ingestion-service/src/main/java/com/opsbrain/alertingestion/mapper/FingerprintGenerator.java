package com.opsbrain.alertingestion.mapper;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds a stable dedup key. Same logical problem → same fingerprint,
 * so incident-service can group repeated firings into one incident.
 */
@Component
public class FingerprintGenerator {

    public String generate(String source, String alertName, String service,
                           Map<String, String> labels) {
        StringBuilder sb = new StringBuilder()
                .append(source).append('|')
                .append(alertName).append('|')
                .append(service == null ? "" : service);

        // Sort labels for determinism — HashMap order is not guaranteed
        if (labels != null) {
            for (var e : new TreeMap<>(labels).entrySet()) {
                sb.append('|').append(e.getKey()).append('=').append(e.getValue());
            }
        }
        return sha256(sb.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.substring(0, 32); // 128-bit prefix is plenty
        } catch (Exception e) {
            // SHA-256 always available; fall back to hashCode if not
            return Integer.toHexString(input.hashCode());
        }
    }
}