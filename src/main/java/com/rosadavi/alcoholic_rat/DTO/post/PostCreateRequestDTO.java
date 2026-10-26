package com.rosadavi.alcoholic_rat.DTO.post;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequestDTO(
        String barId,
        @Size(max = 1000) String description,
        @Valid List<PostDrinkRequestDTO> drinks
) {
}
