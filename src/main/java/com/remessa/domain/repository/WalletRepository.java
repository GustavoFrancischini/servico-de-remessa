package com.remessa.domain.repository;

import com.remessa.domain.model.Wallet;

import java.util.Optional;
import java.util.UUID;

/** Porta (outbound) para persistência de carteiras; a implementação vive na camada de infraestrutura. */
public interface WalletRepository {

    Wallet save(Wallet wallet);

    /**
     * Persiste as alterações de saldo de uma carteira já existente.
     * Deve ser chamado após {@link Wallet#debitBrl} ou {@link Wallet#creditUsd}.
     */
    Wallet update(Wallet wallet);

    Optional<Wallet> findByUserId(UUID userId);
}
