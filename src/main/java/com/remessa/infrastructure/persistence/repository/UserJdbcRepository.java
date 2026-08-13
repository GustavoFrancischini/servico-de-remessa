package com.remessa.infrastructure.persistence.repository;

import com.remessa.infrastructure.persistence.entity.UserEntity;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/** Repositório técnico gerado pelo Micronaut Data; usado apenas pelos adapters de infraestrutura. */
@JdbcRepository(dialect = Dialect.H2)
public interface UserJdbcRepository extends CrudRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocumentValue(String documentValue);
}
