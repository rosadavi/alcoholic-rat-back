package com.rosadavi.alcoholic_rat.DTO.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO (
        @NotBlank @Email String login,
        @NotBlank @Size(min = 8, max = 72) String password
) {}
