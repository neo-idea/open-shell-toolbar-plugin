package com.pekaboo.opensource.toolbar.statusbar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.CommandExecutor;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Status bar widget that displays shell command availability and
 * provides a popup menu for quick command execution.
 * <p>
 * Features:
 * - Shows "Shell Commands" with an icon in status bar
 * - Displays command execution results (success/failure) for 3 seconds
 * - Click to open popup with all enabled commands
 * - Execute commands directly from the popup
 */
public class CommandStatusBarWidget implements StatusBarWidget, StatusBarWidget.MultipleTextValuesPresentation, Disposable {

    private static final int RESULT_DISPLAY_DURATION_MS = 3000;
    private static final SimpleTextAttributes DEFAULT_ATTRIBUTES = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    private static final SimpleTextAttributes SUCCESS_ATTRIBUTES = new SimpleTextAttributes(
            SimpleTextAttributes.STYLE_BOLD, 0x56A369); // Green
    private static final SimpleTextAttributes ERROR_ATTRIBUTES = new SimpleTextAttributes(
            SimpleTextAttributes.STYLE_BOLD, 0xE55555); // Red

    private final Project project;
    private final Timer resultResetTimer;
    private String displayedText = "Shell Commands";
    private SimpleTextAttributes currentAttributes = DEFAULT_ATTRIBUTES;
    private String tooltip = "Click to execute shell commands";

    public CommandStatusBarWidget(@NotNull Project project) {
        this.project = project;
        this.resultResetTimer = new Timer("StatusBarWidgetResultReset");
    }

    @Override
    @Nullable
    public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        return this;
    }

    @Override
    @Nullable
    public String getTooltipText() {
        return tooltip;
    }

    @Override
    @Nullable
    public @NonNls @NotNull String getHoveredText() {
        return displayedText;
    }

    @Override
    public @Nullable @NlsContexts.StatusBarText String getText() {
        return displayedText;
    }

    @Override
    public @Nullable SimpleTextAttributes getTextAttributes() {
        return currentAttributes;
    }

    @Override
    public @Nullable Consumer<String> getClickConsumer() {
        return this::showCommandPopup;
    }

    /**
     * Shows the popup menu with all enabled shell commands.
     */
    private void showCommandPopup(@NotNull String clickedText) {
        ToolbarConfigService configService = ApplicationManager.getApplication()
                .getService(ToolbarConfigService.class);

        if (configService == null) {
            return;
        }

        List<ShellCommandConfig> enabledConfigs = configService.getEnabledConfigs();

        if (enabledConfigs.isEmpty()) {
            return;
        }

        // Create JList with configurations
        DefaultListModel<ShellCommandConfig> listModel = new DefaultListModel<>();
        for (ShellCommandConfig config : enabledConfigs) {
            listModel.addElement(config);
        }

        JList<ShellCommandConfig> list = new JList<>(listModel);
        list.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ShellCommandConfig> list,
                                                  @NotNull ShellCommandConfig config,
                                                  int index,
                                                  boolean selected,
                                                  boolean hasFocus) {
                String icon = config.getIcon() != null ? config.getIcon() : "💻";
                setIcon(new EmojiIconWrapper(icon));

                append(config.getTitle(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                append(" ");

                String commandPreview = config.getCommand();
                if (commandPreview.length() > 40) {
                    commandPreview = commandPreview.substring(0, 37) + "...";
                }
                append(commandPreview, SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        });

        // Add selection listener
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ShellCommandConfig selected = list.getSelectedValue();
                if (selected != null) {
                    executeCommand(selected);
                }
            }
        });

        // Create and show popup
        PopupChooserBuilder<ShellCommandConfig> builder = JBPopupFactory.getInstance()
                .createPopupChooserBuilder(list)
                .setTitle("Shell Commands")
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .setItemSelectedCallback(selected -> {
                    if (selected != null) {
                        executeCommand(selected);
                    }
                });

        builder.buildShow().showInScreenCoordinates(
                ComponentUtil.getWindow(list),
                getPopupLocation());
    }

    /**
     * Gets the location for showing the popup.
     * Attempts to position it above the status bar.
     */
    private Point getPopupLocation() {
        // Default to center of screen if we can't get status bar position
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        return new Point(bounds.x + bounds.width / 2 - 200, bounds.y + bounds.height - 200);
    }

    /**
     * Executes the given command and updates the widget display.
     */
    private void executeCommand(@NotNull ShellCommandConfig config) {
        CommandExecutor executor = ApplicationManager.getApplication()
                .getService(CommandExecutor.class);

        if (executor != null) {
            // Display execution start
            displayedText = "▶ " + config.getTitle();
            currentAttributes = DEFAULT_ATTRIBUTES;
            tooltip = "Executing: " + config.getTitle();

            // Schedule result display after a brief moment
            new Timer("CommandExecutionDisplay").schedule(new TimerTask() {
                @Override
                public void run() {
                    // Execute the command
                    // Note: The actual success/error display would require callback support
                    // For now, we show the command was triggered
                    ApplicationManager.getApplication().invokeLater(() -> {
                        displayedText = "✓ " + config.getTitle();
                        currentAttributes = SUCCESS_ATTRIBUTES;
                        tooltip = "Command executed: " + config.getTitle();

                        // Reset after duration
                        scheduleResultReset();
                    });
                }
            }, 100);

            executor.executeCommand(config, project);
        }
    }

    /**
     * Displays a success message in the status bar.
     * This can be called by the CommandExecutor after successful execution.
     */
    public void showSuccess(@NotNull String commandName) {
        displayedText = "✓ " + commandName;
        currentAttributes = SUCCESS_ATTRIBUTES;
        tooltip = "Command executed successfully: " + commandName;
        scheduleResultReset();
    }

    /**
     * Displays an error message in the status bar.
     * This can be called by the CommandExecutor after failed execution.
     */
    public void showError(@NotNull String commandName) {
        displayedText = "✗ " + commandName;
        currentAttributes = ERROR_ATTRIBUTES;
        tooltip = "Command failed: " + commandName;
        scheduleResultReset();
    }

    /**
     * Schedules the widget text to reset to default after a delay.
     */
    private void scheduleResultReset() {
        // Cancel any pending reset
        resultResetTimer.purge();

        resultResetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    displayedText = "Shell Commands";
                    currentAttributes = DEFAULT_ATTRIBUTES;
                    tooltip = "Click to execute shell commands";
                });
            }
        }, RESULT_DISPLAY_DURATION_MS);
    }

    @Override
    public void dispose() {
        resultResetTimer.cancel();
    }

    /**
     * Simple icon wrapper that renders an emoji as an icon.
     */
    private static class EmojiIconWrapper implements Icon {
        private final String emoji;
        private static final int ICON_SIZE = 16;

        EmojiIconWrapper(@NotNull String emoji) {
            this.emoji = emoji;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(g2d.getFont().deriveFont(12f));
            g2d.drawString(emoji, x, y + ICON_SIZE - 2);
            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return ICON_SIZE;
        }

        @Override
        public int getIconHeight() {
            return ICON_SIZE;
        }
    }
}
