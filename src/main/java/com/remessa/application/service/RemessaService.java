package com.remessa.application.service;

import com.remessa.domain.model.Transfer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Caso de uso de execução de uma remessa internacional (porta inbound consumida pelos controllers).
 *
 * <p>Uma remessa converte {@code amountBrl} do remetente em USD (usando a cotação de compra do dia)
 * e transfere o valor convertido para a carteira em dólar do destinatário.
 */
public interface RemessaService {

    /**
     * Executa uma remessa de {@code amountBrl} reais do remetente para o destinatário.
     *
     * @param senderId   id do usuário remetente
     * @param receiverId id do usuário destinatário
     * @param amountBrl  valor em BRL a enviar; deve ser positivo
     * @return a {@link Transfer} persistida com todos os campos preenchidos
     * @throws com.remessa.domain.exception.UserNotFoundException         se remetente ou destinatário não existirem
     * @throws com.remessa.domain.exception.InsufficientBalanceException  se o remetente não tiver saldo suficiente
     * @throws com.remessa.domain.exception.DailyLimitExceededException   se a remessa ultrapassar o limite diário
     * @throws com.remessa.domain.exception.ExchangeRateUnavailableException se a cotação não estiver disponível
     */
    Transfer executar(UUID senderId, UUID receiverId, BigDecimal amountBrl);
}
