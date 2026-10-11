package com.starmusic.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    Optional<WatchHistory> findByMemberIdAndVideoId(Long memberId, Long videoId);

    List<WatchHistory> findByMemberIdOrderByWatchedAtDesc(Long memberId);
}
