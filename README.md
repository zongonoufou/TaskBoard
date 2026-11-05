# TaskBoard

Application de gestion de projets et de tâches en **tableau Kanban**, avec une **API REST** sécurisée par JWT, une **interface web** intégrée et une **documentation Swagger**.

![Stack](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F) ![Java](https://img.shields.io/badge/Java-21-orange) ![Tests](https://img.shields.io/badge/tests-JUnit%205%20%2B%20MockMvc-blue) ![Licence](https://img.shields.io/badge/licence-MIT-green)

---

## ✨ Fonctionnalités

### Interface web (accessible sur `/`)
- **Kanban** en 3 colonnes (À faire · En cours · Terminé) avec **glisser-déposer**.
- Création / édition / suppression de **tableaux** (titre, description, couleur).
- Tâches avec **priorité**, **échéance**, **assigné** et descriptions.
- **Tableau de bord** : nombre de tableaux, tâches, tâches terminées, **tâches en retard**, répartition par statut, prochaines échéances.
- **Recherche** instantanée dans les tâches, **thème clair/sombre**, responsive (mobile).
- Actions rapides : cocher une tâche comme terminée, changement de priorité.
- Toasts de confirmation et messages d'erreur explicites.

### API REST
| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Créer un compte (renvoie un token) |
| `POST` | `/api/auth/login` | Se connecter (renvoie un token) |
| `GET` | `/api/users/me` | Mon profil |
| `PUT` | `/api/users/me` | Modifier mon profil (mot de passe facultatif) |
| `PATCH` | `/api/users/me/password` | Changer mon mot de passe |
| `DELETE` | `/api/users/me` | Supprimer mon compte + mes tableaux |
| `GET`/`POST` | `/api/boards` | Lister / créer un tableau |
| `GET`/`PUT`/`DELETE` | `/api/boards/{id}` | Détail / modifier / supprimer |
| `GET`/`POST` | `/api/boards/{boardId}/tasks` | Lister (`?status=EN_COURS`) / créer |
| `GET`/`PUT`/`DELETE` | `/api/boards/{boardId}/tasks/{taskId}` | Détail / modifier / supprimer |
| `PATCH` | `.../tasks/{taskId}/move` | Déplacer (statut + position) |
| `PATCH` | `.../tasks/{taskId}/toggle` | Terminé ⇄ À faire |
| `PATCH` | `.../tasks/{taskId}/priority` | Changer la priorité |
| `GET` | `/api/dashboard/stats` | Statistiques |
| `GET` | `/api/health` | Sonde publique |
| `GET` | `/actuator/health` | Sonde Actuator |

Documentation interactive : **`/swagger-ui.html`** (bouton *Authorize* pour coller le token JWT).
Schéma OpenAPI brut : `/v3/api-docs`.

---

## 🚀 Démarrage rapide

### Prérequis
- **Java 21 à 25** (testé avec Java 25 / Maven 3.10)
- Maven n'est pas nécessaire : le wrapper `./mvnw` est fourni.

> ⚠️ **Utilisez la même version de Java pour lancer Maven et l'application.**
> Si `mvn -v` affiche Java 25 et que le projet est compilé pour Java 21, aucun problème.
> En revanche, compilé avec Java 25 on ne peut pas l'exécuter avec Java 21.

### Lancer l'application

**Windows (PowerShell)**
```powershell
mvn spring-boot:run
```
**macOS / Linux**
```bash
git clone https://github.com/zongonoufou/TaskBoard.git
cd TaskBoard
./mvnw spring-boot:run
```

L'application démarre sur **http://localhost:8081** (base H2 en fichier, aucune installation requise).
Au premier lancement, Maven télécharge les dépendances : comptez 1 à 3 minutes.

Puis ouvrir :

| URL | Contenu |
|---|---|
| http://localhost:8081/ | **Interface Kanban** |
| http://localhost:8081/swagger-ui.html | Documentation de l'API |
| http://localhost:8081/h2-console | Console base de données (dev) |

> **Windows** : `mvnw.cmd spring-boot:run` (ou simplement `mvn spring-boot:run` si Maven est installé).

### Avec les données de démonstration

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Un compte est créé automatiquement :

- **Email** : `demo@taskboard.dev`
- **Mot de passe** : `demo1234`

(2 tableaux et 9 tâches déjà remplies, dont une en retard.)

### Avec PostgreSQL (Docker)

```bash
# Docker Desktop doit être démarré
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

Le profil `docker` récupère la configuration dans `compose.yaml` (PostgreSQL 17, base `taskboard`, utilisateur `taskboard` / `taskboard`).

### Tests

```bash
mvn test
```

Les tests couvrent : inscription/connexion, accès protégé sans token (401), accès au
tableau d'autrui (403), doublon d'email (409), validation des DTO (400) et déplacement de tâche.

---

## 🧯 Dépannage

| Symptôme | Cause / Solution |
|---|---|
| `Port 8081 was already in use` | Un autre processus occupe le port. Changez-le : `set PORT=8082` (PowerShell : `$env:PORT=8082`) ou arrêtez le processus. |
| `Could not resolve dependencies` | Pas d'accès à Maven Central : vérifiez le proxy / la connexion réseau. |
| `Lombok does not work: you aren't using a compiler supported by lombok` | Votre JDK est plus récent que Lombok. Ce projet utilise la version gérée par Spring Boot 4.1 (1.18.48+, compatible JDK 25). |
| `Table "USERS" not found` | La base H2 est vide/corrompue : supprimez le dossier `data/` à la racine du projet puis relancez. |
| `Schema-validation` / erreurs SQL au démarrage | Une ancienne base PostgreSQL contient un schéma différent : supprimez le volume Docker (`docker compose down -v`). |
| Page blanche sur `/` | Vérifiez que vous êtes bien sur `http://localhost:8081/` (et non `:8080`). |
| `401 Unauthorized` sur les appels API | Le token est absent, expiré (24 h) ou mal collé : le préfixe `Bearer ` est ajouté automatiquement par Swagger, ne le mettez pas à la main. |

Pour repartir de zéro : `mvn clean` puis supprimez le dossier `data/`.

---

## 🧱 Architecture

```
src/main/java/com/example
├── config/       SecurityConfig (JWT, CORS, 401/403 JSON), OpenApiConfig, DataSeeder
├── controller/   AuthController, UserController, BoardController, TaskController,
│                 DashboardController, HealthController
├── dto/          Requêtes / réponses (validation Jakarta Bean Validation)
├── exception/    Exceptions métier + GlobalExceptionHandler (format d'erreur unique)
├── mapper/       Conversion entité ⇄ DTO
├── model/        UserModel, BoardModel, TaskModel, enums (statut, priorité, rôle)
├── repository/   Spring Data JPA
├── security/     JwtService, JwtAuthenticationFilter, UserPrincipal, handlers JSON
└── service/      Logique métier + contrôle de propriété (403)
```

**Choix techniques**

| Sujet | Décision |
|---|---|
| Authentification | JWT *stateless* (JJWT 0.13), mot de passe BCrypt |
| Autorisation | Le propriétaire est **toujours déduit du token** ; jamais de `ownerId`/`userId` dans l'URL (pas d'IDOR) |
| Transactions | `@Transactional` sur les services, `open-in-view=false` (pas de fuite de session) |
| Erreurs | Un seul format JSON (`timestamp`, `status`, `error`, `message`, `path`, `validationErrors`) |
| Base | H2 par défaut (zéro installation) · PostgreSQL via profil `docker` |
| Front | HTML/CSS/JS natifs servis par Spring Boot, aucun build nécessaire |

---

## 🔐 Comptes et rôles

- `USER` : gère **ses** tableaux et tâches.
- `ADMIN` : accès à `GET/POST/PUT/DELETE /api/users`.

Pour promouvoir un compte en admin, mettez à jour la colonne `role` dans la base :

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'vous@exemple.com';
```

---

## ⚙️ Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `PORT` | `8081` | Port HTTP |
| `DB_URL` | `jdbc:h2:file:./data/taskboard` | URL JDBC |
| `DB_USERNAME` / `DB_PASSWORD` | `sa` / *(vide)* | Identifiants base |
| `DB_DRIVER` | `org.h2.Driver` | Driver JDBC |
| `JWT_SECRET` | clé de dev | **À changer en production** (Base64 ≥ 32 octets) |
| `JWT_EXPIRATION` | `86400000` (24 h) | Durée du token (ms) |
| `CORS_ALLOWED_ORIGINS` | `*` | Origines autorisées (virgules) |
| `H2_CONSOLE_ENABLED` | `true` | Console H2 |

Exemple de production :

```bash
export JWT_SECRET=$(openssl rand -base64 48)
export H2_CONSOLE_ENABLED=false
export CORS_ALLOWED_ORIGINS=https://mon-front.example.com
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

---

## 🧪 Exemple complet en `curl`

```bash
# 1. Inscription (renvoie un token)
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Awa","email":"awa@example.com","password":"secret123"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# 2. Créer un tableau
BOARD=$(curl -s -X POST http://localhost:8081/api/boards \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Lancement","description":"V1","color":"#6366f1"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

# 3. Ajouter une tâche
curl -s -X POST "http://localhost:8081/api/boards/$BOARD/tasks" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Maquetter","priority":"HAUTE","status":"EN_COURS","dueDate":"2026-12-01"}'

# 4. Statistiques
curl -s http://localhost:8081/api/dashboard/stats -H "Authorization: Bearer $TOKEN"
```

---

## 🩺 Ajouts par rapport à la version initiale

Cette version corrige et complète le projet d'origine :

1. **Correction des imports cassés** (`com.example.Config.security.*`) qui empêchaient la compilation.
2. **Login impossible réparé** : `/login` était protégé par l'authentification ; il est désormais public sous `/api/auth/login`.
3. **`getMail()` → `getEmail()`** (le DTO utilisait `email`).
4. **`jwt.expiration=3600*24`** n'était pas une valeur numérique → `86400000`.
5. **Faille d'autorisation majeure corrigée** : n'importe quel utilisateur connecté pouvait lire/modifier/supprimer les tableaux et tâches des autres. Chaque opération vérifie maintenant la propriété (403 sinon).
6. **`GET /api/users` exposait tous les comptes** → réservé aux ADMIN ; `/api/users/me` pour son profil.
7. **Démarrage sans PostgreSQL** : profil H2 par défaut (`spring.jpa`/docker en option).
8. **Console H2 réparée** pour Spring Boot 4 (`spring-boot-h2console`).
9. **LazyInitializationException** : `@Transactional` + `open-in-view=false`.
10. **Tâche d'un autre tableau modifiable** via un `boardId` arbitraire → vérification de la paire (boardId, taskId).
11. **Suppression en cascade** réellement fonctionnelle (tableau → tâches, compte → tableaux).
12. **Documentation Swagger** réparée (`/swagger-ui.html`, schéma *bearerAuth*), `springdoc` 3.2 compatible Boot 4.
13. **Jackson 3** (Boot 4) pris en compte pour les réponses d'erreur de sécurité.
14. **Nouvelles fonctionnalités** : priorités, échéances, assignés, recherche, tableau de bord, thème sombre, UI Kanban, données de démo, tests d'intégration.

---

## 👀 Tester l'interface en 2 minutes

1. Ouvrir **http://localhost:8081/**
2. Cliquer sur **« Créer un compte »**, saisir un nom, un email et un mot de passe (6 caractères min.).
3. Cliquer sur **« + Créer un tableau »** (bouton dans le menu de gauche ou sur le tableau de bord) : titre + couleur.
4. Dans le tableau, cliquer sur **« + »** dans une colonne pour ajouter une tâche (priorité, échéance, assigné).
5. **Glisser-déposer** une carte d'une colonne à l'autre.
6. Sur une carte : **✓** pour terminer, **✎** pour modifier, poubelle pour supprimer.
7. Revenir au **Tableau de bord** : les compteurs, la répartition par statut et les échéances se mettent à jour.
8. Tester le **thème sombre** (icône lune en bas du menu) et la **recherche** en haut à droite.

Avec le profil `dev`, le compte **demo@taskboard.dev / demo1234** contient déjà deux tableaux remplis.

---

## 📄 Licence

MIT.
