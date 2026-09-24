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

@Component
public class AuthRelayFilter implements GlobalFilter, Ordered {

    private static final String[] IDENTITY_HEADERS = {"X-User-Id", "X-User-Name", "X-User-Role"};

    private final WebClient webClient;

    public AuthRelayFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
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

        return webClient.get()
                .uri("http://star-member-service/api/members/me")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(MemberInfo.class)
                .flatMap(m -> {
                    ServerHttpRequest authed = stripped.mutate()
                            .header("X-User-Id", String.valueOf(m.id()))
                            .header("X-User-Name", m.username() == null ? "" : m.username())
                            .header("X-User-Role", m.role() == null ? "" : m.role())
                            .build();
                    return chain.filter(clean.mutate().request(authed).build());
                })
                .onErrorResume(e -> chain.filter(clean));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
