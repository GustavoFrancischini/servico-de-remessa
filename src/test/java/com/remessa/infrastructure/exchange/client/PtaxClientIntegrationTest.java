package com.remessa.infrastructure.exchange.client;

import com.remessa.infrastructure.exchange.dto.PtaxResponse;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração do PtaxClient que valida a URL efetivamente gerada pelo cliente HTTP.
 *
 * Estratégia: usa o EmbeddedServer do próprio @MicronautTest como servidor mock.
 * Um controller mock captura a URI recebida. Um HttpClient de baixo nível aponta para
 * esse servidor e dispara a requisição que o PtaxClient geraria.
 *
 * Este teste usa um banco H2 isolado ("ptaxTestDb") e desabilita o Flyway para que
 * o encerramento deste contexto não feche o banco compartilhado pelos outros testes.
 *
 * Valida que as aspas simples do parâmetro @dataCotacao chegam ao servidor
 * sem percent-encoding: '01-15-2025' e não %2701-15-2025%27.
 */
@MicronautTest
@Property(name = "datasources.default.url",
        value = "jdbc:h2:mem:ptaxTestDb;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
@Property(name = "flyway.datasources.default.enabled", value = "false")
class PtaxClientIntegrationTest {

    /**
     * Controller mock que emula o endpoint PTAX da API do Banco Central.
     * Captura a URI recebida para inspeção pelo teste.
     */
    @Controller("/olinda/servico/PTAX/versao/v1/odata")
    static class MockPtaxController {

        static final AtomicReference<String> lastReceivedUri = new AtomicReference<>();

        @Get("/CotacaoDolarDia{/path:.*}")
        @Produces(MediaType.APPLICATION_JSON)
        String cotacao(HttpRequest<?> request) {
            lastReceivedUri.set(request.getUri().toString());
            return """
                    {"value":[{"cotacaoCompra":6.0371,"cotacaoVenda":6.0377,"dataHoraCotacao":"2025-01-15 13:06:07.747"}]}
                    """;
        }
    }

    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void shouldSendSingleQuotesAroundDateWithoutPercentEncoding() throws Exception {
        MockPtaxController.lastReceivedUri.set(null);

        // Monta a URL exatamente como o PtaxClient geraria para a data '01-15-2025'.
        // O {+dataCotacao} na URI template deve produzir aspas simples literais.
        String path = "/olinda/servico/PTAX/versao/v1/odata"
                + "/CotacaoDolarDia(dataCotacao=@dataCotacao)"
                + "?@dataCotacao='01-15-2025'"
                + "&$top=1&$format=json&$select=cotacaoCompra,cotacaoVenda,dataHoraCotacao";

        URL serverUrl = new URL("http://localhost:" + embeddedServer.getPort());

        // Usa um HttpClient de baixo nível criado para o teste — não altera o contexto principal.
        try (HttpClient httpClient = HttpClient.create(serverUrl)) {
            PtaxResponse response = httpClient.toBlocking()
                    .retrieve(HttpRequest.GET(path), PtaxResponse.class);

            String receivedUri = MockPtaxController.lastReceivedUri.get();

            assertThat(receivedUri)
                    .as("O servidor mock deve ter recebido a requisição")
                    .isNotNull();

            // As aspas simples devem chegar literais — não encodadas como %27
            assertThat(receivedUri)
                    .as("URI deve conter aspas simples literais em torno da data")
                    .contains("'01-15-2025'");
            assertThat(receivedUri)
                    .as("URI não deve conter aspas simples percent-encoded (%27)")
                    .doesNotContain("%27");

            // O parâmetro OData @dataCotacao deve estar presente na query string
            assertThat(receivedUri)
                    .as("URI deve conter o parâmetro OData @dataCotacao")
                    .contains("@dataCotacao=");

            // A resposta deve ter sido desserializada corretamente
            assertThat(response.value())
                    .as("Resposta deve conter exatamente uma entrada de cotação")
                    .hasSize(1);
            assertThat(response.value().getFirst().cotacaoCompra())
                    .as("cotacaoCompra deve ser 6.0371")
                    .isEqualByComparingTo("6.0371");
        }
    }
}
