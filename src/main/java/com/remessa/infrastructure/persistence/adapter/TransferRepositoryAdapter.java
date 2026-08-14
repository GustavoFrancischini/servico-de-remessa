package com.remessa.infrastructure.persistence.adapter;

import com.remessa.domain.model.Transfer;
import com.remessa.domain.repository.TransferRepository;
import com.remessa.infrastructure.persistence.entity.TransferEntity;
import com.remessa.infrastructure.persistence.repository.TransferJdbcRepository;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Adapta o repositório técnico do Micronaut Data para a porta de domínio {@link TransferRepository}. */
@Singleton
public class TransferRepositoryAdapter implements TransferRepository {

    private final TransferJdbcRepository jdbcRepository;

    public TransferRepositoryAdapter(TransferJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public Transfer save(Transfer transfer) {
        return toDomain(jdbcRepository.save(toEntity(transfer)));
    }

    @Override
    public BigDecimal sumAmountBrlBySenderAndDate(UUID senderId, LocalDate date) {
        return jdbcRepository.sumAmountBrlBySenderAndDate(senderId, date);
    }

    private TransferEntity toEntity(Transfer transfer) {
        return new TransferEntity(
                transfer.id(),
                transfer.senderId(),
                transfer.receiverId(),
                transfer.amountBrl(),
                transfer.amountUsd(),
                transfer.exchangeRate(),
                transfer.executedAt());
    }

    private Transfer toDomain(TransferEntity entity) {
        return new Transfer(
                entity.getId(),
                entity.getSenderId(),
                entity.getReceiverId(),
                entity.getAmountBrl(),
                entity.getAmountUsd(),
                entity.getExchangeRate(),
                entity.getExecutedAt());
    }
}
