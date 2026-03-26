# SpringUp IntelliJ IDEA Plugin

An IntelliJ IDEA plugin that improves developer productivity while building Spring Boot applications.

## Requirements

- IntelliJ IDEA 2025.1 or later
- Java 21
- Spring Boot project with JPA entities

## Build & Run

```bash
./gradlew build          # Compile and build
./gradlew runIde         # Launch IntelliJ sandbox with plugin loaded
./gradlew buildPlugin    # Build distributable plugin ZIP
./gradlew verifyPlugin   # Verify IDE compatibility
./gradlew test           # Run tests
./gradlew clean          # Clean build outputs
```

## Features

### Generate CRUD

Right-click a JPA entity class and select **New → Generate CRUD** to scaffold a full CRUD REST API layer in one step.

**What gets generated:**

| File                        | Location                  | Description                                            |
|-----------------------------|---------------------------|--------------------------------------------------------|
| `EntityNameDto`             | Entity package            | Record with all fields including ID                    |
| `CreateEntityNameCmd`       | Service package           | Record with non-ID fields                              |
| `UpdateEntityNameCmd`       | Service package           | Record with non-ID fields                              |
| `CreateEntityNameRequest`   | Controller package        | Record with non-ID fields                              |
| `UpdateEntityNameRequest`   | Controller package        | Record with non-ID fields                              |
| `EntityNameRepository`      | Entity package            | `JpaRepository` with derived ID type                   |
| `EntityNameService`         | Entity package            | `@Service` with full CRUD methods and `@Transactional` |
| `EntityNameController`      | Controller package        | `@RestController` mapped to `/api/{entities}`          |
| `EntityNameControllerTests` | Controller package (test) | Integration tests using `RestTestClient`               |

**Behaviour:**
- The primary key type (`Long`, `UUID`, etc.) is derived from the field annotated with `@Id` — no hardcoded assumptions.
- Package layout is auto-detected: if the entity is in a `*.domain` package, the controller goes to `*.web` and the service/repository stay in `*.domain`. Otherwise, standard `*.controller`, `*.service`, `*.repository` suffixes are used.
- All packages and directories are created automatically if they do not exist.
- Generated files are automatically reformatted and have imports optimised.

**How to use:**

1. Open a JPA entity class (`@Entity`).
2. Right-click in the editor or on the class in the Project view.
3. Choose **New → Generate CRUD**.
4. Review or adjust the fully-qualified class names for Controller, Service, and Repository in the dialog.
5. Click **OK**.
