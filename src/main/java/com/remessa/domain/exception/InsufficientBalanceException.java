package com.remessa.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

/** Lançada quando o remetente não possui saldo BRL suficiente para executar a remessa. */
public class InsufficientBalanceException extends DomainException {

    public InsufficientBalanceException(UUID userId, BigDecimal required, BigDecimal available) {
        super(String.format(
                "Saldo insuficiente para o usuário %s: necessário=%s, disponível=%s",
                userId, required, available));
    }
}
