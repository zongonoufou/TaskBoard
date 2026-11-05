package com.example.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.dto.BoardRequestDTO;
import com.example.dto.BoardResponseDTO;
import com.example.model.BoardModel;
import com.example.model.TaskStatus;

/**
 * Conversion entité <-> DTO pour les tableaux.
 */
@Component
public class BoardMapper {

    public BoardModel toEntity(BoardRequestDTO dto) {
        return BoardModel.builder()
                .title(dto.getTitle().trim())
                .description(trimToNull(dto.getDescription()))
                .color(dto.getColor())
                .build();
    }

    public BoardResponseDTO toResponseDTO(BoardModel board) {
        return toResponseDTO(board, Map.of(), 0L);
    }

    public BoardResponseDTO toResponseDTO(BoardModel board, Map<TaskStatus, Long> countByStatus, long taskCount) {
        Map<TaskStatus, Long> full = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            full.put(status, countByStatus.getOrDefault(status, 0L));
        }
        return BoardResponseDTO.builder()
                .id(board.getId())
                .title(board.getTitle())
                .description(board.getDescription())
                .color(board.getColor())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .ownerId(board.getOwner() != null ? board.getOwner().getId() : null)
                .ownerName(board.getOwner() != null ? board.getOwner().getName() : null)
                .taskCount(taskCount)
                .taskCountByStatus(full)
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
