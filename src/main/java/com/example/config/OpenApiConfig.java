package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Métadonnées de la documentation OpenAPI + bouton "Authorize" (Bearer JWT)
 * dans Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI taskBoardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskBoard API")
                        .version("1.0.0")
                        .description("""
                                API REST de gestion de tableaux et de tâches (Kanban).

                                **Démarrage rapide**
                                1. `POST /api/auth/register` pour créer un compte
                                2. `POST /api/auth/login` pour récupérer un token JWT
                                3. Cliquez sur **Authorize** et collez le token (sans le préfixe `Bearer `)
                                4. Créez un tableau, puis des tâches
                                """)
                        .contact(new Contact().name("TaskBoard").url("https://github.com/zongonoufou/TaskBoard"))
                        .license(new License().name("MIT")))
                .addServersItem(new Server().url("/").description("Serveur courant"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token retourné par POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
