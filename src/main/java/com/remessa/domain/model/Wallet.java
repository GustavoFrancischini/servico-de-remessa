package com.remessa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Carteira de um usuário, com saldos independentes em Real e em Dólar. */
public final class Wallet {

    private final UUID id;
    private final UUID userId;
    private final BigDecimal balanceBrl;
    private final BigDecimal balanceUsd;
    private final Instant createdAt;

    public Wallet(UUID id, UUID userId, BigDecimal balanceBrl, BigDecimal balanceUsd, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.balanceBrl = balanceBrl;
        this.balanceUsd = balanceUsd;
        this.createdAt = createdAt;
    }

    /** Cria a carteira que todo usuário recebe automaticamente no momento do cadastro. */
    public static Wallet zeroBalanceFor(UUID userId) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new Wallet(UUID.randomUUID(), userId, zero, zero, Instant.now());
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public BigDecimal balanceBrl() {
        return balanceBrl;
    }

    public BigDecimal balanceUsd() {
        return balanceUsd;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
