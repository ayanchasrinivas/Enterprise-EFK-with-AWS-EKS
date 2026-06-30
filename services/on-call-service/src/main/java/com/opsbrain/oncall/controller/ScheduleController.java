package com.opsbrain.oncall.controller;

import com.opsbrain.oncall.dto.CreateScheduleRequest;
import com.opsbrain.oncall.entity.OnCallSchedule;
import com.opsbrain.oncall.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Schedules", description = "On-call schedule management APIs")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @Operation(summary = "Create a new on-call schedule")
    public ResponseEntity<OnCallSchedule> createSchedule(
            @RequestParam Long teamId,
            @Valid @RequestBody CreateScheduleRequest request) {
        log.info("Creating schedule for team: {}", teamId);
        OnCallSchedule schedule = scheduleService.createSchedule(teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedule);
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get schedules by team")
    public ResponseEntity<List<OnCallSchedule>> getSchedulesByTeam(@PathVariable Long teamId) {
        List<OnCallSchedule> schedules = scheduleService.getSchedulesByTeamId(teamId);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/service/{service}")
    @Operation(summary = "Get schedules by service")
    public ResponseEntity<List<OnCallSchedule>> getSchedulesByService(@PathVariable String service) {
        List<OnCallSchedule> schedules = scheduleService.getSchedulesByService(service);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<OnCallSchedule> getScheduleById(@PathVariable Long id) {
        OnCallSchedule schedule = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/id/{scheduleId}")
    @Operation(summary = "Get schedule by schedule ID")
    public ResponseEntity<OnCallSchedule> getScheduleByScheduleId(@PathVariable String scheduleId) {
        OnCallSchedule schedule = scheduleService.getScheduleByScheduleId(scheduleId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active schedules")
    public ResponseEntity<List<OnCallSchedule>> getActiveSchedules() {
        List<OnCallSchedule> schedules = scheduleService.getActiveSchedules();
        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update schedule status")
    public ResponseEntity<OnCallSchedule> updateScheduleStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        OnCallSchedule schedule = scheduleService.updateScheduleStatus(id, active);
        return ResponseEntity.ok(schedule);
    }
}
