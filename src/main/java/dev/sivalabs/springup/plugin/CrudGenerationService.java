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
        String basePath = entityName.toLowerCase() + "s";
        String dtoName = entityName + "Dto";

        List<PsiField> nonIdFields = getRelevantFields(entityClass, false);
        String createCmdCall = recordConstructorCall("Create" + entityName + "Cmd", nonIdFields, "request", false);
        String updateCmdCall = recordConstructorCall("Update" + entityName + "Cmd", nonIdFields, "request", false);

        String dtoFqn = entityPackage(entityClass) + "." + dtoName;
        String serviceFqn = config.servicePackage() + "." + serviceName;
        String createCmdFqn = config.servicePackage() + ".Create" + entityName + "Cmd";
        String updateCmdFqn = config.servicePackage() + ".Update" + entityName + "Cmd";

        String text = "package " + config.controllerPackage() + ";\n\n"
                + "import " + dtoFqn + ";\n"
                + "import " + serviceFqn + ";\n"
                + "import " + createCmdFqn + ";\n"
                + "import " + updateCmdFqn + ";\n"
                + "import org.springframework.http.ResponseEntity;\n"
                + "import org.springframework.web.bind.annotation.*;\n\n"
                + "import java.util.List;\n\n"
                + "@RestController\n"
                + "@RequestMapping(\"/" + basePath + "\")\n"
                + "public class " + controllerName + " {\n\n"
                + "    private final " + serviceName + " service;\n\n"
                + "    public " + controllerName + "(" + serviceName + " service) {\n"
                + "        this.service = service;\n"
                + "    }\n\n"
                + "    @GetMapping\n"
                + "    public List<" + dtoName + "> getAll() {\n"
                + "        return service.findAll();\n"
                + "    }\n\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    public ResponseEntity<" + dtoName + "> getById(@PathVariable Long id) {\n"
                + "        return service.findById(id)\n"
                + "                .map(ResponseEntity::ok)\n"
                + "                .orElse(ResponseEntity.notFound().build());\n"
                + "    }\n\n"
                + "    @PostMapping\n"
                + "    public " + dtoName + " create(@RequestBody Create" + entityName + "Request request) {\n"
                + "        return service.create(" + createCmdCall + ");\n"
                + "    }\n\n"
                + "    @PutMapping(\"/{id}\")\n"
                + "    public ResponseEntity<" + dtoName + "> update(@PathVariable Long id,"
                + " @RequestBody Update" + entityName + "Request request) {\n"
                + "        return service.update(id, " + updateCmdCall + ")\n"
                + "                .map(ResponseEntity::ok)\n"
                + "                .orElse(ResponseEntity.notFound().build());\n"
                + "    }\n\n"
                + "    @DeleteMapping(\"/{id}\")\n"
                + "    public ResponseEntity<Void> delete(@PathVariable Long id) {\n"
                + "        service.deleteById(id);\n"
                + "        return ResponseEntity.noContent().build();\n"
                + "    }\n"
                + "}\n";

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

        String text = "package " + config.servicePackage() + ";\n\n"
                + "import " + entityClass.getQualifiedName() + ";\n"
                + "import " + dtoFqn + ";\n"
                + "import " + repositoryFqn + ";\n"
                + "import org.springframework.stereotype.Service;\n\n"
                + "import java.util.List;\n"
                + "import java.util.Optional;\n\n"
                + "@Service\n"
                + "public class " + serviceName + " {\n\n"
                + "    private final " + repositoryName + " repository;\n\n"
                + "    public " + serviceName + "(" + repositoryName + " repository) {\n"
                + "        this.repository = repository;\n"
                + "    }\n\n"
                + "    public List<" + dtoName + "> findAll() {\n"
                + "        return repository.findAll().stream().map(this::toDto).toList();\n"
                + "    }\n\n"
                + "    public Optional<" + dtoName + "> findById(Long id) {\n"
                + "        return repository.findById(id).map(this::toDto);\n"
                + "    }\n\n"
                + "    public " + dtoName + " create(Create" + entityName + "Cmd cmd) {\n"
                + "        " + entityName + " entity = new " + entityName + "();\n"
                + createEntityMapping
                + "        return toDto(repository.save(entity));\n"
                + "    }\n\n"
                + "    public Optional<" + dtoName + "> update(Long id, Update" + entityName + "Cmd cmd) {\n"
                + "        return repository.findById(id).map(entity -> {\n"
                + updateEntityMapping
                + "            return toDto(repository.save(entity));\n"
                + "        });\n"
                + "    }\n\n"
                + "    public void deleteById(Long id) {\n"
                + "        repository.deleteById(id);\n"
                + "    }\n\n"
                + "    private " + dtoName + " toDto(" + entityName + " entity) {\n"
                + "        return " + toDtoCall + ";\n"
                + "    }\n"
                + "}\n";

        createJavaFile(project, dir, serviceName, text);
    }

    private static void createRepository(Project project, PsiClass entityClass, CrudGenerationConfig config) {
        PsiDirectory dir = getOrCreatePackageDir(project, config.repositoryPackage());
        String entityName = entityClass.getName();
        String idType = Optional.ofNullable(findIdField(entityClass))
                .map(f -> f.getType().getPresentableText())
                .orElse("Long");

        String text = "package " + config.repositoryPackage() + ";\n\n"
                + "import " + entityClass.getQualifiedName() + ";\n"
                + "import org.springframework.data.jpa.repository.JpaRepository;\n\n"
                + "public interface " + config.repositoryName()
                + " extends JpaRepository<" + entityName + ", " + idType + "> {\n"
                + "}\n";

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
        return "package " + pkg + ";\n\n"
                + "public record " + className + "(" + components + ") {}\n";
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
