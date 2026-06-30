package com.opsbrain.oncall.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnCallResponse {

    @JsonProperty("schedule_id")
    private String scheduleId;

    @JsonProperty("service")
    private String service;

    @JsonProperty("team_name")
    private String teamName;

    @JsonProperty("current_on_call")
    private OnCallMemberResponse currentOnCall;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;
}
