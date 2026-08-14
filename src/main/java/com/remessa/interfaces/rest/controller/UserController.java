package com.remessa.interfaces.rest.controller;

import com.remessa.application.service.UserService;
import com.remessa.domain.model.UserAccount;
import com.remessa.interfaces.rest.dto.CreatePessoaFisicaRequest;
import com.remessa.interfaces.rest.dto.CreatePessoaJuridicaRequest;
import com.remessa.interfaces.rest.dto.CreditWalletRequest;
import com.remessa.interfaces.rest.dto.UserResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import jakarta.validation.Valid;

import java.util.UUID;

@Controller("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Post("/pf")
    @Status(HttpStatus.CREATED)
    public UserResponse createPessoaFisica(@Valid @Body CreatePessoaFisicaRequest request) {
        UserAccount account = userService.createPessoaFisica(
                request.fullName(), request.email(), request.password(), request.cpf());
        return UserResponse.from(account);
    }

    @Post("/pj")
    @Status(HttpStatus.CREATED)
    public UserResponse createPessoaJuridica(@Valid @Body CreatePessoaJuridicaRequest request) {
        UserAccount account = userService.createPessoaJuridica(
                request.fullName(), request.email(), request.password(), request.cnpj());
        return UserResponse.from(account);
    }

    @Get("/{id}")
    public UserResponse getById(UUID id) {
        return UserResponse.from(userService.getAccount(id));
    }

    /** Crédito direto de saldo, sem validação de origem — uso apenas para preparar dados de teste. */
    @Post("/{id}/credit")
    public UserResponse credit(UUID id, @Body CreditWalletRequest request) {
        UserAccount account = userService.creditWallet(id, request.amountBrl(), request.amountUsd());
        return UserResponse.from(account);
    }
}
