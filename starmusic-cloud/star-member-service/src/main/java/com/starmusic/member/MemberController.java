package com.starmusic.member;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    public record Member(long id, String username, String nickname, String email,
                         String level, String createdAt) {
    }

    public record RegisterRequest(String username, String password, String nickname, String email) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, Member member) {
    }

    private final Map<Long, Member> members = new ConcurrentHashMap<>();
    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(100);

    public MemberController() {
        Member demo = new Member(1, "starfan", "星光粉絲", "fan@starmusic.tv",
                "VIP", Instant.now().toString());
        members.put(1L, demo);
        passwords.put("starfan", "123456");
    }

    @PostMapping("/register")
    public ResponseEntity<Member> register(@RequestBody RegisterRequest req) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "帳號與密碼為必填");
        }
        boolean exists = members.values().stream()
                .anyMatch(m -> m.username().equals(req.username()));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "帳號已存在");
        }
        long id = seq.incrementAndGet();
        Member member = new Member(id, req.username(),
                req.nickname() == null ? req.username() : req.nickname(),
                req.email(), "一般會員", Instant.now().toString());
        members.put(id, member);
        passwords.put(req.username(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        String stored = passwords.get(req.username());
        if (stored == null || !stored.equals(req.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
        }
        Member member = members.values().stream()
                .filter(m -> m.username().equals(req.username()))
                .findFirst()
                .orElseThrow();
        String token = "star-" + UUID.randomUUID();
        return ResponseEntity.ok(new LoginResponse(token, member));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> get(@PathVariable long id) {
        Member member = members.get(id);
        return member == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(member);
    }
}
