package io.github.shhome.homehub.user.service;

import io.github.shhome.homehub.common.exception.BusinessException;
import io.github.shhome.homehub.user.entity.User;
import io.github.shhome.homehub.user.entity.UserRole;
import io.github.shhome.homehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.shhome.homehub.common.exception.ErrorCode.USER_ALREADY_EXISTS;
import static io.github.shhome.homehub.common.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(
            String username,
            CharSequence rawPassword,
            UserRole role
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(USER_ALREADY_EXISTS);
        }

        String passwordHash =
                passwordEncoder.encode(rawPassword);

        User user = User.of(
                username,
                passwordHash,
                role
        );

        return userRepository.save(user);
    }

    @Transactional
    public User enable(String username) {
        User user = getUser(username);
        user.enable();

        return user;
    }

    @Transactional
    public User disable(String username) {
        User user = getUser(username);
        user.disable();

        return user;
    }

    @Transactional
    public User resetPassword(
            String username,
            CharSequence rawPassword
    ) {
        User user = getUser(username);

        String passwordHash =
                passwordEncoder.encode(rawPassword);

        user.changePasswordHash(passwordHash);

        return user;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new BusinessException(USER_NOT_FOUND)
                );
    }
}
