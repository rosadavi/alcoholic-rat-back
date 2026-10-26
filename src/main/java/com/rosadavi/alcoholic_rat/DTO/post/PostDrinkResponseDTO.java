package com.rosadavi.alcoholic_rat.DTO.post;

import com.rosadavi.alcoholic_rat.domain.post.PostDrink;

import java.math.BigDecimal;
import java.time.Instant;

public record PostDrinkResponseDTO(
        String id,
        String drinkId,
        String title,
        Integer volumeMl,
        BigDecimal alcoholPercentage,
        Instant addedAt
) {
    public PostDrinkResponseDTO(PostDrink d) {
        this(d.getId(), d.getDrink() != null ? d.getDrink().getId() : null, d.getTitle(), d.getVolumeMl(), d.getAlcoholPercentage(), d.getAddedAt());
    }
}
