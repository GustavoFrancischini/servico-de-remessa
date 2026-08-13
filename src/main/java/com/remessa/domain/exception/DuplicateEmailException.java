package com.remessa.domain.exception;

public class DuplicateEmailException extends DomainException {
    public DuplicateEmailException(String email) {
        super("Já existe um usuário cadastrado com o e-mail: " + email);
    }
}
