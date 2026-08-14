package com.remessa.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro imutável de uma operação de remessa executada com sucesso.
 *
 * <p>Representa a conversão de {@code amountBrl} (debitado do remetente) para
 * {@code amountUsd} (creditado ao destinatário) usando a {@code exchangeRate} do dia.
 *
 * <p>{@code executedAt} é armazenado como {@link LocalDateTime} (sem timezone) para
 * garantir que o "dia da transação" seja sempre o dia local do servidor, sem risco
 * de deslocamento de fuso ao persistir em coluna TIMESTAMP sem timezone.
 */
public final class Transfer {

    private final UUID id;
    private final UUID senderId;
    private final UUID receiverId;
    private final BigDecimal amountBrl;
    private final BigDecimal amountUsd;
    private final BigDecimal exchangeRate;
    private final LocalDateTime executedAt;

    public Transfer(UUID id, UUID senderId, UUID receiverId,
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

    /** Factory method — gera um novo ID e carimba o instante de execução no horário local. */
    public static Transfer create(UUID senderId, UUID receiverId,
                                  BigDecimal amountBrl, BigDecimal amountUsd,
                                  BigDecimal exchangeRate) {
        return new Transfer(UUID.randomUUID(), senderId, receiverId,
                amountBrl, amountUsd, exchangeRate, LocalDateTime.now());
    }

    public UUID id() {
        return id;
    }

    public UUID senderId() {
        return senderId;
    }

    public UUID receiverId() {
        return receiverId;
    }

    public BigDecimal amountBrl() {
        return amountBrl;
    }

    public BigDecimal amountUsd() {
        return amountUsd;
    }

    public BigDecimal exchangeRate() {
        return exchangeRate;
    }

    public LocalDateTime executedAt() {
        return executedAt;
    }
}
