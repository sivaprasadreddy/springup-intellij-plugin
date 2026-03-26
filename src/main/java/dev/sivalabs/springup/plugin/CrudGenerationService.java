package dev.sivalabs.springup.plugin;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CrudGenerationService {

    public static void generate(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            createEntityDto(project, entityClass);
            createCreateCmd(project, entityClass, config);
            createUpdateCmd(project, entityClass, config);
            createCreateRequest(project, entityClass, config);
            createUpdateRequest(project, entityClass, config);
            createRepository(project, entityClass, config);
            createService(project, entityClass, config);
            createController(project, entityClass, config);
            createControllerTest(project, entityClass, config);
        });
    }

    // EntityNameDto — same package as Entity, includes all fields (with ID)
    private static void createEntityDto(Project project, PsiClass entityClass) {
        String pkg = entityPackage(entityClass);
        PsiDirectory dir = getOrCreatePackageDir(project, pkg);
        String className = entityClass.getName() + "Dto";
        List<PsiField> fields = getRelevantFields(entityClass, true);

        createJavaFile(project, dir, className, buildRecord(pkg, className, fields));
    }

    // CreateEntityNameCmd — service package, non-ID fields
    private static void createCreateCmd(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        String className = "Create" + entityClass.getName() + "Cmd";
        PsiDirectory dir = getOrCreatePackageDir(project, config.servicePackage());
        List<PsiField> fields = getRelevantFields(entityClass, false);

        createJavaFile(project, dir, className, buildRecord(config.servicePackage(), className, fields));
    }

    // UpdateEntityNameCmd — service package, non-ID fields
    private static void createUpdateCmd(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        String className = "Update" + entityClass.getName() + "Cmd";
        PsiDirectory dir = getOrCreatePackageDir(project, config.servicePackage());
        List<PsiField> fields = getRelevantFields(entityClass, false);

        createJavaFile(project, dir, className, buildRecord(config.servicePackage(), className, fields));
    }

    // CreateEntityNameRequest — controller (web) package, non-ID fields
    private static void createCreateRequest(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        String className = "Create" + entityClass.getName() + "Request";
        PsiDirectory dir = getOrCreatePackageDir(project, config.controllerPackage());
        List<PsiField> fields = getRelevantFields(entityClass, false);

        createJavaFile(project, dir, className, buildRecord(config.controllerPackage(), className, fields));
    }

    // UpdateEntityNameRequest — controller (web) package, non-ID fields
    private static void createUpdateRequest(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        String className = "Update" + entityClass.getName() + "Request";
        PsiDirectory dir = getOrCreatePackageDir(project, config.controllerPackage());
        List<PsiField> fields = getRelevantFields(entityClass, false);

        createJavaFile(project, dir, className, buildRecord(config.controllerPackage(), className, fields));
    }

    private static void createController(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        PsiDirectory dir = getOrCreatePackageDir(project, config.controllerPackage());

        String entityName = entityClass.getName();
        String serviceName = config.serviceName();
        String controllerName = config.controllerName();
        String basePath = entityName.toLowerCase(Locale.getDefault()) + "s";
        String dtoName = entityName + "Dto";

        List<PsiField> nonIdFields = getRelevantFields(entityClass, false);
        String createCmdCall = recordConstructorCall("Create" + entityName + "Cmd", nonIdFields, "request", false);
        String updateCmdCall = recordConstructorCall("Update" + entityName + "Cmd", nonIdFields, "request", false);

        String dtoFqn = entityPackage(entityClass) + "." + dtoName;
        String serviceFqn = config.servicePackage() + "." + serviceName;
        String createCmdFqn = config.servicePackage() + ".Create" + entityName + "Cmd";
        String updateCmdFqn = config.servicePackage() + ".Update" + entityName + "Cmd";
        String idType = Optional.ofNullable(findIdField(entityClass))
                .map(f -> f.getType().getPresentableText())
                .orElse("Long");

        String text = """
                package %s;

                import %s;
                import %s;
                import %s;
                import %s;
                import jakarta.validation.Valid;
                import org.springframework.http.HttpStatus;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.*;

                import java.util.List;

                @RestController
                @RequestMapping("/api/%s")
                class %s {

                    private final %s service;

                    %s(%s service) {
                        this.service = service;
                    }

                    @GetMapping
                    List<%s> getAll() {
                        return service.findAll();
                    }

                    @GetMapping("/{id}")
                    ResponseEntity<%s> getById(@PathVariable %s id) {
                        return service.findById(id)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    }

                    @PostMapping
                    @ResponseStatus(HttpStatus.CREATED)
                    %s create(@RequestBody @Valid Create%sRequest request) {
                        return service.create(%s);
                    }

                    @PutMapping("/{id}")
                    ResponseEntity<%s> update(@PathVariable %s id, @RequestBody @Valid Update%sRequest request) {
                        return service.update(id, %s)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    }

                    @DeleteMapping("/{id}")
                    ResponseEntity<Void> delete(@PathVariable %s id) {
                        service.deleteById(id);
                        return ResponseEntity.noContent().build();
                    }
                }
                """.formatted(
                config.controllerPackage(),
                dtoFqn, serviceFqn, createCmdFqn, updateCmdFqn,
                basePath, controllerName,
                serviceName,
                controllerName, serviceName,
                dtoName,
                dtoName, idType,
                dtoName, entityName, createCmdCall,
                dtoName, idType, entityName, updateCmdCall,
                idType);

        createJavaFile(project, dir, controllerName, text);
    }

    private static void createService(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        PsiDirectory dir = getOrCreatePackageDir(project, config.servicePackage());

        String entityName = entityClass.getName();
        String serviceName = config.serviceName();
        String repositoryName = config.repositoryName();
        String dtoName = entityName + "Dto";

        List<PsiField> allFields = getRelevantFields(entityClass, true);
        List<PsiField> nonIdFields = getRelevantFields(entityClass, false);

        String createEntityMapping = entityFromRecordMapping(nonIdFields, "entity", "cmd", "        ");
        String updateEntityMapping = entityFromRecordMapping(nonIdFields, "entity", "cmd", "            ");
        String toDtoCall = recordConstructorCall(dtoName, allFields, "entity", true);

        String dtoFqn = entityPackage(entityClass) + "." + dtoName;
        String repositoryFqn = config.repositoryPackage() + "." + repositoryName;
        String idType = Optional.ofNullable(findIdField(entityClass))
                .map(f -> f.getType().getPresentableText())
                .orElse("Long");

        String text = """
                package %s;

                import %s;
                import %s;
                import %s;
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;

                import java.util.List;
                import java.util.Optional;

                @Service
                public class %s {

                    private final %s repository;

                    public %s(%s repository) {
                        this.repository = repository;
                    }

                    @Transactional(readOnly = true)
                    public List<%s> findAll() {
                        return repository.findAll().stream().map(this::toDto).toList();
                    }

                    @Transactional(readOnly = true)
                    public Optional<%s> findById(%s id) {
                        return repository.findById(id).map(this::toDto);
                    }

                    @Transactional
                    public %s create(Create%sCmd cmd) {
                        %s entity = new %s();
                %s        return toDto(repository.save(entity));
                    }

                    @Transactional
                    public Optional<%s> update(%s id, Update%sCmd cmd) {
                        return repository.findById(id).map(entity -> {
                %s            return toDto(repository.save(entity));
                        });
                    }

                    @Transactional
                    public void deleteById(%s id) {
                        repository.deleteById(id);
                    }

                    private %s toDto(%s entity) {
                        return %s;
                    }
                }
                """.formatted(
                config.servicePackage(),
                entityClass.getQualifiedName(), dtoFqn, repositoryFqn,
                serviceName,
                repositoryName,
                serviceName, repositoryName,
                dtoName,
                dtoName, idType,
                dtoName, entityName,
                entityName, entityName,
                createEntityMapping,
                dtoName, idType, entityName,
                updateEntityMapping,
                idType,
                dtoName, entityName,
                toDtoCall);

        createJavaFile(project, dir, serviceName, text);
    }

    private static void createControllerTest(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        PsiDirectory dir = getOrCreateTestPackageDir(project, config.controllerPackage());

        String entityName = entityClass.getName();
        String basePath = entityName.toLowerCase(Locale.getDefault()) + "s";
        String testClassName = entityName + "ControllerTests";

        String text = """
                package %s;

                import org.junit.jupiter.api.Test;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
                import org.springframework.boot.test.context.SpringBootTest;
                import org.springframework.test.context.ActiveProfiles;
                import org.springframework.test.web.servlet.client.RestTestClient;

                import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

                @SpringBootTest(webEnvironment = RANDOM_PORT)
                //@Import(TestcontainersConfiguration.class)
                @AutoConfigureRestTestClient
                @ActiveProfiles("test")
                class %sControllerTests {

                    @Autowired
                    RestTestClient restTestClient;

                    @Test
                    void shouldReturnAll%ss() {
                        restTestClient.get()
                                .uri("/api/%s")
                                .exchange()
                                .expectStatus().isOk();
                    }

                    @Test
                    void shouldCreate%s() {
                        Create%sRequest request = new Create%sRequest();
                        restTestClient.post()
                                .uri("/api/%s")
                                .body(request)
                                .exchange()
                                .expectStatus().isCreated();
                    }

                    @Test
                    void shouldGet%sById() {
                        var id = "";

                        restTestClient.get()
                                .uri("/api/%s/{id}", id)
                                .exchange()
                                .expectStatus().isOk();
                    }

                    @Test
                    void shouldReturn404When%sDoesNotExist() {
                        restTestClient.get()
                                .uri("/api/%s/{id}", "999999")
                                .exchange()
                                .expectStatus().isNotFound();
                    }

                    @Test
                    void shouldUpdate%s() {
                        var id = "";
                        Update%sRequest request = new Update%sRequest();
                        restTestClient.put()
                                .uri("/api/%s/{id}", id)
                                .body(request)
                                .exchange()
                                .expectStatus().isOk();
                    }

                    @Test
                    void shouldDelete%s() {
                        var id = "";

                        restTestClient.delete()
                                .uri("/api/%s/{id}", id)
                                .exchange()
                                .expectStatus().isNoContent();

                        restTestClient.get()
                                .uri("/api/%s/{id}", id)
                                .exchange()
                                .expectStatus().isNotFound();
                    }
                }
                """.formatted(
                config.controllerPackage(),
                entityName,
                entityName, basePath,
                entityName, entityName, entityName, basePath,
                entityName, basePath,
                entityName, basePath,
                entityName, entityName, entityName, basePath,
                entityName,
                basePath, basePath);

        createJavaFile(project, dir, testClassName, text);
    }

    private static void createRepository(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        PsiDirectory dir = getOrCreatePackageDir(project, config.repositoryPackage());
        String entityName = entityClass.getName();
        String idType = Optional.ofNullable(findIdField(entityClass))
                .map(f -> f.getType().getPresentableText())
                .orElse("Long");

        String text = """
                package %s;

                import %s;
                import org.springframework.data.jpa.repository.JpaRepository;

                public interface %s extends JpaRepository<%s, %s> {
                }
                """.formatted(
                config.repositoryPackage(),
                entityClass.getQualifiedName(),
                config.repositoryName(),
                entityName, idType);

        createJavaFile(project, dir, config.repositoryName(), text);
    }

    // --- helpers ---

    private static List<PsiField> getRelevantFields(PsiClass entityClass, boolean includeId) {
        List<PsiField> result = new ArrayList<>();
        for (PsiField field : entityClass.getAllFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue;

            PsiModifierList list = field.getModifierList();
            if (list != null && (
                    list.findAnnotation("jakarta.persistence.OneToMany") != null ||
                    list.findAnnotation("jakarta.persistence.ManyToMany") != null ||
                    list.findAnnotation("javax.persistence.OneToMany") != null ||
                    list.findAnnotation("javax.persistence.ManyToMany") != null
            )) continue;

            boolean isId = list != null && (
                    list.findAnnotation("jakarta.persistence.Id") != null ||
                    list.findAnnotation("javax.persistence.Id") != null
            );
            if (!includeId && isId) continue;

            result.add(field);
        }
        return result;
    }

    /** Generates a {@code public record Foo(Type field, ...) {}} source string. */
    private static String buildRecord(String pkg, String className, List<PsiField> fields) {
        StringBuilder components = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) components.append(", ");
            PsiField f = fields.get(i);
            // Use canonical (fully qualified) type name so that shortenClassReferences
            // can resolve each type and add the required imports automatically.
            components.append(f.getType().getCanonicalText()).append(" ").append(f.getName());
        }
        return """
                package %s;

                public record %s(%s) {}
                """.formatted(pkg, className, components);
    }

    /**
     * Builds entity-setter lines from a record source:
     * {@code entity.setFoo(cmd.foo());} — entity uses JavaBean setters, record uses plain accessors.
     */
    private static String entityFromRecordMapping(List<PsiField> fields,
                                                   String entityVar, String recordVar,
                                                   String indent) {
        StringBuilder sb = new StringBuilder();
        for (PsiField field : fields) {
            String name = field.getName();
            String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            sb.append(indent).append(entityVar).append(".set").append(cap)
              .append("(").append(recordVar).append(".").append(name).append("());\n");
        }
        return sb.toString();
    }

    /**
     * Builds a record constructor call expression.
     * {@code useGetters=true}  → {@code new Foo(source.getBar(), ...)}  (entity → record)
     * {@code useGetters=false} → {@code new Foo(source.bar(), ...)}     (record → record)
     */
    private static String recordConstructorCall(String recordClass, List<PsiField> fields,
                                                 String sourceVar, boolean useGetters) {
        StringBuilder sb = new StringBuilder("new ").append(recordClass).append("(");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(", ");
            String name = fields.get(i).getName();
            if (useGetters) {
                String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                sb.append(sourceVar).append(".get").append(cap).append("()");
            } else {
                sb.append(sourceVar).append(".").append(name).append("()");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private static String entityPackage(PsiClass entityClass) {
        String fqn = entityClass.getQualifiedName();
        int dot = fqn != null ? fqn.lastIndexOf('.') : -1;
        return dot >= 0 ? fqn.substring(0, dot) : "";
    }

    private static void createJavaFile(Project project, PsiDirectory dir, String className, String content) {
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        PsiJavaFile javaFile = (PsiJavaFile) factory.createFileFromText(
                className + ".java",
                JavaFileType.INSTANCE,
                content
        );

        PsiElement added = dir.add(javaFile);
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
        styleManager.shortenClassReferences(added);
        if (added instanceof PsiFile file) {
            styleManager.optimizeImports(file);
        }
        CodeStyleManager.getInstance(project).reformat(added);
    }

    private static @Nullable PsiField findIdField(PsiClass entityClass) {
        for (PsiField field : entityClass.getAllFields()) {
            PsiModifierList list = field.getModifierList();
            if (list != null && (
                    list.findAnnotation("jakarta.persistence.Id") != null ||
                    list.findAnnotation("javax.persistence.Id") != null)) {
                return field;
            }
        }
        return null;
    }

    private static PsiDirectory getOrCreatePackageDir(Project project, String packageName) {
        PsiPackage psiPackage = JavaPsiFacade.getInstance(project).findPackage(packageName);
        if (psiPackage != null) {
            for (PsiDirectory dir : psiPackage.getDirectories()) {
                if (!dir.getVirtualFile().getPath().contains("/test/")) {
                    return dir;
                }
            }
        }

        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        VirtualFile mainRoot = null;
        for (VirtualFile root : roots) {
            if (!root.getPath().contains("/test/")) {
                mainRoot = root;
                break;
            }
        }
        if (mainRoot == null) throw new IllegalStateException("No main source root found");

        PsiDirectory dir = PsiManager.getInstance(project).findDirectory(mainRoot);
        if (dir == null) throw new IllegalStateException("Cannot resolve source root");

        for (String part : packageName.split("\\.")) {
            PsiDirectory next = dir.findSubdirectory(part);
            if (next == null) next = dir.createSubdirectory(part);
            dir = next;
        }
        return dir;
    }

    private static PsiDirectory getOrCreateTestPackageDir(Project project, String packageName) {
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        VirtualFile testRoot = null;
        for (VirtualFile root : roots) {
            if (root.getPath().contains("/test/")) {
                testRoot = root;
                break;
            }
        }
        if (testRoot == null) throw new IllegalStateException("No test source root found");

        PsiDirectory dir = PsiManager.getInstance(project).findDirectory(testRoot);
        if (dir == null) throw new IllegalStateException("Cannot resolve test source root");

        for (String part : packageName.split("\\.")) {
            PsiDirectory next = dir.findSubdirectory(part);
            if (next == null) next = dir.createSubdirectory(part);
            dir = next;
        }
        return dir;
    }
}
