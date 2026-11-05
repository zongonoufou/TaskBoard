package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.BoardRequestDTO;
import com.example.dto.BoardResponseDTO;
import com.example.service.BoardService;
import com.example.service.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Tableaux de l'utilisateur connecté. L'identifiant du propriétaire n'est jamais
 * accepté dans l'URL : il est toujours déduit du token JWT.
 */
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Tag(name = "Tableaux", description = "Créer et gérer ses tableaux (projets)")
public class BoardController {

    private final BoardService boardService;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "Créer un tableau")
    public ResponseEntity<BoardResponseDTO> create(@Valid @RequestBody BoardRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.create(currentUser.id(), dto));
    }

    @GetMapping
    @Operation(summary = "Mes tableaux", description = "Avec compteurs de tâches par statut")
    public ResponseEntity<List<BoardResponseDTO>> myBoards() {
        return ResponseEntity.ok(boardService.myBoards(currentUser.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un tableau")
    public ResponseEntity<BoardResponseDTO> byId(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.get(id, currentUser.id()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un tableau")
    public ResponseEntity<BoardResponseDTO> update(@PathVariable Long id,
                                                   @Valid @RequestBody BoardRequestDTO dto) {
        return ResponseEntity.ok(boardService.update(id, currentUser.id(), dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un tableau", description = "Supprime aussi ses tâches")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boardService.delete(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
