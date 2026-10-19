package com.rosadavi.alcoholic_rat.services;

import com.rosadavi.alcoholic_rat.DTO.user.RegisterRequestDTO;
import com.rosadavi.alcoholic_rat.domain.user.User;
import com.rosadavi.alcoholic_rat.domain.user.UserRole;
import com.rosadavi.alcoholic_rat.exception.LoginAlreadyExistsException;
import com.rosadavi.alcoholic_rat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public User register (RegisterRequestDTO data) {
        String login = data.login().trim().toLowerCase();

        if(repository.existsByLogin(login)) {
            throw new LoginAlreadyExistsException(login);
        }

        User user = new User(login, passwordEncoder.encode(data.password()), UserRole.USER);
        return repository.save(user);
    }
}
