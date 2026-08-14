package com.remessa.application.service;

import com.remessa.domain.exception.DuplicateDocumentException;
import com.remessa.domain.exception.DuplicateEmailException;
import com.remessa.domain.exception.UserNotFoundException;
import com.remessa.domain.model.Document;
import com.remessa.domain.model.User;
import com.remessa.domain.model.UserAccount;
import com.remessa.domain.model.Wallet;
import com.remessa.domain.repository.UserRepository;
import com.remessa.domain.repository.WalletRepository;
import com.remessa.domain.security.PasswordHasher;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.UUID;

@Singleton
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordHasher passwordHasher;

    public UserServiceImpl(UserRepository userRepository, WalletRepository walletRepository,
                            PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public UserAccount createPessoaFisica(String fullName, String email, String rawPassword, String cpf) {
        return createUser(fullName, email, rawPassword, Document.cpf(cpf));
    }

    @Override
    @Transactional
    public UserAccount createPessoaJuridica(String fullName, String email, String rawPassword, String cnpj) {
        return createUser(fullName, email, rawPassword, Document.cnpj(cnpj));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount getAccount(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        Wallet wallet = walletRepository.findByUserId(id).orElseThrow(() -> new UserNotFoundException(id));
        return new UserAccount(user, wallet);
    }

    @Override
    @Transactional
    public UserAccount creditWallet(UUID userId, BigDecimal amountBrl, BigDecimal amountUsd) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (amountBrl != null) {
            wallet = wallet.creditBrl(amountBrl);
        }
        if (amountUsd != null) {
            wallet = wallet.creditUsd(amountUsd);
        }

        Wallet updatedWallet = walletRepository.update(wallet);
        return new UserAccount(user, updatedWallet);
    }

    private UserAccount createUser(String fullName, String email, String rawPassword, Document document) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        if (userRepository.existsByDocumentValue(document.value())) {
            throw new DuplicateDocumentException(document.value());
        }

        User user = User.create(fullName, email, passwordHasher.hash(rawPassword), document);
        User savedUser = userRepository.save(user);
        Wallet savedWallet = walletRepository.save(Wallet.zeroBalanceFor(savedUser.id()));
        return new UserAccount(savedUser, savedWallet);
    }
}
