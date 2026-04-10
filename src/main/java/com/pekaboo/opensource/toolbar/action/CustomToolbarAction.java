package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.CommandExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Custom toolbar action that executes a specific shell command.
 * Each instance is bound to a ShellCommandConfig and displays its title and icon.
 */
public class CustomToolbarAction extends AnAction {

    private final ShellCommandConfig config;

    /**
     * Creates a new toolbar action for the given command configuration.
     *
     * @param config The command configuration this action will execute
     */
    public CustomToolbarAction(@NotNull ShellCommandConfig config) {
        this.config = config;

        // Set up the presentation with config values
        Presentation presentation = getTemplatePresentation();
        presentation.setText(config.getTitle(), false);
        presentation.setDescription(config.getCommand());

        // Use emoji icon if available
        if (config.getIcon() != null && !config.getIcon().isEmpty()) {
            presentation.setIcon(new EmojiIcon(config.getIcon()));
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        CommandExecutor executor = project != null
                ? project.getService(CommandExecutor.class)
                : com.intellij.openapi.application.ApplicationManager.getApplication().getService(CommandExecutor.class);

        if (executor != null) {
            executor.executeCommand(config, project);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        Presentation presentation = e.getPresentation();

        // Update visibility based on enabled state
        presentation.setVisible(config.isEnabled());
        presentation.setEnabled(config.isEnabled());

        // Update text and description from config
        presentation.setText(config.getTitle(), false);
        presentation.setDescription(config.getCommand());
    }

    /**
     * Gets the configuration associated with this action.
     */
    @NotNull
    public ShellCommandConfig getConfig() {
        return config;
    }
}
