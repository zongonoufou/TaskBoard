package com.example.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.BoardRequestDTO;
import com.example.dto.BoardResponseDTO;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.BoardMapper;
import com.example.model.BoardModel;
import com.example.model.TaskModel;
import com.example.model.TaskStatus;
import com.example.model.UserModel;
import com.example.repository.BoardRepository;
import com.example.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logique métier des tableaux. Chaque opération vérifie que le tableau
 * appartient bien à l'utilisateur authentifié (sinon 403).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final BoardMapper boardMapper;

    // CREATE

    @Transactional
    public BoardResponseDTO create(Long ownerId, BoardRequestDTO dto) {
        UserModel owner = userService.getUserEntityById(ownerId);
        BoardModel board = boardMapper.toEntity(dto);
        board.setOwner(owner);
        return boardMapper.toResponseDTO(boardRepository.save(board));
    }

    // READ

    public List<BoardResponseDTO> myBoards(Long ownerId) {
        return boardRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::withCounts)
                .toList();
    }

    public BoardResponseDTO get(Long id, Long requesterId) {
        BoardModel board = getOwnedBoard(id, requesterId);
        return withCounts(board);
    }

    public BoardModel getBoardEntityById(Long id) {
        return boardRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tableau introuvable avec id=" + id));
    }

    /** Récupère le tableau ET vérifie la propriété (403 si ce n'est pas le bon utilisateur). */
    public BoardModel getOwnedBoard(Long boardId, Long userId) {
        BoardModel board = getBoardEntityById(boardId);
        assertOwnership(board, userId);
        return board;
    }

    public void assertOwnership(BoardModel board, Long userId) {
        if (board.getOwner() == null || !board.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Ce tableau ne vous appartient pas");
        }
    }

    // UPDATE

    @Transactional
    public BoardResponseDTO update(Long id, Long requesterId, BoardRequestDTO dto) {
        BoardModel board = getOwnedBoard(id, requesterId);
        board.setTitle(dto.getTitle().trim());
        board.setDescription(dto.getDescription());
        if (dto.getColor() != null && !dto.getColor().isBlank()) {
            board.setColor(dto.getColor());
        }
        return boardMapper.toResponseDTO(boardRepository.save(board));
    }

    // DELETE

    @Transactional
    public void delete(Long id, Long requesterId) {
        BoardModel board = getOwnedBoard(id, requesterId);
        // Les tâches sont supprimées par la cascade JPA (@OneToMany cascade = ALL)
        boardRepository.delete(board);
    }

    /** Ajoute les compteurs de tâches (par statut + total) au DTO : une seule requête. */
    public BoardResponseDTO withCounts(BoardModel board) {
        List<TaskModel> tasks = taskRepository
                .findByBoardIdOrderByStatusAscPositionAscIdAsc(board.getId());
        Map<TaskStatus, Long> counts = new LinkedHashMap<>();
        for (TaskModel task : tasks) {
            counts.merge(task.getStatus(), 1L, Long::sum);
        }
        return boardMapper.toResponseDTO(board, counts, tasks.size());
    }
}
