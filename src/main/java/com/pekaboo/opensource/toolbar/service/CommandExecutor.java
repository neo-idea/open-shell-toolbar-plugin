package com.pekaboo.opensource.toolbar.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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

/**
 * Application-level service for executing shell commands.
 * Supports variable substitution and shows results via IntelliJ notifications.
 */
@Service(Service.Level.APP)
public class CommandExecutor {

    private static final Logger LOG = Logger.getInstance(CommandExecutor.class);
    private static final String NOTIFICATION_GROUP_ID = "Shell Toolbar Notifications";

    // Pattern for $(command) substitution
    private static final Pattern COMMAND_SUBSTITUTION = Pattern.compile("\\$\\(([^)]+)\\)");

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

        try {
            String command = substituteVariables(config.getCommand(), project);
            String workingDir = substituteVariables(config.getWorkingDir(), project);

            ProcessBuilder processBuilder = new ProcessBuilder();

            // Set up shell command based on OS
            List<String> commandList = buildShellCommand(command);
            processBuilder.command(commandList);

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
            if (workingDir != null) {
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

            if (exitCode == 0) {
                String successMessage = output.length() > 0
                        ? output.toString().trim()
                        : "Command executed successfully";
                showNotification(
                        "✓ " + config.getTitle(),
                        truncate(successMessage, 200),
                        NotificationType.INFORMATION,
                        project
                );
            } else {
                String errorMessage = output.length() > 0
                        ? output.toString().trim()
                        : "Command failed with exit code: " + exitCode;
                showNotification(
                        "✗ " + config.getTitle(),
                        truncate(errorMessage, 200),
                        NotificationType.ERROR,
                        project
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showNotification(
                    "Command Interrupted",
                    "The command '" + config.getTitle() + "' was interrupted.",
                    NotificationType.WARNING,
                    project
            );
            LOG.warn("Command interrupted: " + config.getTitle(), e);
        } catch (Exception e) {
            showNotification(
                    "Execution Failed",
                    "Failed to execute '" + config.getTitle() + "': " + e.getMessage(),
                    NotificationType.ERROR,
                    project
            );
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
            // Unix-like: use bash -c
            result.add("bash");
            result.add("-c");
        }

        result.add(command);
        return result;
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
