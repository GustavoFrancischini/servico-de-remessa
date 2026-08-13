package com.remessa.infrastructure.persistence.adapter;

import com.remessa.domain.model.Document;
import com.remessa.domain.model.DocumentType;
import com.remessa.domain.model.User;
import com.remessa.domain.repository.UserRepository;
import com.remessa.infrastructure.persistence.entity.UserEntity;
import com.remessa.infrastructure.persistence.repository.UserJdbcRepository;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.UUID;

/** Adapta o repositório técnico do Micronaut Data para a porta de domínio {@link UserRepository}. */
@Singleton
public class UserRepositoryAdapter implements UserRepository {

    private final UserJdbcRepository jdbcRepository;

    public UserRepositoryAdapter(UserJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public User save(User user) {
        return toDomain(jdbcRepository.save(toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbcRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbcRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jdbcRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocumentValue(String documentValue) {
        return jdbcRepository.existsByDocumentValue(documentValue);
    }

    private UserEntity toEntity(User user) {
        return new UserEntity(
                user.id(),
                user.fullName(),
                user.email(),
                user.passwordHash(),
                user.document().type().name(),
                user.document().value(),
                user.createdAt());
    }

    private User toDomain(UserEntity entity) {
        Document document = Document.reconstruct(
                DocumentType.valueOf(entity.getDocumentType()), entity.getDocumentValue());
        return new User(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                document,
                entity.getCreatedAt());
    }
}
