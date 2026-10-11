package com.starmusic.config;

import com.starmusic.member.AuthTokenEntity;
import com.starmusic.member.AuthTokenRepository;
import com.starmusic.member.JwtSupport;
import com.starmusic.member.MemberEntity;
import com.starmusic.member.MemberRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 單體版的 AuthRelayFilter：驗證 Bearer token（JWT 或舊版不透明 token），
 * 把身分注入 X-User-* 標頭給下游 controller；同時剝除用戶端自帶的 X-User-* 偽造標頭。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthHeaderFilter extends OncePerRequestFilter {

    private static final Set<String> IDENTITY_HEADERS =
            Set.of("x-user-id", "x-user-name", "x-user-role");

    private final JwtSupport jwt;
    private final AuthTokenRepository tokens;
    private final MemberRepository members;

    public AuthHeaderFilter(JwtSupport jwt, AuthTokenRepository tokens, MemberRepository members) {
        this.jwt = jwt;
        this.tokens = tokens;
        this.members = members;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        Map<String, String> identity = resolveIdentity(request.getHeader(HttpHeaders.AUTHORIZATION));
        chain.doFilter(new IdentityRequest(request, identity), response);
    }

    private Map<String, String> resolveIdentity(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Map.of();
        }
        String token = authorization.substring(7).trim();
        try {
            Claims c = jwt.parse(token).getPayload();
            return identity(c.getSubject(), c.get("username", String.class),
                    c.get("role", String.class));
        } catch (Exception ignored) {
            // 非 JWT：舊版不透明 token，查 auth_tokens 表
        }
        return tokens.findById(token)
                .flatMap(t -> members.findById(t.getMemberId()))
                .map(m -> identity(String.valueOf(m.getId()), m.getUsername(), m.getRole()))
                .orElse(Map.of());
    }

    private Map<String, String> identity(String id, String username, String role) {
        Map<String, String> m = new HashMap<>();
        m.put("x-user-id", id == null ? "" : id);
        m.put("x-user-name", username == null ? ""
                : URLEncoder.encode(username, StandardCharsets.UTF_8));
        m.put("x-user-role", role == null ? "" : role);
        return m;
    }

    /** 覆寫身分標頭：有登入就注入，沒登入一律擋掉（防偽造）。 */
    static class IdentityRequest extends HttpServletRequestWrapper {

        private final Map<String, String> identity;

        IdentityRequest(HttpServletRequest request, Map<String, String> identity) {
            super(request);
            this.identity = identity;
        }

        @Override
        public String getHeader(String name) {
            String key = name.toLowerCase();
            if (IDENTITY_HEADERS.contains(key)) {
                return identity.get(key);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = getHeader(name);
            if (IDENTITY_HEADERS.contains(name.toLowerCase())) {
                return Collections.enumeration(value == null ? Set.of() : Set.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>();
            Enumeration<String> e = super.getHeaderNames();
            while (e.hasMoreElements()) {
                String n = e.nextElement();
                if (!IDENTITY_HEADERS.contains(n.toLowerCase())) {
                    names.add(n);
                }
            }
            names.addAll(identity.keySet());
            return Collections.enumeration(names);
        }
    }
}
