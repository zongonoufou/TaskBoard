package com.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tableau (projet) appartenant à un utilisateur. Un tableau contient des tâches.
 */
@Entity
@Table(name = "boards", indexes = @Index(name = "idx_boards_owner", columnList = "owner_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    /** Couleur d'accent du tableau (format hexadécimal, ex. #6366f1). */
    @Column(length = 9)
    @Builder.Default
    private String color = "#6366f1";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserModel owner;

    /** Suppression en cascade des tâches du tableau. */
    @Builder.Default
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskModel> tasks = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.color == null || this.color.isBlank()) {
            this.color = "#6366f1";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
