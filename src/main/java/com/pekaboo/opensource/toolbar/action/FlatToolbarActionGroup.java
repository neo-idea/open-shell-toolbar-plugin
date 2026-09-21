package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.pekaboo.opensource.toolbar.model.DisplayMode;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat display mode: every enabled command renders as its OWN toolbar button
 * ("3 commands = 3 buttons"). This group is registered non-popup
 * ({@code popup=false}), so the toolbar flattens its children inline.
 *
 * <p>It is invisible unless the display mode is {@link DisplayMode#FLAT} and
 * at least one command is enabled; the sibling {@link ToolbarActionGroup}
 * (single dropdown icon) covers the opposite case. Toolbar buttons cannot be
 * rebuilt by toggling a group's popup flag at runtime, so the two variants
 * are separate groups whose <em>visibility</em> switches — presentation
 * visibility IS honored live by toolbar update passes.</p>
 */
public class FlatToolbarActionGroup extends DefaultActionGroup {

    public FlatToolbarActionGroup() {
        // popup=false → children render as individual buttons, no dropdown.
        super("Shell Commands (Flat)", false);
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        ToolbarConfigService service = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);
        if (service == null) {
            return AnAction.EMPTY_ARRAY;
        }
        List<AnAction> actions = new ArrayList<>();
        for (ShellCommandConfig config : service.getEnabledConfigs()) {
            actions.add(new CustomToolbarAction(config));
        }
        return actions.toArray(new AnAction[0]);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(isFlatWithCommands());
        e.getPresentation().setEnabled(isFlatWithCommands());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    static boolean isFlatWithCommands() {
        ToolbarConfigService service = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);
        return service != null
                && service.getDisplayMode() == DisplayMode.FLAT
                && !service.getEnabledConfigs().isEmpty();
    }
}
