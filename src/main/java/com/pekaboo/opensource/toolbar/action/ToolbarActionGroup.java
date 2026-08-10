package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.IconLoader;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Popup action group that renders as a SINGLE ICON on the main toolbar.
 *
 * <p>The icon appears to the LEFT of the IDE settings (gear) button.
 * Clicking the icon shows a dropdown menu with configured shell commands
 * and a "Configure" entry. This guarantees the plugin icon is always
 * visible immediately after installation -- even before any commands are
 * configured.</p>
 */
public class ToolbarActionGroup extends DefaultActionGroup {

    private static final javax.swing.Icon PLUGIN_ICON =
            IconLoader.getIcon("/META-INF/pluginIcon.svg", ToolbarActionGroup.class);

    public ToolbarActionGroup() {
        // popup = true: the group renders as a single toolbar icon button.
        // Clicking it opens a dropdown menu populated by getChildren().
        super("Shell Commands", true);
        getTemplatePresentation().setIcon(PLUGIN_ICON);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Always show the icon on the toolbar.
        e.getPresentation().setEnabled(true);
        e.getPresentation().setVisible(true);
        if (e.getPresentation().getIcon() == null) {
            e.getPresentation().setIcon(PLUGIN_ICON);
        }
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();

        ToolbarConfigService service = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);

        if (service != null) {
            List<ShellCommandConfig> configs = service.getEnabledConfigs();
            for (ShellCommandConfig config : configs) {
                actions.add(new CustomToolbarAction(config));
            }
            if (!configs.isEmpty()) {
                actions.add(Separator.getInstance());
            }
        }

        actions.add(new ConfigureShellCommandsAction());

        return actions.toArray(new AnAction[0]);
    }
}
