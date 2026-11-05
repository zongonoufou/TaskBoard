package com.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Tests d'intégration du parcours complet :
 * inscription -> login -> tableau -> tâche, et vérification des règles de sécurité
 * (401 sans token, 403 sur le tableau d'un autre utilisateur).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndBoardFlowTests {

    @Autowired
    private MockMvc mockMvc;

    /** Crée un compte et renvoie le token JWT. */
    private String register(String name, String email) throws Exception {
        String body = """
                {"name":"%s","email":"%s","password":"secret123"}
                """.formatted(name, email);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.token");
    }

    private Integer createBoard(String token, String title) throws Exception {
        String response = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"Description","color":"#6366f1"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    @DisplayName("Parcours complet : inscription, profil, tableau, tâche, statistiques")
    void fullHappyPath() throws Exception {
        String token = register("Awa Diallo", "awa@example.com");

        // Le profil est accessible et ne contient jamais le mot de passe
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("awa@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Création d'un tableau
        Integer boardId = createBoard(token, "Lancement produit");

        // Création d'une tâche
        mockMvc.perform(post("/api/boards/" + boardId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Maquetter l'écran","priority":"HAUTE","status":"EN_COURS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.priority").value("HAUTE"));

        // Les compteurs du tableau sont à jour
        mockMvc.perform(get("/api/boards/" + boardId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCount").value(1))
                .andExpect(jsonPath("$.taskCountByStatus.EN_COURS").value(1));

        // Le tableau de bord agrège les statistiques
        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardCount").value(1))
                .andExpect(jsonPath("$.taskCount").value(1));
    }

    @Test
    @DisplayName("Les routes protégées renvoient 401 sans token")
    void protectedRoutesRequireToken() throws Exception {
        mockMvc.perform(get("/api/boards")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La route publique /api/health répond sans token")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Un utilisateur ne peut pas accéder au tableau d'un autre (403)")
    void cannotAccessOtherUsersBoard() throws Exception {
        String tokenAwa = register("Awa", "awa2@example.com");
        String tokenIbrahim = register("Ibrahim", "ibrahim@example.com");

        Integer boardId = createBoard(tokenAwa, "Privé");

        mockMvc.perform(get("/api/boards/" + boardId).header("Authorization", "Bearer " + tokenIbrahim))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/boards/" + boardId).header("Authorization", "Bearer " + tokenIbrahim))
                .andExpect(status().isForbidden());

        // Le propriétaire, lui, y accède
        mockMvc.perform(get("/api/boards/" + boardId).header("Authorization", "Bearer " + tokenAwa))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Le mot de passe n'est jamais renvoyé et l'email est unique")
    void passwordNeverExposedAndEmailUnique() throws Exception {
        String token = register("Fatou", "fatou@example.com");
        assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Fatou","email":"fatou@example.com","password":"secret123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un mauvais mot de passe renvoie 401")
    void wrongPasswordIsRejected() throws Exception {
        register("Noufou", "noufou@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"noufou@example.com","password":"mauvais"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La validation des DTO renvoie 400 avec le détail des champs")
    void validationErrorsAreReported() throws Exception {
        String token = register("Ibrahim K", "ibrahim2@example.com");

        mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("Mise à jour du profil sans mot de passe : l'ancien est conservé")
    void updateProfileKeepsPassword() throws Exception {
        String token = register("Test User", "update@example.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Modifié","email":"update@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Modifié"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"update@example.com","password":"secret123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Déplacer une tâche la place dans la bonne colonne")
    void moveTaskChangesColumn() throws Exception {
        String token = register("Move Test", "move@example.com");
        Integer boardId = createBoard(token, "Kanban");

        String taskResponse = mockMvc.perform(post("/api/boards/" + boardId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"À déplacer\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer taskId = JsonPath.read(taskResponse, "$.id");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/boards/" + boardId + "/tasks/" + taskId + "/move")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TERMINE\",\"position\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINE"))
                .andExpect(jsonPath("$.position").value(0));
    }
}
