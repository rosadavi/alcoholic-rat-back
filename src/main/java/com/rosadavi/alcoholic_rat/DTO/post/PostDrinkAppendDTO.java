package com.rosadavi.alcoholic_rat.DTO.post;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PostDrinkAppendDTO(
        @NotEmpty @Valid List<PostDrinkRequestDTO> drinks
        ) {
}
