package com.opsbrain.oncall.repository;

import com.opsbrain.oncall.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByMemberId(String memberId);

    Optional<TeamMember> findByEmail(String email);

    List<TeamMember> findByTeamIdAndActive(Long teamId, Boolean active);

    List<TeamMember> findByTeamId(Long teamId);
}
