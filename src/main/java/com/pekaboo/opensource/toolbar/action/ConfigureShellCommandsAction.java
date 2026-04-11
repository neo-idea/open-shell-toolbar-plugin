package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
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
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "pekaboo.shell.toolbar");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
