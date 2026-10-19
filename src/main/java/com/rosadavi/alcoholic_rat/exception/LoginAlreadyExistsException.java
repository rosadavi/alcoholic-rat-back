package com.rosadavi.alcoholic_rat.exception;

public class LoginAlreadyExistsException extends RuntimeException{
    public LoginAlreadyExistsException(String login) {
        super("Login already exists: " + login);
    }
}
