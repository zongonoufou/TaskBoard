package com.example.mapper;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.dto.TaskRequestDTO;
import com.example.dto.TaskResponseDTO;
import com.example.model.TaskModel;
import com.example.model.TaskStatus;

/**
 * Conversion entité <-> DTO pour les tâches.
 */
@Component
public class TaskMapper {

    public TaskModel toEntity(TaskRequestDTO dto) {
        return TaskModel.builder()
                .title(dto.getTitle().trim())
                .description(trimToNull(dto.getDescription()))
                .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.A_FAIRE)
                .build();
    }

    public TaskResponseDTO toResponseDTO(TaskModel task) {
        boolean overdue = task.getDueDate() != null
                && task.getStatus() != TaskStatus.TERMINE
                && task.getDueDate().isBefore(LocalDate.now());

        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .position(task.getPosition())
                .assignee(task.getAssignee())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .boardId(task.getBoard() != null ? task.getBoard().getId() : null)
                .overdue(overdue)
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
