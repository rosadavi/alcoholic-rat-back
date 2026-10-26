package com.rosadavi.alcoholic_rat.services;

import com.rosadavi.alcoholic_rat.DTO.drink.DrinkRequestDTO;
import com.rosadavi.alcoholic_rat.domain.drink.Drink;
import com.rosadavi.alcoholic_rat.exception.DrinkAlreadyExistsException;
import com.rosadavi.alcoholic_rat.repository.DrinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DrinkService {
    @Autowired
    DrinkRepository repository;

    public Drink register(DrinkRequestDTO data) {
        if(repository.existsByTitleIgnoreCase(data.title().trim())) {
            throw new DrinkAlreadyExistsException(data.title());
        }

        Drink drink = new Drink(data.title().trim(), data.description(), data.volumeMl(), data.alcoholPercentage());
        return repository.save(drink);
    }

    public List<Drink> findAll() {
        // Tratar mensagem de erro caso não exista nenhum drink cadastrado
        return repository.findAll();
    }
}
