package com.remessa.interfaces.rest.dto;

import com.remessa.domain.model.UserAccount;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String documentType,
        String documentValue,
        BigDecimal balanceBrl,
        BigDecimal balanceUsd,
        Instant createdAt) {

    public static UserResponse from(UserAccount account) {
        return new UserResponse(
                account.user().id(),
                account.user().fullName(),
                account.user().email(),
                account.user().document().type().name(),
                account.user().document().value(),
                account.wallet().balanceBrl(),
                account.wallet().balanceUsd(),
                account.user().createdAt());
    }
}
