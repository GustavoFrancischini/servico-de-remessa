package com.remessa.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Usuário do sistema, PF ou PJ conforme o {@link DocumentType} do seu {@link Document}. */
public final class User {

    private final UUID id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final Document document;
    private final Instant createdAt;

    public User(UUID id, String fullName, String email, String passwordHash, Document document, Instant createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.document = document;
        this.createdAt = createdAt;
    }

    public static User create(String fullName, String email, String passwordHash, Document document) {
        return new User(UUID.randomUUID(), fullName, email, passwordHash, document, Instant.now());
    }

    public UUID id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Document document() {
        return document;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
