package com.rosadavi.alcoholic_rat.domain.post;

import com.rosadavi.alcoholic_rat.domain.drink.Drink;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Table(name = "post_drink")
@Entity(name = "post_drink")
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class PostDrink {

    public PostDrink(Post post, Drink drink, String title, Integer volumeMl, BigDecimal alcoholPercentage) {
        this.post = post;
        this.drink = drink;
        this.title = title;
        this.volumeMl = volumeMl;
        this.alcoholPercentage = alcoholPercentage;
        this.addedAt = Instant.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drink_id", updatable = false)
    private Drink drink;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false)
    private Integer volumeMl;

    @Column(nullable = false, updatable = false, precision = 4, scale = 1)
    private BigDecimal alcoholPercentage;

    @Column(nullable = false, updatable = false)
    private Instant addedAt;
}
