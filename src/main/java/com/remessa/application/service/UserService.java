package com.remessa.application.service;

import com.remessa.domain.model.UserAccount;

import java.math.BigDecimal;
import java.util.UUID;

/** Caso de uso de gerenciamento de usuários (porta inbound consumida pelos controllers). */
public interface UserService {

    UserAccount createPessoaFisica(String fullName, String email, String rawPassword, String cpf);

    UserAccount createPessoaJuridica(String fullName, String email, String rawPassword, String cnpj);

    UserAccount getAccount(UUID id);

    /** Credita saldo diretamente na carteira do usuário; {@code amountBrl}/{@code amountUsd} são opcionais. */
    UserAccount creditWallet(UUID userId, BigDecimal amountBrl, BigDecimal amountUsd);
}
