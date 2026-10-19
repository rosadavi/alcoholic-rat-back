package com.rosadavi.alcoholic_rat.repository;

import com.rosadavi.alcoholic_rat.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, String> {
    UserDetails findByLogin(String login);
    boolean existsByLogin(String login);
}
