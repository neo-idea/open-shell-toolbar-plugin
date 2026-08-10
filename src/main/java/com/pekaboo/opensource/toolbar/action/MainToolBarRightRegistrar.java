package com.pekaboo.opensource.toolbar.action;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the shell-command icon in every toolbar group that exists
 * on the current IDE build, without removing the static fallback.
 *
 * <p>This is additive: the group is statically in MainToolBar (plugin.xml),
 * and we ADD it to New-UI groups here. In Classic UI the New-UI groups
 * don't exist so no duplicate; in New UI MainToolBar is hidden so no
 * duplicate either.</p>
 */
public final class MainToolBarRightRegistrar implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(MainToolBarRightRegistrar.class);

    // All candidate toolbar groups across IntelliJ versions.
    private static final String[] TOOLBAR_GROUPS = {
            "MainToolbarRight",   // New UI 2024+ (lowercase 'b')
            "MainToolBarRight",   // Classic UI (uppercase 'B')
    };

    private static volatile boolean done = false;

    @Override
    public void runActivity(@NotNull Project project) {
        // Always show the notification — helps user confirm plugin is active.
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                Notifications.Bus.notify(
                        NotificationGroupManager.getInstance()
                                .getNotificationGroup("Shell Toolbar Notifications")
                                .createNotification(
                                        "Shell Toolbar Installed",
                                        "Look for the shell icon in the top toolbar.\n" +
                                        "Can't find it? Press Ctrl+Shift+A (Find Action) and type \"Shell Commands\".",
                                        NotificationType.INFORMATION),
                        project);
            } catch (Exception ignored) {
                // Notification group might not be ready yet — non-critical.
            }
        });

        if (done) {
            return;
        }

        ActionManager am = ActionManager.getInstance();
        AnAction group = am.getAction("ShellToolbarGroup");
        if (group == null) {
            LOG.warn("ShellToolbarGroup not found in ActionManager");
            return;
        }

        // ADD (not move) to every toolbar group that exists.
        // We keep the static MainToolBar registration AND add to New UI groups.
        // No duplicate risk: in Classic UI the New UI groups don't exist,
        // in New UI MainToolBar is hidden by default.
        int added = 0;
        for (String groupId : TOOLBAR_GROUPS) {
            try {
                AnAction candidate = am.getAction(groupId);
                if (candidate instanceof DefaultActionGroup) {
                    DefaultActionGroup dg = (DefaultActionGroup) candidate;
                    if (!dg.containsAction(group)) {
                        dg.add(group);
                        added++;
                        LOG.info("ShellToolbarGroup added to " + groupId);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to add to " + groupId + ": " + e.getMessage());
            }
        }

        LOG.info("ShellToolbarGroup registration complete. Added to " + added
                + " additional group(s). Static MainToolBar registration preserved.");
        done = true;
    }
}
