package com.example.model;

/**
 * Colonnes du tableau Kanban. L'ordre de déclaration est l'ordre d'affichage.
 */
public enum TaskStatus {
    A_FAIRE("À faire"),
    EN_COURS("En cours"),
    TERMINE("Terminé");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
