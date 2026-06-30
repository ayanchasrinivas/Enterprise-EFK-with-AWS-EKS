package com.opsbrain.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;

    @JsonProperty("incident_id")
    private String incidentId;

    private String channel;

    private String recipient;

    private String status;

    @JsonProperty("retry_count")
    private Integer retryCount;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("sent_at")
    private LocalDateTime sentAt;
}
