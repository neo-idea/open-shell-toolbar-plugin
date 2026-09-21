package com.pekaboo.opensource.toolbar.statusbar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.pekaboo.opensource.toolbar.action.ConfigureShellCommandsAction;
import com.pekaboo.opensource.toolbar.action.DisabledAction;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.CommandExecutor;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import com.pekaboo.opensource.toolbar.ui.CommandListRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CommandStatusBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation, Disposable {

    private static final int RESULT_DISPLAY_DURATION_MS = 3000;
    private final Project project;
    private final Timer resultResetTimer;
    private String displayedText = "Shell Commands";
    private String tooltip = "Click to execute shell commands";
    private volatile StatusBar statusBar;

    public CommandStatusBarWidget(@NotNull Project project) {
        this.project = project;
        this.resultResetTimer = new Timer("StatusBarWidgetResultReset");
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    private void repaintWidget() {
        StatusBar bar = statusBar;
        if (bar != null) {
            bar.updateWidget(ID());
        }
    }

    @Override
    public @NotNull String ID() {
        return "ShellToolbarStatusBar";
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public @NotNull String getText() {
        return displayedText;
    }

    @Override
    public float getAlignment() {
        return Component.CENTER_ALIGNMENT;
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return tooltip;
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return e -> showCommandPopup();
    }

    private void showCommandPopup() {
        ToolbarConfigService configService = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);
        if (configService == null) return;

        // The click consumer only fires after install(), so the bar is set in practice.
        StatusBar bar = statusBar;
        if (bar == null) return;
        Component anchor = bar.getComponent();

        List<ShellCommandConfig> enabledConfigs = configService.getEnabledConfigs();

        if (enabledConfigs.isEmpty()) {
            // Same behaviour as the top toolbar dropdown: show a hint plus the
            // Configure entry instead of silently doing nothing.
            DefaultActionGroup group = new DefaultActionGroup();
            group.add(new DisabledAction("No commands configured"));
            group.addSeparator();
            group.add(new ConfigureShellCommandsAction());
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    "Shell Commands",
                    group,
                    DataManager.getInstance().getDataContext(anchor),
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    true);
            popup.showUnderneathOf(anchor);
            return;
        }

        // Rendered with the shared CommandListRenderer (emoji icon + bold title +
        // gray command preview) — identical to the toolbar dropdown's visual language.
        JBPopup popup = JBPopupFactory.getInstance()
                .createPopupChooserBuilder(enabledConfigs)
                .setTitle("Shell Commands")
                .setRenderer(new CommandListRenderer())
                .setNamerForFiltering(config -> String.valueOf(config.getTitle()) + " "
                        + String.valueOf(config.getCommand()))
                .setItemChosenCallback(this::executeCommand)
                .createPopup();
        popup.showUnderneathOf(anchor);
    }

    private void executeCommand(@NotNull ShellCommandConfig config) {
        CommandExecutor executor = ApplicationManager.getApplication().getService(CommandExecutor.class);
        if (executor != null) {
            executor.executeCommand(config, project);
            displayedText = "▶ " + config.getTitle();
            repaintWidget();
            scheduleResultReset();
        }
    }

    private void scheduleResultReset() {
        resultResetTimer.purge();
        resultResetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    displayedText = "Shell Commands";
                    tooltip = "Click to execute shell commands";
                    repaintWidget();
                });
            }
        }, RESULT_DISPLAY_DURATION_MS);
    }

    @Override
    public void dispose() {
        resultResetTimer.cancel();
    }
}
