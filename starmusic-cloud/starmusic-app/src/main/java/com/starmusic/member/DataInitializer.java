package com.starmusic.member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MemberRepository members;
    private final TransactionRepository transactions;
    private final PasswordEncoder encoder;
    private final Environment env;

    public DataInitializer(MemberRepository members, TransactionRepository transactions,
                           PasswordEncoder encoder, Environment env) {
        this.members = members;
        this.transactions = transactions;
        this.encoder = encoder;
        this.env = env;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!members.existsByUsername("admin")) {
            MemberEntity admin = new MemberEntity();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode(seedPassword("STARMUSIC_ADMIN_PASSWORD", "admin")));
            admin.setNickname("系統管理員");
            admin.setEmail("admin@starmusic.tv");
            admin.setRole("ADMIN");
            admin.setLevel("管理員");
            members.save(admin);
        }

        if (!members.existsByUsername("starfan")) {
            MemberEntity fan = new MemberEntity();
            fan.setUsername("starfan");
            fan.setPassword(encoder.encode(seedPassword("STARMUSIC_DEMO_PASSWORD", "starfan")));
            fan.setNickname("星光粉絲");
            fan.setEmail("fan@starmusic.tv");
            fan.setRole("MEMBER");
            fan.setLevel("VIP");
            fan.setBalance(new BigDecimal("3680.00"));
            fan = members.save(fan);

            seed(fan.getId(), "TOPUP", "5000.00", "5000.00", "開通會員儲值", "SYSTEM");
            seed(fan.getId(), "CONSUME", "-799.00", "4201.00", "購買：乘風2026 成團紀念專輯", "SYSTEM");
            seed(fan.getId(), "CONSUME", "-521.00", "3680.00", "購買：星光娛樂週刊 NO.521", "SYSTEM");
        }
    }

    private String seedPassword(String envKey, String account) {
        String configured = env.getProperty(envKey);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String generated = UUID.randomUUID().toString();
        log.info("[DEV] '{}' 種子帳號密碼（{} 未設定，自動產生）: {}", account, envKey, generated);
        return generated;
    }

    private void seed(Long memberId, String type, String amount, String after, String note, String op) {
        TransactionEntity t = new TransactionEntity();
        t.setMemberId(memberId);
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setBalanceAfter(new BigDecimal(after));
        t.setNote(note);
        t.setOperator(op);
        transactions.save(t);
    }
}
