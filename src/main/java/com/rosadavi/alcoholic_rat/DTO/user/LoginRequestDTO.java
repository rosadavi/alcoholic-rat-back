package com.rosadavi.alcoholic_rat.DTO.user;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String login,
        @NotBlank String password
) {
}
