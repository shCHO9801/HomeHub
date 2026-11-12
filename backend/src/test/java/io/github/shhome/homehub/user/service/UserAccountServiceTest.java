package io.github.shhome.homehub.user.service;

import io.github.shhome.homehub.common.exception.BusinessException;
import io.github.shhome.homehub.user.entity.User;
import io.github.shhome.homehub.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static io.github.shhome.homehub.common.exception.ErrorCode.USER_ALREADY_EXISTS;
import static io.github.shhome.homehub.common.exception.ErrorCode.USER_NOT_FOUND;
import static io.github.shhome.homehub.user.entity.UserRole.ADMIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 계정 서비스")
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    @DisplayName("비밀번호를 해시해 계정을 생성한다")
    void createsAccountWithEncodedPassword() {
        String username = "sunghyun-admin";
        String rawPassword = "very-strong-password";
        String passwordHash = "{bcrypt}$2a$encoded";

        when(userRepository.existsByUsername(username))
                .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(passwordHash);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        User user = userAccountService.create(
                username,
                rawPassword,
                ADMIN
        );

        assertEquals(username, user.getUsername());
        assertEquals(passwordHash, user.getPasswordHash());
        assertNotEquals(rawPassword, user.getPasswordHash());
        assertEquals(ADMIN, user.getRole());
        assertEquals(true, user.isEnabled());

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("중복 사용자명으로 계정을 생성할 수 없다")
    void duplicateUsernameCannotBeCreated() {
        String username = "sunghyun-admin";

        when(userRepository.existsByUsername(username))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.create(
                        username,
                        "very-strong-password",
                        ADMIN
                )
        );

        assertEquals(
                USER_ALREADY_EXISTS,
                exception.getErrorCode()
        );

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    @DisplayName("비활성화된 계정을 활성화한다")
    void enablesAccount() {
        String username = "sunghyun-admin";
        User user = User.of(
                username,
                "{bcrypt}$2a$encoded",
                ADMIN
        );
        user.disable();

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        User enabledUser =
                userAccountService.enable(username);

        assertTrue(enabledUser.isEnabled());
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("활성화된 계정을 비활성화한다")
    void disablesAccount() {
        String username = "sunghyun-admin";
        User user = User.of(
                username,
                "{bcrypt}$2a$encoded",
                ADMIN
        );

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        User disabledUser =
                userAccountService.disable(username);

        assertFalse(disabledUser.isEnabled());
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("존재하지 않는 계정은 변경할 수 없다")
    void missingAccountCannotBeChanged() {
        String username = "missing-user";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userAccountService.enable(username)
        );

        assertEquals(
                USER_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("사용자 비밀번호를 새 해시로 변경한다")
    void resetsPassword() {
        String username = "sunghyun-admin";
        String rawPassword = "new-strong-password";
        String newPasswordHash = "{bcrypt}$2a$new-encoded";

        User user = User.of(
                username,
                "{bcrypt}$2a$old-encoded",
                ADMIN
        );

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(newPasswordHash);

        User updatedUser = userAccountService.resetPassword(
                username,
                rawPassword
        );

        assertEquals(
                newPasswordHash,
                updatedUser.getPasswordHash()
        );
        assertNotEquals(
                rawPassword,
                updatedUser.getPasswordHash()
        );

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).encode(rawPassword);
    }
}
