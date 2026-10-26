package com.rosadavi.alcoholic_rat.domain.drink;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Table(name = "drinks")
@Entity(name = "drinks")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Drink {
    public Drink(String title, String description, Integer volumeMl, BigDecimal alcoholPercentage) {
        this.title = title;
        this.description = description;
        this.volumeMl = volumeMl;
        this.alcoholPercentage = alcoholPercentage;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false)
    private String title;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private Integer volumeMl;
    @Column(nullable = false)
    private BigDecimal alcoholPercentage;
}
