package com.rosadavi.alcoholic_rat.DTO.bar;

import jakarta.validation.constraints.*;

public record BarRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude
) {}
