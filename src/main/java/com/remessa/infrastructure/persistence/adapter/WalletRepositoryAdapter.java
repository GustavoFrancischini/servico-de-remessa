package com.remessa.infrastructure.persistence.adapter;

import com.remessa.domain.model.Wallet;
import com.remessa.domain.repository.WalletRepository;
import com.remessa.infrastructure.persistence.entity.WalletEntity;
import com.remessa.infrastructure.persistence.repository.WalletJdbcRepository;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.UUID;

/** Adapta o repositório técnico do Micronaut Data para a porta de domínio {@link WalletRepository}. */
@Singleton
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJdbcRepository jdbcRepository;

    public WalletRepositoryAdapter(WalletJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        return toDomain(jdbcRepository.save(toEntity(wallet)));
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        return jdbcRepository.findByUserId(userId).map(this::toDomain);
    }

    private WalletEntity toEntity(Wallet wallet) {
        return new WalletEntity(
                wallet.id(), wallet.userId(), wallet.balanceBrl(), wallet.balanceUsd(), wallet.createdAt());
    }

    private Wallet toDomain(WalletEntity entity) {
        return new Wallet(
                entity.getId(),
                entity.getUserId(),
                entity.getBalanceBrl(),
                entity.getBalanceUsd(),
                entity.getCreatedAt());
    }
}
