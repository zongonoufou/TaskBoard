package com.example.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.example.model.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long ownerId;
    private String ownerName;

    /** Nombre de tâches du tableau. */
    private long taskCount;

    /** Répartition des tâches par statut (utile pour les compteurs du Kanban). */
    private Map<TaskStatus, Long> taskCountByStatus;
}
