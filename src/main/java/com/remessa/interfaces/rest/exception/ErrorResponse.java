package com.remessa.interfaces.rest.exception;

import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.getCode(), status.getReason(), message, path);
    }
}
