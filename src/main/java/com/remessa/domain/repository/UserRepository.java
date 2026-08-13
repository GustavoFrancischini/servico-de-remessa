package com.remessa.domain.repository;

import com.remessa.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/** Porta (outbound) para persistência de usuários; a implementação vive na camada de infraestrutura. */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocumentValue(String documentValue);
}
