package com.example.dto;

import java.util.List;
import java.util.Map;

import com.example.model.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Statistiques agrégées renvoyées à l'interface : nombre de tableaux, de tâches,
 * répartition par statut/priorité et nombre de tâches en retard.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private long boardCount;
    private long taskCount;
    private long completedCount;
    private long overdueCount;
    private Map<TaskStatus, Long> tasksByStatus;
    private Map<String, Long> tasksByPriority;
    private List<TaskResponseDTO> upcomingTasks;
}
