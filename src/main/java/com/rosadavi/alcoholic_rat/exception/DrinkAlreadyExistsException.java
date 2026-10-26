package com.rosadavi.alcoholic_rat.exception;

public class DrinkAlreadyExistsException extends RuntimeException {
    public DrinkAlreadyExistsException(String drink) {
        super("Drink already exists: " + drink);
    }
}
