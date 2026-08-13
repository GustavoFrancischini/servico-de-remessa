package com.remessa.domain.exception;

public class DuplicateDocumentException extends DomainException {
    public DuplicateDocumentException(String documentValue) {
        super("Já existe um usuário cadastrado com o documento: " + documentValue);
    }
}
