package dev.sivalabs.springup.plugin;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class CrudGenerationService {

    public static void generate(Project project, PsiClass entityClass,
                                CrudGenerationConfig config) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            createRepository(project, entityClass, config);
            createService(project, entityClass, config);
            createController(project, entityClass, config);

            if (config.generateRequestDto()) {
                createRequestDto(project, entityClass, config);
            }
            if (config.generateResponseDto()) {
                createResponseDto(project, entityClass, config);
            }
        });
    }

    private static void createResponseDto(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        String packageName = config.servicePackage() + ".dto";
        PsiDirectory dir = getOrCreatePackageDir(project, packageName);

        String entityName = entityClass.getName();
        String className = entityName + "ResponseDto";

        String fields = buildDtoFields(entityClass, true);

        String text = """
        package %s;

        public class %s {

        %s

        }
        """.formatted(packageName, className, fields);

        createJavaFile(project, dir, className, text);
    }

    private static void createRequestDto(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        String packageName = config.servicePackage() + ".dto";
        PsiDirectory dir = getOrCreatePackageDir(project, packageName);

        String entityName = entityClass.getName();
        String className = entityName + "RequestDto";

        String fields = buildDtoFields(entityClass, false);

        String text = """
        package %s;

        public class %s {

        %s

        }
        """.formatted(packageName, className, fields);

        createJavaFile(project, dir, className, text);
    }

    private static void createController(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        PsiDirectory dir = getOrCreatePackageDir(project, config.controllerPackage());

        String entityName = entityClass.getName();
        String serviceName = config.serviceName();
        String controllerName = config.controllerName();

        String basePath = entityName.toLowerCase() + "s";

        String text = """
        package %s;

        import %s;
        import %s;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.*;

        import java.util.List;

        @RestController
        @RequestMapping("/%s")
        public class %s {

            private final %s service;

            public %s(%s service) {
                this.service = service;
            }

            @GetMapping
            public List<%s> getAll() {
                return service.findAll();
            }

            @GetMapping("/{id}")
            public ResponseEntity<%s> getById(@PathVariable Long id) {
                return service.findById(id)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
            }

            @PostMapping
            public %s create(@RequestBody %s entity) {
                return service.save(entity);
            }

            @PutMapping("/{id}")
            public ResponseEntity<%s> update(@PathVariable Long id, @RequestBody %s entity) {
                return service.findById(id)
                        .map(existing -> {
                            entity.setId(id);
                            return ResponseEntity.ok(service.save(entity));
                        })
                        .orElse(ResponseEntity.notFound().build());
            }

            @DeleteMapping("/{id}")
            public ResponseEntity<Void> delete(@PathVariable Long id) {
                service.deleteById(id);
                return ResponseEntity.noContent().build();
            }
        }
        """.formatted(
                config.controllerPackage(),
                entityClass.getQualifiedName(),
                config.servicePackage() + "." + serviceName,
                basePath,
                controllerName,
                serviceName,
                controllerName,
                serviceName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName
        );

        createJavaFile(project, dir, controllerName, text);
    }

    private static void createService(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        PsiDirectory dir = getOrCreatePackageDir(project, config.servicePackage());

        String entityName = entityClass.getName();
        String repositoryName = config.repositoryName();
        String serviceName = config.serviceName();

        String text = """
        package %s;

        import %s;
        import %s;
        import org.springframework.stereotype.Service;

        import java.util.List;
        import java.util.Optional;

        @Service
        public class %s {

            private final %s repository;

            public %s(%s repository) {
                this.repository = repository;
            }

            public List<%s> findAll() {
                return repository.findAll();
            }

            public Optional<%s> findById(Long id) {
                return repository.findById(id);
            }

            public %s save(%s entity) {
                return repository.save(entity);
            }

            public void deleteById(Long id) {
                repository.deleteById(id);
            }
        }
        """.formatted(
                config.servicePackage(),
                entityClass.getQualifiedName(),
                config.repositoryPackage() + "." + repositoryName,
                serviceName,
                repositoryName,
                serviceName,
                repositoryName,
                entityName,
                entityName,
                entityName,
                entityName
        );

        createJavaFile(project, dir, serviceName, text);
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
                entityName,
                idType
        );

        createJavaFile(project, dir, config.repositoryName(), text);
    }

    private static String buildDtoFields(PsiClass entityClass, boolean includeId) {
        StringBuilder sb = new StringBuilder();

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

            sb.append("    private ")
                    .append(field.getType().getPresentableText())
                    .append(" ")
                    .append(field.getName())
                    .append(";\n\n");
        }

        return sb.toString();
    }

    /*private static List<PsiField> dtoFields(PsiClass entityClass) {
        return Arrays.stream(entityClass.getAllFields())
                .filter(f -> !f.hasModifierProperty(PsiModifier.STATIC))
                .filter(f -> !hasAnyAnnotation(f,
                        "jakarta.persistence.OneToMany",
                        "jakarta.persistence.ManyToMany",
                        "javax.persistence.OneToMany",
                        "javax.persistence.ManyToMany"))
                .toList();
    }

    private static boolean hasAnyAnnotation(PsiField field, String... annotationFQNs) {
        PsiModifierList modifierList = field.getModifierList();
        if (modifierList == null) return false;

        for (String fqn : annotationFQNs) {
            if (modifierList.findAnnotation(fqn) != null) {
                return true;
            }
        }
        return false;
    }*/

    private static void createJavaFile(Project project, PsiDirectory dir, String className, String content) {
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        PsiJavaFile javaFile = (PsiJavaFile) factory.createFileFromText(
                className + ".java",
                JavaFileType.INSTANCE,
                content
        );

        PsiElement added = dir.add(javaFile);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(added);
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
            PsiDirectory[] dirs = psiPackage.getDirectories();
            if (dirs.length > 0) return dirs[0];
        }

        // fallback: create under a source root
        ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        if (roots.length == 0) throw new IllegalStateException("No source roots found");

        PsiDirectory dir = PsiManager.getInstance(project).findDirectory(roots[0]);
        if (dir == null) throw new IllegalStateException("Cannot resolve source root");

        for (String part : packageName.split("\\.")) {
            PsiDirectory next = dir.findSubdirectory(part);
            if (next == null) next = dir.createSubdirectory(part);
            dir = next;
        }
        return dir;
    }
}