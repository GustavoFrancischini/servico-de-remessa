package com.remessa.domain.policy;

import com.remessa.domain.model.DocumentType;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resolve a política de limite diário aplicável a um tipo de documento.
 * Todas as implementações de {@link DailyLimitPolicy} são injetadas automaticamente pelo Micronaut;
 * adicionar um novo tipo de usuário não exige alterar esta classe.
 */
@Singleton
public class DailyLimitPolicyResolver {

    private final List<DailyLimitPolicy> policies;

    public DailyLimitPolicyResolver(List<DailyLimitPolicy> policies) {
        this.policies = policies;
    }

    public BigDecimal dailyLimitFor(DocumentType documentType) {
        return policies.stream()
                .filter(policy -> policy.appliesTo(documentType))
                .findFirst()
                .map(DailyLimitPolicy::dailyLimit)
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhuma política de limite diário definida para " + documentType));
    }
}
