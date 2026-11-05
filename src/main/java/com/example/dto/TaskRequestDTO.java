package com.example.dto;

import java.time.LocalDate;

import com.example.model.TaskPriority;
import com.example.model.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Création / mise à jour d'une tâche. Tous les champs sauf le titre sont facultatifs,
 * ce qui permet de faire des mises à jour partielles (ex. déplacer une carte :
 * <code>{"title": "...", "status": "EN_COURS"}</code>).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 2, max = 100, message = "Le titre doit contenir entre 2 et 100 caractères")
    private String title;

    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDate dueDate;

    @Size(max = 80, message = "Le nom de l'assigné ne doit pas dépasser 80 caractères")
    private String assignee;

    /** Position dans la colonne (ignorée à la création : calculée automatiquement). */
    private Integer position;
}
