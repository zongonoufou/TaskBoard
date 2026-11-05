package com.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tâche d'un tableau. Le statut correspond à la colonne du Kanban.
 */
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_board", columnList = "board_id"),
        @Index(name = "idx_tasks_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.A_FAIRE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MOYENNE;

    /** Échéance facultative de la tâche. */
    private LocalDate dueDate;

    /**
     * Position dans la colonne : permet le réordonnancement (glisser-déposer).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    /** Nom de l'assigné (champ libre, pas encore de gestion de projet multi-utilisateurs). */
    @Column(length = 80)
    private String assignee;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardModel board;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = TaskStatus.A_FAIRE;
        }
        if (this.priority == null) {
            this.priority = TaskPriority.MOYENNE;
        }
        if (this.position == null) {
            this.position = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
