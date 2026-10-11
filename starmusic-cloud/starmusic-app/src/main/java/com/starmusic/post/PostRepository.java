package com.starmusic.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

    List<PostEntity> findByStatus(String status);

    Page<PostEntity> findByStatus(String status, Pageable pageable);

    Page<PostEntity> findByStatusAndType(String status, String type, Pageable pageable);

    Page<PostEntity> findByMemberId(Long memberId, Pageable pageable);

    long countByMemberIdAndCreatedAtAfter(Long memberId, Instant after);

    Optional<PostEntity> findByFilename(String filename);
}
