package com.example.repository;

import com.example.model.TaskModel;
import com.example.model.TaskPriority;
import com.example.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskModel, Long> {

    List<TaskModel> findByBoardIdOrderByStatusAscPositionAscIdAsc(Long boardId);

    List<TaskModel> findByBoardIdAndStatusOrderByPositionAscIdAsc(Long boardId, TaskStatus status);

    Optional<TaskModel> findByIdAndBoardId(Long id, Long boardId);

    long countByBoardOwnerId(Long ownerId);

    /** Nombre de tâches par statut pour un tableau (compteurs du Kanban). */
    @Query("""
            select t.status, count(t)
            from TaskModel t
            where t.board.id = :boardId
            group by t.status
            """)
    List<Object[]> countByStatusForBoard(@Param("boardId") Long boardId);

    /** Nombre de tâches par statut pour tous les tableaux d'un utilisateur (dashboard). */
    @Query("""
            select t.status, count(t)
            from TaskModel t
            where t.board.owner.id = :ownerId
            group by t.status
            """)
    List<Object[]> countByStatusForOwner(@Param("ownerId") Long ownerId);

    /** Nombre de tâches par priorité pour tous les tableaux d'un utilisateur (dashboard). */
    @Query("""
            select t.priority, count(t)
            from TaskModel t
            where t.board.owner.id = :ownerId
            group by t.priority
            """)
    List<Object[]> countByPriorityForOwner(@Param("ownerId") Long ownerId);

    /** Position maximale dans une colonne : sert à ajouter une carte en fin de colonne. */
    @Query("select coalesce(max(t.position), -1) from TaskModel t where t.board.id = :boardId and t.status = :status")
    int findMaxPosition(@Param("boardId") Long boardId, @Param("status") TaskStatus status);

    /** Tâches non terminées avec une échéance, triées par date (prochaines échéances). */
    @Query("""
            select t from TaskModel t
            join fetch t.board b
            where b.owner.id = :ownerId
              and t.status <> :doneStatus
              and t.dueDate is not null
            order by t.dueDate asc
            """)
    List<TaskModel> findUpcomingByOwner(@Param("ownerId") Long ownerId,
                                        @Param("doneStatus") TaskStatus doneStatus);

    @Query("""
            select count(t) from TaskModel t
            where t.board.owner.id = :ownerId
              and t.status <> :doneStatus
              and t.dueDate is not null
              and t.dueDate < :today
            """)
    long countOverdue(@Param("ownerId") Long ownerId,
                      @Param("doneStatus") TaskStatus doneStatus,
                      @Param("today") LocalDate today);

    @Query("""
            select count(t) from TaskModel t
            where t.board.owner.id = :ownerId and t.priority = :priority and t.status <> :doneStatus
            """)
    long countOpenByPriority(@Param("ownerId") Long ownerId,
                             @Param("priority") TaskPriority priority,
                             @Param("doneStatus") TaskStatus doneStatus);
}
