package com.rosadavi.alcoholic_rat.DTO.bar;

public record BarResponseDTO(
        String name,
        String description,
        Double latitude,
        Double longitude
) {
}
