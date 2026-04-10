package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Dynamic toolbar action group that populates itself from the ToolbarConfigService.
 * Automatically reflects changes to command configurations.
 */
public class ToolbarActionGroup extends DefaultActionGroup {

    private static final long REBUILD_INTERVAL_MS = 5000; // Rebuild at most every 5 seconds
    private long lastRebuildTime = 0;

    public ToolbarActionGroup() {
        super("Shell Commands", true);
        setPopup(true);
        rebuildActions();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void updateChildren() {
        // Throttle rebuilds to avoid excessive updates
        long now = System.currentTimeMillis();
        if (now - lastRebuildTime > REBUILD_INTERVAL_MS) {
            rebuildActions();
            lastRebuildTime = now;
        }
        super.updateChildren();
    }

    /**
     * Rebuilds the action list from the current configurations.
     */
    private void rebuildActions() {
        // Clear existing actions
        removeAll();

        // Get configurations from service
        ToolbarConfigService service = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);

        if (service != null) {
            List<ShellCommandConfig> configs = service.getEnabledConfigs();

            // Create an action for each enabled configuration
            for (ShellCommandConfig config : configs) {
                add(new CustomToolbarAction(config));
            }

            // Add separator and configuration action if there are any configs
            if (!configs.isEmpty()) {
                addSeparator();
            }
        }

        // Always add the "Configure..." action to open settings
        add(new ConfigureShellCommandsAction());
    }
}
