package com.remessa.interfaces.rest.controller;

import com.remessa.interfaces.rest.dto.CreatePessoaFisicaRequest;
import com.remessa.interfaces.rest.dto.CreatePessoaJuridicaRequest;
import com.remessa.interfaces.rest.dto.UserResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MicronautTest
class UserControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void shouldCreateAndFetchPessoaFisica() {
        String email = "pf-" + UUID.randomUUID() + "@example.com";
        CreatePessoaFisicaRequest request = new CreatePessoaFisicaRequest(
                "Ana Silva", email, "secret123", "529.982.247-25");

        UserResponse created = client.toBlocking().retrieve(
                HttpRequest.POST("/api/users/pf", request), UserResponse.class);

        assertThat(created.id()).isNotNull();
        assertThat(created.balanceBrl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.balanceUsd()).isEqualByComparingTo(BigDecimal.ZERO);

        UserResponse fetched = client.toBlocking().retrieve(
                HttpRequest.GET("/api/users/" + created.id()), UserResponse.class);

        assertThat(fetched.email()).isEqualTo(email);
        assertThat(fetched.documentType()).isEqualTo("CPF");
    }

    @Test
    void shouldCreatePessoaJuridica() {
        String email = "pj-" + UUID.randomUUID() + "@example.com";
        CreatePessoaJuridicaRequest request = new CreatePessoaJuridicaRequest(
                "Empresa LTDA", email, "secret123", "11.222.333/0001-81");

        UserResponse created = client.toBlocking().retrieve(
                HttpRequest.POST("/api/users/pj", request), UserResponse.class);

        assertThat(created.documentType()).isEqualTo("CNPJ");
        assertThat(created.documentValue()).isEqualTo("11222333000181");
    }

    @Test
    void shouldReturnConflictWhenEmailIsDuplicated() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        CreatePessoaFisicaRequest request = new CreatePessoaFisicaRequest(
                "Ana Silva", email, "secret123", "111.444.777-35");

        client.toBlocking().retrieve(HttpRequest.POST("/api/users/pf", request), UserResponse.class);

        CreatePessoaFisicaRequest duplicated = new CreatePessoaFisicaRequest(
                "Outra Pessoa", email, "secret123", "529.982.247-25");

        assertThatThrownBy(() -> client.toBlocking()
                .exchange(HttpRequest.POST("/api/users/pf", duplicated), Argument.of(UserResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(exception -> assertThat(((HttpClientResponseException) exception).getStatus().getCode())
                        .isEqualTo(HttpStatus.CONFLICT.getCode()));
    }

    @Test
    void shouldReturnNotFoundForUnknownUser() {
        assertThatThrownBy(() -> client.toBlocking()
                .exchange(HttpRequest.GET("/api/users/" + UUID.randomUUID()), Argument.of(UserResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(exception -> assertThat(((HttpClientResponseException) exception).getStatus().getCode())
                        .isEqualTo(HttpStatus.NOT_FOUND.getCode()));
    }
}
