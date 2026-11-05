package com.example.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Sonde de santé publique (utilisée par le front pour afficher l'état de l'API). */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Santé")
@SecurityRequirements
public class HealthController {

    @GetMapping
    @Operation(summary = "L'API est-elle disponible ?")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "application", "TaskBoard", "version", "1.0.0");
    }
}
