package com.starmusic.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_tokens")
public class AuthTokenEntity {

    @Id
    private String token;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Instant expiresAt;

    protected AuthTokenEntity() {
    }

    public AuthTokenEntity(String token, Long memberId, Instant expiresAt) {
        this.token = token;
        this.memberId = memberId;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
