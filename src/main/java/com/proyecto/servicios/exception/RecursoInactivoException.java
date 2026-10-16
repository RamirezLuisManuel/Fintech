package com.proyecto.servicios.exception;

public class RecursoInactivoException extends RuntimeException {
    public RecursoInactivoException(String message) {
        super(message);
    }
}