package com.example.dto;

import com.example.model.TaskStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Corps de requête pour déplacer une carte (changement de colonne / d'ordre).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveTaskRequestDTO {

    @NotNull(message = "Le statut cible est obligatoire")
    private TaskStatus status;

    /** Nouvelle position (0 = première de la colonne). Si absent, la carte va en fin de colonne. */
    private Integer position;
}
