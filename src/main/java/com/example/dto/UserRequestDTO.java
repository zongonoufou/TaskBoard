package com.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mise à jour du profil : le mot de passe est facultatif
 * (s'il est vide, l'ancien mot de passe est conservé).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email n'est pas valide")
    @Size(max = 180, message = "L'email ne doit pas dépasser 180 caractères")
    private String email;

    @Size(min = 6, max = 72, message = "Le mot de passe doit contenir entre 6 et 72 caractères")
    private String password;
}
