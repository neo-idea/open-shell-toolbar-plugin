package com.pekaboo.opensource.toolbar.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * Application-level service for executing shell commands.
 * Supports variable substitution, built-in terminal mode, and shows
 * results via IntelliJ notifications.
 */
@Service(Service.Level.APP)
public class CommandExecutor {

    private static final Logger LOG = Logger.getInstance(CommandExecutor.class);
    private static final String NOTIFICATION_GROUP_ID = "Shell Toolbar Notifications";
    private static final String PATH_MARKER_START = "__OST_PATH_START__";
    private static final String PATH_MARKER_END = "__OST_PATH_END__";

    /** Timeout for background (silent) execution in seconds. */
    private static final long BACKGROUND_TIMEOUT_SECONDS = 120;

    /** Maximum output captured for the notification balloon (characters). */
    private static final int MAX_OUTPUT_CHARS = 5000;

    private static final Pattern COMMAND_SUBSTITUTION = Pattern.compile("\\$\\(([^)]+)\\)");
    private static volatile String enrichedPath;

    public void executeCommand(@NotNull ShellCommandConfig config, @Nullable Project project) {
        if (!config.isEnabled()) {
            showNotification("Command Disabled",
                    "The command '" + config.getTitle() + "' is disabled.",
                    NotificationType.WARNING, project);
            return;
        }

        if (config.getCommand() == null || config.getCommand().trim().isEmpty()) {
            showNotification("Execution Failed",
                    "The command '" + config.getTitle() + "' is empty.",
                    NotificationType.ERROR, project);
            return;
        }

        String resolvedCommand = substituteVariables(config.getCommand(), project);
        String resolvedWorkingDir = substituteVariables(config.getWorkingDir(), project);

        if (config.isOpenInTerminal()) {
            openInBuiltInTerminal(resolvedCommand, resolvedWorkingDir, project, config.getTitle());
            return;
        }

        Application application = ApplicationManager.getApplication();
        application.executeOnPooledThread(() ->
                runProcess(config, resolvedCommand, resolvedWorkingDir, project, application));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  IntelliJ built-in terminal (for long-running / interactive commands)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Opens the command in IntelliJ's built-in Terminal tool window.
     * Falls back to the OS terminal if the Terminal plugin is unavailable
     * or project is null.
     */
    private void openInBuiltInTerminal(@NotNull String command,
                                       @Nullable String workingDir,
                                       @Nullable Project project,
                                       @NotNull String title) {
        String dir = resolveWorkingDir(workingDir, project);
        String tabName = title != null && !title.isEmpty() ? title : "Shell Command";

        if (project == null) {
            // No project context — fall back to OS terminal.
            openInExternalTerminal(command, dir, project);
            return;
        }

        final String finalDir = dir;
        final String finalCommand = command;
        final String finalTabName = tabName;
        final Project finalProject = project;

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Use reflection-free call — TerminalView is on the classpath
                // via the bundledPlugin("org.jetbrains.plugins.terminal") dependency.
                org.jetbrains.plugins.terminal.TerminalView terminalView =
                        org.jetbrains.plugins.terminal.TerminalView.getInstance(finalProject);

                if (terminalView == null) {
                    LOG.warn("TerminalView is null, falling back to OS terminal");
                    openInExternalTerminal(finalCommand, finalDir, finalProject);
                    return;
                }

                // Create a new terminal tab with the working directory.
                var widget = terminalView.createLocalShellWidget(finalDir, finalTabName);

                // Execute the command after the terminal shell initializes.
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        Thread.sleep(300); // brief delay for shell readiness
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    try {
                        widget.executeCommand(finalCommand);
                    } catch (Exception e) {
                        LOG.error("Failed to execute command in terminal", e);
                    }
                });
            } catch (NoSuchMethodError nsme) {
                LOG.warn("TerminalView API not available on this IDE version: " + nsme.getMessage());
                openInExternalTerminal(finalCommand, finalDir, finalProject);
            } catch (Exception e) {
                LOG.error("Failed to open built-in terminal, falling back to OS terminal", e);
                openInExternalTerminal(finalCommand, finalDir, finalProject);
            }
        });
    }

    @NotNull
    private String resolveWorkingDir(@Nullable String workingDir, @Nullable Project project) {
        if (workingDir != null && !workingDir.isEmpty()) {
            return workingDir;
        }
        if (project != null && project.getBasePath() != null) {
            return project.getBasePath();
        }
        return System.getProperty("user.home");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  OS terminal fallback
    // ═══════════════════════════════════════════════════════════════════

    private void openInExternalTerminal(@NotNull String command,
                                        @Nullable String workingDir,
                                        @Nullable Project project) {
        String dir = resolveWorkingDir(workingDir, project);
        String fullCommand = "cd " + shellQuote(dir) + " && " + command;

        try {
            if (SystemInfo.isMac) {
                openMacTerminal(fullCommand);
            } else if (SystemInfo.isWindows) {
                openWindowsTerminal(fullCommand);
            } else {
                openLinuxTerminal(fullCommand);
            }
        } catch (Exception e) {
            LOG.error("Failed to open external terminal", e);
        }
    }

    private void openMacTerminal(@NotNull String fullCommand) throws Exception {
        String escaped = fullCommand.replace("\\", "\\\\").replace("\"", "\\\"");
        String script = "tell application \"Terminal\"\n  activate\n  do script \"" + escaped + "\"\nend tell";
        new ProcessBuilder("osascript", "-e", script).redirectErrorStream(true).start();
    }

    private void openWindowsTerminal(@NotNull String fullCommand) throws Exception {
        new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", fullCommand).redirectErrorStream(true).start();
    }

    private void openLinuxTerminal(@NotNull String fullCommand) throws Exception {
        String[][] attempts = {
                {"gnome-terminal", "--", "bash", "-c", fullCommand + "; exec bash"},
                {"konsole", "-e", "bash", "-c", fullCommand + "; exec bash"},
                {"xterm", "-e", "bash", "-c", fullCommand + "; exec bash"},
        };
        for (String[] cmd : attempts) {
            try {
                new ProcessBuilder(cmd).redirectErrorStream(true).start();
                return;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("No supported terminal emulator found.");
    }

    @NotNull
    private static String shellQuote(@NotNull String path) {
        return "'" + path.replace("'", "'\"'\"'") + "'";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Background execution (for short-lived commands)
    // ═══════════════════════════════════════════════════════════════════

    private void runProcess(@NotNull ShellCommandConfig config,
                            @NotNull String command,
                            @Nullable String workingDir,
                            @Nullable Project project,
                            @NotNull Application application) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(buildShellCommand(command));

            String path = getEnrichedPath();
            if (path != null && !path.isEmpty()) {
                Map<String, String> env = processBuilder.environment();
                String current = env.get("PATH");
                env.put("PATH", path + (current != null && !current.isEmpty()
                        ? File.pathSeparator + current : ""));
            }

            if (workingDir != null && !workingDir.isEmpty()) {
                File dir = new File(workingDir);
                if (dir.exists() || dir.mkdirs()) {
                    processBuilder.directory(dir);
                }
            }

            processBuilder.redirectErrorStream(true);
            LOG.info("Executing command: " + String.join(" ", processBuilder.command()));

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (output.length() < MAX_OUTPUT_CHARS) {
                            output.append(line).append("\n");
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Error reading process output", e);
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            boolean finished = process.waitFor(BACKGROUND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                readerThread.join(2000);
                String msg = "Command timed out after " + BACKGROUND_TIMEOUT_SECONDS + "s.\n" +
                        "If this is a long-running command, enable \"Open in Terminal\".";
                application.invokeLater(() -> showNotification(
                        "⏱ " + config.getTitle(), msg, NotificationType.WARNING, project));
                return;
            }

            readerThread.join(2000);
            int exitCode = process.exitValue();
            String message = output.toString().trim();
            boolean success = exitCode == 0;
            String ttl = (success ? "\u2705 " : "\u274C ") + config.getTitle();
            String content = message.isEmpty()
                    ? (success ? "Command executed successfully"
                               : "Command failed with exit code: " + exitCode)
                    : truncate(message, 200);
            application.invokeLater(() ->
                    showNotification(ttl, content,
                            success ? NotificationType.INFORMATION : NotificationType.ERROR, project));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            application.invokeLater(() -> showNotification(
                    "Command Interrupted",
                    "The command '" + config.getTitle() + "' was interrupted.",
                    NotificationType.WARNING, project));
        } catch (Exception e) {
            application.invokeLater(() -> showNotification(
                    "Execution Failed",
                    "Failed to execute '" + config.getTitle() + "': " + e.getMessage(),
                    NotificationType.ERROR, project));
            LOG.error("Failed to execute command: " + config.getTitle(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Shell command building & PATH enrichment
    // ═══════════════════════════════════════════════════════════════════

    @NotNull
    private List<String> buildShellCommand(@NotNull String command) {
        List<String> result = new ArrayList<>();
        if (SystemInfo.isWindows) {
            result.add("cmd");
            result.add("/c");
        } else {
            String shell = System.getenv("SHELL");
            if (shell == null || shell.isEmpty()) {
                shell = SystemInfo.isMac ? "/bin/zsh" : "/bin/bash";
            }
            result.add(shell);
            result.add("-l");
            result.add("-c");
        }
        result.add(command);
        return result;
    }

    @Nullable
    private static String getEnrichedPath() {
        if (enrichedPath != null) return enrichedPath;
        if (SystemInfo.isWindows) { enrichedPath = ""; return enrichedPath; }
        String shell = System.getenv("SHELL");
        if (shell == null || shell.isEmpty()) {
            shell = SystemInfo.isMac ? "/bin/zsh" : "/bin/bash";
        }
        try {
            Process probe = new ProcessBuilder(shell, "-l", "-i", "-c",
                    "echo \"" + PATH_MARKER_START + "$PATH" + PATH_MARKER_END + "\"")
                    .redirectErrorStream(true).start();
            if (!probe.waitFor(5, TimeUnit.SECONDS)) {
                probe.destroyForcibly();
                LOG.warn("PATH probe timed out");
            }
            String output = readAll(probe);
            int start = output.indexOf(PATH_MARKER_START);
            int end = output.indexOf(PATH_MARKER_END);
            if (start >= 0 && end > start) {
                enrichedPath = output.substring(start + PATH_MARKER_START.length(), end).trim();
            }
        } catch (Exception e) {
            LOG.warn("Could not resolve enriched PATH", e);
        }
        if (enrichedPath == null) enrichedPath = "";
        return enrichedPath;
    }

    @NotNull
    private static String readAll(@NotNull Process process) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Variable substitution
    // ═══════════════════════════════════════════════════════════════════

    @NotNull
    private String substituteVariables(@Nullable String input, @Nullable Project project) {
        if (input == null) return "";
        String result = input;
        Map<String, String> replacements = new HashMap<>();
        if (project != null) {
            String projectPath = project.getBasePath();
            if (projectPath != null) {
                replacements.put("{{rootPath}}", projectPath);
                replacements.put("{{workspaceFolder}}", projectPath);
                replacements.put("$ProjectFileDir$", projectPath);
            }
        }
        String homeDir = System.getProperty("user.home");
        if (homeDir != null) replacements.put("$HOME", homeDir);
        String userName = System.getProperty("user.name");
        if (userName != null) replacements.put("$USER", userName);
        String apiKey = System.getenv("API_KEY");
        if (apiKey != null) replacements.put("$API_KEY", apiKey);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        Matcher matcher = COMMAND_SUBSTITUTION.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(executeSubstitutionCommand(matcher.group(1))));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @NotNull
    private String executeSubstitutionCommand(@NotNull String command) {
        switch (command.trim().toLowerCase()) {
            case "pwd": return new File(".").getAbsolutePath();
            case "date": return java.time.LocalDate.now().toString();
            case "time": return java.time.LocalTime.now().toString();
            default: return "";
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Notification helpers
    // ═══════════════════════════════════════════════════════════════════

    private void showNotification(@NotNull String title, @NotNull String content,
                                   @NotNull NotificationType type, @Nullable Project project) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, content, type);
        Notifications.Bus.notify(notification, project);
    }

    @NotNull
    private String truncate(@NotNull String str, int maxLength) {
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
}
