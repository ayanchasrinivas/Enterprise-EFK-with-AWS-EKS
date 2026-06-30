package com.opsbrain.oncall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduleRequest {

    @NotBlank(message = "Service name is required")
    private String service;

    @NotBlank(message = "Schedule name is required")
    private String name;

    private String description;

    @NotNull(message = "Rotation type is required (WEEKLY, BIWEEKLY, MONTHLY)")
    private String rotationType;

    @NotNull(message = "Rotation length in days is required")
    private Integer rotationLengthDays;
}
