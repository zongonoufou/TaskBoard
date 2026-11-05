package com.example.repository;

import com.example.model.UserModel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserModel, Long> {

    Optional<UserModel> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
