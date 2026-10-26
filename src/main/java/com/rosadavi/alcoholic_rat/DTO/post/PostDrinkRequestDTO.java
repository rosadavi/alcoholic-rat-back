package com.rosadavi.alcoholic_rat.DTO.post;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PostDrinkRequestDTO (
        String drinkId,
        @Size(max = 100) String title,
        @Positive @Max(5000) Integer volumeMl,
        @DecimalMin("0") @DecimalMax("100")BigDecimal alcoholPercentage
        ){}
