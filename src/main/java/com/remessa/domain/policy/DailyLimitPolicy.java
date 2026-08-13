package com.remessa.domain.policy;

import com.remessa.domain.model.DocumentType;

import java.math.BigDecimal;

/**
 * Estratégia que define o limite diário transacionado de um tipo de usuário.
 * Novas categorias de usuário podem ser adicionadas implementando esta interface,
 * sem alterar o código que já depende dela (Open/Closed Principle).
 */
public sealed interface DailyLimitPolicy permits PessoaFisicaDailyLimitPolicy, PessoaJuridicaDailyLimitPolicy {

    boolean appliesTo(DocumentType documentType);

    BigDecimal dailyLimit();
}
