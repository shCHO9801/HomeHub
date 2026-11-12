package io.github.shhome.homehub.user.cli;

import io.github.shhome.homehub.user.entity.User;
import io.github.shhome.homehub.user.entity.UserRole;
import io.github.shhome.homehub.user.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
@Profile("account-cli")
@RequiredArgsConstructor
public class AccountCliRunner implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 16;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final AccountCliProperties properties;
    private final UserAccountService userAccountService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        AccountAction action = requireAction(properties.action());

        switch (action) {
            case CREATE -> createAccount();
            case ENABLE -> enableAccount();
            case DISABLE -> disableAccount();
            case RESET_PASSWORD -> resetPassword();
        }
    }

    private AccountAction requireAction(AccountAction action) {
        if (action == null) {
            throw new IllegalArgumentException(
                    "계정 관리 명령은 필수입니다."
            );
        }

        return action;
    }

    private void enableAccount() {
        String username = requireUsername(properties.username());

        User user = userAccountService.enable(username);

        log.info(
                "Account enabled. id={}, username={}, role={}",
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    private void disableAccount() {
        String username = requireUsername(properties.username());

        User user = userAccountService.disable(username);

        log.info(
                "Account disabled. id={}, username={}, role={}",
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }

    private String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "사용자명은 필수입니다."
            );
        }

        String normalized = username.trim();

        if (normalized.length() > 50) {
            throw new IllegalArgumentException(
                    "사용자명은 50자 이하여야 합니다."
            );
        }

        return normalized;
    }

    private void createAccount() throws IOException {
        String username = requireUsername(properties.username());
        UserRole role = requireRole(properties.role());
        Path passwordFile = requirePasswordFile(
                properties.passwordFile()
        );

        char[] password = readPassword(passwordFile);

        try {
            User user = userAccountService.create(
                    username,
                    CharBuffer.wrap(password),
                    role
            );

            log.info(
                    "Account created. id={}, username={}, role={}",
                    user.getId(),
                    user.getUsername(),
                    user.getRole()
            );
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private UserRole requireRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException(
                    "사용자 역할은 필수입니다."
            );
        }

        return role;
    }

    private Path requirePasswordFile(Path passwordFile) {
        if (passwordFile == null) {
            throw new IllegalArgumentException(
                    "비밀번호 Secret 파일은 필수입니다."
            );
        }

        if (!Files.isRegularFile(passwordFile)
                || !Files.isReadable(passwordFile)) {
            throw new IllegalArgumentException(
                    "비밀번호 Secret 파일을 읽을 수 없습니다."
            );
        }

        return passwordFile;
    }

    private char[] readPassword(Path passwordFile)
            throws IOException {
        char[] buffer =
                new char[MAX_PASSWORD_LENGTH + 2];

        int length = 0;

        try (Reader reader = Files.newBufferedReader(
                passwordFile,
                UTF_8
        )) {
            int read;

            while (length < buffer.length
                    && (read = reader.read(
                    buffer,
                    length,
                    buffer.length - length
            )) != -1) {
                length += read;
            }

            if (reader.read() != -1) {
                throw new IllegalArgumentException(
                        "비밀번호가 너무 깁니다."
                );
            }

            while (length > 0
                    && (buffer[length - 1] == '\n'
                    || buffer[length - 1] == '\r')) {
                length--;
            }

            if (length < MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException(
                        "비밀번호는 16자 이상이어야 합니다."
                );
            }

            if (length > MAX_PASSWORD_LENGTH) {
                throw new IllegalArgumentException(
                        "비밀번호는 128자 이하여야 합니다."
                );
            }

            return Arrays.copyOf(buffer, length);
        } finally {
            Arrays.fill(buffer, '\0');
        }
    }

    private void resetPassword() throws IOException {
        String username = requireUsername(
                properties.username()
        );

        Path passwordFile = requirePasswordFile(
                properties.passwordFile()
        );

        char[] password = readPassword(passwordFile);

        try {
            User user = userAccountService.resetPassword(
                    username,
                    CharBuffer.wrap(password)
            );

            log.info(
                    "Account password reset. id={}, username={}",
                    user.getId(),
                    user.getUsername()
            );
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}