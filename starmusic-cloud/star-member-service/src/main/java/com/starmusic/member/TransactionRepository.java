package com.starmusic.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
