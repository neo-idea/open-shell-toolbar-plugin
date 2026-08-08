package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Best-effort registration of the shell command group on MainToolBarRight.
 * Newer IDE builds (e.g. WebStorm 2026.1 and remote-development backends) no
 * longer provide the MainToolBarRight group, so the group is statically
 * registered on MainToolBar in plugin.xml and only added here when the group
 * actually exists.
 */
public final class MainToolBarRightRegistrar implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction group = actionManager.getAction("ShellToolbarGroup");
        AnAction right = actionManager.getAction("MainToolBarRight");
        if (group == null || !(right instanceof DefaultActionGroup)) {
            return;
        }
        DefaultActionGroup rightGroup = (DefaultActionGroup) right;
        if (rightGroup.containsAction(group)) {
            return;
        }
        rightGroup.add(group);
    }
}
