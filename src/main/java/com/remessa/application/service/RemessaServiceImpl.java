package com.remessa.application.service;

import com.remessa.domain.exception.DailyLimitExceededException;
import com.remessa.domain.exception.InsufficientBalanceException;
import com.remessa.domain.exception.UserNotFoundException;
import com.remessa.domain.gateway.ExchangeRateGateway;
import com.remessa.domain.model.Transfer;
import com.remessa.domain.model.User;
import com.remessa.domain.model.Wallet;
import com.remessa.domain.policy.DailyLimitPolicyResolver;
import com.remessa.domain.repository.TransferRepository;
import com.remessa.domain.repository.UserRepository;
import com.remessa.domain.repository.WalletRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Singleton
public class RemessaServiceImpl implements RemessaService {

    /** Escala do valor em USD persistido na tabela transfers (NUMERIC 19,4). */
    private static final int USD_SCALE = 4;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final ExchangeRateGateway exchangeRateGateway;
    private final DailyLimitPolicyResolver dailyLimitPolicyResolver;

    public RemessaServiceImpl(UserRepository userRepository,
                               WalletRepository walletRepository,
                               TransferRepository transferRepository,
                               ExchangeRateGateway exchangeRateGateway,
                               DailyLimitPolicyResolver dailyLimitPolicyResolver) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.exchangeRateGateway = exchangeRateGateway;
        this.dailyLimitPolicyResolver = dailyLimitPolicyResolver;
    }

    @Override
    @Transactional
    public Transfer executar(UUID senderId, UUID receiverId, BigDecimal amountBrl) {

        // 1. Carregar remetente e destinatário
        User sender   = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException(receiverId));

        // 2. Carregar carteiras
        Wallet senderWallet   = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));
        Wallet receiverWallet = walletRepository.findByUserId(receiverId)
                .orElseThrow(() -> new UserNotFoundException(receiverId));

        // 3. Validar saldo BRL do remetente
        if (amountBrl.compareTo(senderWallet.balanceBrl()) > 0) {
            throw new InsufficientBalanceException(senderId, amountBrl, senderWallet.balanceBrl());
        }

        // 4. Consultar total já transacionado pelo remetente hoje
        BigDecimal alreadyUsedToday = transferRepository
                .sumAmountBrlBySenderAndDate(senderId, LocalDate.now());

        // 5. Aplicar política de limite diário
        BigDecimal dailyLimit  = dailyLimitPolicyResolver.dailyLimitFor(sender.document().type());
        BigDecimal totalAfter  = alreadyUsedToday.add(amountBrl);
        if (totalAfter.compareTo(dailyLimit) > 0) {
            throw new DailyLimitExceededException(senderId, dailyLimit, alreadyUsedToday, amountBrl);
        }

        // 6. Obter cotação de compra do dia
        BigDecimal exchangeRate = exchangeRateGateway.getCotacaoCompra(LocalDate.now());

        // 7. Converter BRL → USD  (cotação PTAX = quantos BRL valem 1 USD)
        BigDecimal amountUsd = amountBrl.divide(exchangeRate, USD_SCALE, RoundingMode.HALF_EVEN);

        // 8. Debitar BRL do remetente (retorna nova instância imutável)
        Wallet debitedSenderWallet = senderWallet.debitBrl(amountBrl);

        // 9. Creditar USD no destinatário (retorna nova instância imutável)
        Wallet creditedReceiverWallet = receiverWallet.creditUsd(amountUsd);

        // 10. Persistir as duas carteiras
        walletRepository.update(debitedSenderWallet);
        walletRepository.update(creditedReceiverWallet);

        // 11. Persistir a Transfer
        Transfer transfer = Transfer.create(senderId, receiverId, amountBrl, amountUsd, exchangeRate);
        return transferRepository.save(transfer);
    }
}
