package com.example.repository;

import com.example.model.BoardModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<BoardModel, Long> {

    List<BoardModel> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("select b from BoardModel b join fetch b.owner where b.id = :id")
    Optional<BoardModel> findByIdWithOwner(@Param("id") Long id);

    long countByOwnerId(Long ownerId);
}
