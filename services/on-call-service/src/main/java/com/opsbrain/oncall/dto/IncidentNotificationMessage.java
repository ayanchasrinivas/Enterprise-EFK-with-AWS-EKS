package com.opsbrain.oncall.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentNotificationMessage {

    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("affected_service")
    private String affectedService;

    @JsonProperty("root_cause")
    private String rootCause;

    @JsonProperty("on_call_member_id")
    private String onCallMemberId;

    @JsonProperty("on_call_member_name")
    private String onCallMemberName;

    @JsonProperty("on_call_member_email")
    private String onCallMemberEmail;

    @JsonProperty("on_call_member_phone")
    private String onCallMemberPhone;

    @JsonProperty("on_call_member_slack_id")
    private String onCallMemberSlackId;

    @JsonProperty("timestamp")
    private Long timestamp;
}
