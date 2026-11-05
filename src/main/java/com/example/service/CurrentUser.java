package com.example.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.exception.ResourceNotFoundException;
import com.example.security.UserPrincipal;

/**
 * Accès à l'utilisateur authentifié (id, nom, rôle) depuis n'importe quelle couche.
 */
@Component
public class CurrentUser {

    public UserPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new ResourceNotFoundException("Aucun utilisateur authentifié");
    }

    public Long id() {
        return get().getId();
    }

    /** Le compte peut-il administrer l'application ? */
    public boolean isAdmin() {
        return get().getRole() == com.example.model.Role.ADMIN;
    }
}
