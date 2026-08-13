package com.remessa.infrastructure.exchange.client;

import com.remessa.infrastructure.exchange.dto.PtaxResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

/**
 * HTTP client declarativo para a API PTAX do Banco Central do Brasil.
 *
 * Endpoint base: https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata
 *
 * Recurso utilizado: CotacaoDolarDia(dataCotacao=@dataCotacao)
 *
 * A sintaxe {+dataCotacao} utiliza "reserved expansion" (RFC 6570 §3.2.3), que suprime
 * o percent-encoding de caracteres reservados — incluindo as aspas simples obrigatórias
 * pelo protocolo OData. Isso garante que o valor '01-15-2025' chegue ao servidor
 * exatamente como é, sem ser codificado para %2701-15-2025%27.
 */
@Client("${bcb.ptax.base-url}")
public interface PtaxClient {

    /**
     * Consulta a cotação do dólar para uma data específica.
     *
     * @param dataCotacao data no formato {@code 'MM-dd-yyyy'} (com aspas simples inclusas),
     *                    ex: {@code '01-15-2025'}. Deve ser passado com as aspas simples,
     *                    pois a expansão {+...} as preserva na URL final.
     * @return resposta OData com a lista de cotações do dia
     */
    @Get("/CotacaoDolarDia(dataCotacao=@dataCotacao)?@dataCotacao={+dataCotacao}&$top=1&$format=json&$select=cotacaoCompra,cotacaoVenda,dataHoraCotacao")
    PtaxResponse getCotacaoDolarDia(String dataCotacao);
}
