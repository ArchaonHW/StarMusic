package com.starmusic.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    List<VideoComment> findByVideoIdOrderByCreatedAtDesc(Long videoId);
}
