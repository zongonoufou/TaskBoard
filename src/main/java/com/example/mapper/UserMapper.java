package com.example.mapper;

import org.springframework.stereotype.Component;

import com.example.dto.RegisterRequestDTO;
import com.example.dto.UserResponseDTO;
import com.example.model.UserModel;

/**
 * Conversion entité <-> DTO pour les utilisateurs.
 */
@Component
public class UserMapper {

    public UserModel toEntity(RegisterRequestDTO dto) {
        return UserModel.builder()
                .name(dto.getName().trim())
                .email(normalizeEmail(dto.getEmail()))
                .password(dto.getPassword()) // haché plus tard par le service
                .build();
    }

    public UserResponseDTO toResponseDTO(UserModel user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /** Les emails sont stockés et comparés en minuscules. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
