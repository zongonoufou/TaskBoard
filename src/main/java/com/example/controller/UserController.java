package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.ChangePasswordDTO;
import com.example.dto.UserRequestDTO;
import com.example.dto.UserResponseDTO;
import com.example.service.CurrentUser;
import com.example.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Profil de l'utilisateur connecté (+ administration réservée aux ADMIN). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Profil et administration")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    @Operation(summary = "Mon profil")
    public ResponseEntity<UserResponseDTO> me() {
        return ResponseEntity.ok(userService.getUserById(currentUser.id()));
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour mon profil", description = "Le mot de passe est facultatif")
    public ResponseEntity<UserResponseDTO> updateMe(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.updateUser(currentUser.id(), dto));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Changer mon mot de passe")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(currentUser.id(), dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @Operation(summary = "Supprimer mon compte", description = "Supprime aussi tous mes tableaux et tâches")
    public ResponseEntity<Void> deleteMe() {
        userService.deleteUser(currentUser.id());
        return ResponseEntity.noContent().build();
    }

    // --- Administration ---

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les utilisateurs (ADMIN)")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Operation(summary = "Consulter un utilisateur (soi-même ou ADMIN)")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un utilisateur (ADMIN)")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un utilisateur (ADMIN)")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur (ADMIN)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
