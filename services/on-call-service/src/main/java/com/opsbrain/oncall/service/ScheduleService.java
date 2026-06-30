package com.opsbrain.oncall.service;

import com.opsbrain.oncall.dto.CreateScheduleRequest;
import com.opsbrain.oncall.entity.OnCallSchedule;
import com.opsbrain.oncall.entity.Team;
import com.opsbrain.oncall.repository.OnCallScheduleRepository;
import com.opsbrain.oncall.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {

    private final OnCallScheduleRepository scheduleRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public OnCallSchedule createSchedule(Long teamId, CreateScheduleRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + teamId));

        OnCallSchedule schedule = OnCallSchedule.builder()
                .scheduleId(UUID.randomUUID().toString())
                .team(team)
                .service(request.getService())
                .name(request.getName())
                .description(request.getDescription())
                .rotationType(request.getRotationType())
                .rotationLengthDays(request.getRotationLengthDays())
                .active(true)
                .build();

        schedule = scheduleRepository.save(schedule);
        log.info("Created schedule: {} for team: {}", schedule.getName(), team.getTeamName());
        return schedule;
    }

    @Transactional(readOnly = true)
    public List<OnCallSchedule> getSchedulesByTeamId(Long teamId) {
        return scheduleRepository.findByTeamId(teamId);
    }

    @Transactional(readOnly = true)
    public List<OnCallSchedule> getSchedulesByService(String service) {
        return scheduleRepository.findByService(service);
    }

    @Transactional(readOnly = true)
    public OnCallSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public OnCallSchedule getScheduleByScheduleId(String scheduleId) {
        return scheduleRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));
    }

    @Transactional
    public OnCallSchedule updateScheduleStatus(Long id, Boolean active) {
        OnCallSchedule schedule = getScheduleById(id);
        schedule.setActive(active);
        return scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<OnCallSchedule> getActiveSchedules() {
        return scheduleRepository.findByActive(true);
    }
}
