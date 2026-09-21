package com.pekaboo.opensource.toolbar.action;

import javax.swing.*;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.CommandExecutor;
import com.pekaboo.opensource.toolbar.ui.CommandIconManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        applyIcon(presentation);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        // CommandExecutor is an application-level service; resolve it from the
        // application container regardless of the current project context.
        CommandExecutor executor = com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(CommandExecutor.class);

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
        // Re-apply the icon: async-loaded URL/SVG icons land here on the
        // update pass right after the load finishes.
        applyIcon(presentation);
    }

    private static final String DEFAULT_ICON = "\uD83D\uDCBB"; // 💻

    /**
     * Emoji icons render directly; image sources (URL / data URI / inline
     * SVG) resolve through {@link CommandIconManager} which may load them in
     * the background — until ready the default emoji is shown.
     */
    private void applyIcon(@NotNull Presentation presentation) {
        String raw = config.getIcon();
        if (CommandIconManager.isImageSource(raw)) {
            Icon icon = CommandIconManager.resolve(raw);
            presentation.setIcon(icon != null ? icon : new EmojiIcon(DEFAULT_ICON));
        } else if (raw != null && !raw.isEmpty()) {
            presentation.setIcon(new EmojiIcon(raw));
        }
    }

    /**
     * Gets the configuration associated with this action.
     */
    @NotNull
    public ShellCommandConfig getConfig() {
        return config;
    }
}
