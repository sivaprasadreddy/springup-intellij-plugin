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
    private JTextField controllerNameField;
    private JTextField serviceNameField;
    private JTextField repositoryNameField;
    private JTextField controllerPackageField;
    private JTextField servicePackageField;
    private JTextField repositoryPackageField;
    private JCheckBox generateRequestDtoCheckBox;
    private JCheckBox generateResponseDtoCheckBox;

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
        controllerNameField = new JTextField(entityName + "Controller");
        serviceNameField = new JTextField(entityName + "Service");
        repositoryNameField = new JTextField(entityName + "Repository");
        controllerPackageField = new JTextField("com.example.controller");
        servicePackageField = new JTextField("com.example.service");
        repositoryPackageField = new JTextField("com.example.repository");
        generateRequestDtoCheckBox = new JCheckBox("Generate Request DTO", true);
        generateResponseDtoCheckBox = new JCheckBox("Generate Response DTO", true);

        panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Controller Name")); panel.add(controllerNameField);
        panel.add(new JLabel("Controller Package")); panel.add(controllerPackageField);
        panel.add(new JLabel("Service Name")); panel.add(serviceNameField);
        panel.add(new JLabel("Service Package")); panel.add(servicePackageField);
        panel.add(new JLabel("Repository Name")); panel.add(repositoryNameField);
        panel.add(new JLabel("Repository Package")); panel.add(repositoryPackageField);
        panel.add(generateRequestDtoCheckBox); panel.add(generateResponseDtoCheckBox);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    public CrudGenerationConfig getConfig() {
        return new CrudGenerationConfig(
            controllerNameField.getText().trim(),
            serviceNameField.getText().trim(),
            repositoryNameField.getText().trim(),
            controllerPackageField.getText().trim(),
            servicePackageField.getText().trim(),
            repositoryPackageField.getText().trim(),
            generateRequestDtoCheckBox.isSelected(),
            generateResponseDtoCheckBox.isSelected()
        );
    }
}