package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ToolbarActionGroup extends DefaultActionGroup {

    public ToolbarActionGroup() {
        // Non-popup group: command actions are rendered inline on the main
        // toolbar as individual icon buttons, in configuration order.
        super("Shell Commands", false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(true);
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        removeAll();

        ToolbarConfigService service = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);

        if (service != null) {
            List<ShellCommandConfig> configs = service.getEnabledConfigs();
            for (ShellCommandConfig config : configs) {
                add(new CustomToolbarAction(config));
            }
            if (!configs.isEmpty()) {
                addSeparator();
            }
        }

        add(new ConfigureShellCommandsAction());

        return super.getChildren(e);
    }
}
