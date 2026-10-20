package com.rosadavi.alcoholic_rat.services;

import com.rosadavi.alcoholic_rat.DTO.bar.BarRequestDTO;
import com.rosadavi.alcoholic_rat.domain.bar.Bar;
import com.rosadavi.alcoholic_rat.exception.BarAlreadyExistsException;
import com.rosadavi.alcoholic_rat.repository.BarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BarService {
    @Autowired
    BarRepository repository;

    public Bar register(BarRequestDTO data) {
        if(repository.existsByName(data.name())) {
            throw new BarAlreadyExistsException(data.name());
        }

        Bar bar = new Bar(data.name(), data.description(), data.latitude(), data.longitude());
        return repository.save(bar);
    }

    // tratar o erro (exception) pra caso nao tenha nenhum bar cadastrado
    public List<Bar> findAll() {
        return repository.findAll();
    }
}
