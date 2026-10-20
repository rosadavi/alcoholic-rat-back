package com.rosadavi.alcoholic_rat.controller;

import com.rosadavi.alcoholic_rat.DTO.user.LoginRequestDTO;
import com.rosadavi.alcoholic_rat.DTO.user.LoginResponseDTO;
import com.rosadavi.alcoholic_rat.DTO.user.RegisterRequestDTO;
import com.rosadavi.alcoholic_rat.DTO.user.RegisterResponseDTO;
import com.rosadavi.alcoholic_rat.auth.security.TokenService;
import com.rosadavi.alcoholic_rat.domain.user.User;
import com.rosadavi.alcoholic_rat.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody @Valid RegisterRequestDTO data) {
        var user = userService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponseDTO(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        var credentials = new UsernamePasswordAuthenticationToken(data.login().trim().toLowerCase(), data.password());
        var auth = authenticationManager.authenticate(credentials);
        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }
}
