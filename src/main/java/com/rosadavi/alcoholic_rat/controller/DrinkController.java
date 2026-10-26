package com.rosadavi.alcoholic_rat.controller;

import com.rosadavi.alcoholic_rat.DTO.drink.DrinkRequestDTO;
import com.rosadavi.alcoholic_rat.domain.drink.Drink;
import com.rosadavi.alcoholic_rat.services.DrinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/drinks")
@RequiredArgsConstructor
public class DrinkController {
    @Autowired
    DrinkService drinkService;

    @PostMapping
    public ResponseEntity<Drink> createDrink(@RequestBody @Valid DrinkRequestDTO data) {
        var drink = drinkService.register(data);
        return ResponseEntity.created(URI.create("/drinks/" + drink.getId())).body(drink);
    }

    @GetMapping
    public ResponseEntity<List<Drink>> getAllDrinks() {
        var drinks = drinkService.findAll();
        return ResponseEntity.ok().body(drinks);
    }
}
