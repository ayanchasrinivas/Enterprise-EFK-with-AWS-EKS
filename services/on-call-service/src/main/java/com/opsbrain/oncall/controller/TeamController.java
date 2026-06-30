package com.opsbrain.oncall.controller;

import com.opsbrain.oncall.dto.CreateTeamMemberRequest;
import com.opsbrain.oncall.dto.OnCallMemberResponse;
import com.opsbrain.oncall.entity.Team;
import com.opsbrain.oncall.entity.TeamMember;
import com.opsbrain.oncall.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teams", description = "Team management APIs")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<Team> createTeam(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String description) {
        log.info("Creating team: {}", name);
        Team team = teamService.createTeam(name, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        Team team = teamService.getTeamById(id);
        return ResponseEntity.ok(team);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get team by name")
    public ResponseEntity<Team> getTeamByName(@PathVariable String name) {
        Team team = teamService.getTeamByName(name);
        return ResponseEntity.ok(team);
    }

    @PostMapping("/{teamId}/members")
    @Operation(summary = "Add member to team")
    public ResponseEntity<TeamMember> addTeamMember(
            @PathVariable Long teamId,
            @Valid @RequestBody CreateTeamMemberRequest request) {
        log.info("Adding member {} to team: {}", request.getName(), teamId);
        TeamMember member = teamService.addTeamMember(teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @GetMapping("/{teamId}/members")
    @Operation(summary = "Get team members")
    public ResponseEntity<List<OnCallMemberResponse>> getTeamMembers(@PathVariable Long teamId) {
        List<OnCallMemberResponse> members = teamService.getTeamMembers(teamId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/members/{memberId}")
    @Operation(summary = "Get team member by ID")
    public ResponseEntity<TeamMember> getTeamMemberById(@PathVariable Long memberId) {
        TeamMember member = teamService.getTeamMemberById(memberId);
        return ResponseEntity.ok(member);
    }

    @PutMapping("/members/{memberId}")
    @Operation(summary = "Update team member")
    public ResponseEntity<TeamMember> updateTeamMember(
            @PathVariable Long memberId,
            @Valid @RequestBody CreateTeamMemberRequest request) {
        log.info("Updating team member: {}", memberId);
        TeamMember member = teamService.updateTeamMember(memberId, request);
        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/members/{memberId}")
    @Operation(summary = "Remove team member")
    public ResponseEntity<Void> removeTeamMember(@PathVariable Long memberId) {
        log.info("Removing team member: {}", memberId);
        teamService.removeTeamMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
