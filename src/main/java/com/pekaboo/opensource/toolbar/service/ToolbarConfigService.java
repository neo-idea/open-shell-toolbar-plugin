package com.pekaboo.opensource.toolbar.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import com.intellij.openapi.diagnostic.Logger;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.intellij.openapi.application.ApplicationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Application-level service for managing shell command configurations.
 * Persists command configurations to pekaboo-shell-toolbar.xml.
 */
@Service(Service.Level.APP)
@State(name = "ShellToolbarConfig", storages = @Storage("pekaboo-shell-toolbar.xml"))
public class ToolbarConfigService {

    private static final Logger LOG = Logger.getInstance(ToolbarConfigService.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Gets the singleton instance of this service.
     */
    public static ToolbarConfigService getInstance() {
        return ApplicationManager.getApplication().getService(ToolbarConfigService.class);
    }

    /**
     * Inner class to hold the persistent state.
     * Uses @XCollection annotation for proper XML serialization.
     */
    public static class State {
        @SuppressWarnings("unused")
        public List<ShellCommandConfig> configs;
    }

    private final State state = new State();

    /**
     * Gets the current state for persistence.
     */
    @Nullable
    public State getState() {
        return state;
    }

    /**
     * Loads state from persisted storage.
     */
    public void loadState(@NotNull State state) {
        synchronized (this) {
            this.state.configs = state.configs != null ? new ArrayList<>(state.configs) : new ArrayList<>();
        }
    }

    /**
     * Gets all command configurations.
     * Thread-safe operation.
     *
     * @return List of all shell command configurations
     */
    @NotNull
    public synchronized List<ShellCommandConfig> getConfigs() {
        ensureDefaultConfigs();
        return new CopyOnWriteArrayList<>(state.configs);
    }

    /**
     * Adds a new command configuration.
     *
     * @param config The configuration to add
     */
    public synchronized void addConfig(@NotNull ShellCommandConfig config) {
        ensureDefaultConfigs();
        state.configs.add(config);
    }

    /**
     * Removes a command configuration by ID.
     *
     * @param id The ID of the configuration to remove
     * @return true if removed, false if not found
     */
    public synchronized boolean removeConfig(@NotNull String id) {
        ensureDefaultConfigs();
        return state.configs.removeIf(config -> id.equals(config.getId()));
    }

    /**
     * Updates an existing command configuration.
     *
     * @param config The configuration to update (identified by ID)
     * @return true if updated, false if not found
     */
    public synchronized boolean updateConfig(@NotNull ShellCommandConfig config) {
        ensureDefaultConfigs();
        for (int i = 0; i < state.configs.size(); i++) {
            if (state.configs.get(i).getId().equals(config.getId())) {
                state.configs.set(i, config);
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a configuration by ID.
     *
     * @param id The ID to search for
     * @return The configuration, or null if not found
     */
    @Nullable
    public synchronized ShellCommandConfig getConfigById(@NotNull String id) {
        ensureDefaultConfigs();
        return state.configs.stream()
                .filter(config -> id.equals(config.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all enabled configurations.
     *
     * @return List of enabled shell command configurations
     */
    @NotNull
    public synchronized List<ShellCommandConfig> getEnabledConfigs() {
        ensureDefaultConfigs();
        return state.configs.stream()
                .filter(ShellCommandConfig::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Ensures default configurations exist if none are present.
     * This runs on first load to provide sample commands.
     */
    private synchronized void ensureDefaultConfigs() {
        if (state.configs == null) {
            state.configs = new ArrayList<>();
            // Add sample default configurations
            state.configs.add(createDefaultConfig(
                    "open-terminal",
                    "Open Terminal",
                    "open -na Terminal",
                    "$ProjectFileDir$",
                    "⌘"
            ));

            state.configs.add(createDefaultConfig(
                    "launch-browser",
                    "Launch Browser",
                    "open http://localhost:8080",
                    "$ProjectFileDir$",
                    "🌐"
            ));

            state.configs.add(createDefaultConfig(
                    "open-settings",
                    "Open Settings",
                    "open -a 'System Preferences'",
                    null,
                    "⚙️"
            ));
        }
    }

    /**
     * Creates a default configuration helper.
     */
    private ShellCommandConfig createDefaultConfig(String id, String title, String command, String workingDir, String icon) {
        ShellCommandConfig config = new ShellCommandConfig();
        config.setId(id);
        config.setTitle(title);
        config.setCommand(command);
        config.setWorkingDir(workingDir);
        config.setIcon(icon);
        config.setEnabled(true);
        return config;
    }

    /**
     * Clears all configurations.
     * Use with caution - this cannot be undone.
     */
    public synchronized void clearAllConfigs() {
        ensureDefaultConfigs();
        state.configs.clear();
    }

    /**
     * Gets the count of configurations.
     *
     * @return Number of configurations
     */
    public synchronized int getConfigCount() {
        ensureDefaultConfigs();
        return state.configs.size();
    }

    /**
     * Exports all configurations to a JSON file.
     *
     * @param filePath The file path to export to
     * @return true if export succeeded, false otherwise
     */
    public synchronized boolean exportToJson(@NotNull String filePath) {
        ensureDefaultConfigs();
        try {
            Path path = Paths.get(filePath);
            // Ensure parent directory exists
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (FileWriter writer = new FileWriter(path.toFile())) {
                GSON.toJson(state.configs, writer);
                LOG.info("Exported " + state.configs.size() + " configurations to " + filePath);
                return true;
            }
        } catch (IOException e) {
            LOG.error("Failed to export configurations to " + filePath, e);
            return false;
        }
    }

    /**
     * Imports configurations from a JSON file.
     * Replaces existing configurations with imported ones.
     *
     * @param filePath The file path to import from
     * @return true if import succeeded, false otherwise
     */
    public synchronized boolean importFromJson(@NotNull String filePath) {
        ensureDefaultConfigs();
        try (FileReader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<List<ShellCommandConfig>>() {}.getType();
            List<ShellCommandConfig> imported = GSON.fromJson(reader, listType);

            if (imported != null) {
                state.configs.clear();
                state.configs.addAll(imported);
                LOG.info("Imported " + imported.size() + " configurations from " + filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            LOG.error("Failed to import configurations from " + filePath, e);
            return false;
        }
    }

    /**
     * Exports configurations to a JSON string.
     *
     * @return JSON string representation of configurations
     */
    @NotNull
    public synchronized String exportToJsonString() {
        ensureDefaultConfigs();
        return GSON.toJson(state.configs);
    }

    /**
     * Imports configurations from a JSON string.
     * Replaces existing configurations with imported ones.
     *
     * @param jsonString The JSON string to import from
     * @return true if import succeeded, false otherwise
     */
    public synchronized boolean importFromJsonString(@NotNull String jsonString) {
        ensureDefaultConfigs();
        try {
            Type listType = new TypeToken<List<ShellCommandConfig>>() {}.getType();
            List<ShellCommandConfig> imported = GSON.fromJson(jsonString, listType);

            if (imported != null) {
                state.configs.clear();
                state.configs.addAll(imported);
                LOG.info("Imported " + imported.size() + " configurations from JSON string");
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.error("Failed to import configurations from JSON string", e);
            return false;
        }
    }
}
