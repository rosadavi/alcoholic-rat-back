package com.rosadavi.alcoholic_rat.DTO.user;

import com.rosadavi.alcoholic_rat.domain.user.User;
import com.rosadavi.alcoholic_rat.domain.user.UserRole;

public record RegisterResponseDTO(
        String id,
        String login,
        UserRole role
) {
    public RegisterResponseDTO(User user) {
        this(user.getId(), user.getLogin(), user.getRole());
    }
}
