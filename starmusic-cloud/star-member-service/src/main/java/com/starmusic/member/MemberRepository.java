package com.starmusic.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MemberEntity m where m.id = :id")
    Optional<MemberEntity> findByIdForUpdate(@Param("id") long id);
}
