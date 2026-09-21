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
 * Popup display mode (default): the shell commands render as a SINGLE ICON on
 * the main toolbar; clicking it opens a dropdown with the commands plus a
 * "Configure" entry.
 *
 * <p>In FLAT mode with at least one enabled command this group hides itself
 * and the sibling {@link FlatToolbarActionGroup} takes over, rendering one
 * button per command. Toolbar buttons cannot switch between "dropdown" and
 * "flat" at runtime, so the two looks are separate groups whose visibility
 * toggles — presentation visibility IS honored live by toolbar updates.</p>
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
        // Visible unless FLAT mode has commands (then the flat group shows).
        boolean visible = !FlatToolbarActionGroup.isFlatWithCommands();
        e.getPresentation().setEnabled(true);
        e.getPresentation().setVisible(visible);
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
            if (configs.isEmpty()) {
                // Parity with the status bar popup: hint instead of an empty menu.
                actions.add(new DisabledAction("No commands configured"));
                actions.add(Separator.getInstance());
            } else {
                for (ShellCommandConfig config : configs) {
                    actions.add(new CustomToolbarAction(config));
                }
                actions.add(Separator.getInstance());
            }
        }

        actions.add(new ConfigureShellCommandsAction());

        return actions.toArray(new AnAction[0]);
    }
}
