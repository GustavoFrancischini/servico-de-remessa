package com.remessa.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Carteira de um usuário, com saldos independentes em Real e em Dólar.
 *
 * <p>Wallet é imutável: as operações de saldo retornam uma nova instância
 * com o valor atualizado, preservando o estado anterior para rollback transacional.
 */
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

    /**
     * Retorna uma nova Wallet com {@code amount} debitado do saldo em BRL.
     *
     * @param amount valor a debitar; deve ser positivo e não superior ao saldo atual
     * @throws IllegalArgumentException se {@code amount} for nulo, não-positivo ou maior que o saldo
     */
    public Wallet debitBrl(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor a debitar deve ser positivo");
        }
        if (amount.compareTo(balanceBrl) > 0) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente: saldo=" + balanceBrl + ", débito=" + amount);
        }
        return new Wallet(id, userId, balanceBrl.subtract(amount), balanceUsd, createdAt);
    }

    /**
     * Retorna uma nova Wallet com {@code amount} creditado no saldo em USD.
     *
     * @param amount valor a creditar; deve ser positivo
     * @throws IllegalArgumentException se {@code amount} for nulo ou não-positivo
     */
    public Wallet creditUsd(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor a creditar deve ser positivo");
        }
        return new Wallet(id, userId, balanceBrl, balanceUsd.add(amount), createdAt);
    }

    /**
     * Retorna uma nova Wallet com {@code amount} creditado no saldo em BRL.
     *
     * @param amount valor a creditar; deve ser positivo
     * @throws IllegalArgumentException se {@code amount} for nulo ou não-positivo
     */
    public Wallet creditBrl(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor a creditar deve ser positivo");
        }
        return new Wallet(id, userId, balanceBrl.add(amount), balanceUsd, createdAt);
    }
}
