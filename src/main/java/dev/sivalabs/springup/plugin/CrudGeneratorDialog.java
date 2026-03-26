package dev.sivalabs.springup.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CrudGeneratorDialog extends DialogWrapper {
    private final Project project;
    private final PsiClass entityClass;

    private JPanel panel;
    private JTextField controllerFqnField;
    private JTextField serviceFqnField;
    private JTextField repositoryFqnField;

    public CrudGeneratorDialog(Project project, PsiClass entityClass) {
        super(project);
        this.project = project;
        this.entityClass = entityClass;

        initUiDefaults();
        init();
        setTitle("Generate CRUD for " + entityClass.getName());
    }

    private void initUiDefaults() {
        String entityName = entityClass.getName();
        String entityPackage = entityClass.getQualifiedName() != null
                ? entityClass.getQualifiedName().substring(0, entityClass.getQualifiedName().lastIndexOf('.'))
                : "com.example";

        String controllerPkg;
        String servicePkg;
        String repositoryPkg;

        if (entityPackage.endsWith(".domain")) {
            String basePackage = entityPackage.substring(0, entityPackage.lastIndexOf('.'));
            controllerPkg = basePackage + ".web";
            servicePkg = entityPackage;
            repositoryPkg = entityPackage;
        } else {
            controllerPkg = entityPackage + ".controller";
            servicePkg = entityPackage + ".service";
            repositoryPkg = entityPackage + ".repository";
        }

        controllerFqnField = new JTextField(controllerPkg + "." + entityName + "Controller");
        serviceFqnField = new JTextField(servicePkg + "." + entityName + "Service");
        repositoryFqnField = new JTextField(repositoryPkg + "." + entityName + "Repository");
        panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Controller")); panel.add(controllerFqnField);
        panel.add(new JLabel("Service")); panel.add(serviceFqnField);
        panel.add(new JLabel("Repository")); panel.add(repositoryFqnField);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    public CrudGenerationConfig getConfig() {
        return new CrudGenerationConfig(
            simpleName(controllerFqnField.getText().trim()),
            simpleName(serviceFqnField.getText().trim()),
            simpleName(repositoryFqnField.getText().trim()),
            packageName(controllerFqnField.getText().trim()),
            packageName(serviceFqnField.getText().trim()),
            packageName(repositoryFqnField.getText().trim())
        );
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }

    private static String packageName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(0, dot) : "";
    }
}