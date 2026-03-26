package dev.sivalabs.springup.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiClass;
import com.intellij.util.ui.JBUI;
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

        panel = new JPanel(new GridBagLayout());
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = JBUI.insets(4, 0, 4, 8);

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1.0;
        fieldGbc.gridwidth = GridBagConstraints.REMAINDER;
        fieldGbc.insets = JBUI.insets(4, 0);

        panel.add(new JLabel("Controller"), labelGbc);
        panel.add(controllerFqnField, fieldGbc);
        panel.add(new JLabel("Service"), labelGbc);
        panel.add(serviceFqnField, fieldGbc);
        panel.add(new JLabel("Repository"), labelGbc);
        panel.add(repositoryFqnField, fieldGbc);
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