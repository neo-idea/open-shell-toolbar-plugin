package com.pekaboo.opensource.toolbar.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.pekaboo.opensource.toolbar.ui.ConfigManagerPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Configurable implementation for integrating shell toolbar settings into IntelliJ's Settings dialog.
 * Provides the configuration panel for managing shell command configurations.
 */
public class ConfigSettingsConfigurable implements SearchableConfigurable {

    private final Project project;
    private ConfigManagerPanel configPanel;

    private static final String DISPLAY_NAME = "Shell Toolbar";
    private static final String ID = "pekaboo.shell.toolbar";
    private static final String HELP_TOPIC = "settings.shell.toolbar";

    public ConfigSettingsConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (configPanel == null) {
            configPanel = new ConfigManagerPanel(project);
        }
        return configPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        return configPanel != null && configPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (configPanel != null) {
            configPanel.apply();
        }
    }

    @Override
    public void reset() {
        if (configPanel != null) {
            configPanel.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        if (configPanel != null) {
            configPanel.dispose();
            configPanel = null;
        }
    }

    @Nullable
    @Override
    public @NlsContexts.ConfigurableName String getHelpTopic() {
        return HELP_TOPIC;
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        // Optional: implement search filtering if needed
        return null;
    }
}
