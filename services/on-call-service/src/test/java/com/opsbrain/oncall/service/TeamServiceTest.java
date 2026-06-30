package com.opsbrain.oncall.service;

import com.opsbrain.oncall.dto.CreateTeamMemberRequest;
import com.opsbrain.oncall.entity.Team;
import com.opsbrain.oncall.entity.TeamMember;
import com.opsbrain.oncall.repository.TeamMemberRepository;
import com.opsbrain.oncall.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private TeamService teamService;

    private Team testTeam;

    @BeforeEach
    void setUp() {
        testTeam = Team.builder()
                .id(1L)
                .teamName("Platform Team")
                .description("Core platform team")
                .active(true)
                .build();
    }

    @Test
    void testCreateTeam_ShouldCreateAndReturnTeam() {
        when(teamRepository.save(any(Team.class))).thenReturn(testTeam);

        Team result = teamService.createTeam("Platform Team", "Core platform team");

        assertNotNull(result);
        assertEquals("Platform Team", result.getTeamName());
        assertTrue(result.getActive());
    }

    @Test
    void testGetTeamById_ShouldReturnTeam() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));

        Team result = teamService.getTeamById(1L);

        assertNotNull(result);
        assertEquals("Platform Team", result.getTeamName());
    }

    @Test
    void testGetTeamById_ShouldThrowWhenNotFound() {
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> teamService.getTeamById(999L));
    }

    @Test
    void testAddTeamMember_ShouldCreateAndReturnMember() {
        CreateTeamMemberRequest request = new CreateTeamMemberRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("+1234567890");
        request.setSlackUserId("U12345");

        TeamMember member = TeamMember.builder()
                .id(1L)
                .team(testTeam)
                .name("John Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .slackUserId("U12345")
                .active(true)
                .build();

        when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(member);

        TeamMember result = teamService.addTeamMember(1L, request);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
    }
}
