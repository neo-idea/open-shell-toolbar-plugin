package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Ensures the shell-command icon appears exactly ONCE on the main toolbar,
 * preferring the RIGHT segment (immediately left of the settings gear)
 * when the IDE provides it.
 *
 * <p>The group is statically registered in {@code MainToolBar} via
 * plugin.xml as a universal fallback. At startup, if {@code MainToolBarRight}
 * exists we MOVE the group there (removing from {@code MainToolBar}) to
 * achieve the ideal placement and avoid a duplicate icon. On IDE builds
 * where {@code MainToolBarRight} no longer exists (e.g. 2026.1+) the
 * fallback registration stays in place.</p>
 */
public final class MainToolBarRightRegistrar implements StartupActivity {

    private static volatile boolean relocated = false;

    @Override
    public void runActivity(@NotNull Project project) {
        if (relocated) {
            return;
        }

        ActionManager am = ActionManager.getInstance();
        AnAction group = am.getAction("ShellToolbarGroup");
        if (group == null) {
            return;
        }

        AnAction right = am.getAction("MainToolBarRight");
        if (!(right instanceof DefaultActionGroup)) {
            // MainToolBarRight not available on this IDE build;
            // keep the static MainToolBar registration.
            return;
        }

        DefaultActionGroup rightGroup = (DefaultActionGroup) right;

        // Prevent duplicate if somehow already present.
        if (rightGroup.containsAction(group)) {
            relocated = true;
            return;
        }

        // Move from MainToolBar → MainToolBarRight to avoid two icons.
        AnAction main = am.getAction("MainToolBar");
        if (main instanceof DefaultActionGroup) {
            DefaultActionGroup mainGroup = (DefaultActionGroup) main;
            if (mainGroup.containsAction(group)) {
                mainGroup.remove(group);
            }
        }

        rightGroup.add(group);
        relocated = true;
    }
}
