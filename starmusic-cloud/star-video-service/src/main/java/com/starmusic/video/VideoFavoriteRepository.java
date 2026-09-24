package com.starmusic.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoFavoriteRepository extends JpaRepository<VideoFavorite, Long> {

    Optional<VideoFavorite> findByMemberIdAndVideoId(Long memberId, Long videoId);

    List<VideoFavorite> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
