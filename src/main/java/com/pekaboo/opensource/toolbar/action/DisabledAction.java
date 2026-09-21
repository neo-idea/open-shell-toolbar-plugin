package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A permanently disabled action used as a hint entry in menus and popups
 * (e.g. "No commands configured"). Rendered grayed-out and not clickable.
 */
public class DisabledAction extends AnAction {

    public DisabledAction(@NotNull String text) {
        super(text);
        getTemplatePresentation().setEnabled(false);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(false);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // no-op: this action is a non-interactive hint
    }
}
