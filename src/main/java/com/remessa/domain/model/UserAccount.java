package com.remessa.domain.model;

/** Agregado de leitura que combina o usuário com sua carteira, usado para consultas. */
public record UserAccount(User user, Wallet wallet) {
}
