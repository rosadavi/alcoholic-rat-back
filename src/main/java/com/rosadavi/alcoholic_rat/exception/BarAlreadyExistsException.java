package com.rosadavi.alcoholic_rat.exception;

public class BarAlreadyExistsException extends RuntimeException {
    public BarAlreadyExistsException(String bar) {
        super("Bar already exists: " + bar);
    }
}
