package com.pekaboo.opensource.toolbar.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the Shell Toolbar status bar widget.
 * This factory is registered in plugin.xml and creates instances
 * of CommandStatusBarWidget for each project.
 */
public class CommandStatusBarWidgetFactory implements StatusBarWidgetFactory {

    @NonNls
    private static final String ID = "ShellToolbarStatusBar";

    @Override
    @NotNull
    public String getId() {
        return ID;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Shell Toolbar";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }

    @Override
    @NotNull
    public StatusBarWidget createWidget(@NotNull Project project) {
        return new CommandStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
