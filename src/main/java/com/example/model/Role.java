package com.example.model;

/**
 * Rôles disponibles. Le rôle est utilisé par Spring Security
 * ({@code hasRole('ADMIN')}) et facilite l'ajout d'un back-office plus tard.
 */
public enum Role {
    USER,
    ADMIN
}
