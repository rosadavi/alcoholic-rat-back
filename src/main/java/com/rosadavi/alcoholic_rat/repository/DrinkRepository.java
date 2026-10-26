package com.rosadavi.alcoholic_rat.repository;

import com.rosadavi.alcoholic_rat.domain.drink.Drink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrinkRepository extends JpaRepository<Drink, String> {
    boolean existsByTitleIgnoreCase(String name);
}
