package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.IconLoader;
import com.pekaboo.opensource.toolbar.model.DisplayMode;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shell-command group on the main toolbar.
 *
 * <p>Two display modes (see {@link DisplayMode}, switchable in Settings):
 * <ul>
 *   <li>{@code POPUP} (default) — renders as a SINGLE ICON to the left of the
 *       IDE settings (gear) button; clicking it opens a dropdown with the
 *       commands plus a "Configure" entry.</li>
 *   <li>{@code FLAT} — one individual toolbar button per enabled command,
 *   e.g. three commands = three buttons directly on the toolbar.</li>
 * </ul>
 * With no enabled commands the group always falls back to the popup look so
 * the Configure entry stays reachable.</p>
 */
public class ToolbarActionGroup extends DefaultActionGroup {

    private static final javax.swing.Icon PLUGIN_ICON =
            IconLoader.getIcon("/META-INF/pluginIcon.svg", ToolbarActionGroup.class);

    public ToolbarActionGroup() {
        // popup = true: the group renders as a single toolbar icon button.
        // Clicking it opens a dropdown menu populated by getChildren().
        super("Shell Commands", true);
        getTemplatePresentation().setIcon(PLUGIN_ICON);
        // Re-render promptly when the display mode (or command list) changes.
        ToolbarConfigService.subscribe(() ->
                ApplicationManager.getApplication().invokeLater(this::syncPopupFlag));
    }

    /**
     * Keeps the popup flag in sync with the configured display mode. Called on
     * the EDT via the config-change topic; {@link #getChildren} re-checks it
     * as a safety net for the next toolbar update pass.
     */
    private void syncPopupFlag() {
        ToolbarConfigService service = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);
        if (service == null) return;
        boolean popup = service.getDisplayMode() != DisplayMode.FLAT
                || service.getEnabledConfigs().isEmpty();
        setPopup(popup);
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
            boolean flat = service.getDisplayMode() == DisplayMode.FLAT && !configs.isEmpty();

            // In FLAT mode the group must not be a popup so the toolbar renders
            // the command buttons inline (one button per command).
            setPopup(!flat);

            if (configs.isEmpty()) {
                // Parity with the status bar popup: offer a hint instead of an empty menu.
                actions.add(new DisabledAction("No commands configured"));
                actions.add(Separator.getInstance());
            } else {
                for (ShellCommandConfig config : configs) {
                    actions.add(new CustomToolbarAction(config));
                }
                if (!flat) {
                    actions.add(Separator.getInstance());
                }
            }
        }

        // In FLAT mode the toolbar already shows every command; keep it clean
        // and skip the Configure entry there (Settings / tool window remain
        // available). All other modes keep it.
        if (!isFlatMode(service)) {
            actions.add(new ConfigureShellCommandsAction());
        }

        return actions.toArray(new AnAction[0]);
    }

    private static boolean isFlatMode(@Nullable ToolbarConfigService service) {
        return service != null
                && service.getDisplayMode() == DisplayMode.FLAT
                && !service.getEnabledConfigs().isEmpty();
    }
}
