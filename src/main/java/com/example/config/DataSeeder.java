package com.example.config;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.model.BoardModel;
import com.example.model.Role;
import com.example.model.TaskModel;
import com.example.model.TaskPriority;
import com.example.model.TaskStatus;
import com.example.model.UserModel;
import com.example.repository.BoardRepository;
import com.example.repository.TaskRepository;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Jeu de données de démonstration, uniquement sur les profils "demo" et "dev".
 * <pre>
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
 *   Compte : demo@taskboard.dev / demo1234
 * </pre>
 */
@Slf4j
@Component
@Profile({"demo", "dev"})
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase("demo@taskboard.dev")) {
            log.info("Données de démo déjà présentes.");
            return;
        }

        UserModel demo = userRepository.save(UserModel.builder()
                .name("Noufou Zongo")
                .email("demo@taskboard.dev")
                .password(passwordEncoder.encode("demo1234"))
                .role(Role.USER)
                .build());

        BoardModel produit = boardRepository.save(BoardModel.builder()
                .title("Lancement produit")
                .description("Préparer la mise en ligne de la version 1.0")
                .color("#6366f1")
                .owner(demo)
                .build());

        BoardModel marketing = boardRepository.save(BoardModel.builder()
                .title("Marketing Q4")
                .description("Campagne de fin d'année")
                .color("#ec4899")
                .owner(demo)
                .build());

        List<TaskModel> demo = new java.util.ArrayList<>(List.of(
                task(produit, "Rédiger le cahier des charges", TaskStatus.TERMINE, TaskPriority.HAUTE,
                        LocalDate.now().minusDays(6), "Awa"),
                task(produit, "Maquettes de l'interface", TaskStatus.TERMINE, TaskPriority.MOYENNE,
                        LocalDate.now().minusDays(2), "Noufou"),
                task(produit, "Intégration de l'API de paiement", TaskStatus.EN_COURS, TaskPriority.URGENTE,
                        LocalDate.now().plusDays(3), "Ibrahim"),
                task(produit, "Tests utilisateurs", TaskStatus.EN_COURS, TaskPriority.HAUTE,
                        LocalDate.now().plusDays(5), "Awa"),
                task(produit, "Rédiger la documentation", TaskStatus.A_FAIRE, TaskPriority.BASSE,
                        LocalDate.now().plusDays(12), null),
                task(produit, "Configurer le monitoring", TaskStatus.A_FAIRE, TaskPriority.MOYENNE,
                        LocalDate.now().minusDays(1), "Ibrahim"),
                task(marketing, "Plan de communication", TaskStatus.TERMINE, TaskPriority.HAUTE,
                        LocalDate.now().minusDays(10), "Noufou"),
                task(marketing, "Visuels réseaux sociaux", TaskStatus.EN_COURS, TaskPriority.MOYENNE,
                        LocalDate.now().plusDays(2), "Fatou"),
                task(marketing, "Partenariats influenceurs", TaskStatus.A_FAIRE, TaskPriority.HAUTE,
                        LocalDate.now().plusDays(8), "Fatou")
        ));

        // Positions consécutives (0, 1, 2...) dans chaque colonne de chaque tableau
        Map<String, Integer> nextPosition = new java.util.HashMap<>();
        for (TaskModel task : demo) {
            String key = task.getBoard().getId() + ":" + task.getStatus();
            int position = nextPosition.merge(key, 1, Integer::sum) - 1;
            task.setPosition(position);
        }
        taskRepository.saveAll(demo);

        log.info("✅ Données de démo créées : demo@taskboard.dev / demo1234");
    }

    private TaskModel task(BoardModel board, String title, TaskStatus status, TaskPriority priority,
                           LocalDate dueDate, String assignee) {
        return TaskModel.builder()
                .title(title)
                .status(status)
                .priority(priority)
                .dueDate(dueDate)
                .assignee(assignee)
                .board(board)
                .description("Tâche de démonstration.")
                .build();
    }
}
