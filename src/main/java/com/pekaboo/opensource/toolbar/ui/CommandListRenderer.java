package com.pekaboo.opensource.toolbar.ui;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.pekaboo.opensource.toolbar.action.EmojiIcon;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Shared renderer for shell-command list popups (status bar, and any future
 * list surface).
 *
 * <p> Renders emoji icon + bold title + gray command preview — the same visual
 * language as the toolbar dropdown (emoji icon + title), so the top (toolbar)
 * and bottom (status bar) surfaces render consistently.</p>
 */
public class CommandListRenderer extends ColoredListCellRenderer<ShellCommandConfig> {

    private static final int COMMAND_PREVIEW_LENGTH = 40;
    private static final String DEFAULT_ICON = "\uD83D\uDCBB"; // 💻

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends ShellCommandConfig> list,
                                         ShellCommandConfig config,
                                         int index,
                                         boolean selected,
                                         boolean hasFocus) {
        if (config == null) {
            return;
        }
        setIcon(new EmojiIcon(config.getIcon() != null && !config.getIcon().isEmpty()
                ? config.getIcon() : DEFAULT_ICON));
        append(config.getTitle() != null ? config.getTitle() : "",
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        append("  ");
        String preview = config.getCommand();
        if (preview != null && preview.length() > COMMAND_PREVIEW_LENGTH) {
            preview = preview.substring(0, COMMAND_PREVIEW_LENGTH - 3) + "...";
        }
        append(preview != null ? preview : "", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
}
