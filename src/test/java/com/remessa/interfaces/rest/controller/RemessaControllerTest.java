package com.remessa.interfaces.rest.controller;

import com.remessa.domain.exception.ExchangeRateUnavailableException;
import com.remessa.domain.gateway.ExchangeRateGateway;
import com.remessa.domain.model.Transfer;
import com.remessa.domain.model.Wallet;
import com.remessa.domain.repository.TransferRepository;
import com.remessa.domain.repository.WalletRepository;
import com.remessa.infrastructure.exchange.adapter.ExchangeRateGatewayAdapter;
import com.remessa.interfaces.rest.dto.CreatePessoaFisicaRequest;
import com.remessa.interfaces.rest.dto.CreateRemessaRequest;
import com.remessa.interfaces.rest.dto.RemessaResponse;
import com.remessa.interfaces.rest.dto.UserResponse;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Testes de integração do endpoint POST /api/remessas.
 *
 * transactional=false é necessário para que creditarSaldoBrl() seja visível
 * pelo endpoint (que roda em sua própria transação). Consequência: dados não
 * são revertidos entre testes — cada teste usa CPFs únicos e exclusivos.
 *
 * CPFs foram calculados manualmente usando o algoritmo de Document.java,
 * com prefixos sequenciais 100000001..900000001 + 100000002.
 * Nenhum CPF coincide com os de outros arquivos de teste do projeto.
 *
 * Porta aleatória (-1) configurada em application-test.yml para evitar
 * conflito de BindException com outros contextos de teste na mesma JVM.
 */
@MicronautTest(transactional = false)
@Property(name = "datasources.default.url",
        value = "jdbc:h2:mem:remessaControllerTestDb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
class RemessaControllerTest {

    /**
     * Substitui o ExchangeRateGatewayAdapter por um mock Mockito via @Factory + @Replaces.
     * A instância estática é compartilhada entre o factory (startup) e os métodos de teste
     * (reconfiguração por cenário).
     *
     * @Requires(beans = RemessaControllerTest.class) restringe esta factory ao contexto
     * que instancia RemessaControllerTest, evitando que o @Replaces vaze para outros
     * contextos de teste (como UserControllerTest) que não precisam deste mock.
     */
    @Factory
    @Requires(beans = RemessaControllerTest.class)
    static class MockExchangeRateGatewayFactory {

        static final ExchangeRateGateway MOCK_INSTANCE = mock(ExchangeRateGateway.class);

        @Bean
        @Singleton
        @Replaces(ExchangeRateGatewayAdapter.class)
        ExchangeRateGateway exchangeRateGateway() {
            return MOCK_INSTANCE;
        }
    }

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    WalletRepository walletRepository;

    @Inject
    TransferRepository transferRepository;

    /** Cotação padrão R$6,00 restaurada antes de cada teste. */
    @BeforeEach
    void resetGatewayMock() {
        reset(MockExchangeRateGatewayFactory.MOCK_INSTANCE);
        when(MockExchangeRateGatewayFactory.MOCK_INSTANCE.getCotacaoCompra(any(LocalDate.class)))
                .thenReturn(new BigDecimal("6.0000"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserResponse criarUsuario(String cpf) {
        String email = "remessa-test-" + UUID.randomUUID() + "@example.com";
        return client.toBlocking().retrieve(
                HttpRequest.POST("/api/users/pf",
                        new CreatePessoaFisicaRequest("Test User", email, "secret123", cpf)),
                UserResponse.class);
    }

    private void creditarSaldoBrl(UUID userId, String valorBrl) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        walletRepository.update(new Wallet(
                wallet.id(), wallet.userId(),
                new BigDecimal(valorBrl), wallet.balanceUsd(),
                wallet.createdAt()));
    }

    private void persistirTransferencia(UUID senderId, UUID receiverId, String amountBrl) {
        transferRepository.save(new Transfer(
                UUID.randomUUID(), senderId, receiverId,
                new BigDecimal(amountBrl), new BigDecimal("1.0000"),
                new BigDecimal("6.0000"), LocalDateTime.now()));
    }

    // -------------------------------------------------------------------------
    // CPFs usados nos testes — todos comprovadamente aceitos por Document.cpf()
    // em testes reais do projeto (TransferRepositoryAdapterTest, DocumentTest,
    // UserControllerTest, CpfValidationProbeTest).
    //
    // Com transactional=false cada CPF é usado em no máximo um teste para
    // evitar violação da constraint de unicidade no banco.
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Cenário 1: sucesso → 201 Created
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn201WithRemessaResponseOnSuccess() {
        UserResponse sender   = criarUsuario("222.333.444-05");
        UserResponse receiver = criarUsuario("333.030.440-58");
        creditarSaldoBrl(sender.id(), "3000.00");

        RemessaResponse response = client.toBlocking().retrieve(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(sender.id(), receiver.id(), new BigDecimal("600.00"))),
                RemessaResponse.class);

        assertThat(response.id()).isNotNull();
        assertThat(response.senderId()).isEqualTo(sender.id());
        assertThat(response.receiverId()).isEqualTo(receiver.id());
        assertThat(response.amountBrl()).isEqualByComparingTo("600.00");
        assertThat(response.amountUsd()).isEqualByComparingTo("100.0000"); // 600 / 6.0000
        assertThat(response.exchangeRate()).isEqualByComparingTo("6.0000");
        assertThat(response.executedAt()).isNotNull();
    }

    @Test
    void shouldReturnHttpStatus201() {
        UserResponse sender   = criarUsuario("659.391.270-02");
        UserResponse receiver = criarUsuario("191.826.600-00");
        creditarSaldoBrl(sender.id(), "1000.00");

        var httpResponse = client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(sender.id(), receiver.id(), new BigDecimal("100.00"))),
                Argument.of(RemessaResponse.class));

        assertThat(httpResponse.status().getCode()).isEqualTo(HttpStatus.CREATED.getCode());
    }

    // -------------------------------------------------------------------------
    // Cenário 2: usuário inexistente → 404
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn404WhenSenderDoesNotExist() {
        UserResponse receiver = criarUsuario("444.541.407-74");
        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(UUID.randomUUID(), receiver.id(), new BigDecimal("100.00"))),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.NOT_FOUND.getCode()));
    }

    @Test
    void shouldReturn404WhenReceiverDoesNotExist() {
        UserResponse sender = criarUsuario("558.094.710-05");
        creditarSaldoBrl(sender.id(), "1000.00");

        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(sender.id(), UUID.randomUUID(), new BigDecimal("100.00"))),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.NOT_FOUND.getCode()));
    }

    // -------------------------------------------------------------------------
    // Cenário 3: saldo insuficiente → 422
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn422WhenSenderHasInsufficientBalance() {
        UserResponse sender   = criarUsuario("144.235.506-95");
        UserResponse receiver = criarUsuario("013.546.870-19");
        // saldo inicial é zero — não chama creditarSaldoBrl

        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(sender.id(), receiver.id(), new BigDecimal("0.01"))),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.getCode()));
    }

    // -------------------------------------------------------------------------
    // Cenário 4: limite diário excedido → 422
    // Persiste 9 transferências de R$1.100 (= R$9.900) via TransferRepository real,
    // depois tenta enviar mais R$200 → R$10.100 > R$10.000 (limite PF).
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn422WhenDailyLimitIsExceeded() {
        UserResponse sender   = criarUsuario("297.861.780-25");
        UserResponse receiver = criarUsuario("099.650.796-50");
        creditarSaldoBrl(sender.id(), "15000.00");

        for (int i = 0; i < 9; i++) {
            persistirTransferencia(sender.id(), receiver.id(), "1100.00");
        }

        // 9 × 1100 = 9900 acumulados; envia mais 200 → 10100 > 10000
        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(sender.id(), receiver.id(), new BigDecimal("200.00"))),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.getCode()));
    }

    // -------------------------------------------------------------------------
    // Cenário 5: cotação indisponível → 503
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn503WhenExchangeRateIsUnavailable() {
        when(MockExchangeRateGatewayFactory.MOCK_INSTANCE.getCotacaoCompra(any(LocalDate.class)))
                .thenThrow(new ExchangeRateUnavailableException(LocalDate.now()));

        UserResponse sender   = criarUsuario("529.982.247-25");
        UserResponse receiver = criarUsuario("111.444.777-35");
        creditarSaldoBrl(sender.id(), "1000.00");

        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(sender.id(), receiver.id(), new BigDecimal("100.00"))),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.getCode()));
    }

    // -------------------------------------------------------------------------
    // Cenário 6: request inválido → 400
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn400WhenAmountBrlIsNull() {
        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(UUID.randomUUID(), UUID.randomUUID(), null)),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST.getCode()));
    }

    @Test
    void shouldReturn400WhenAmountBrlIsZero() {
        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO)),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST.getCode()));
    }

    @Test
    void shouldReturn400WhenSenderIdIsNull() {
        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.POST("/api/remessas",
                        new CreateRemessaRequest(null, UUID.randomUUID(), new BigDecimal("100.00"))),
                Argument.of(RemessaResponse.class)))
                .isInstanceOf(HttpClientResponseException.class)
                .satisfies(ex -> assertThat(((HttpClientResponseException) ex).getStatus().getCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST.getCode()));
    }
}
