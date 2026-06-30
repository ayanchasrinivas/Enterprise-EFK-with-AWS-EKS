package com.opsbrain.oncall.service;

import com.opsbrain.oncall.dto.CreateTeamMemberRequest;
import com.opsbrain.oncall.dto.OnCallMemberResponse;
import com.opsbrain.oncall.entity.Team;
import com.opsbrain.oncall.entity.TeamMember;
import com.opsbrain.oncall.repository.TeamMemberRepository;
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
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public Team createTeam(String teamName, String description) {
        Team team = Team.builder()
                .teamName(teamName)
                .description(description)
                .active(true)
                .build();

        team = teamRepository.save(team);
        log.info("Created team: {}", teamName);
        return team;
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Team getTeamByName(String teamName) {
        return teamRepository.findByTeamName(teamName)
                .orElseThrow(() -> new RuntimeException("Team not found with name: " + teamName));
    }

    @Transactional
    public TeamMember addTeamMember(Long teamId, CreateTeamMemberRequest request) {
        Team team = getTeamById(teamId);

        TeamMember member = TeamMember.builder()
                .team(team)
                .memberId(UUID.randomUUID().toString())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .slackUserId(request.getSlackUserId())
                .active(true)
                .build();

        member = teamMemberRepository.save(member);
        log.info("Added member {} to team {}", member.getName(), team.getTeamName());
        return member;
    }

    @Transactional(readOnly = true)
    public List<OnCallMemberResponse> getTeamMembers(Long teamId) {
        return teamMemberRepository.findByTeamIdAndActive(teamId, true)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamMember getTeamMemberById(Long memberId) {
        return teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Team member not found with ID: " + memberId));
    }

    @Transactional
    public TeamMember updateTeamMember(Long memberId, CreateTeamMemberRequest request) {
        TeamMember member = getTeamMemberById(memberId);
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setSlackUserId(request.getSlackUserId());
        return teamMemberRepository.save(member);
    }

    @Transactional
    public void removeTeamMember(Long memberId) {
        TeamMember member = getTeamMemberById(memberId);
        member.setActive(false);
        teamMemberRepository.save(member);
        log.info("Removed member: {}", member.getName());
    }

    private OnCallMemberResponse toMemberResponse(TeamMember member) {
        return OnCallMemberResponse.builder()
                .id(member.getId())
                .memberId(member.getMemberId())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .slackUserId(member.getSlackUserId())
                .active(member.getActive())
                .build();
    }
}
