package com.remessa.infrastructure.exchange.adapter;

import com.remessa.domain.exception.ExchangeRateUnavailableException;
import com.remessa.domain.gateway.ExchangeRateGateway;
import com.remessa.infrastructure.exchange.client.PtaxClient;
import com.remessa.infrastructure.exchange.dto.PtaxResponse;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Adapter que implementa {@link ExchangeRateGateway} consultando a API PTAX do Banco Central.
 *
 * <p>A API PTAX não retorna cotação para fins de semana e feriados. Quando a data solicitada
 * não possui cotação (resposta vazia), o adapter retrocede um dia por vez até encontrar
 * uma cotação disponível ou esgotar o limite de tentativas ({@code bcb.ptax.fallback-days}).
 *
 * <p>Apenas a ausência de cotação (resposta vazia/nula) dispara o fallback.
 * Falhas de infraestrutura ({@link HttpClientException}) são propagadas imediatamente
 * sem tentar dias anteriores, pois indicam problema de conectividade, não de dado.
 */
@Singleton
public class ExchangeRateGatewayAdapter implements ExchangeRateGateway {

    private static final DateTimeFormatter PTAX_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRateGatewayAdapter.class);

    private final PtaxClient ptaxClient;
    private final int fallbackDays;

    @Value("${bcb.ptax.base-url}")
    private String ptaxBaseUrl;

    public ExchangeRateGatewayAdapter(
            PtaxClient ptaxClient,
            @Value("${bcb.ptax.fallback-days:7}") int fallbackDays) {
        this.ptaxClient = ptaxClient;
        this.fallbackDays = fallbackDays;
    }

    @Override
    public BigDecimal getCotacaoCompra(LocalDate date) {
        // Tenta a data solicitada e, se não houver cotação, retrocede dia a dia
        // até o limite configurado. O total de tentativas é 1 (data original) + fallbackDays.
        for (int attempt = 0; attempt <= fallbackDays; attempt++) {
            LocalDate candidate = date.minusDays(attempt);
            BigDecimal cotacao = fetchCotacao(candidate, date);
            if (cotacao != null) {
                return cotacao;
            }
        }
        // Todas as tentativas esgotadas — lança com a data original para clareza no erro
        throw new ExchangeRateUnavailableException(date);
    }

    /**
     * Consulta a cotação para {@code candidate} e retorna o valor se disponível,
     * ou {@code null} se a resposta estiver vazia (indicando ausência de cotação para o dia).
     *
     * @param candidate data a consultar
     * @param originalDate data original da requisição, usada para construir a exceção em caso
     *                     de falha de infraestrutura
     * @throws ExchangeRateUnavailableException se a API retornar erro HTTP (não dispara fallback)
     */
    private BigDecimal fetchCotacao(LocalDate candidate, LocalDate originalDate) {
        String dataCotacao = "'" + candidate.format(PTAX_DATE_FORMAT) + "'";
        try {
            PtaxResponse response = ptaxClient.getCotacaoDolarDia(dataCotacao);
            List<PtaxResponse.CotacaoEntry> entries = response.value();

            if (entries == null || entries.isEmpty()) {
                return null; // sem cotação para este dia — sinaliza ao caller para tentar o anterior
            }

            BigDecimal cotacao = entries.getFirst().cotacaoCompra();
            if (cotacao == null) {
                return null; // campo ausente — trata como sem cotação
            }

            return cotacao;
        } catch (HttpClientException e) {
            logHttpFailure(candidate, dataCotacao, e);
            throw new ExchangeRateUnavailableException(originalDate, e);
        }
    }

    private void logHttpFailure(LocalDate candidate, String dataCotacao, HttpClientException exception) {
        if (exception instanceof HttpClientResponseException responseException) {
            String responseBody = responseException.getResponse().getBody(String.class).orElse("<empty>");
            LOG.error(
                    "PTAX request failed: candidate={}, url={}, status={}, body={}, message={}",
                    candidate,
                    requestUrl(dataCotacao),
                    responseException.getStatus().getCode(),
                    responseBody,
                    exception.getMessage(),
                    exception);
            return;
        }

        LOG.error(
                "PTAX request failed before receiving an HTTP response: candidate={}, dataCotacao={}, message={}",
                candidate,
                dataCotacao,
                exception.getMessage(),
                exception);
    }

    private String requestUrl(String dataCotacao) {
        return ptaxBaseUrl
                + "/CotacaoDolarDia(dataCotacao=@dataCotacao)?@dataCotacao=" + dataCotacao
                + "&$top=1&$format=json&$select=cotacaoCompra,cotacaoVenda,dataHoraCotacao";
    }
}
