package com.remessa.domain.repository;

import com.remessa.domain.model.Wallet;

import java.util.Optional;
import java.util.UUID;

/** Porta (outbound) para persistência de carteiras; a implementação vive na camada de infraestrutura. */
public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findByUserId(UUID userId);
}
