package com.rosadavi.alcoholic_rat.controller;

import com.rosadavi.alcoholic_rat.DTO.bar.BarRequestDTO;
import com.rosadavi.alcoholic_rat.DTO.bar.BarResponseDTO;
import com.rosadavi.alcoholic_rat.domain.bar.Bar;
import com.rosadavi.alcoholic_rat.services.BarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/bars")
@RequiredArgsConstructor
public class BarController {
    @Autowired
    BarService barService;

    @PostMapping
    public ResponseEntity<Bar> createBar(@RequestBody @Valid BarRequestDTO data) {
        var bar = barService.register(data);
        return ResponseEntity.created(URI.create("/bars/" + bar.getId())).body(bar);
    }

    @GetMapping
    public ResponseEntity<List<Bar>> listAllBars() {
        var bars = barService.findAll();
        return ResponseEntity.ok().body(bars);
    }
}
