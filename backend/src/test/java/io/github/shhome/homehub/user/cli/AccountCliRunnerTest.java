package io.github.shhome.homehub.user.cli;

import io.github.shhome.homehub.user.entity.User;
import io.github.shhome.homehub.user.service.UserAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.shhome.homehub.user.cli.AccountAction.*;
import static io.github.shhome.homehub.user.entity.UserRole.ADMIN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("계정 관리 CLI")
class AccountCliRunnerTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private ApplicationArguments applicationArguments;

    @TempDir
    private Path tempDirectory;

    @Test
    @DisplayName("Secret 파일의 비밀번호로 계정을 생성한다")
    void createsAccountFromPasswordFile() throws Exception {
        String username = "sunghyun-admin";
        String password = "very-strong-password";

        Path passwordFile =
                tempDirectory.resolve("admin-password");

        Files.writeString(
                passwordFile,
                password + System.lineSeparator(),
                UTF_8
        );

        AccountCliProperties properties =
                new AccountCliProperties(
                        CREATE,
                        username,
                        ADMIN,
                        passwordFile
                );

        User createdUser = User.of(
                username,
                "{bcrypt}$2a$encoded",
                ADMIN
        );

        when(userAccountService.create(
                eq(username),
                any(CharSequence.class),
                eq(ADMIN)
        )).thenAnswer(invocation -> {
            CharSequence passwordArgument =
                    invocation.getArgument(1);

            assertEquals(
                    password,
                    passwordArgument.toString()
            );

            return createdUser;
        });

        AccountCliRunner runner = new AccountCliRunner(
                properties,
                userAccountService
        );

        runner.run(applicationArguments);

        verify(userAccountService).create(
                eq(username),
                any(CharSequence.class),
                eq(ADMIN)
        );
    }

    @Test
    @DisplayName("비밀번호가 너무 짧으면 계정을 생성하지 않는다")
    void rejectsShortPassword() throws Exception {
        Path passwordFile =
                tempDirectory.resolve("short-password");

        Files.writeString(
                passwordFile,
                "short-password",
                UTF_8
        );

        AccountCliProperties properties =
                new AccountCliProperties(
                        CREATE,
                        "sunghyun-admin",
                        ADMIN,
                        passwordFile
                );

        AccountCliRunner runner = new AccountCliRunner(
                properties,
                userAccountService
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> runner.run(applicationArguments)
        );

        verifyNoInteractions(userAccountService);
    }

    @Test
    @DisplayName("Secret 파일이 없으면 계정을 생성하지 않는다")
    void rejectsMissingPasswordFile() {
        Path missingFile =
                tempDirectory.resolve("missing-password");

        AccountCliProperties properties =
                new AccountCliProperties(
                        CREATE,
                        "sunghyun-admin",
                        ADMIN,
                        missingFile
                );

        AccountCliRunner runner = new AccountCliRunner(
                properties,
                userAccountService
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> runner.run(applicationArguments)
        );

        verifyNoInteractions(userAccountService);
    }

    @Test
    @DisplayName("비밀번호 파일 없이 계정을 활성화한다")
    void enablesAccountWithoutPasswordFile() throws Exception {
        String username = "sunghyun-admin";

        AccountCliProperties properties =
                new AccountCliProperties(
                        ENABLE,
                        username,
                        null,
                        null
                );

        User user = User.of(
                username,
                "{bcrypt}$2a$encoded",
                ADMIN
        );

        when(userAccountService.enable(username))
                .thenReturn(user);

        AccountCliRunner runner = new AccountCliRunner(
                properties,
                userAccountService
        );

        runner.run(applicationArguments);

        verify(userAccountService).enable(username);
        verify(userAccountService, never()).create(
                anyString(),
                any(CharSequence.class),
                any()
        );
    }

    @Test
    @DisplayName("비밀번호 파일 없이 계정을 비활성화한다")
    void disablesAccountWithoutPasswordFile() throws Exception {
        String username = "sunghyun-admin";

        AccountCliProperties properties =
                new AccountCliProperties(
                        DISABLE,
                        username,
                        null,
                        null
                );

        User user = User.of(
                username,
                "{bcrypt}$2a$encoded",
                ADMIN
        );
        user.disable();

        when(userAccountService.disable(username))
                .thenReturn(user);

        AccountCliRunner runner = new AccountCliRunner(
                properties,
                userAccountService
        );

        runner.run(applicationArguments);

        verify(userAccountService).disable(username);
        verify(userAccountService, never()).create(
                anyString(),
                any(CharSequence.class),
                any()
        );
    }

    @Test
    @DisplayName("Secret 파일의 비밀번호로 사용자 비밀번호를 변경한다")
    void resetsPasswordFromPasswordFile() throws Exception {
        String username = "sunghyun-admin";
        String password = "new-strong-password";

        Path passwordFile =
                tempDirectory.resolve("new-admin-password");

        Files.writeString(
                passwordFile,
                password + System.lineSeparator(),
                UTF_8
        );

        AccountCliProperties properties =
                new AccountCliProperties(
                        RESET_PASSWORD,
                        username,
                        null,
                        passwordFile
                );

        User user = User.of(
                username,
                "{bcrypt}$2a$new-encoded",
                ADMIN
        );

        when(userAccountService.resetPassword(
                eq(username),
                any(CharSequence.class)
        )).thenAnswer(invocation -> {
            CharSequence passwordArgument =
                    invocation.getArgument(1);

            assertEquals(
                    password,
                    passwordArgument.toString()
            );

            return user;
        });

        AccountCliRunner runner = new AccountCliRunner(
                properties,
                userAccountService
        );

        runner.run(applicationArguments);

        verify(userAccountService).resetPassword(
                eq(username),
                any(CharSequence.class)
        );
    }

    @Test
    @DisplayName("계정 관리 명령이 없으면 실행하지 않는다")
    void rejectsMissingAction() {
        AccountCliProperties properties =
                new AccountCliProperties(
                        null,
                        "sunghyun-admin",
                        null,
                        null
                );

        AccountCliRunner runner = new AccountCliRunner(
                properties,
                userAccountService
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> runner.run(applicationArguments)
        );

        verifyNoInteractions(userAccountService);
    }
}
