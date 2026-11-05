package com.example.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.ChangePasswordDTO;
import com.example.dto.RegisterRequestDTO;
import com.example.dto.UserRequestDTO;
import com.example.dto.UserResponseDTO;
import com.example.exception.BadRequestException;
import com.example.exception.DuplicateResourceException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.UserMapper;
import com.example.model.UserModel;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logique métier des utilisateurs : inscription, profil, mot de passe.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // CREATE

    /** Inscription publique : le rôle est TOUJOURS USER (jamais fourni par le client). */
    @Transactional
    public UserResponseDTO register(RegisterRequestDTO requestDTO) {
        String email = UserMapper.normalizeEmail(requestDTO.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà : " + email);
        }

        UserModel user = userMapper.toEntity(requestDTO);
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    /** Création d'un utilisateur par un administrateur. */
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        String email = UserMapper.normalizeEmail(requestDTO.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà : " + email);
        }
        if (requestDTO.getPassword() == null || requestDTO.getPassword().isBlank()) {
            throw new BadRequestException("Le mot de passe est obligatoire pour créer un utilisateur");
        }

        UserModel user = UserModel.builder()
                .name(requestDTO.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(requestDTO.getPassword()))
                .build();
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    // READ

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    public UserResponseDTO getUserById(Long id) {
        return userMapper.toResponseDTO(getUserEntityById(id));
    }

    public UserModel getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id=" + id));
    }

    // UPDATE

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO) {
        UserModel existingUser = getUserEntityById(id);

        String newEmail = UserMapper.normalizeEmail(requestDTO.getEmail());
        boolean emailHasChanged = !existingUser.getEmail().equalsIgnoreCase(newEmail);
        if (emailHasChanged && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà : " + newEmail);
        }

        existingUser.setName(requestDTO.getName().trim());
        existingUser.setEmail(newEmail);
        // Mot de passe facultatif : vide = inchangé
        if (requestDTO.getPassword() != null && !requestDTO.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        }
        return userMapper.toResponseDTO(userRepository.save(existingUser));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO requestDTO) {
        UserModel user = getUserEntityById(userId);
        if (!passwordEncoder.matches(requestDTO.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Le mot de passe actuel est incorrect");
        }
        if (passwordEncoder.matches(requestDTO.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);
    }

    // DELETE

    @Transactional
    public void deleteUser(Long id) {
        UserModel user = getUserEntityById(id);
        // Cascade JPA : tableaux puis tâches (voir @OneToMany dans UserModel)
        userRepository.delete(user);
    }
}
