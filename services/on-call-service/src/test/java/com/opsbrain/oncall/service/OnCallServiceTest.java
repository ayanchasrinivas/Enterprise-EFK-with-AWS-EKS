package com.opsbrain.oncall.service;

import com.opsbrain.oncall.entity.OnCallRotation;
import com.opsbrain.oncall.entity.OnCallSchedule;
import com.opsbrain.oncall.entity.Team;
import com.opsbrain.oncall.entity.TeamMember;
import com.opsbrain.oncall.repository.OnCallAssignmentRepository;
import com.opsbrain.oncall.repository.OnCallRotationRepository;
import com.opsbrain.oncall.repository.OnCallScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnCallServiceTest {

    @Mock
    private OnCallScheduleRepository scheduleRepository;

    @Mock
    private OnCallRotationRepository rotationRepository;

    @Mock
    private OnCallAssignmentRepository assignmentRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private OnCallService onCallService;

    private OnCallSchedule testSchedule;
    private OnCallRotation testRotation;
    private TeamMember testMember;

    @BeforeEach
    void setUp() {
        Team team = Team.builder()
                .id(1L)
                .teamName("Platform Team")
                .active(true)
                .build();

        testSchedule = OnCallSchedule.builder()
                .id(1L)
                .scheduleId("schedule-1")
                .team(team)
                .service("api-gateway")
                .name("API Gateway Schedule")
                .rotationType("WEEKLY")
                .rotationLengthDays(7)
                .active(true)
                .build();

        testMember = TeamMember.builder()
                .id(1L)
                .memberId("member-1")
                .name("Alice Smith")
                .email("alice@example.com")
                .phone("+1234567890")
                .slackUserId("U12345")
                .active(true)
                .build();

        testRotation = OnCallRotation.builder()
                .id(1L)
                .schedule(testSchedule)
                .teamMember(testMember)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .sequenceOrder(1)
                .build();
    }

    @Test
    void testGetCurrentOnCall_ShouldReturnCurrentMember() {
        when(scheduleRepository.findActiveScheduleByService("api-gateway"))
                .thenReturn(Optional.of(testSchedule));
        when(rotationRepository.findCurrentRotation(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(testRotation));

        var result = onCallService.getCurrentOnCall("api-gateway");

        assertNotNull(result);
        assertEquals("api-gateway", result.getService());
        assertEquals("Alice Smith", result.getCurrentOnCall().getName());
    }

    @Test
    void testGetCurrentOnCall_ShouldThrowWhenNoSchedule() {
        when(scheduleRepository.findActiveScheduleByService("unknown-service"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> onCallService.getCurrentOnCall("unknown-service"));
    }

    @Test
    void testGetCurrentOnCall_ShouldThrowWhenNoRotation() {
        when(scheduleRepository.findActiveScheduleByService("api-gateway"))
                .thenReturn(Optional.of(testSchedule));
        when(rotationRepository.findCurrentRotation(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> onCallService.getCurrentOnCall("api-gateway"));
    }
}
