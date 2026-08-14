package com.remessa.application.service;

import com.remessa.domain.exception.DailyLimitExceededException;
import com.remessa.domain.exception.ExchangeRateUnavailableException;
import com.remessa.domain.exception.InsufficientBalanceException;
import com.remessa.domain.exception.UserNotFoundException;
import com.remessa.domain.gateway.ExchangeRateGateway;
import com.remessa.domain.model.Document;
import com.remessa.domain.model.DocumentType;
import com.remessa.domain.model.Transfer;
import com.remessa.domain.model.User;
import com.remessa.domain.model.Wallet;
import com.remessa.domain.policy.DailyLimitPolicyResolver;
import com.remessa.domain.repository.TransferRepository;
import com.remessa.domain.repository.UserRepository;
import com.remessa.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemessaServiceImplTest {

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final BigDecimal EXCHANGE_RATE  = new BigDecimal("6.0000");
    private static final BigDecimal PF_DAILY_LIMIT = new BigDecimal("10000.00");
    private static final BigDecimal PJ_DAILY_LIMIT = new BigDecimal("50000.00");

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private ExchangeRateGateway exchangeRateGateway;
    @Mock private DailyLimitPolicyResolver dailyLimitPolicyResolver;

    private RemessaService remessaService;

    @BeforeEach
    void setUp() {
        remessaService = new RemessaServiceImpl(
                userRepository, walletRepository, transferRepository,
                exchangeRateGateway, dailyLimitPolicyResolver);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User userPf(UUID id) {
        return new User(id, "Sender PF", "sender@example.com", "hash",
                Document.reconstruct(DocumentType.CPF, "52998224725"), Instant.now());
    }

    private User userPj(UUID id) {
        return new User(id, "Sender PJ", "pj@example.com", "hash",
                Document.reconstruct(DocumentType.CNPJ, "11222333000181"), Instant.now());
    }

    private Wallet walletWith(UUID userId, String brl, String usd) {
        return new Wallet(UUID.randomUUID(), userId,
                new BigDecimal(brl), new BigDecimal(usd), Instant.now());
    }

    /** Configura o caminho feliz com saldo e limite suficientes. */
    private void setupHappyPath(UUID senderId, UUID receiverId,
                                String senderBrl, String alreadyUsed) {
        User sender   = userPf(senderId);
        User receiver = userPf(receiverId);

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, senderBrl, "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "10.00")));
        when(transferRepository.sumAmountBrlBySenderAndDate(eq(senderId), any(LocalDate.class)))
                .thenReturn(new BigDecimal(alreadyUsed));
        when(dailyLimitPolicyResolver.dailyLimitFor(DocumentType.CPF))
                .thenReturn(PF_DAILY_LIMIT);
        when(exchangeRateGateway.getCotacaoCompra(any(LocalDate.class)))
                .thenReturn(EXCHANGE_RATE);
        when(walletRepository.update(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // Cenário 1: sucesso — fluxo completo
    // -------------------------------------------------------------------------

    @Test
    void shouldExecuteRemessaSuccessfully() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        setupHappyPath(senderId, receiverId, "5000.00", "0.00");

        Transfer result = remessaService.executar(senderId, receiverId, new BigDecimal("1200.00"));

        assertThat(result).isNotNull();
        assertThat(result.senderId()).isEqualTo(senderId);
        assertThat(result.receiverId()).isEqualTo(receiverId);
        assertThat(result.amountBrl()).isEqualByComparingTo("1200.00");
        assertThat(result.exchangeRate()).isEqualByComparingTo(EXCHANGE_RATE);
        assertThat(result.executedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Cenário 2: remetente inexistente
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowUserNotFoundWhenSenderDoesNotExist() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("100.00")))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(senderId.toString());

        verify(transferRepository, never()).save(any());
        verify(walletRepository, never()).update(any());
    }

    // -------------------------------------------------------------------------
    // Cenário 3: destinatário inexistente
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowUserNotFoundWhenReceiverDoesNotExist() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPf(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("100.00")))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(receiverId.toString());

        verify(transferRepository, never()).save(any());
        verify(walletRepository, never()).update(any());
    }

    // -------------------------------------------------------------------------
    // Cenário 4: saldo insuficiente
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowInsufficientBalanceWhenSenderHasNotEnoughBrl() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPf(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(userPf(receiverId)));
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, "500.00", "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "0.00")));

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("500.01")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining(senderId.toString());

        verify(exchangeRateGateway, never()).getCotacaoCompra(any());
        verify(transferRepository, never()).save(any());
        verify(walletRepository, never()).update(any());
    }

    // -------------------------------------------------------------------------
    // Cenário 5: limite diário excedido
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowDailyLimitExceededWhenSenderWouldExceedDailyLimit() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPf(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(userPf(receiverId)));
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, "9000.00", "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "0.00")));
        // Já usou 9500 hoje; tentar mais 600 ultrapassa o limite de 10000
        when(transferRepository.sumAmountBrlBySenderAndDate(eq(senderId), any(LocalDate.class)))
                .thenReturn(new BigDecimal("9500.00"));
        when(dailyLimitPolicyResolver.dailyLimitFor(DocumentType.CPF))
                .thenReturn(PF_DAILY_LIMIT);

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("600.00")))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessageContaining(senderId.toString());

        verify(exchangeRateGateway, never()).getCotacaoCompra(any());
        verify(transferRepository, never()).save(any());
        verify(walletRepository, never()).update(any());
    }

    @Test
    void shouldAllowRemessaWhenExactlyAtDailyLimit() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        // Já usou 9000; envia 1000 → total exato de 10000 (não ultrapassa)
        setupHappyPath(senderId, receiverId, "9000.00", "9000.00");

        Transfer result = remessaService.executar(senderId, receiverId, new BigDecimal("1000.00"));

        assertThat(result).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Cenário 6: cotação indisponível
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowExchangeRateUnavailableWhenGatewayFails() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPf(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(userPf(receiverId)));
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, "1000.00", "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "0.00")));
        when(transferRepository.sumAmountBrlBySenderAndDate(eq(senderId), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(dailyLimitPolicyResolver.dailyLimitFor(DocumentType.CPF))
                .thenReturn(PF_DAILY_LIMIT);
        when(exchangeRateGateway.getCotacaoCompra(any(LocalDate.class)))
                .thenThrow(new ExchangeRateUnavailableException(LocalDate.now()));

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("500.00")))
                .isInstanceOf(ExchangeRateUnavailableException.class);

        verify(transferRepository, never()).save(any());
        verify(walletRepository, never()).update(any());
    }

    // -------------------------------------------------------------------------
    // Cenário 7: conversão BRL → USD correta
    // -------------------------------------------------------------------------

    @Test
    void shouldCalculateCorrectUsdAmountUsingExchangeRate() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        // 1200 BRL ÷ 6.0000 = 200.0000 USD
        setupHappyPath(senderId, receiverId, "5000.00", "0.00");

        Transfer result = remessaService.executar(senderId, receiverId, new BigDecimal("1200.00"));

        assertThat(result.amountUsd()).isEqualByComparingTo("200.0000");
    }

    @Test
    void shouldApplyHalfEvenRoundingOnUsdConversion() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        // 100 BRL ÷ 6.0000 = 16.6666... → arredonda para 16.6667 (HALF_EVEN)
        setupHappyPath(senderId, receiverId, "5000.00", "0.00");

        Transfer result = remessaService.executar(senderId, receiverId, new BigDecimal("100.00"));

        assertThat(result.amountUsd()).isEqualByComparingTo("16.6667");
    }

    // -------------------------------------------------------------------------
    // Cenário 8: atualização correta dos saldos das carteiras
    // -------------------------------------------------------------------------

    @Test
    void shouldDebitBrlFromSenderAndCreditUsdToReceiver() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        setupHappyPath(senderId, receiverId, "5000.00", "0.00");

        remessaService.executar(senderId, receiverId, new BigDecimal("600.00"));

        // Captura as duas chamadas de update
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, org.mockito.Mockito.times(2)).update(walletCaptor.capture());

        Wallet updatedSender   = walletCaptor.getAllValues().stream()
                .filter(w -> w.userId().equals(senderId)).findFirst().orElseThrow();
        Wallet updatedReceiver = walletCaptor.getAllValues().stream()
                .filter(w -> w.userId().equals(receiverId)).findFirst().orElseThrow();

        // 5000 - 600 = 4400
        assertThat(updatedSender.balanceBrl()).isEqualByComparingTo("4400.00");
        // 600 / 6.0000 = 100.0000 USD creditados; saldo anterior do receiver era 10.00
        assertThat(updatedReceiver.balanceUsd()).isEqualByComparingTo("110.0000");
    }

    @Test
    void shouldNotModifyReceiverBrlOrSenderUsd() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        setupHappyPath(senderId, receiverId, "3000.00", "0.00");

        remessaService.executar(senderId, receiverId, new BigDecimal("300.00"));

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, org.mockito.Mockito.times(2)).update(walletCaptor.capture());

        Wallet updatedSender   = walletCaptor.getAllValues().stream()
                .filter(w -> w.userId().equals(senderId)).findFirst().orElseThrow();
        Wallet updatedReceiver = walletCaptor.getAllValues().stream()
                .filter(w -> w.userId().equals(receiverId)).findFirst().orElseThrow();

        // USD do remetente não muda
        assertThat(updatedSender.balanceUsd()).isEqualByComparingTo("0.00");
        // BRL do destinatário não muda
        assertThat(updatedReceiver.balanceBrl()).isEqualByComparingTo("0.00");
    }

    // -------------------------------------------------------------------------
    // Cenário 9: rollback lógico — nenhuma persistência quando falha antes de gravar
    // -------------------------------------------------------------------------

    @Test
    void shouldNotPersistAnythingWhenValidationFailsBeforeUpdates() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPf(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(userPf(receiverId)));
        // Saldo zero: falha na validação de saldo
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, "0.00", "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "0.00")));

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("0.01")))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletRepository, never()).update(any());
        verify(transferRepository, never()).save(any());
        verify(exchangeRateGateway, never()).getCotacaoCompra(any());
    }

    @Test
    void shouldNotPersistAnythingWhenExchangeRateFails() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPf(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(userPf(receiverId)));
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, "1000.00", "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "0.00")));
        when(transferRepository.sumAmountBrlBySenderAndDate(eq(senderId), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(dailyLimitPolicyResolver.dailyLimitFor(DocumentType.CPF))
                .thenReturn(PF_DAILY_LIMIT);
        when(exchangeRateGateway.getCotacaoCompra(any()))
                .thenThrow(new ExchangeRateUnavailableException(LocalDate.now()));

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("100.00")))
                .isInstanceOf(ExchangeRateUnavailableException.class);

        verify(walletRepository, never()).update(any());
        verify(transferRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Limites diários — PJ vs PF
    // -------------------------------------------------------------------------

    @Test
    void shouldApplyPjDailyLimitCorrectly() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPj(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(userPf(receiverId)));
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, "60000.00", "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "0.00")));
        // Já usou 49500; tenta mais 600 → ultrapassa 50000
        when(transferRepository.sumAmountBrlBySenderAndDate(eq(senderId), any(LocalDate.class)))
                .thenReturn(new BigDecimal("49500.00"));
        when(dailyLimitPolicyResolver.dailyLimitFor(DocumentType.CNPJ))
                .thenReturn(PJ_DAILY_LIMIT);

        assertThatThrownBy(() -> remessaService.executar(senderId, receiverId, new BigDecimal("600.00")))
                .isInstanceOf(DailyLimitExceededException.class);
    }

    @Test
    void shouldUseDocumentTypeOfSenderForLimitResolution() {
        UUID senderId   = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(userPj(senderId)));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(userPj(receiverId)));
        when(walletRepository.findByUserId(senderId))
                .thenReturn(Optional.of(walletWith(senderId, "20000.00", "0.00")));
        when(walletRepository.findByUserId(receiverId))
                .thenReturn(Optional.of(walletWith(receiverId, "0.00", "0.00")));
        when(transferRepository.sumAmountBrlBySenderAndDate(eq(senderId), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(dailyLimitPolicyResolver.dailyLimitFor(DocumentType.CNPJ))
                .thenReturn(PJ_DAILY_LIMIT);
        when(exchangeRateGateway.getCotacaoCompra(any()))
                .thenReturn(EXCHANGE_RATE);
        when(walletRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // PJ pode enviar 15000 (< 50000) sem problema
        Transfer result = remessaService.executar(senderId, receiverId, new BigDecimal("15000.00"));

        assertThat(result).isNotNull();
        // Confirma que o resolver foi chamado com CNPJ (tipo do remetente), não CPF
        verify(dailyLimitPolicyResolver).dailyLimitFor(DocumentType.CNPJ);
    }
}
