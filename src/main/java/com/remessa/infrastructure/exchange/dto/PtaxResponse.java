package com.remessa.infrastructure.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO que representa a resposta OData da API PTAX do Banco Central.
 * Isolado na infraestrutura — o domínio nunca vê esta classe.
 *
 * @Serdeable é necessário para que o Micronaut Serde gere a introspection de
 * desserialização em tempo de compilação, evitando o erro
 * "No bean introspection available" ao decodificar respostas via @Client declarativo.
 *
 * Exemplo de payload:
 * {
 *   "@odata.context": "...",
 *   "value": [{ "cotacaoCompra": 6.0371, "cotacaoVenda": 6.0377, "dataHoraCotacao": "2025-01-15 13:06:07.747" }]
 * }
 */
@Serdeable
public record PtaxResponse(
        @JsonProperty("value") List<CotacaoEntry> value
) {

    @Serdeable
    public record CotacaoEntry(
            @JsonProperty("cotacaoCompra") BigDecimal cotacaoCompra,
            @JsonProperty("cotacaoVenda") BigDecimal cotacaoVenda,
            @JsonProperty("dataHoraCotacao") String dataHoraCotacao
    ) {}
}
