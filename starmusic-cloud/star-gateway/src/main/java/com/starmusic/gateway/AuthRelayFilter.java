package com.starmusic.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AuthRelayFilter implements GlobalFilter, Ordered {

    private static final String[] IDENTITY_HEADERS = {"X-User-Id", "X-User-Name", "X-User-Role"};

    private final WebClient webClient;
    private final JwtSupport jwt;

    public AuthRelayFilter(WebClient.Builder webClientBuilder, JwtSupport jwt) {
        this.webClient = webClientBuilder.build();
        this.jwt = jwt;
    }

    public record MemberInfo(long id, String username, String role) {
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(h -> {
                    for (String name : IDENTITY_HEADERS) {
                        h.remove(name);
                    }
                })
                .build();
        ServerWebExchange clean = exchange.mutate().request(stripped).build();

        String authorization = stripped.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return chain.filter(clean);
        }

        String token = authorization.substring(7).trim();
        try {
            io.jsonwebtoken.Claims c = jwt.parse(token).getPayload();
            return chain.filter(withIdentity(clean, stripped, c.getSubject(),
                    c.get("username", String.class), c.get("role", String.class)));
        } catch (Exception ignored) {
            // 非 JWT（舊版不透明 token）才回 member-service 查詢
        }

        return webClient.get()
                .uri("http://star-member-service/api/members/me")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(MemberInfo.class)
                .flatMap(m -> chain.filter(withIdentity(clean, stripped,
                        String.valueOf(m.id()), m.username(), m.role())))
                .onErrorResume(e -> chain.filter(clean));
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, ServerHttpRequest base,
                                           String userId, String username, String role) {
        ServerHttpRequest authed = base.mutate()
                .header("X-User-Id", userId == null ? "" : userId)
                .header("X-User-Name", username == null ? ""
                        : URLEncoder.encode(username, StandardCharsets.UTF_8))
                .header("X-User-Role", role == null ? "" : role)
                .build();
        return exchange.mutate().request(authed).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
