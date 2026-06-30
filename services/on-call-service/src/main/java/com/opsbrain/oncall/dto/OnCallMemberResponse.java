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
public class OnCallMemberResponse {

    private Long id;

    @JsonProperty("member_id")
    private String memberId;

    private String name;

    private String email;

    private String phone;

    @JsonProperty("slack_user_id")
    private String slackUserId;

    private Boolean active;
}
