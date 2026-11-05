package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.MoveTaskRequestDTO;
import com.example.dto.TaskPriorityDTO;
import com.example.dto.TaskRequestDTO;
import com.example.dto.TaskResponseDTO;
import com.example.model.TaskStatus;
import com.example.service.CurrentUser;
import com.example.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Tâches d'un tableau. Chaque route vérifie que le tableau puis la tâche
 * appartiennent à l'utilisateur connecté.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Tâches", description = "Tâches du Kanban d'un tableau")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "Créer une tâche")
    public ResponseEntity<TaskResponseDTO> create(@PathVariable Long boardId,
                                                  @Valid @RequestBody TaskRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(boardId, currentUser.id(), dto));
    }

    @GetMapping
    @Operation(summary = "Lister les tâches", description = "Filtre optionnel : ?status=EN_COURS")
    public ResponseEntity<List<TaskResponseDTO>> list(@PathVariable Long boardId,
                                                      @RequestParam(required = false) TaskStatus status) {
        return ResponseEntity.ok(taskService.getTasksByBoard(boardId, currentUser.id(), status));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Détail d'une tâche")
    public ResponseEntity<TaskResponseDTO> byId(@PathVariable Long boardId, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskById(boardId, taskId, currentUser.id()));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Mettre à jour une tâche")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long boardId,
                                                  @PathVariable Long taskId,
                                                  @Valid @RequestBody TaskRequestDTO dto) {
        return ResponseEntity.ok(taskService.updateTask(boardId, taskId, currentUser.id(), dto));
    }

    @PatchMapping("/{taskId}/move")
    @Operation(summary = "Déplacer une carte", description = "Colonne (statut) + position")
    public ResponseEntity<TaskResponseDTO> move(@PathVariable Long boardId,
                                                @PathVariable Long taskId,
                                                @Valid @RequestBody MoveTaskRequestDTO dto) {
        return ResponseEntity.ok(taskService.moveTask(boardId, taskId, currentUser.id(), dto));
    }

    @PatchMapping("/{taskId}/toggle")
    @Operation(summary = "Basculer terminé / à faire")
    public ResponseEntity<TaskResponseDTO> toggle(@PathVariable Long boardId, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.toggleTask(boardId, taskId, currentUser.id()));
    }

    @PatchMapping("/{taskId}/priority")
    @Operation(summary = "Changer la priorité")
    public ResponseEntity<TaskResponseDTO> priority(@PathVariable Long boardId,
                                                    @PathVariable Long taskId,
                                                    @Valid @RequestBody TaskPriorityDTO dto) {
        return ResponseEntity.ok(taskService.changePriority(boardId, taskId, currentUser.id(), dto.getPriority()));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Supprimer une tâche")
    public ResponseEntity<Void> delete(@PathVariable Long boardId, @PathVariable Long taskId) {
        taskService.deleteTask(boardId, taskId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
