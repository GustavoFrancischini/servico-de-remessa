package com.remessa.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    private static Wallet walletWith(String brl, String usd) {
        return new Wallet(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal(brl), new BigDecimal(usd), Instant.now());
    }

    // -------------------------------------------------------------------------
    // debitBrl
    // -------------------------------------------------------------------------

    @Test
    void debitBrl_shouldReturnNewWalletWithReducedBrlBalance() {
        Wallet wallet = walletWith("1000.00", "0.00");

        Wallet result = wallet.debitBrl(new BigDecimal("300.00"));

        assertThat(result.balanceBrl()).isEqualByComparingTo("700.00");
        assertThat(result.balanceUsd()).isEqualByComparingTo("0.00");
    }

    @Test
    void debitBrl_shouldNotMutateOriginalWallet() {
        Wallet original = walletWith("1000.00", "0.00");

        original.debitBrl(new BigDecimal("300.00"));

        assertThat(original.balanceBrl()).isEqualByComparingTo("1000.00");
    }

    @Test
    void debitBrl_shouldPreserveOtherFields() {
        Wallet wallet = walletWith("1000.00", "50.00");

        Wallet result = wallet.debitBrl(new BigDecimal("100.00"));

        assertThat(result.id()).isEqualTo(wallet.id());
        assertThat(result.userId()).isEqualTo(wallet.userId());
        assertThat(result.balanceUsd()).isEqualByComparingTo("50.00");
        assertThat(result.createdAt()).isEqualTo(wallet.createdAt());
    }

    @Test
    void debitBrl_shouldAllowDebitOfExactBalance() {
        Wallet wallet = walletWith("500.00", "0.00");

        Wallet result = wallet.debitBrl(new BigDecimal("500.00"));

        assertThat(result.balanceBrl()).isEqualByComparingTo("0.00");
    }

    @Test
    void debitBrl_shouldThrowWhenAmountExceedsBalance() {
        Wallet wallet = walletWith("100.00", "0.00");

        assertThatThrownBy(() -> wallet.debitBrl(new BigDecimal("100.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void debitBrl_shouldThrowWhenAmountIsZero() {
        Wallet wallet = walletWith("100.00", "0.00");

        assertThatThrownBy(() -> wallet.debitBrl(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debitBrl_shouldThrowWhenAmountIsNegative() {
        Wallet wallet = walletWith("100.00", "0.00");

        assertThatThrownBy(() -> wallet.debitBrl(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debitBrl_shouldThrowWhenAmountIsNull() {
        Wallet wallet = walletWith("100.00", "0.00");

        assertThatThrownBy(() -> wallet.debitBrl(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // creditUsd
    // -------------------------------------------------------------------------

    @Test
    void creditUsd_shouldReturnNewWalletWithIncreasedUsdBalance() {
        Wallet wallet = walletWith("1000.00", "10.00");

        Wallet result = wallet.creditUsd(new BigDecimal("5.5000"));

        assertThat(result.balanceUsd()).isEqualByComparingTo("15.5000");
        assertThat(result.balanceBrl()).isEqualByComparingTo("1000.00");
    }

    @Test
    void creditUsd_shouldNotMutateOriginalWallet() {
        Wallet original = walletWith("1000.00", "10.00");

        original.creditUsd(new BigDecimal("5.00"));

        assertThat(original.balanceUsd()).isEqualByComparingTo("10.00");
    }

    @Test
    void creditUsd_shouldPreserveOtherFields() {
        Wallet wallet = walletWith("800.00", "20.00");

        Wallet result = wallet.creditUsd(new BigDecimal("3.00"));

        assertThat(result.id()).isEqualTo(wallet.id());
        assertThat(result.userId()).isEqualTo(wallet.userId());
        assertThat(result.balanceBrl()).isEqualByComparingTo("800.00");
        assertThat(result.createdAt()).isEqualTo(wallet.createdAt());
    }

    @Test
    void creditUsd_shouldThrowWhenAmountIsZero() {
        Wallet wallet = walletWith("0.00", "0.00");

        assertThatThrownBy(() -> wallet.creditUsd(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creditUsd_shouldThrowWhenAmountIsNegative() {
        Wallet wallet = walletWith("0.00", "10.00");

        assertThatThrownBy(() -> wallet.creditUsd(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creditUsd_shouldThrowWhenAmountIsNull() {
        Wallet wallet = walletWith("0.00", "10.00");

        assertThatThrownBy(() -> wallet.creditUsd(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // Composição: debit seguido de credit (simula fluxo de remessa)
    // -------------------------------------------------------------------------

    @Test
    void debitBrlThenCreditUsd_shouldProduceCorrectBalances() {
        Wallet sender = walletWith("2000.00", "0.00");

        Wallet debited = sender.debitBrl(new BigDecimal("1000.00"));

        assertThat(debited.balanceBrl()).isEqualByComparingTo("1000.00");
        assertThat(debited.balanceUsd()).isEqualByComparingTo("0.00");

        Wallet receiver = walletWith("0.00", "5.00");
        Wallet credited = receiver.creditUsd(new BigDecimal("165.8200"));

        assertThat(credited.balanceUsd()).isEqualByComparingTo("170.8200");
        assertThat(credited.balanceBrl()).isEqualByComparingTo("0.00");
    }
}
