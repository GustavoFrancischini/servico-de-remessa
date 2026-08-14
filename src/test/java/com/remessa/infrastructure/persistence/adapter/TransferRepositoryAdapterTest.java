package com.remessa.infrastructure.persistence.adapter;

import com.remessa.domain.model.Document;
import com.remessa.domain.model.Transfer;
import com.remessa.domain.model.User;
import com.remessa.domain.repository.TransferRepository;
import com.remessa.domain.repository.UserRepository;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para TransferRepositoryAdapter.
 *
 * Usa banco H2 isolado ("transferTestDb") via @Property para evitar que dados
 * persistidos aqui contaminem outros testes que compartilham o banco principal.
 * Flyway é desabilitado pois a migration é aplicada pelo contexto principal;
 * aqui recriamos apenas as tabelas necessárias via schema-generate.
 *
 * transactional=false é necessário para verificar dados efetivamente persistidos,
 * sem rollback automático ao final de cada teste.
 */
@MicronautTest(transactional = false)
@Property(name = "datasources.default.url",
        value = "jdbc:h2:mem:transferTestDb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
@Property(name = "flyway.datasources.default.locations", value = "classpath:db/migration")
class TransferRepositoryAdapterTest {

    @Inject
    TransferRepository transferRepository;

    @Inject
    UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Cria e persiste um usuário PF com CPF único para satisfazer FKs de transfers. */
    private User persistUser(String cpf) {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        User user = User.create("Test User", email, "hash", Document.cpf(cpf));
        return userRepository.save(user);
    }

    private Transfer transferAt(UUID senderId, UUID receiverId,
                                String amountBrl, LocalDateTime executedAt) {
        // Constrói diretamente com executedAt controlado para testar a query por data
        return new Transfer(UUID.randomUUID(), senderId, receiverId,
                new BigDecimal(amountBrl), new BigDecimal("10.0000"),
                new BigDecimal("6.0000"), executedAt);
    }

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------

    @Test
    void save_shouldPersistTransferAndReturnWithSameFields() {
        User sender   = persistUser("529.982.247-25");
        User receiver = persistUser("111.444.777-35");

        Transfer transfer = Transfer.create(
                sender.id(), receiver.id(),
                new BigDecimal("1000.0000"),
                new BigDecimal("165.8200"),
                new BigDecimal("6.0310"));

        Transfer saved = transferRepository.save(transfer);

        assertThat(saved.id()).isEqualTo(transfer.id());
        assertThat(saved.senderId()).isEqualTo(sender.id());
        assertThat(saved.receiverId()).isEqualTo(receiver.id());
        assertThat(saved.amountBrl()).isEqualByComparingTo("1000.0000");
        assertThat(saved.amountUsd()).isEqualByComparingTo("165.8200");
        assertThat(saved.exchangeRate()).isEqualByComparingTo("6.0310");
        assertThat(saved.executedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // sumAmountBrlBySenderAndDate
    // -------------------------------------------------------------------------

    @Test
    void sumAmountBrlBySenderAndDate_shouldReturnZeroWhenNoTransfersOnDate() {
        User sender = persistUser("222.333.444-05");

        BigDecimal sum = transferRepository.sumAmountBrlBySenderAndDate(
                sender.id(), LocalDate.of(2025, 1, 15));

        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sumAmountBrlBySenderAndDate_shouldSumAllTransfersOnGivenDay() {
        User sender   = persistUser("333.030.440-58");
        User receiver = persistUser("444.541.407-74");

        LocalDate targetDate = LocalDate.of(2025, 6, 10);
        LocalDateTime morning   = targetDate.atTime(9,  0);
        LocalDateTime afternoon = targetDate.atTime(15, 0);

        transferRepository.save(transferAt(sender.id(), receiver.id(), "1000.0000", morning));
        transferRepository.save(transferAt(sender.id(), receiver.id(), "2500.0000", afternoon));

        BigDecimal sum = transferRepository.sumAmountBrlBySenderAndDate(sender.id(), targetDate);

        assertThat(sum).isEqualByComparingTo("3500.0000");
    }

    @Test
    void sumAmountBrlBySenderAndDate_shouldNotIncludeTransfersFromOtherDays() {
        User sender   = persistUser("558.094.710-05");
        User receiver = persistUser("659.391.270-02");

        LocalDate targetDate   = LocalDate.of(2025, 7, 1);
        LocalDate previousDate = LocalDate.of(2025, 6, 30);
        LocalDate nextDate     = LocalDate.of(2025, 7, 2);

        LocalDateTime onTarget   = targetDate.atStartOfDay();
        LocalDateTime onPrevious = previousDate.atStartOfDay();
        LocalDateTime onNext     = nextDate.atStartOfDay();

        transferRepository.save(transferAt(sender.id(), receiver.id(), "5000.0000", onTarget));
        transferRepository.save(transferAt(sender.id(), receiver.id(), "9999.0000", onPrevious));
        transferRepository.save(transferAt(sender.id(), receiver.id(), "9999.0000", onNext));

        BigDecimal sum = transferRepository.sumAmountBrlBySenderAndDate(sender.id(), targetDate);

        assertThat(sum).isEqualByComparingTo("5000.0000");
    }

    @Test
    void sumAmountBrlBySenderAndDate_shouldNotIncludeTransfersFromOtherSenders() {
        User senderA  = persistUser("191.826.600-00");
        User senderB  = persistUser("297.861.780-25");
        User receiver = persistUser("013.546.870-19");

        LocalDate date         = LocalDate.of(2025, 8, 20);
        LocalDateTime executedAt = date.atStartOfDay();

        transferRepository.save(transferAt(senderA.id(), receiver.id(), "1000.0000", executedAt));
        transferRepository.save(transferAt(senderB.id(), receiver.id(), "9000.0000", executedAt));

        BigDecimal sumA = transferRepository.sumAmountBrlBySenderAndDate(senderA.id(), date);
        BigDecimal sumB = transferRepository.sumAmountBrlBySenderAndDate(senderB.id(), date);

        assertThat(sumA).isEqualByComparingTo("1000.0000");
        assertThat(sumB).isEqualByComparingTo("9000.0000");
    }
}
