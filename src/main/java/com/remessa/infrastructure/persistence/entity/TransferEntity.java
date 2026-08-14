package com.remessa.infrastructure.persistence.entity;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedEntity("transfers")
public class TransferEntity {

    @Id
    private final UUID id;
    private final UUID senderId;
    private final UUID receiverId;
    private final BigDecimal amountBrl;
    private final BigDecimal amountUsd;
    private final BigDecimal exchangeRate;
    private final LocalDateTime executedAt;

    public TransferEntity(UUID id, UUID senderId, UUID receiverId,
                          BigDecimal amountBrl, BigDecimal amountUsd,
                          BigDecimal exchangeRate, LocalDateTime executedAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amountBrl = amountBrl;
        this.amountUsd = amountUsd;
        this.exchangeRate = exchangeRate;
        this.executedAt = executedAt;
    }

    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getReceiverId() { return receiverId; }
    public BigDecimal getAmountBrl() { return amountBrl; }
    public BigDecimal getAmountUsd() { return amountUsd; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
