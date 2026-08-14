package com.remessa.interfaces.rest.dto;

import com.remessa.domain.model.Transfer;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** DTO de resposta da operação de remessa. Não expõe entidades de infraestrutura. */
@Serdeable
public record RemessaResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        BigDecimal amountBrl,
        BigDecimal amountUsd,
        BigDecimal exchangeRate,
        LocalDateTime executedAt) {

    public static RemessaResponse from(Transfer transfer) {
        return new RemessaResponse(
                transfer.id(),
                transfer.senderId(),
                transfer.receiverId(),
                transfer.amountBrl(),
                transfer.amountUsd(),
                transfer.exchangeRate(),
                transfer.executedAt());
    }
}
