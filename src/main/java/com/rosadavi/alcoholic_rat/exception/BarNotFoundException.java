package com.rosadavi.alcoholic_rat.exception;

public class BarNotFoundException extends RuntimeException {
    public BarNotFoundException(String message) {
        super("Bar not found: " + message);
    }
}
