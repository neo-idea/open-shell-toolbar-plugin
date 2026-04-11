package com.pekaboo.opensource.toolbar.statusbar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Consumer;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.CommandExecutor;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CommandStatusBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation, Disposable {

    private static final int RESULT_DISPLAY_DURATION_MS = 3000;
    private final Project project;
    private final Timer resultResetTimer;
    private String displayedText = "Shell Commands";
    private String tooltip = "Click to execute shell commands";

    public CommandStatusBarWidget(@NotNull Project project) {
        this.project = project;
        this.resultResetTimer = new Timer("StatusBarWidgetResultReset");
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

        List<ShellCommandConfig> enabledConfigs = configService.getEnabledConfigs();
        if (enabledConfigs.isEmpty()) return;

        List<ShellCommandConfig> items = new ArrayList<>(enabledConfigs);
        JList<ShellCommandConfig> list = new JList<>(items.toArray(new ShellCommandConfig[0]));
        list.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ShellCommandConfig> list,
                                                  @NotNull ShellCommandConfig config,
                                                  int index, boolean selected, boolean hasFocus) {
                setIcon(new EmojiIconWrapper(config.getIcon() != null ? config.getIcon() : "💻"));
                append(config.getTitle(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                append("  ");
                String preview = config.getCommand();
                if (preview != null && preview.length() > 40) preview = preview.substring(0, 37) + "...";
                append(preview != null ? preview : "", SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        });

        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(items)
                .setTitle("Shell Commands")
                .setItemChosenCallback(this::executeCommand)
                .createPopup()
                .showInFocusCenter();
    }

    private void executeCommand(@NotNull ShellCommandConfig config) {
        CommandExecutor executor = ApplicationManager.getApplication().getService(CommandExecutor.class);
        if (executor != null) {
            executor.executeCommand(config, project);
            displayedText = "▶ " + config.getTitle();
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
                });
            }
        }, RESULT_DISPLAY_DURATION_MS);
    }

    @Override
    public void dispose() {
        resultResetTimer.cancel();
    }

    private static class EmojiIconWrapper implements Icon {
        private final String emoji;
        private static final int SIZE = 16;

        EmojiIconWrapper(@NotNull String emoji) { this.emoji = emoji; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(g2d.getFont().deriveFont(12f));
            g2d.drawString(emoji, x, y + SIZE - 2);
            g2d.dispose();
        }
        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }
}
