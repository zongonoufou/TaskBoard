package com.example.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.model.TaskPriority;
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
public class TaskResponseDTO {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Integer position;
    private String assignee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long boardId;

    /** Vrai si la tâche est en retard (échéance dépassée et non terminée). */
    private boolean overdue;
}
