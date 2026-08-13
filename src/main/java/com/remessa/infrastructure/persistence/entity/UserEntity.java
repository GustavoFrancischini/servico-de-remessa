package com.remessa.infrastructure.persistence.entity;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;

import java.time.Instant;
import java.util.UUID;

@MappedEntity("users")
public class UserEntity {

    @Id
    private final UUID id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final String documentType;
    private final String documentValue;
    private final Instant createdAt;

    public UserEntity(UUID id, String fullName, String email, String passwordHash,
                       String documentType, String documentValue, Instant createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.documentType = documentType;
        this.documentValue = documentValue;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentValue() {
        return documentValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
