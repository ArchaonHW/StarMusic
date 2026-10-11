package com.starmusic.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private static final Duration TOKEN_TTL = Duration.ofDays(7);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    public record MemberDto(long id, String username, String nickname, String email,
                            String role, String level, BigDecimal balance, String createdAt) {
        static MemberDto of(MemberEntity e) {
            return new MemberDto(e.getId(), e.getUsername(), e.getNickname(), e.getEmail(),
                    e.getRole(), e.getLevel(), e.getBalance(), e.getCreatedAt().toString());
        }
    }

    public record TransactionDto(long id, String type, BigDecimal amount, BigDecimal balanceAfter,
                                 String note, String operator, String createdAt) {
        static TransactionDto of(TransactionEntity e) {
            return new TransactionDto(e.getId(), e.getType(), e.getAmount(), e.getBalanceAfter(),
                    e.getNote(), e.getOperator(), e.getCreatedAt().toString());
        }
    }

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 32) String username,
                                  @NotBlank @Size(min = 6, max = 72) String password,
                                  @Size(max = 50) String nickname,
                                  @Email @Size(max = 120) String email) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token, MemberDto member) {
    }

    public record TransactionRequest(@NotBlank String type,
                                     @NotNull BigDecimal amount,
                                     @Size(max = 200) String note) {
    }

    private final MemberRepository members;
    private final TransactionRepository transactions;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final JwtSupport jwt;
    private final LoginThrottle throttle = new LoginThrottle();

    public MemberController(MemberRepository members, TransactionRepository transactions,
                            AuthTokenRepository tokens, PasswordEncoder encoder,
                            JwtSupport jwt) {
        this.members = members;
        this.transactions = transactions;
        this.tokens = tokens;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<MemberDto> register(@Valid @RequestBody RegisterRequest req) {
        MemberEntity m = new MemberEntity();
        m.setUsername(req.username().trim());
        m.setPassword(encoder.encode(req.password()));
        m.setNickname(req.nickname() == null || req.nickname().isBlank()
                ? req.username().trim() : req.nickname().trim());
        m.setEmail(req.email());
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(MemberDto.of(members.save(m)));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "帳號已存在");
        }
    }

    @PostMapping("/login")
    @Transactional
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        throttle.check(req.username());
        MemberEntity m = members.findByUsername(req.username())
                .filter(e -> encoder.matches(req.password(), e.getPassword()))
                .orElseThrow(() -> {
                    throttle.fail(req.username());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
                });
        throttle.ok(req.username());
        return new LoginResponse(
                jwt.issue(m.getId(), m.getUsername(), m.getRole(), TOKEN_TTL),
                MemberDto.of(m));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = bearerToken(authorization);
        if (token != null) {
            tokens.deleteById(token);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MemberDto me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return MemberDto.of(authenticate(authorization));
    }

    @GetMapping("/{id}")
    public MemberDto get(@PathVariable long id,
                         @RequestHeader(value = "X-User-Id", required = false) Long userId,
                         @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireSelfOrAdmin(id, userId, role);
        return MemberDto.of(find(id));
    }

    @GetMapping
    public List<MemberDto> listAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return members.findAll().stream().map(MemberDto::of).toList();
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionDto> transactions(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireSelfOrAdmin(id, userId, role);
        return transactions.findByMemberIdOrderByCreatedAtDesc(id).stream()
                .map(TransactionDto::of).toList();
    }

    @PostMapping("/{id}/transactions")
    @Transactional
    public TransactionDto addTransaction(
            @PathVariable long id,
            @Valid @RequestBody TransactionRequest req,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Name", required = false) String operator) {
        requireAdmin(role);
        validateAmount(req.type(), req.amount());

        MemberEntity m = members.findByIdForUpdate(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "會員不存在"));
        BigDecimal after = m.getBalance().add(req.amount());
        if (after.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "餘額不足");
        }
        m.setBalance(after);
        members.save(m);

        TransactionEntity t = new TransactionEntity();
        t.setMemberId(id);
        t.setType(req.type());
        t.setAmount(req.amount());
        t.setBalanceAfter(after);
        t.setNote(req.note());
        String op = operator == null ? null
                : java.net.URLDecoder.decode(operator, java.nio.charset.StandardCharsets.UTF_8);
        t.setOperator(op == null || op.isBlank() ? "ADMIN" : op);
        return TransactionDto.of(transactions.save(t));
    }

    private void validateAmount(String type, BigDecimal amount) {
        int sign = amount.signum();
        boolean ok = switch (type) {
            case "TOPUP", "REFUND" -> sign > 0;
            case "CONSUME" -> sign < 0;
            case "ADJUST" -> sign != 0;
            default -> false;
        };
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效的交易類型或金額方向");
        }
        if (amount.abs().compareTo(MAX_AMOUNT) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "金額超出上限");
        }
    }

    private MemberEntity authenticate(String authorization) {
        String token = bearerToken(authorization);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登入");
        }
        Long memberId = jwtMemberId(token);
        if (memberId != null) {
            return find(memberId);
        }
        // 相容舊版不透明 token（star-*，存於 auth_tokens 表）
        AuthTokenEntity t = tokens.findById(token).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登入已失效"));
        if (t.getExpiresAt().isBefore(Instant.now())) {
            tokens.delete(t);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登入已過期");
        }
        return find(t.getMemberId());
    }

    private Long jwtMemberId(String token) {
        try {
            return Long.valueOf(jwt.parse(token).getPayload().getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private MemberEntity find(long id) {
        return members.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "會員不存在"));
    }

    private void requireSelfOrAdmin(long id, Long userId, String role) {
        if (!"ADMIN".equals(role) && (userId == null || userId != id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能查看自己的資料");
        }
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }

    static class LoginThrottle {

        private static final int MAX_ATTEMPTS = 5;
        private static final Duration WINDOW = Duration.ofMinutes(5);

        private final Map<String, List<Instant>> failures = new ConcurrentHashMap<>();

        void check(String key) {
            List<Instant> list = failures.get(key);
            if (list != null && list.size() >= MAX_ATTEMPTS) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS, "嘗試次數過多，請稍後再試");
            }
        }

        void fail(String key) {
            Instant cutoff = Instant.now().minus(WINDOW);
            failures.compute(key, (k, list) -> {
                List<Instant> l = list == null ? new CopyOnWriteArrayList<>() : list;
                l.removeIf(t -> t.isBefore(cutoff));
                l.add(Instant.now());
                return l;
            });
        }

        void ok(String key) {
            failures.remove(key);
        }
    }
}
