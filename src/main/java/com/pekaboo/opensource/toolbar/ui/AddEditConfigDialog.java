package com.pekaboo.opensource.toolbar.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for adding or editing a shell command configuration.
 * Provides form fields for all config properties with smart input assistance.
 */
public class AddEditConfigDialog extends DialogWrapper {

    private final Project project;
    private final boolean isEditMode;
    private ShellCommandConfig existingConfig;

    private JBTextField titleField;
    private JBTextArea commandField;
    private JBTextField workingDirField;
    private JBTextField iconField;
    private JBCheckBox enabledCheckBox;

    // Buttons for variable insertion
    private final List<JButton> variableButtons = new ArrayList<>();
    // Buttons for emoji presets
    private final List<JButton> emojiButtons = new ArrayList<>();

    private static final String[] VARIABLES = {
        "{{rootPath}}", "$HOME", "$USER", "$API_KEY", "{{workspaceFolder}}", "$(pwd)"
    };

    private static final String[] EMOJI_PRESETS = {
        "\uD83D\uDCBB",  // 💻 Computer
        "\uD83C\uDF10",  // 🌐 Globe
        "\u2699\uFE0F",  // ⚙️ Gear
        "\uD83D\uDE80",  // 🚀 Rocket
        "\uD83D\uDCC1",  // 📁 Folder
        "\uD83D\uDCC4",  // 📄 Page
        "\uD83D\uDD27",  // 🔧 Wrench
        "\uD83C\uDFA8",  // 🎨 Palette
        "\uD83D\uDCF1",  // 📱 Phone
        "\uD83D\uDD12",  // 🔒 Lock
        "\u2B50",        // ⭐ Star
        "\u2705",        // ✅ Check
        "\uD83D\uDCCA",  // 📊 Chart
        "\uD83D\uDD28",  // 🔨 Hammer
        "\uD83C\uDFAF",  // 🎯 Target
        "\uD83D\uDCA1"   // 💡 Bulb
    };

    /**
     * Constructor for adding a new config.
     *
     * @param project The current project
     */
    public AddEditConfigDialog(@NotNull Project project) {
        this(project, null);
    }

    /**
     * Constructor for adding or editing a config.
     *
     * @param project The current project
     * @param config  Existing config to edit, or null for new config
     */
    public AddEditConfigDialog(@NotNull Project project, @Nullable ShellCommandConfig config) {
        super(project);
        this.project = project;
        this.existingConfig = config;
        this.isEditMode = config != null;

        setTitle(isEditMode ? "Edit Shell Command" : "Add Shell Command");
        init();
        if (config != null) {
            populateFromConfig(config);
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        titleField = new JBTextField();
        titleField.setToolTipText("Enter a descriptive title for this command");

        commandField = new JBTextArea(3, 40);
        commandField.setLineWrap(true);
        commandField.setWrapStyleWord(true);
        commandField.setToolTipText("Enter the shell command to execute");

        workingDirField = new JBTextField();
        workingDirField.setToolTipText("Working directory for command execution (leave empty for default)");

        iconField = new JBTextField();
        iconField.setToolTipText("Enter an emoji icon (e.g., 💻)");

        enabledCheckBox = new JBCheckBox("Enabled", true);
        enabledCheckBox.setToolTipText("Enable or disable this command");

        // Create variable buttons panel
        JPanel variablePanel = createVariableButtonsPanel();

        // Create emoji presets panel
        JPanel emojiPanel = createEmojiPresetsPanel();

        // Create browse button for working directory
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseWorkingDirectory());

        JPanel workingDirPanel = new JPanel(new BorderLayout());
        workingDirPanel.add(workingDirField, BorderLayout.CENTER);
        workingDirPanel.add(browseButton, BorderLayout.EAST);
        workingDirPanel.setBorder(JBUI.Borders.empty(0, 0, 0, 0));

        // Create scroll pane for command text area
        JBScrollPane commandScrollPane = new JBScrollPane(commandField);
        commandScrollPane.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.GRAY, 1),
                JBUI.Borders.empty(2)
        ));
        commandScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        commandScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Build the main form
        FormBuilder formBuilder = FormBuilder.createFormBuilder()
                .addLabeledComponent("Title:", titleField)
                .addComponent(createLabelPanel("Command:", "The shell command to execute"))
                .addComponent(commandScrollPane)
                .addComponent(variablePanel)
                .addVerticalGap(4)
                .addComponent(createLabelPanel("Working Directory:", "Leave empty for project directory"))
                .addComponent(workingDirPanel)
                .addVerticalGap(4)
                .addComponent(createLabelPanel("Icon:", "Click an emoji or enter your own"))
                .addComponent(iconField)
                .addComponent(emojiPanel)
                .addVerticalGap(8)
                .addComponent(enabledCheckBox);

        JPanel mainPanel = formBuilder.getPanel();
        mainPanel.setPreferredSize(JBUI.size(550, 400));
        mainPanel.setBorder(JBUI.Borders.empty(10));

        return mainPanel;
    }

    /**
     * Creates a panel with a label and optional tooltip.
     */
    private JPanel createLabelPanel(String labelText, String tooltipText) {
        JPanel panel = new JPanel(new BorderLayout());
        JBLabel label = new JBLabel(labelText);
        label.setToolTipText(tooltipText);
        panel.add(label, BorderLayout.WEST);
        panel.setBorder(JBUI.Borders.empty(4, 0, 2, 0));
        return panel;
    }

    /**
     * Creates the panel with variable insertion buttons.
     */
    private JPanel createVariableButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        panel.setBorder(JBUI.Borders.empty(4, 18, 4, 0));

        for (String variable : VARIABLES) {
            JButton button = createVariableButton(variable);
            variableButtons.add(button);
            panel.add(button);
        }

        return panel;
    }

    /**
     * Creates a variable insertion button.
     */
    private JButton createVariableButton(String variable) {
        JButton button = new JButton(variable);
        button.setFont(UIUtil.getLabelFont().deriveFont(Font.PLAIN, 10f));
        button.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.GRAY, 1),
                JBUI.Borders.empty(2, 6)
        ));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.addActionListener(e -> insertVariable(variable));

        // Hover effect
        button.setModel(new DefaultButtonModel() {
            @Override
            public boolean isRollover() {
                return true;
            }
        });

        return button;
    }

    /**
     * Creates the panel with emoji preset buttons.
     */
    private JPanel createEmojiPresetsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        panel.setBorder(JBUI.Borders.empty(4, 18, 4, 0));

        for (String emoji : EMOJI_PRESETS) {
            JButton button = createEmojiButton(emoji);
            emojiButtons.add(button);
            panel.add(button);
        }

        return panel;
    }

    /**
     * Creates an emoji preset button.
     */
    private JButton createEmojiButton(String emoji) {
        JButton button = new JButton(emoji);
        button.setFont(new java.awt.Font("Segoe UI Emoji", Font.PLAIN, 16));
        button.setBorder(JBUI.Borders.empty(4));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Click to insert " + emoji);
        button.addActionListener(e -> iconField.setText(emoji));

        // Hover effect
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        return button;
    }

    /**
     * Inserts a variable at the current cursor position in the command field.
     */
    private void insertVariable(String variable) {
        int cursorPos = commandField.getCaretPosition();
        String currentText = commandField.getText();

        StringBuilder newText = new StringBuilder(currentText);
        newText.insert(cursorPos, variable);

        commandField.setText(newText.toString());
        commandField.setCaretPosition(cursorPos + variable.length());
        commandField.requestFocus();
    }

    /**
     * Opens a file chooser to select a working directory.
     */
    private void browseWorkingDirectory() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,  // choose files
                true,  // choose folders
                false, // choose jars
                false, // choose jars as files
                false, // choose jar contents
                true   // choose multiple
        );
        descriptor.setTitle("Select Working Directory");
        descriptor.setDescription("Choose the working directory for the command");

        com.intellij.openapi.vfs.VirtualFile chosen = FileChooser.chooseFile(descriptor, project, null);
        if (chosen != null) {
            workingDirField.setText(chosen.getPath());
        }
    }

    /**
     * Populates form fields from an existing config.
     */
    private void populateFromConfig(ShellCommandConfig config) {
        titleField.setText(config.getTitle() != null ? config.getTitle() : "");
        commandField.setText(config.getCommand() != null ? config.getCommand() : "");
        workingDirField.setText(config.getWorkingDir() != null ? config.getWorkingDir() : "");
        iconField.setText(config.getIcon() != null ? config.getIcon() : "💻");
        enabledCheckBox.setSelected(config.isEnabled());
    }

    /**
     * Validates the form before submission.
     */
    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> validations = new ArrayList<>();

        if (titleField.getText().trim().isEmpty()) {
            validations.add(new ValidationInfo("Title is required", titleField));
        }

        if (commandField.getText().trim().isEmpty()) {
            validations.add(new ValidationInfo("Command is required", commandField));
        }

        return validations;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        List<ValidationInfo> validations = doValidateAll();
        return validations.isEmpty() ? null : validations.get(0);
    }

    /**
     * Creates a ShellCommandConfig from the form values.
     *
     * @return A new ShellCommandConfig with form values
     */
    @NotNull
    public ShellCommandConfig getConfig() {
        ShellCommandConfig config = existingConfig != null ? existingConfig : new ShellCommandConfig();

        config.setTitle(titleField.getText().trim());
        config.setCommand(commandField.getText().trim());
        config.setWorkingDir(workingDirField.getText().trim().isEmpty() ? null : workingDirField.getText().trim());
        config.setIcon(iconField.getText().trim().isEmpty() ? "💻" : iconField.getText().trim());
        config.setEnabled(enabledCheckBox.isSelected());

        return config;
    }

    /**
     * Sets the config to edit (for external use).
     */
    public void setConfig(@NotNull ShellCommandConfig config) {
        this.existingConfig = config;
        populateFromConfig(config);
    }
}
