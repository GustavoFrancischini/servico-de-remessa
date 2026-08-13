package com.remessa.domain.policy;

import com.remessa.domain.model.DocumentType;
import jakarta.inject.Singleton;

import java.math.BigDecimal;

@Singleton
public final class PessoaFisicaDailyLimitPolicy implements DailyLimitPolicy {

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("10000.00");

    @Override
    public boolean appliesTo(DocumentType documentType) {
        return documentType == DocumentType.CPF;
    }

    @Override
    public BigDecimal dailyLimit() {
        return DAILY_LIMIT;
    }
}
