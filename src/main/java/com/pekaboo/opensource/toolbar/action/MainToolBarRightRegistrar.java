package com.pekaboo.opensource.toolbar.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Ensures the shell-command icon appears in the best available toolbar
 * location across all IntelliJ versions and UI modes (Classic + New UI).
 *
 * <p>IntelliJ renamed the toolbar groups when the New UI shipped:
 * <ul>
 *   <li><b>Classic UI</b>: {@code MainToolBar}, {@code MainToolBarRight}</li>
 *   <li><b>New UI (2024+)</b>: {@code MainToolbarRight} (lowercase 'b')</li>
 * </ul>
 *
 * The group is statically registered in {@code MainToolBar} via plugin.xml
 * as a universal fallback. At startup this registrar tries to MOVE it to
 * the best available group for the current UI mode.</p>
 */
public final class MainToolBarRightRegistrar implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(MainToolBarRightRegistrar.class);

    /**
     * Candidate groups in priority order. The first one that exists wins.
     * Covers New UI, Classic UI, and all IntelliJ-based IDEs.
     */
    private static final String[] RIGHT_GROUP_CANDIDATES = {
            "MainToolbarRight",   // New UI (2024+, lowercase 'b')
            "MainToolBarRight",   // Classic UI (uppercase 'B')
    };

    private static final String FALLBACK_GROUP = "MainToolBar"; // universal fallback

    private static volatile boolean relocated = false;

    @Override
    public void runActivity(@NotNull Project project) {
        if (relocated) {
            return;
        }

        ActionManager am = ActionManager.getInstance();
        AnAction group = am.getAction("ShellToolbarGroup");
        if (group == null) {
            LOG.warn("ShellToolbarGroup action not found — cannot register on toolbar");
            return;
        }

        // Remove from the static fallback group first to avoid duplicates.
        AnAction fallback = am.getAction(FALLBACK_GROUP);
        if (fallback instanceof DefaultActionGroup) {
            DefaultActionGroup fallbackGroup = (DefaultActionGroup) fallback;
            if (fallbackGroup.containsAction(group)) {
                fallbackGroup.remove(group);
                LOG.info("Removed ShellToolbarGroup from " + FALLBACK_GROUP);
            }
        }

        // Try each candidate group in order.
        for (String groupId : RIGHT_GROUP_CANDIDATES) {
            AnAction candidate = am.getAction(groupId);
            if (candidate instanceof DefaultActionGroup) {
                DefaultActionGroup candidateGroup = (DefaultActionGroup) candidate;
                candidateGroup.add(group);
                relocated = true;
                LOG.info("ShellToolbarGroup registered in " + groupId);
                return;
            }
        }

        // No right-side group found — fall back to MainToolBar (always exists).
        if (fallback instanceof DefaultActionGroup) {
            ((DefaultActionGroup) fallback).add(group);
            LOG.info("ShellToolbarGroup registered in fallback " + FALLBACK_GROUP);
        }
        relocated = true;
    }
}
