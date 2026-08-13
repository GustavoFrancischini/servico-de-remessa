package com.remessa.infrastructure.exchange.adapter;

import com.remessa.domain.exception.ExchangeRateUnavailableException;
import com.remessa.infrastructure.exchange.client.PtaxClient;
import com.remessa.infrastructure.exchange.dto.PtaxResponse;
import io.micronaut.http.client.exceptions.HttpClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateGatewayAdapterTest {

    @Mock
    private PtaxClient ptaxClient;

    // Helpers para construir respostas

    private static PtaxResponse withCotacao(String value) {
        return new PtaxResponse(List.of(
                new PtaxResponse.CotacaoEntry(new BigDecimal(value), new BigDecimal("6.0377"), "2025-01-15 13:06:07.747")
        ));
    }

    private static PtaxResponse empty() {
        return new PtaxResponse(Collections.emptyList());
    }

    private ExchangeRateGatewayAdapter adapterWith(int fallbackDays) {
        return new ExchangeRateGatewayAdapter(ptaxClient, fallbackDays);
    }

    // -------------------------------------------------------------------------
    // Cenário 1: cotação disponível na primeira tentativa
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnCotacaoCompraWhenAvailableOnRequestedDate() {
        LocalDate date = LocalDate.of(2025, 1, 15); // quarta-feira
        when(ptaxClient.getCotacaoDolarDia(anyString())).thenReturn(withCotacao("6.0371"));

        BigDecimal result = adapterWith(7).getCotacaoCompra(date);

        assertThat(result).isEqualByComparingTo("6.0371");
        // Apenas uma chamada — sem necessidade de fallback
        verify(ptaxClient, times(1)).getCotacaoDolarDia(anyString());
    }

    @Test
    void shouldFormatDateAsMonthDayYearWithSingleQuotes() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        when(ptaxClient.getCotacaoDolarDia(anyString())).thenReturn(withCotacao("6.0371"));

        adapterWith(7).getCotacaoCompra(date);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ptaxClient).getCotacaoDolarDia(captor.capture());
        assertThat(captor.getValue()).isEqualTo("'01-15-2025'");
    }

    // -------------------------------------------------------------------------
    // Cenário 2: cotação encontrada após retroceder dias (fim de semana / feriado)
    // -------------------------------------------------------------------------

    @Test
    void shouldFallBackToPreviousDayWhenRequestedDateHasNoCotacao() {
        // Simula segunda-feira (2025-01-20) sem cotação; sexta (2025-01-17) tem cotação
        LocalDate monday   = LocalDate.of(2025, 1, 20);
        LocalDate sunday   = LocalDate.of(2025, 1, 19);
        LocalDate saturday = LocalDate.of(2025, 1, 18);
        LocalDate friday   = LocalDate.of(2025, 1, 17);

        // Retorna vazio para segunda, domingo e sábado; retorna cotação na sexta
        when(ptaxClient.getCotacaoDolarDia("'" + monday.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy")) + "'"))
                .thenReturn(empty());
        when(ptaxClient.getCotacaoDolarDia("'" + sunday.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy")) + "'"))
                .thenReturn(empty());
        when(ptaxClient.getCotacaoDolarDia("'" + saturday.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy")) + "'"))
                .thenReturn(empty());
        when(ptaxClient.getCotacaoDolarDia("'" + friday.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy")) + "'"))
                .thenReturn(withCotacao("5.9800"));

        BigDecimal result = adapterWith(7).getCotacaoCompra(monday);

        assertThat(result).isEqualByComparingTo("5.9800");
        // Deve ter tentado: segunda, domingo, sábado, sexta = 4 chamadas
        verify(ptaxClient, times(4)).getCotacaoDolarDia(anyString());
    }

    @Test
    void shouldReturnCotacaoFoundOnFirstFallbackDay() {
        LocalDate requested  = LocalDate.of(2025, 1, 18); // sábado
        LocalDate previousDay = LocalDate.of(2025, 1, 17); // sexta

        when(ptaxClient.getCotacaoDolarDia("'01-18-2025'")).thenReturn(empty());
        when(ptaxClient.getCotacaoDolarDia("'01-17-2025'")).thenReturn(withCotacao("6.0100"));

        BigDecimal result = adapterWith(7).getCotacaoCompra(requested);

        assertThat(result).isEqualByComparingTo("6.0100");
        verify(ptaxClient, times(2)).getCotacaoDolarDia(anyString());
    }

    // -------------------------------------------------------------------------
    // Cenário 3: cotação indisponível após esgotar todas as N tentativas
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowAfterExhaustingAllFallbackAttempts() {
        LocalDate date = LocalDate.of(2025, 1, 18);
        // Todas as tentativas (data original + 3 dias anteriores) retornam vazio
        when(ptaxClient.getCotacaoDolarDia(anyString())).thenReturn(empty());

        assertThatThrownBy(() -> adapterWith(3).getCotacaoCompra(date))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessageContaining("2025-01-18"); // mensagem referencia a data original

        // 1 tentativa original + 3 dias de fallback = 4 chamadas no total
        verify(ptaxClient, times(4)).getCotacaoDolarDia(anyString());
    }

    @Test
    void shouldMakeExactlyOnePlusNCallsBeforeThrowing() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        when(ptaxClient.getCotacaoDolarDia(anyString())).thenReturn(empty());

        int fallbackDays = 5;
        assertThatThrownBy(() -> adapterWith(fallbackDays).getCotacaoCompra(date))
                .isInstanceOf(ExchangeRateUnavailableException.class);

        verify(ptaxClient, times(1 + fallbackDays)).getCotacaoDolarDia(anyString());
    }

    @Test
    void shouldQueryDatesInDescendingOrderDuringFallback() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        when(ptaxClient.getCotacaoDolarDia(anyString())).thenReturn(empty());

        assertThatThrownBy(() -> adapterWith(2).getCotacaoCompra(date))
                .isInstanceOf(ExchangeRateUnavailableException.class);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(ptaxClient, times(3)).getCotacaoDolarDia(captor.capture());

        // Datas consultadas em ordem decrescente: 15, 14, 13 de janeiro
        assertThat(captor.getAllValues()).containsExactly(
                "'01-15-2025'",
                "'01-14-2025'",
                "'01-13-2025'"
        );
    }

    // -------------------------------------------------------------------------
    // Cenário 4: HttpClientException não dispara fallback
    // -------------------------------------------------------------------------

    @Test
    void shouldPropagateHttpClientExceptionWithoutFallback() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        when(ptaxClient.getCotacaoDolarDia(anyString()))
                .thenThrow(new HttpClientException("Connection refused"));

        assertThatThrownBy(() -> adapterWith(7).getCotacaoCompra(date))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessageContaining("2025-01-15")
                .hasCauseInstanceOf(HttpClientException.class);

        // Apenas uma tentativa — falha de conectividade não aciona retrocesso de datas
        verify(ptaxClient, times(1)).getCotacaoDolarDia(anyString());
    }

    @Test
    void shouldPropagateHttpClientExceptionEvenWhenItOccursDuringFallback() {
        LocalDate date = LocalDate.of(2025, 1, 18); // sábado
        // Primeiro dia vazio, segundo lança HttpClientException
        when(ptaxClient.getCotacaoDolarDia("'01-18-2025'")).thenReturn(empty());
        when(ptaxClient.getCotacaoDolarDia("'01-17-2025'"))
                .thenThrow(new HttpClientException("Timeout"));

        assertThatThrownBy(() -> adapterWith(7).getCotacaoCompra(date))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasCauseInstanceOf(HttpClientException.class);

        // Parou na segunda tentativa, não continuou retrocedendo
        verify(ptaxClient, times(2)).getCotacaoDolarDia(anyString());
    }

    // -------------------------------------------------------------------------
    // Casos de borda
    // -------------------------------------------------------------------------

    @Test
    void shouldTreatNullValueListAsAbsenceOfCotacao() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        // Primeiro dia retorna null no campo value, segundo tem cotação
        when(ptaxClient.getCotacaoDolarDia("'01-15-2025'"))
                .thenReturn(new PtaxResponse(null));
        when(ptaxClient.getCotacaoDolarDia("'01-14-2025'"))
                .thenReturn(withCotacao("6.0200"));

        BigDecimal result = adapterWith(7).getCotacaoCompra(date);

        assertThat(result).isEqualByComparingTo("6.0200");
    }

    @Test
    void shouldTreatNullCotacaoCompraFieldAsAbsenceOfCotacao() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        PtaxResponse nullCotacao = new PtaxResponse(List.of(
                new PtaxResponse.CotacaoEntry(null, new BigDecimal("6.0377"), "2025-01-15 13:06:07.747")
        ));
        when(ptaxClient.getCotacaoDolarDia("'01-15-2025'")).thenReturn(nullCotacao);
        when(ptaxClient.getCotacaoDolarDia("'01-14-2025'")).thenReturn(withCotacao("6.0200"));

        BigDecimal result = adapterWith(7).getCotacaoCompra(date);

        assertThat(result).isEqualByComparingTo("6.0200");
    }
}
