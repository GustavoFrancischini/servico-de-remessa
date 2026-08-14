package com.remessa.interfaces.rest.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Serdeable
public record CreateRemessaRequest(
        @NotNull UUID senderId,
        @NotNull UUID receiverId,
        @NotNull @DecimalMin(value = "0.01", message = "O valor da remessa deve ser maior que zero") BigDecimal amountBrl) {
}
