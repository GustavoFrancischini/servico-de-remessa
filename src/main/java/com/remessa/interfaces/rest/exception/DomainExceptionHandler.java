package com.remessa.interfaces.rest.exception;

import com.remessa.domain.exception.DomainException;
import com.remessa.domain.exception.DuplicateDocumentException;
import com.remessa.domain.exception.DuplicateEmailException;
import com.remessa.domain.exception.InvalidDocumentException;
import com.remessa.domain.exception.UserNotFoundException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

/** Traduz exceções de domínio em respostas HTTP consistentes, centralizando o tratamento de erros. */
@Produces
@Singleton
public class DomainExceptionHandler implements ExceptionHandler<DomainException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, DomainException exception) {
        HttpStatus status = statusFor(exception);
        ErrorResponse body = ErrorResponse.of(status, exception.getMessage(), request.getPath());
        return HttpResponse.status(status).body(body);
    }

    private HttpStatus statusFor(DomainException exception) {
        if (exception instanceof DuplicateEmailException || exception instanceof DuplicateDocumentException) {
            return HttpStatus.CONFLICT;
        }
        if (exception instanceof UserNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof InvalidDocumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
