package com.rosadavi.alcoholic_rat.repository;

import com.rosadavi.alcoholic_rat.domain.bar.Bar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarRepository extends JpaRepository<Bar, String> {
    boolean existsByNameIgnoreCase(String name);
}
