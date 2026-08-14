package com.remessa.interfaces.rest.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

/** Ambos os campos são opcionais; informe pelo menos um valor positivo para creditar. */
@Serdeable
public record CreditWalletRequest(BigDecimal amountBrl, BigDecimal amountUsd) {
}
