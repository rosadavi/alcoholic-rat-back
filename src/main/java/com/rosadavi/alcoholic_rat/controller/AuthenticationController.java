package com.rosadavi.alcoholic_rat.controller;

import com.rosadavi.alcoholic_rat.DTO.user.RegisterRequestDTO;
import com.rosadavi.alcoholic_rat.DTO.user.RegisterResponseDTO;
import com.rosadavi.alcoholic_rat.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody @Valid RegisterRequestDTO data) {
        var user = userService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponseDTO(user));
    }
}
