package com.rosadavi.alcoholic_rat.exception;

public class DrinkNotFoundException extends RuntimeException {
    public DrinkNotFoundException(String message) {
        super("Drink not found: " + message);
    }
}
