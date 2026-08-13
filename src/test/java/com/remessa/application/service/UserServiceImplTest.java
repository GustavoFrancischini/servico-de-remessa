package com.remessa.application.service;

import com.remessa.domain.exception.DuplicateDocumentException;
import com.remessa.domain.exception.DuplicateEmailException;
import com.remessa.domain.exception.UserNotFoundException;
import com.remessa.domain.model.User;
import com.remessa.domain.model.UserAccount;
import com.remessa.domain.model.Wallet;
import com.remessa.domain.repository.UserRepository;
import com.remessa.domain.repository.WalletRepository;
import com.remessa.domain.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String VALID_CPF = "529.982.247-25";
    private static final String VALID_CNPJ = "11.222.333/0001-81";

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, walletRepository, passwordHasher);
    }

    @Test
    void shouldCreatePessoaFisicaWithZeroBalanceWallet() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByDocumentValue(anyString())).thenReturn(false);
        when(passwordHasher.hash(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount account = userService.createPessoaFisica("Ana Silva", "ana@example.com", "secret123", VALID_CPF);

        assertThat(account.user().fullName()).isEqualTo("Ana Silva");
        assertThat(account.user().document().value()).isEqualTo("52998224725");
        assertThat(account.wallet().balanceBrl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.wallet().balanceUsd()).isEqualByComparingTo(BigDecimal.ZERO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().passwordHash()).isEqualTo("hashed-password");
    }

    @Test
    void shouldCreatePessoaJuridicaWithZeroBalanceWallet() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByDocumentValue(anyString())).thenReturn(false);
        when(passwordHasher.hash(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount account = userService.createPessoaJuridica(
                "Empresa LTDA", "contato@empresa.com", "secret123", VALID_CNPJ);

        assertThat(account.user().document().value()).isEqualTo("11222333000181");
        assertThat(account.wallet().balanceBrl()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createPessoaFisica("Ana Silva", "ana@example.com", "secret123", VALID_CPF))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDocumentAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByDocumentValue(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createPessoaFisica("Ana Silva", "ana@example.com", "secret123", VALID_CPF))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldThrowUserNotFoundWhenAccountDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAccount(id))
                .isInstanceOf(UserNotFoundException.class);
    }
}
