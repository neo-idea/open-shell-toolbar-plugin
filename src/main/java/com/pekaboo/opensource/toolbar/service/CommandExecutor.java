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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * Application-level service for executing shell commands.
 * Supports variable substitution and shows results via IntelliJ notifications.
 */
@Service(Service.Level.APP)
public class CommandExecutor {

    private static final Logger LOG = Logger.getInstance(CommandExecutor.class);
    private static final String NOTIFICATION_GROUP_ID = "Shell Toolbar Notifications";
    private static final String PATH_MARKER_START = "__OST_PATH_START__";
    private static final String PATH_MARKER_END = "__OST_PATH_END__";

    // Pattern for $(command) substitution
    private static final Pattern COMMAND_SUBSTITUTION = Pattern.compile("\\$\\(([^)]+)\\)");

    // Cached full PATH resolved from the user's interactive login shell. GUI-launched
    // IDEs inherit a minimal PATH that misses node/nvm/volta/homebrew binaries, so
    // commands like pnpm/npm/node are not found. This is resolved once and injected
    // into every spawned process.
    private static volatile String enrichedPath;

    /**
     * Executes a shell command based on the provided configuration.
     *
     * @param config The command configuration to execute
     * @param project The current project (can be null for application-level commands)
     */
    public void executeCommand(@NotNull ShellCommandConfig config, @Nullable Project project) {
        if (!config.isEnabled()) {
            showNotification("Command Disabled", "The command '" + config.getTitle() + "' is disabled.",
                    NotificationType.WARNING, project);
            return;
        }

        if (config.getCommand() == null || config.getCommand().trim().isEmpty()) {
            showNotification("Execution Failed", "The command '" + config.getTitle() + "' is empty.",
                    NotificationType.ERROR, project);
            return;
        }

        // Resolve variables before dispatching to the background thread.
        String resolvedCommand = substituteVariables(config.getCommand(), project);
        String resolvedWorkingDir = substituteVariables(config.getWorkingDir(), project);

        Application application = ApplicationManager.getApplication();
        application.executeOnPooledThread(() -> runProcess(config, resolvedCommand, resolvedWorkingDir, project, application));
    }

    private void runProcess(@NotNull ShellCommandConfig config,
                            @NotNull String command,
                            @Nullable String workingDir,
                            @Nullable Project project,
                            @NotNull Application application) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            // Set up shell command based on OS
            List<String> commandList = buildShellCommand(command);
            processBuilder.command(commandList);

            // Inject the user's full PATH so binaries installed via nvm/volta/homebrew/
            // corepack (e.g. pnpm) are resolvable even when the IDE was launched from
            // the GUI and inherited a minimal environment.
            String path = getEnrichedPath();
            if (path != null && !path.isEmpty()) {
                Map<String, String> env = processBuilder.environment();
                String current = env.get("PATH");
                env.put("PATH", path + (current != null && !current.isEmpty() ? File.pathSeparator + current : ""));
            }

            // Set working directory if specified
            if (workingDir != null && !workingDir.isEmpty()) {
                File dir = new File(workingDir);
                if (dir.exists() || dir.mkdirs()) {
                    processBuilder.directory(dir);
                } else {
                    LOG.warn("Could not create working directory: " + workingDir);
                }
            }

            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);

            LOG.info("Executing command: " + String.join(" ", commandList));
            if (workingDir != null && !workingDir.isEmpty()) {
                LOG.info("Working directory: " + workingDir);
            }

            // Start the process
            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            String message = output.toString().trim();
            boolean success = exitCode == 0;
            String title = (success ? "✓ " : "✗ ") + config.getTitle();
            String content = message.isEmpty()
                    ? (success ? "Command executed successfully" : "Command failed with exit code: " + exitCode)
                    : truncate(message, 200);
            NotificationType type = success ? NotificationType.INFORMATION : NotificationType.ERROR;
            application.invokeLater(() -> showNotification(title, content, type, project));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            application.invokeLater(() -> showNotification(
                    "Command Interrupted",
                    "The command '" + config.getTitle() + "' was interrupted.",
                    NotificationType.WARNING,
                    project));
            LOG.warn("Command interrupted: " + config.getTitle(), e);
        } catch (Exception e) {
            application.invokeLater(() -> showNotification(
                    "Execution Failed",
                    "Failed to execute '" + config.getTitle() + "': " + e.getMessage(),
                    NotificationType.ERROR,
                    project));
            LOG.error("Failed to execute command: " + config.getTitle(), e);
        }
    }

    /**
     * Builds the appropriate shell command based on the operating system.
     */
    @NotNull
    private List<String> buildShellCommand(@NotNull String command) {
        List<String> result = new ArrayList<>();

        if (SystemInfo.isWindows) {
            // Windows: use cmd /c
            result.add("cmd");
            result.add("/c");
        } else {
            // Use the user's login shell so ~/.zprofile/~/.bash_profile is loaded.
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

    /**
     * Resolves the full PATH from the user's interactive login shell (cached).
     * Falls back to null if it cannot be determined, in which case the inherited
     * environment is used unchanged.
     */
    @Nullable
    private static String getEnrichedPath() {
        if (enrichedPath != null) {
            return enrichedPath;
        }
        String shell = System.getenv("SHELL");
        if (shell == null || shell.isEmpty()) {
            shell = SystemInfo.isMac ? "/bin/zsh" : "/bin/bash";
        }
        try {
            // Interactive login shell loads ~/.zshrc/~/.bashrc so PATH set up by
            // nvm/volta/homebrew is captured.
            Process probe = new ProcessBuilder(shell, "-l", "-i", "-c",
                    "echo \"" + PATH_MARKER_START + "$PATH" + PATH_MARKER_END + "\"")
                    .redirectErrorStream(true).start();
            String output = readAll(probe);
            probe.waitFor(5, TimeUnit.SECONDS);
            int start = output.indexOf(PATH_MARKER_START);
            int end = output.indexOf(PATH_MARKER_END);
            if (start >= 0 && end > start) {
                enrichedPath = output.substring(start + PATH_MARKER_START.length(), end).trim();
            }
        } catch (Exception e) {
            LOG.warn("Could not resolve enriched PATH from " + shell, e);
        }
        if (enrichedPath == null) {
            enrichedPath = "";
        }
        return enrichedPath;
    }

    @NotNull
    private static String readAll(@NotNull Process process) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Substitutes variables in the command string.
     * Supports:
     * - {{rootPath}} - Project root path
     * - {{workspaceFolder}} - Project root path (alias for rootPath)
     * - $ProjectFileDir$ - IntelliJ variable (kept for compatibility)
     * - $HOME - User home directory
     * - $USER - Current username
     * - $API_KEY - API key from environment
     * - $(pwd) - Current working directory (command substitution)
     * - $(date) - Current date
     * - $(time) - Current time
     */
    @NotNull
    private String substituteVariables(@Nullable String input, @Nullable Project project) {
        if (input == null) {
            return "";
        }

        String result = input;
        Map<String, String> replacements = new HashMap<>();

        // Project-specific variables
        if (project != null) {
            String projectPath = project.getBasePath();
            if (projectPath != null) {
                replacements.put("{{rootPath}}", projectPath);
                replacements.put("{{workspaceFolder}}", projectPath);
                replacements.put("$ProjectFileDir$", projectPath);
            }
        }

        // System environment variables
        String homeDir = System.getProperty("user.home");
        if (homeDir != null) {
            replacements.put("$HOME", homeDir);
        }

        String userName = System.getProperty("user.name");
        if (userName != null) {
            replacements.put("$USER", userName);
        }

        // API key from environment (if set)
        String apiKey = System.getenv("API_KEY");
        if (apiKey != null) {
            replacements.put("$API_KEY", apiKey);
        }

        // Apply static replacements
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Handle command substitution $(command)
        Matcher matcher = COMMAND_SUBSTITUTION.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String cmd = matcher.group(1);
            String substitutionResult = executeSubstitutionCommand(cmd);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(substitutionResult));
        }
        matcher.appendTail(sb);
        result = sb.toString();

        return result;
    }

    /**
     * Executes a simple command for substitution purposes.
     * Supports: pwd, date, time
     */
    @NotNull
    private String executeSubstitutionCommand(@NotNull String command) {
        try {
            switch (command.trim().toLowerCase()) {
                case "pwd":
                    return new File(".").getAbsolutePath();
                case "date":
                    return java.time.LocalDate.now().toString();
                case "time":
                    return java.time.LocalTime.now().toString();
                default:
                    // For security, don't execute arbitrary commands in substitution
                    return "";
            }
        } catch (Exception e) {
            LOG.warn("Failed to execute substitution command: " + command, e);
            return "";
        }
    }

    /**
     * Shows a notification balloon to the user.
     */
    private void showNotification(@NotNull String title, @NotNull String content,
                                   @NotNull NotificationType type, @Nullable Project project) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(title, content, type);

        Notifications.Bus.notify(notification, project);
    }

    /**
     * Truncates a string to a maximum length.
     */
    @NotNull
    private String truncate(@NotNull String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
