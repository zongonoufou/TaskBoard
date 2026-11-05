package com.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email n'est pas valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
