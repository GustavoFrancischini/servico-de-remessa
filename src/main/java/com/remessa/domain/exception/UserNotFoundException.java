package com.remessa.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(UUID id) {
        super("Usuário não encontrado: " + id);
    }
}
