package com.starmusic.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByMemberIdAndPostId(Long memberId, Long postId);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    @Query("select l.postId, count(l) from PostLike l where l.postId in :ids group by l.postId")
    List<Object[]> countByPostIdIn(@Param("ids") List<Long> ids);
}
