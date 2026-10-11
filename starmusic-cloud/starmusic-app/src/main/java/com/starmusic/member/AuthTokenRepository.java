package com.starmusic.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuthTokenRepository extends JpaRepository<AuthTokenEntity, String> {

    @Modifying
    @Query("delete from AuthTokenEntity t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
