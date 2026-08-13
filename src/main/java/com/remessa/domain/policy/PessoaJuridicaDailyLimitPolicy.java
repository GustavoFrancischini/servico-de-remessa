package com.remessa.domain.policy;

import com.remessa.domain.model.DocumentType;
import jakarta.inject.Singleton;

import java.math.BigDecimal;

@Singleton
public final class PessoaJuridicaDailyLimitPolicy implements DailyLimitPolicy {

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000.00");

    @Override
    public boolean appliesTo(DocumentType documentType) {
        return documentType == DocumentType.CNPJ;
    }

    @Override
    public BigDecimal dailyLimit() {
        return DAILY_LIMIT;
    }
}
