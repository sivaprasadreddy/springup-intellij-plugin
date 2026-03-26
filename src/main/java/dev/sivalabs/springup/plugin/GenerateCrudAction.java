package dev.sivalabs.springup.plugin;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class GenerateCrudAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiClass psiClass = getSelectedPsiClass(e);
        boolean visible = psiClass != null && isJpaEntity(psiClass);

        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(visible);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiClass entityClass = getSelectedPsiClass(e);
        if (project == null || entityClass == null) return;

        CrudGeneratorDialog dialog = new CrudGeneratorDialog(project, entityClass);
        if (!dialog.showAndGet()) return;

        CrudGenerationConfig config = dialog.getConfig();
        CrudGenerationService.generate(project, entityClass, config);
    }

    private PsiClass getSelectedPsiClass(AnActionEvent e) {
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (element instanceof PsiClass psiClass) return psiClass;
        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }

    private boolean isJpaEntity(PsiClass psiClass) {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) return false;

        return modifierList.findAnnotation("jakarta.persistence.Entity") != null
            || modifierList.findAnnotation("javax.persistence.Entity") != null;
    }
}