package com.pekaboo.opensource.toolbar.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * Application-level service for executing shell commands.
 * Supports variable substitution, built-in terminal mode, and shows
 * results via IntelliJ notifications.
 * <p>Registered as an {@code applicationService} in plugin.xml (must not also
 * carry the {@code @Service} annotation to avoid duplicate registration).</p>
 */
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
     *
     * <p>Strategy (fixes "clicking never opens the built-in terminal"):
     * <ol>
     *   <li>Modern "Reworked Terminal" tab API (2025.2+), invoked via
     *       reflection so the plugin still compiles against 2023.3.</li>
     *   <li>Classic {@code TerminalView} API (2023.3+, deprecated but
     *       functional in newer IDEs).</li>
     *   <li>OS terminal — only as a last resort, with a visible notification
     *       so the fallback is never silent.</li>
     * </ol>
     */
    private void openInBuiltInTerminal(@NotNull String command,
                                       @Nullable String workingDir,
                                       @Nullable Project project,
                                       @NotNull String title) {
        String dir = resolveWorkingDir(workingDir, project);
        String tabName = title.isEmpty() ? "Shell Command" : title;

        if (project == null) {
            // No project context — fall back to OS terminal.
            openInExternalTerminal(command, dir, null);
            return;
        }

        final Project finalProject = project;
        ApplicationManager.getApplication().invokeLater(() -> {
            if (openInReworkedTerminal(finalProject, dir, tabName, command)) {
                return;
            }
            if (openInClassicTerminal(finalProject, dir, tabName, command)) {
                return;
            }
            LOG.warn("Built-in Terminal unavailable; falling back to the OS terminal");
            showNotification("⚠ " + tabName,
                    "IntelliJ built-in terminal is unavailable in this IDE; opening the OS terminal instead.",
                    NotificationType.WARNING, finalProject);
            openInExternalTerminal(command, dir, finalProject);
        });
    }

    /**
     * Modern terminal API (2025.2+):
     * {@code TerminalToolWindowTabsManager.getInstance(project).createTabBuilder()
     *   .workingDirectory(dir).tabName(name).createTab()} followed by
     * {@code tab.view.createSendTextBuilder().shouldExecute().send(command)}.
     *
     * <p>Invoked entirely via reflection — these classes do not exist in the
     * 2023.3 compile target. Returns {@code false} (not an error) when the IDE
     * predates the API, so the caller can use the classic path.</p>
     */
    private boolean openInReworkedTerminal(@NotNull Project project,
                                           @NotNull String workingDir,
                                           @NotNull String tabName,
                                           @NotNull String command) {
        try {
            Class<?> managerClass = Class.forName(
                    "com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager");
            Object manager = managerClass.getMethod("getInstance", Project.class).invoke(null, project);

            Class<?> builderClass = Class.forName(
                    "com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabBuilder");
            Object builder = managerClass.getMethod("createTabBuilder").invoke(manager);
            builder = builderClass.getMethod("workingDirectory", String.class).invoke(builder, workingDir);
            builder = builderClass.getMethod("tabName", String.class).invoke(builder, tabName);
            Object tab = builderClass.getMethod("createTab").invoke(builder);

            Class<?> tabClass = Class.forName(
                    "com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab");
            Object view = tabClass.getMethod("getView").invoke(tab);

            Class<?> viewInterface = Class.forName(
                    "com.intellij.terminal.frontend.view.TerminalView");
            Object textBuilder = viewInterface.getMethod("createSendTextBuilder").invoke(view);

            Class<?> sendBuilderClass = Class.forName(
                    "org.jetbrains.plugins.terminal.view.TerminalSendTextBuilder");
            // shouldExecute() appends the newline so the command is actually run;
            // send() buffers the text while the shell process is still starting.
            sendBuilderClass.getMethod("shouldExecute").invoke(textBuilder);
            sendBuilderClass.getMethod("send", String.class).invoke(textBuilder, command);
            return true;
        } catch (ClassNotFoundException e) {
            // IDE older than 2025.2 — the classic path below is the right one.
            return false;
        } catch (Throwable t) {
            Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null)
                    ? t.getCause() : t;
            LOG.warn("Reworked terminal API failed, falling back to the classic one", cause);
            return false;
        }
    }

    /**
     * Classic terminal API (2023.3+): creates a classic terminal tab and types
     * the command into it. {@code executeCommand} buffers the command until the
     * shell session is ready, so no EDT sleep is required.
     */
    private boolean openInClassicTerminal(@NotNull Project project,
                                          @NotNull String workingDir,
                                          @NotNull String tabName,
                                          @NotNull String command) {
        try {
            TerminalView terminalView = TerminalView.getInstance(project);
            ShellTerminalWidget widget = terminalView.createLocalShellWidget(workingDir, tabName);
            executeCommandWhenShellReady(project, tabName, widget, command, 0);
            return true;
        } catch (Throwable t) {
            LOG.warn("Classic terminal API failed", t);
            return false;
        }
    }

    /** Max attempts to type the command into a freshly created classic terminal tab. */
    private static final int TERMINAL_COMMAND_MAX_ATTEMPTS = 6;
    /** Delay between attempts; retries run on a pooled executor, never the EDT. */
    private static final long TERMINAL_COMMAND_RETRY_MS = 300;

    /**
     * Types the command into the terminal tab, retrying briefly if the shell
     * session is still initializing. Never blocks or sleeps on the EDT.
     */
    private void executeCommandWhenShellReady(@NotNull Project project,
                                              @NotNull String tabName,
                                              @NotNull ShellTerminalWidget widget,
                                              @NotNull String command,
                                              int attempt) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                widget.executeCommand(command);
            } catch (Exception e) {
                if (attempt + 1 < TERMINAL_COMMAND_MAX_ATTEMPTS) {
                    CompletableFuture
                            .delayedExecutor(TERMINAL_COMMAND_RETRY_MS, TimeUnit.MILLISECONDS)
                            .execute(() -> executeCommandWhenShellReady(
                                    project, tabName, widget, command, attempt + 1));
                } else {
                    LOG.warn("Could not type command into terminal after "
                            + TERMINAL_COMMAND_MAX_ATTEMPTS + " attempts", e);
                    showNotification("⚠ " + tabName,
                            "The terminal tab is open, but the command could not be typed automatically. Please run it manually.",
                            NotificationType.WARNING, project);
                }
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
            // Log only the raw (pre-substitution) command: the resolved command may
            // contain secrets such as $API_KEY that must not reach idea.log.
            LOG.info("Executing command: " + config.getCommand());

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (output) {
                            if (output.length() < MAX_OUTPUT_CHARS) {
                                output.append(line).append("\n");
                            }
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
            String message;
            synchronized (output) {
                message = output.toString().trim();
            }
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
        String cached = enrichedPath;
        if (cached != null) return cached;
        synchronized (CommandExecutor.class) {
            if (enrichedPath != null) return enrichedPath;
            enrichedPath = probeEnrichedPath();
            return enrichedPath;
        }
    }

    @NotNull
    private static String probeEnrichedPath() {
        if (SystemInfo.isWindows) return "";
        String shell = System.getenv("SHELL");
        if (shell == null || shell.isEmpty()) {
            shell = SystemInfo.isMac ? "/bin/zsh" : "/bin/bash";
        }
        try {
            Process probe = new ProcessBuilder(shell, "-l", "-c",
                    "echo \"" + PATH_MARKER_START + "$PATH" + PATH_MARKER_END + "\"")
                    .redirectErrorStream(true).start();
            if (!probe.waitFor(5, TimeUnit.SECONDS)) {
                probe.destroyForcibly();
                LOG.warn("PATH probe timed out");
                return "";
            }
            String output = readAll(probe);
            int start = output.indexOf(PATH_MARKER_START);
            int end = output.indexOf(PATH_MARKER_END);
            if (start >= 0 && end > start) {
                return output.substring(start + PATH_MARKER_START.length(), end).trim();
            }
        } catch (Exception e) {
            LOG.warn("Could not resolve enriched PATH", e);
        }
        return "";
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
            String replacement = resolveSubstitutionCommand(matcher.group(1));
            // Unknown $(...) is left untouched so the shell resolves it natively
            // (e.g. `echo $(git rev-parse HEAD)`).
            matcher.appendReplacement(sb, Matcher.quoteReplacement(
                    replacement != null ? replacement : matcher.group(0)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolves a supported substitution command, or null if it should be left
     * for the shell to execute.
     */
    @Nullable
    private String resolveSubstitutionCommand(@NotNull String command) {
        switch (command.trim().toLowerCase()) {
            case "pwd": return new File(".").getAbsolutePath();
            case "date": return java.time.LocalDate.now().toString();
            case "time": return java.time.LocalTime.now().toString();
            default: return null;
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
