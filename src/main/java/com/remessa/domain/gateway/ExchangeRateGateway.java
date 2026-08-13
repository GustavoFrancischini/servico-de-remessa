package com.remessa.domain.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Porta (outbound) para consulta da cotação do dólar.
 * A implementação vive na camada de infraestrutura e não deve vazar
 * nenhuma dependência de HTTP ou do formato da API externa para o domínio.
 */
public interface ExchangeRateGateway {

    /**
     * Retorna a cotação de compra do dólar (BRL/USD) para a data informada.
     *
     * @param date data desejada
     * @return cotação de compra em BRL por USD
     * @throws com.remessa.domain.exception.ExchangeRateUnavailableException se a API estiver
     *         indisponível, retornar erro ou não houver cotação para a data
     */
    BigDecimal getCotacaoCompra(LocalDate date);
}
