package com.example.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.MoveTaskRequestDTO;
import com.example.dto.TaskRequestDTO;
import com.example.dto.TaskResponseDTO;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.TaskMapper;
import com.example.model.BoardModel;
import com.example.model.TaskModel;
import com.example.model.TaskPriority;
import com.example.model.TaskStatus;
import com.example.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logique métier des tâches : création, mise à jour, déplacement (Kanban),
 * priorité, bascule terminé/à faire et suppression.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardService boardService;
    private final TaskMapper taskMapper;

    // CREATE

    @Transactional
    public TaskResponseDTO createTask(Long boardId, Long userId, TaskRequestDTO requestDTO) {
        BoardModel board = boardService.getOwnedBoard(boardId, userId);

        TaskModel task = taskMapper.toEntity(requestDTO);
        task.setBoard(board);
        if (requestDTO.getPriority() != null) {
            task.setPriority(requestDTO.getPriority());
        }
        if (requestDTO.getDueDate() != null) {
            task.setDueDate(requestDTO.getDueDate());
        }
        if (requestDTO.getAssignee() != null && !requestDTO.getAssignee().isBlank()) {
            task.setAssignee(requestDTO.getAssignee().trim());
        }
        // La nouvelle carte est placée en fin de sa colonne
        task.setPosition(taskRepository.findMaxPosition(boardId, task.getStatus()) + 1);

        return taskMapper.toResponseDTO(taskRepository.save(task));
    }

    // READ

    public List<TaskResponseDTO> getTasksByBoard(Long boardId, Long userId, TaskStatus status) {
        boardService.getOwnedBoard(boardId, userId);
        List<TaskModel> tasks = status == null
                ? taskRepository.findByBoardIdOrderByStatusAscPositionAscIdAsc(boardId)
                : taskRepository.findByBoardIdAndStatusOrderByPositionAscIdAsc(boardId, status);
        return tasks.stream().map(taskMapper::toResponseDTO).toList();
    }

    /**
     * Récupère une tâche en vérifiant qu'elle appartient bien au tableau de l'URL
     * (impossible de modifier une tâche via un autre tableau).
     */
    public TaskModel getTaskOfBoard(Long boardId, Long taskId, Long userId) {
        boardService.getOwnedBoard(boardId, userId);
        return taskRepository.findByIdAndBoardId(taskId, boardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tâche introuvable avec id=" + taskId + " dans le tableau " + boardId));
    }

    public TaskResponseDTO getTaskById(Long boardId, Long taskId, Long userId) {
        return taskMapper.toResponseDTO(getTaskOfBoard(boardId, taskId, userId));
    }

    // UPDATE

    @Transactional
    public TaskResponseDTO updateTask(Long boardId, Long taskId, Long userId, TaskRequestDTO requestDTO) {
        TaskModel task = getTaskOfBoard(boardId, taskId, userId);

        TaskStatus previousStatus = task.getStatus();
        task.setTitle(requestDTO.getTitle().trim());
        task.setDescription(blankToNull(requestDTO.getDescription()));
        if (requestDTO.getPriority() != null) {
            task.setPriority(requestDTO.getPriority());
        }
        if (requestDTO.getDueDate() != null) {
            task.setDueDate(requestDTO.getDueDate());
        }
        if (requestDTO.getAssignee() != null) {
            task.setAssignee(blankToNull(requestDTO.getAssignee()));
        }

        TaskResponseDTO saved = taskMapper.toResponseDTO(taskRepository.save(task));

        // Changement de colonne demandé : on réutilise la logique de déplacement
        // pour garder des positions cohérentes dans les deux colonnes.
        if (requestDTO.getStatus() != null && requestDTO.getStatus() != previousStatus) {
            return moveTask(boardId, taskId, userId,
                    MoveTaskRequestDTO.builder().status(requestDTO.getStatus()).build());
        }
        return saved;
    }

    /** Déplacement d'une carte (colonne + ordre) : glisser-déposer du Kanban. */
    @Transactional
    public TaskResponseDTO moveTask(Long boardId, Long taskId, Long userId, MoveTaskRequestDTO requestDTO) {
        TaskModel task = getTaskOfBoard(boardId, taskId, userId);
        TaskStatus targetStatus = requestDTO.getStatus();
        int maxPosition = taskRepository.findMaxPosition(boardId, targetStatus);
        int targetPosition = requestDTO.getPosition() == null
                ? maxPosition + 1
                : Math.max(0, Math.min(requestDTO.getPosition(), maxPosition + 1));

        // On libère la place à la position cible dans la colonne de destination
        // (puis on reconstruit des positions continues de 0 à n-1).
        List<TaskModel> targetColumn = new ArrayList<>(taskRepository
                .findByBoardIdAndStatusOrderByPositionAscIdAsc(boardId, targetStatus));
        if (task.getStatus() == targetStatus) {
            targetColumn.removeIf(candidate -> candidate.getId().equals(task.getId()));
        }
        targetColumn.add(Math.min(targetPosition, targetColumn.size()), task);

        // Et on referme le trou laissé dans la colonne d'origine
        if (task.getStatus() != targetStatus) {
            List<TaskModel> sourceColumn = new ArrayList<>(taskRepository
                    .findByBoardIdAndStatusOrderByPositionAscIdAsc(boardId, task.getStatus()));
            sourceColumn.removeIf(candidate -> candidate.getId().equals(task.getId()));
            normalizePositions(sourceColumn);
        }

        task.setStatus(targetStatus);
        normalizePositions(targetColumn);
        return taskMapper.toResponseDTO(taskRepository.save(task));
    }

    /** Change uniquement la colonne (raccourci de l'API). */
    @Transactional
    public TaskResponseDTO changeStatus(Long boardId, Long taskId, Long userId, TaskStatus status) {
        return moveTask(boardId, taskId, userId, MoveTaskRequestDTO.builder().status(status).build());
    }

    /** Bascule terminé / à faire (case à cocher de l'interface). */
    @Transactional
    public TaskResponseDTO toggleTask(Long boardId, Long taskId, Long userId) {
        TaskModel task = getTaskOfBoard(boardId, taskId, userId);
        TaskStatus target = task.getStatus() == TaskStatus.TERMINE ? TaskStatus.A_FAIRE : TaskStatus.TERMINE;
        return moveTask(boardId, taskId, userId, MoveTaskRequestDTO.builder().status(target).build());
    }

    /** Change la priorité (menu déroulant de la carte). */
    @Transactional
    public TaskResponseDTO changePriority(Long boardId, Long taskId, Long userId, TaskPriority priority) {
        TaskModel task = getTaskOfBoard(boardId, taskId, userId);
        task.setPriority(priority);
        return taskMapper.toResponseDTO(taskRepository.save(task));
    }

    // DELETE

    @Transactional
    public void deleteTask(Long boardId, Long taskId, Long userId) {
        TaskModel task = getTaskOfBoard(boardId, taskId, userId);
        TaskStatus status = task.getStatus();
        taskRepository.delete(task);
        taskRepository.flush();

        // On referme le trou laissé dans la colonne
        List<TaskModel> column = taskRepository.findByBoardIdAndStatusOrderByPositionAscIdAsc(boardId, status);
        normalizePositions(new ArrayList<>(column));
    }

    /**
     * Réindexe les positions d'une colonne (0, 1, 2, ...) puis sauvegarde.
     * Robuste aux trous et aux doublons de position.
     */
    private void normalizePositions(List<TaskModel> column) {
        column.sort(Comparator.comparingInt(TaskModel::getPosition)
                .thenComparing(TaskModel::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        int index = 0;
        for (TaskModel item : column) {
            item.setPosition(index++);
        }
        taskRepository.saveAll(column);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
