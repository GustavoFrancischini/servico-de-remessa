package com.remessa.domain.exception;

import java.time.LocalDate;

/** Lançada quando não é possível obter a cotação do dólar para uma determinada data. */
public class ExchangeRateUnavailableException extends DomainException {

    public ExchangeRateUnavailableException(LocalDate date) {
        super("Cotação do dólar indisponível para a data: " + date);
    }

    public ExchangeRateUnavailableException(LocalDate date, Throwable cause) {
        super("Cotação do dólar indisponível para a data: " + date);
        initCause(cause);
    }
}
