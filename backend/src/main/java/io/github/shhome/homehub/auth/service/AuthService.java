package io.github.shhome.homehub.auth.service;

import io.github.shhome.homehub.auth.dto.CurrentUserResponse;
import io.github.shhome.homehub.common.exception.BusinessException;
import io.github.shhome.homehub.user.entity.User;
import io.github.shhome.homehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.shhome.homehub.common.exception.ErrorCode.AUTHENTICATION_REQUIRED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    public CurrentUserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new BusinessException(AUTHENTICATION_REQUIRED));

        return CurrentUserResponse.from(user);
    }
}
