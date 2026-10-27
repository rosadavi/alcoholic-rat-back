package com.rosadavi.alcoholic_rat.exception;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String message) {
        super("Post not found: " + message);
    }
}
