package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDTO {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Durée de validité du token en secondes. */
    private long expiresIn;

    private UserResponseDTO user;
}
