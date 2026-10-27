package com.rosadavi.alcoholic_rat.exception;


// A mensagem de erros FORBIDDEN é aplicada dependendo do contexto, então não é necessário adicionar "Post forbidden" na mensagem de erro.
public class PostForbiddenException extends RuntimeException {
    public PostForbiddenException(String message) {
        super(message);
    }
}
