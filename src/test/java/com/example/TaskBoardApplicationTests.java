package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie que le contexte Spring démarre (avec une base H2 en mémoire).
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskBoardApplicationTests {

    @Test
    void contextLoads() {
        // Le test échoue si un bean est mal configuré
    }
}
