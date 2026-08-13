package com.remessa.infrastructure.persistence.entity;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@MappedEntity("wallets")
public class WalletEntity {

    @Id
    private final UUID id;
    private final UUID userId;
    private final BigDecimal balanceBrl;
    private final BigDecimal balanceUsd;
    private final Instant createdAt;

    public WalletEntity(UUID id, UUID userId, BigDecimal balanceBrl, BigDecimal balanceUsd, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.balanceBrl = balanceBrl;
        this.balanceUsd = balanceUsd;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getBalanceBrl() {
        return balanceBrl;
    }

    public BigDecimal getBalanceUsd() {
        return balanceUsd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
