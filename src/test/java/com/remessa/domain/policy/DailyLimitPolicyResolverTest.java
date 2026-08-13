package com.remessa.domain.policy;

import com.remessa.domain.model.DocumentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyLimitPolicyResolverTest {

    private final DailyLimitPolicyResolver resolver = new DailyLimitPolicyResolver(
            List.of(new PessoaFisicaDailyLimitPolicy(), new PessoaJuridicaDailyLimitPolicy()));

    @Test
    void shouldResolveTenThousandLimitForCpf() {
        assertThat(resolver.dailyLimitFor(DocumentType.CPF)).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void shouldResolveFiftyThousandLimitForCnpj() {
        assertThat(resolver.dailyLimitFor(DocumentType.CNPJ)).isEqualByComparingTo(new BigDecimal("50000.00"));
    }
}
