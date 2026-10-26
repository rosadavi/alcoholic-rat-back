package com.rosadavi.alcoholic_rat.DTO.drink;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DrinkRequestDTO(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 1000) String description,
        @NotNull @Positive @Max(5000) Integer volumeMl,
        @NotNull @DecimalMin("0") @DecimalMax("100")BigDecimal alcoholPercentage
        ) {}
