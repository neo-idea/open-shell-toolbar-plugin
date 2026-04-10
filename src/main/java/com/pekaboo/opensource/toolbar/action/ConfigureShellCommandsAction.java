package com.pekaboo.opensource.toolbar.action;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.pekaboo.opensource.toolbar.settings.ConfigSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

/**
 * Action that opens the Shell Toolbar settings dialog.
 */
public class ConfigureShellCommandsAction extends AnAction {

    public ConfigureShellCommandsAction() {
        super("Configure Shell Commands...", "Manage shell command configurations", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // Open the settings dialog and navigate to Shell Toolbar settings
        if (project != null) {
            ShowSettingsUtilImpl.showSettingsDialog(project, ConfigSettingsConfigurable.class, true);
        } else {
            // For non-project context, try to open application-level settings
            ShowSettingsUtilImpl.showSettingsDialog(null, ConfigSettingsConfigurable.class, true);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
