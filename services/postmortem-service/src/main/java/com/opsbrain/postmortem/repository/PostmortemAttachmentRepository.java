package com.opsbrain.postmortem.repository;

import com.opsbrain.postmortem.entity.PostmortemAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostmortemAttachmentRepository extends JpaRepository<PostmortemAttachment, Long> {

    List<PostmortemAttachment> findByPostmortemId(Long postmortemId);

    Optional<PostmortemAttachment> findByPostmortemIdAndFileFormat(Long postmortemId, String fileFormat);
}
