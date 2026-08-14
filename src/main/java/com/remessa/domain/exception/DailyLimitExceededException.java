package com.remessa.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

/** Lançada quando a remessa solicitada ultrapassaria o limite diário de transações do remetente. */
public class DailyLimitExceededException extends DomainException {

    public DailyLimitExceededException(UUID userId, BigDecimal limit, BigDecimal alreadyUsed, BigDecimal requested) {
        super(String.format(
                "Limite diário excedido para o usuário %s: limite=%s, já utilizado=%s, solicitado=%s",
                userId, limit, alreadyUsed, requested));
    }
}
