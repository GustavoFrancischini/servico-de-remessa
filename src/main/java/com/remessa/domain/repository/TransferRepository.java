package com.remessa.domain.repository;

import com.remessa.domain.model.Transfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Porta (outbound) para persistência de remessas; a implementação vive na camada de infraestrutura. */
public interface TransferRepository {

    Transfer save(Transfer transfer);

    /**
     * Retorna a soma de {@code amount_brl} de todas as remessas enviadas pelo usuário
     * na data informada (usando {@code executed_at} truncado para o dia).
     *
     * <p>Usado para validar o limite diário antes de executar uma nova remessa.
     * Retorna {@link BigDecimal#ZERO} quando não houver remessas no dia.
     *
     * @param senderId id do usuário remetente
     * @param date     data a considerar (fuso horário do servidor)
     */
    BigDecimal sumAmountBrlBySenderAndDate(UUID senderId, LocalDate date);
}
