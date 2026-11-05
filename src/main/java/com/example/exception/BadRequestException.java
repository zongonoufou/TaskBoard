package com.example.exception;

/**
 * Erreur métier générique -> HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
