package com.remessa.domain.security;

/** Porta (outbound) para hashing de senhas; a implementação concreta vive na infraestrutura. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
