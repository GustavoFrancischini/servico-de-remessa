package com.remessa.domain.exception;

/** Exceção base para toda violação de regra de negócio do domínio. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
