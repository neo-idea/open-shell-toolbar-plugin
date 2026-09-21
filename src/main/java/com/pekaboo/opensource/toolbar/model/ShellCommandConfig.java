package com.pekaboo.opensource.toolbar.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuration model for a shell command.
 * Represents a single shell command that can be executed from the toolbar.
 */
public class ShellCommandConfig {

    private String id;
    private String title;
    private String command;
    private String workingDir;
    private String icon;
    private boolean enabled;

    /**
     * When true (the default for new commands) the command is run inside the
     * IntelliJ built-in Terminal tool window. This is required for long-running
     * or interactive commands such as {@code pnpm next start}, {@code npm run dev},
     * {@code tail -f}, etc. Quick one-off commands can opt out to run silently
     * in the background with a notification balloon.
     */
    private boolean openInTerminal;

    /**
     * Default constructor - generates a unique ID and sets default values.
     * Commands run in the built-in terminal by default.
     */
    public ShellCommandConfig() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.icon = "\uD83D\uDCBB";
        this.openInTerminal = true;
    }

    /**
     * Full constructor for creating a command configuration.
     *
     * @param id             Unique identifier (use null for auto-generated UUID)
     * @param title          Display title for the command
     * @param command        The shell command to execute
     * @param workingDir     Working directory for command execution
     * @param icon           Icon emoji string (e.g., "💻", "🚀", "⚙️")
     * @param enabled        Whether the command is enabled
     */
    public ShellCommandConfig(String id, String title, String command, String workingDir, String icon, boolean enabled) {
        this(id, title, command, workingDir, icon, enabled, false);
    }

    /**
     * Full constructor with openInTerminal flag.
     *
     * @param id             Unique identifier (use null for auto-generated UUID)
     * @param title          Display title for the command
     * @param command        The shell command to execute
     * @param workingDir     Working directory for command execution
     * @param icon           Icon emoji string
     * @param enabled        Whether the command is enabled
     * @param openInTerminal Whether to open in an external terminal
     */
    public ShellCommandConfig(String id, String title, String command, String workingDir,
                              String icon, boolean enabled, boolean openInTerminal) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = title;
        this.command = command;
        this.workingDir = workingDir;
        this.icon = icon != null ? icon : "\uD83D\uDCBB";
        this.enabled = enabled;
        this.openInTerminal = openInTerminal;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getWorkingDir() { return workingDir; }
    public void setWorkingDir(String workingDir) { this.workingDir = workingDir; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isOpenInTerminal() { return openInTerminal; }
    public void setOpenInTerminal(boolean openInTerminal) { this.openInTerminal = openInTerminal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShellCommandConfig that = (ShellCommandConfig) o;
        return enabled == that.enabled &&
                openInTerminal == that.openInTerminal &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(command, that.command) &&
                Objects.equals(workingDir, that.workingDir) &&
                Objects.equals(icon, that.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, command, workingDir, icon, enabled, openInTerminal);
    }

    @Override
    public String toString() {
        return title != null ? title : "Unnamed Command";
    }
}
