package io.github.shhome.homehub.auth.dto;

import io.github.shhome.homehub.user.entity.User;
import io.github.shhome.homehub.user.entity.UserRole;

public record CurrentUserResponse(
        Long id,
        String username,
        UserRole role
) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}