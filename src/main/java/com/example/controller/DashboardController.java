package com.example.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.DashboardStatsDTO;
import com.example.dto.TaskResponseDTO;
import com.example.mapper.TaskMapper;
import com.example.model.TaskPriority;
import com.example.model.TaskStatus;
import com.example.repository.BoardRepository;
import com.example.repository.TaskRepository;
import com.example.service.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Tableau de bord : statistiques de l'utilisateur connecté. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tableau de bord", description = "Statistiques et prochaines échéances")
public class DashboardController {

    private final BoardRepository boardRepository;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final CurrentUser currentUser;

    @GetMapping("/stats")
    @Operation(summary = "Mes statistiques",
            description = "Compteurs par statut/priorité, tâches en retard, prochaines échéances")
    public DashboardStatsDTO stats() {
        Long userId = currentUser.id();

        Map<TaskStatus, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : taskRepository.countByStatusForOwner(userId)) {
            byStatus.put((TaskStatus) row[0], ((Number) row[1]).longValue());
        }

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (TaskPriority priority : TaskPriority.values()) {
            byPriority.put(priority.name(), 0L);
        }
        for (Object[] row : taskRepository.countByPriorityForOwner(userId)) {
            byPriority.put(((TaskPriority) row[0]).name(), ((Number) row[1]).longValue());
        }

        List<TaskResponseDTO> upcoming = taskRepository
                .findUpcomingByOwner(userId, TaskStatus.TERMINE)
                .stream()
                .limit(10)
                .map(taskMapper::toResponseDTO)
                .toList();

        return DashboardStatsDTO.builder()
                .boardCount(boardRepository.countByOwnerId(userId))
                .taskCount(taskRepository.countByBoardOwnerId(userId))
                .completedCount(byStatus.get(TaskStatus.TERMINE))
                .overdueCount(taskRepository.countOverdue(userId, TaskStatus.TERMINE, LocalDate.now()))
                .tasksByStatus(byStatus)
                .tasksByPriority(byPriority)
                .upcomingTasks(upcoming)
                .build();
    }
}
