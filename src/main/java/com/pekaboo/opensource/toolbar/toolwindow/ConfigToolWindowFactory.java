package com.pekaboo.opensource.toolbar.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.CommandExecutor;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tool Window Factory for Shell Command Configuration.
 * Provides a rich, interactive UI for managing shell commands.
 */
public class ConfigToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ConfigToolWindowPanel panel = new ConfigToolWindowPanel(project);
        com.intellij.ui.ContentFactory contentFactory = com.intellij.ui.ContentFactory.getInstance();
        com.intellij.ui.Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * Main panel for the configuration tool window.
     */
    public static class ConfigToolWindowPanel extends JPanel {

        private final Project project;
        private final ToolbarConfigService configService;
        private final CommandExecutor commandExecutor;

        private SearchTextField searchField;
        private JBList<ShellCommandConfig> configList;
        private DefaultListModel<ShellCommandConfig> listModel;
        private JLabel statusLabel;

        public ConfigToolWindowPanel(@NotNull Project project) {
            this.project = project;
            Application application = ApplicationManager.getApplication();
            this.configService = application.getService(ToolbarConfigService.class);
            this.commandExecutor = application.getService(CommandExecutor.class);

            initUI();
            loadConfigs();
        }

        private void initUI() {
            setLayout(new BorderLayout(0, 5));
            setBorder(JBUI.Borders.empty(10));

            // Top: Search field
            searchField = new SearchTextField();
            searchField.addDocumentListener(new com.intellij.openapi.editor.event.DocumentListener() {
                @Override
                public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent e) {
                    filterConfigs(searchField.getText());
                }
            });
            searchField.setEmptyText("Search commands...");

            JPanel searchPanel = JBUI.Panels.simplePanel(searchField)
                    .withBorder(JBUI.Borders.empty(0, 0, 8, 0));
            add(searchPanel, BorderLayout.NORTH);

            // Center: Config list with custom renderer
            listModel = new DefaultListModel<>();
            configList = new JBList<>(listModel);
            configList.setCellRenderer(new ConfigListCellRenderer());
            configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            configList.setVisibleRowCount(10);

            // Double-click to execute
            configList.addListSelectionListener(e -> updateStatus());
            configList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && !e.isConsumed()) {
                        ShellCommandConfig selected = configList.getSelectedValue();
                        if (selected != null) {
                            executeCommand(selected);
                        }
                    }
                }
            });

            // Right-click context menu
            configList.addMouseListener(new PopupMenuAdapter() {
                @Override
                public void invokePopup(Component component, int x, int y) {
                    showContextMenu(component, x, y);
                }
            });

            JBScrollPane scrollPane = new JBScrollPane(configList);
            scrollPane.setBorder(JBUI.Borders.compound(
                    JBUI.Borders.customLine(UIUtil.getBorderColor(), 1),
                    JBUI.Borders.empty(5)
            ));
            add(scrollPane, BorderLayout.CENTER);

            // Bottom: Action toolbar and status
            JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

            // Action buttons
            DefaultActionGroup actionGroup = new DefaultActionGroup();
            actionGroup.add(new RunAction());
            actionGroup.addSeparator();
            actionGroup.add(new AddAction());
            actionGroup.add(new EditAction());
            actionGroup.add(new DeleteAction());
            actionGroup.addSeparator();
            actionGroup.add(new MoveUpAction());
            actionGroup.add(new MoveDownAction());

            ActionToolbar actionToolbar = ActionManager.getInstance()
                    .createActionToolbar("ShellCommandToolbar", actionGroup, false);
            actionToolbar.setTargetComponent(this);
            actionToolbar.setOrientation(ActionToolbar.HORIZONTAL);
            actionToolbar.setReservePlaceAutoPopupIcon(false);

            JPanel toolbarPanel = JBUI.Panels.simplePanel()
                    .addToCenter(actionToolbar.getComponent())
                    .withBorder(JBUI.Borders.empty(8, 0, 0, 0));

            // Status label
            statusLabel = new JLabel();
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
            statusLabel.setBorder(JBUI.Borders.empty(8, 5, 0, 5));

            bottomPanel.add(toolbarPanel, BorderLayout.CENTER);
            bottomPanel.add(statusLabel, BorderLayout.SOUTH);

            add(bottomPanel, BorderLayout.SOUTH);

            // Keyboard shortcuts
            registerKeyboardActions();
        }

        private void registerKeyboardActions() {
            InputMap inputMap = configList.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap actionMap = configList.getActionMap();

            // Enter to run
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "run");
            actionMap.put("run", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ShellCommandConfig selected = configList.getSelectedValue();
                    if (selected != null) {
                        executeCommand(selected);
                    }
                }
            });

            // Delete key
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
            actionMap.put("delete", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteSelectedConfig();
                }
            });

            // Escape to clear search
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
            actionMap.put("clearSearch", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchField.setText("");
                    searchField.requestFocus();
                }
            });
        }

        private void loadConfigs() {
            listModel.clear();
            List<ShellCommandConfig> configs = configService.getConfigs();
            for (ShellCommandConfig config : configs) {
                listModel.addElement(config);
            }
            updateStatus();
        }

        private void filterConfigs(String searchText) {
            listModel.clear();
            String lowerSearch = searchText.toLowerCase();

            for (ShellCommandConfig config : configService.getConfigs()) {
                String title = config.getTitle() != null ? config.getTitle().toLowerCase() : "";
                String command = config.getCommand() != null ? config.getCommand().toLowerCase() : "";

                if (title.contains(lowerSearch) || command.contains(lowerSearch)) {
                    listModel.addElement(config);
                }
            }
            updateStatus();
        }

        private void updateStatus() {
            int totalCount = configService.getConfigCount();
            int visibleCount = listModel.size();
            int selectedIndex = configList.getSelectedIndex();

            String text = String.format("%d of %d commands", visibleCount, totalCount);
            if (selectedIndex >= 0) {
                ShellCommandConfig selected = listModel.getElementAt(selectedIndex);
                text += " | Selected: " + (selected != null ? selected.getTitle() : "");
            }
            statusLabel.setText(text);
        }

        private void executeCommand(@NotNull ShellCommandConfig config) {
            commandExecutor.executeCommand(config, project);
        }

        private void showContextMenu(Component component, int x, int y) {
            ShellCommandConfig selected = configList.getSelectedValue();
            if (selected == null) {
                return;
            }

            DefaultActionGroup popupGroup = new DefaultActionGroup();
            popupGroup.add(new RunAction());
            popupGroup.addSeparator();
            popupGroup.add(new EditAction());
            popupGroup.add(new DeleteAction());
            popupGroup.addSeparator();
            popupGroup.add(new ToggleEnabledAction(selected));

            ActionPopupMenu popupMenu = ActionManager.getInstance()
                    .createActionPopupMenu("ShellCommandContextMenu", popupGroup);
            popupMenu.getComponent().show(component, x, y);
        }

        private void addNewConfig() {
            ConfigEditorDialog dialog = new ConfigEditorDialog(project, null);
            if (dialog.showAndGet()) {
                ShellCommandConfig newConfig = dialog.getConfig();
                configService.addConfig(newConfig);
                refreshList();
            }
        }

        private void editSelectedConfig() {
            ShellCommandConfig selected = configList.getSelectedValue();
            if (selected == null) {
                Messages.showWarningDialog(project, "Please select a command to edit.", "No Selection");
                return;
            }

            // Create a copy for editing
            ShellCommandConfig copy = new ShellCommandConfig(
                    selected.getId(),
                    selected.getTitle(),
                    selected.getCommand(),
                    selected.getWorkingDir(),
                    selected.getIcon(),
                    selected.isEnabled()
            );

            ConfigEditorDialog dialog = new ConfigEditorDialog(project, copy);
            if (dialog.showAndGet()) {
                configService.updateConfig(dialog.getConfig());
                refreshList();
            }
        }

        private void deleteSelectedConfig() {
            ShellCommandConfig selected = configList.getSelectedValue();
            if (selected == null) {
                Messages.showWarningDialog(project, "Please select a command to delete.", "No Selection");
                return;
            }

            String title = selected.getTitle() != null ? selected.getTitle() : "this command";
            int result = Messages.showYesNoDialog(
                    project,
                    "Are you sure you want to delete '" + title + "'?",
                    "Confirm Delete",
                    "Delete",
                    "Cancel",
                    AllIcons.General.QuestionDialog
            );

            if (result == Messages.YES) {
                configService.removeConfig(selected.getId());
                refreshList();
            }
        }

        private void moveSelectionUp() {
            int index = configList.getSelectedIndex();
            if (index > 0) {
                // Swap in the actual service data
                List<ShellCommandConfig> configs = new ArrayList<>(configService.getConfigs());
                ShellCommandConfig temp = configs.get(index);
                configs.set(index, configs.get(index - 1));
                configs.set(index - 1, temp);

                // Update service
                configService.clearAllConfigs();
                for (ShellCommandConfig config : configs) {
                    configService.addConfig(config);
                }

                refreshList();
                configList.setSelectedIndex(index - 1);
            }
        }

        private void moveSelectionDown() {
            int index = configList.getSelectedIndex();
            List<ShellCommandConfig> configs = configService.getConfigs();
            if (index >= 0 && index < configs.size() - 1) {
                // Swap in the actual service data
                List<ShellCommandConfig> configList = new ArrayList<>(configs);
                ShellCommandConfig temp = configList.get(index);
                configList.set(index, configList.get(index + 1));
                configList.set(index + 1, temp);

                // Update service
                configService.clearAllConfigs();
                for (ShellCommandConfig config : configList) {
                    configService.addConfig(config);
                }

                refreshList();
                configList.setSelectedIndex(index + 1);
            }
        }

        private void toggleEnabled(ShellCommandConfig config) {
            config.setEnabled(!config.isEnabled());
            configService.updateConfig(config);
            refreshList();
        }

        private void refreshList() {
            String searchText = searchField.getText();
            if (searchText.isEmpty()) {
                loadConfigs();
            } else {
                filterConfigs(searchText);
            }
        }

        // ==================== Actions ====================

        private class RunAction extends AnAction {
            RunAction() {
                super("Run", "Execute the selected command", AllIcons.Actions.Execute);
                registerCustomShortcutSet(CommonShortcuts.getRun(), configList);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ShellCommandConfig selected = configList.getSelectedValue();
                if (selected != null) {
                    executeCommand(selected);
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(configList.getSelectedValue() != null);
            }
        }

        private class AddAction extends AnAction {
            AddAction() {
                super("Add", "Add a new command", AllIcons.General.Add);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                addNewConfig();
            }
        }

        private class EditAction extends AnAction {
            EditAction() {
                super("Edit", "Edit the selected command", AllIcons.Actions.Edit);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                editSelectedConfig();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(configList.getSelectedValue() != null);
            }
        }

        private class DeleteAction extends AnAction {
            DeleteAction() {
                super("Delete", "Delete the selected command", AllIcons.General.Remove);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteSelectedConfig();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(configList.getSelectedValue() != null);
            }
        }

        private class MoveUpAction extends AnAction {
            MoveUpAction() {
                super("Move Up", "Move the selected command up", AllIcons.Actions.MoveUp);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                moveSelectionUp();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                int index = configList.getSelectedIndex();
                e.getPresentation().setEnabled(index > 0);
            }
        }

        private class MoveDownAction extends AnAction {
            MoveDownAction() {
                super("Move Down", "Move the selected command down", AllIcons.Actions.MoveDown);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                moveSelectionDown();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                int index = configList.getSelectedIndex();
                e.getPresentation().setEnabled(index >= 0 && index < listModel.getSize() - 1);
            }
        }

        private class ToggleEnabledAction extends ToggleAction {
            private final ShellCommandConfig config;

            ToggleEnabledAction(ShellCommandConfig config) {
                super(config.isEnabled() ? "Disable" : "Enable",
                        config.isEnabled() ? "Disable this command" : "Enable this command",
                        config.isEnabled() ? AllIcons.Actions.Hide : AllIcons.Actions.Show);
                this.config = config;
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return config.isEnabled();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                config.setEnabled(state);
                configService.updateConfig(config);
                refreshList();
            }
        }
    }

    /**
     * Custom cell renderer for the command list.
     * Renders each item with icon, title, command, and working directory.
     */
    private static class ConfigListCellRenderer extends JPanel implements ListCellRenderer<ShellCommandConfig> {

        private final JLabel iconLabel;
        private final JLabel titleLabel;
        private final JLabel commandLabel;
        private final JLabel dirLabel;
        private final JPanel contentPanel;

        ConfigListCellRenderer() {
            super(new BorderLayout(5, 0));
            setOpaque(true);
            setBorder(JBUI.Borders.empty(8, 10));

            // Icon panel (left)
            iconLabel = new JLabel();
            iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 20f));
            iconLabel.setPreferredSize(new Dimension(40, 40));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel iconPanel = JBUI.Panels.simplePanel(iconLabel)
                    .withBorder(JBUI.Borders.empty(0, 5, 0, 0));

            // Content panel (center)
            contentPanel = new JPanel(new BorderLayout(0, 2));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(JBUI.Borders.empty(2, 5));

            titleLabel = new JLabel();
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));

            commandLabel = new JLabel();
            commandLabel.setFont(commandLabel.getFont().deriveFont(Font.PLAIN, 11f));

            JPanel textPanel = JBUI.Panels.simplePanel()
                    .addToTop(titleLabel)
                    .addToBottom(commandLabel);

            contentPanel.add(textPanel, BorderLayout.CENTER);

            // Working directory (right)
            dirLabel = new JLabel();
            dirLabel.setFont(dirLabel.getFont().deriveFont(Font.PLAIN, 10f));
            dirLabel.setForeground(UIUtil.getInactiveTextColor());
            dirLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 0));

            add(iconPanel, BorderLayout.WEST);
            add(contentPanel, BorderLayout.CENTER);
            add(dirLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ShellCommandConfig> list,
                                                      ShellCommandConfig value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            // Set background for alternate rows
            if (isSelected) {
                setBackground(UIUtil.getListSelectionBackground());
                setForeground(UIUtil.getListSelectionForeground());
                contentPanel.setForeground(UIUtil.getListSelectionForeground());
            } else {
                setBackground(UIUtil.getDecoratedRowColor() % 2 == index % 2
                        ? UIUtil.getListBackground()
                        : UIUtil.getDecoratedRowColor());
                setForeground(UIUtil.getListForeground());
                contentPanel.setForeground(UIUtil.getListForeground());
            }

            // Set content
            if (value != null) {
                iconLabel.setText(value.getIcon() != null ? value.getIcon() : "💻");
                titleLabel.setText(value.getTitle() != null ? value.getTitle() : "Unnamed Command");

                String command = value.getCommand() != null ? value.getCommand() : "";
                commandLabel.setText(command.isEmpty() ? "No command" : command);
                commandLabel.setForeground(isSelected
                        ? UIUtil.getListSelectionForeground()
                        : SimpleTextAttributes.GRAY_ATTRIBUTES.getFgColor());

                String workingDir = value.getWorkingDir();
                if (workingDir != null && !workingDir.isEmpty()) {
                    dirLabel.setText(ellipsis(workingDir, 30));
                    dirLabel.setVisible(true);
                } else {
                    dirLabel.setText("");
                    dirLabel.setVisible(false);
                }

                // Dim disabled items
                if (!value.isEnabled() && !isSelected) {
                    titleLabel.setForeground(UIUtil.getInactiveTextColor());
                    iconLabel.setEnabled(false);
                } else {
                    iconLabel.setEnabled(true);
                }
            } else {
                iconLabel.setText("");
                titleLabel.setText("");
                commandLabel.setText("");
                dirLabel.setText("");
            }

            return this;
        }

        private String ellipsis(String str, int maxLen) {
            if (str.length() <= maxLen) return str;
            return "..." + str.substring(str.length() - maxLen + 3);
        }
    }

    /**
     * Dialog for adding or editing command configurations.
     */
    private static class ConfigEditorDialog extends DialogWrapper {

        private final ShellCommandConfig originalConfig;
        private ShellCommandConfig resultConfig;

        private JBTextField titleField;
        private JBTextField commandField;
        private JBTextField workingDirField;
        private JBTextField iconField;
        private JBCheckBox enabledCheckBox;

        public ConfigEditorDialog(@Nullable Project project, @Nullable ShellCommandConfig config) {
            super(project, config == null);
            this.originalConfig = config;
            setTitle(config == null ? "Add New Command" : "Edit Command");
            setOKButtonText(config == null ? "Add" : "Save");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(JBUI.Borders.empty(10));
            panel.setPreferredSize(new Dimension(500, 300));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = JBUI.insets(5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;

            // Title
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel("Title:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            titleField = new JBTextField();
            titleField.setPreferredSize(new Dimension(300, 30));
            panel.add(titleField, gbc);

            // Command
            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel("Command:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            commandField = new JBTextField();
            commandField.setPreferredSize(new Dimension(300, 30));
            panel.add(commandField, gbc);

            // Variable insertion buttons
            row++;
            gbc.gridx = 1;
            gbc.gridy = row;
            JPanel varPanel = createVariableButtonsPanel();
            panel.add(varPanel, gbc);

            // Working Directory
            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel("Working Dir:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
            workingDirField = new JBTextField();
            workingDirField.setPreferredSize(new Dimension(250, 30));
            dirPanel.add(workingDirField, BorderLayout.CENTER);

            JButton browseButton = new JButton("Browse...");
            browseButton.addActionListener(e -> browseDirectory());
            dirPanel.add(browseButton, BorderLayout.EAST);
            panel.add(dirPanel, gbc);

            // Icon
            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel("Icon:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            JPanel iconPanel = new JPanel(new BorderLayout(5, 0));
            iconField = new JBTextField();
            iconField.setPreferredSize(new Dimension(100, 30));
            iconField.setMaximumSize(new Dimension(100, 30));
            iconPanel.add(iconField, BorderLayout.WEST);
            iconPanel.add(createEmojiPalette(), BorderLayout.CENTER);
            panel.add(iconPanel, gbc);

            // Enabled checkbox
            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 2;
            enabledCheckBox = new JBCheckBox("Enabled");
            panel.add(enabledCheckBox, gbc);

            // Load existing values
            if (originalConfig != null) {
                titleField.setText(originalConfig.getTitle());
                commandField.setText(originalConfig.getCommand());
                workingDirField.setText(originalConfig.getWorkingDir());
                iconField.setText(originalConfig.getIcon());
                enabledCheckBox.setSelected(originalConfig.isEnabled());
            } else {
                iconField.setText("💻");
                enabledCheckBox.setSelected(true);
            }

            return panel;
        }

        private JPanel createVariableButtonsPanel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            panel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));

            String[] variables = {"{{rootPath}}", "$HOME", "{{workspaceFolder}}", "$(pwd)", "$USER"};
            for (String var : variables) {
                JButton button = new JButton(var);
                button.setFont(button.getFont().deriveFont(10f));
                button.setPreferredSize(new Dimension(button.getPreferredSize().width + 10, 24));
                button.addActionListener(e -> {
                    String currentText = commandField.getText();
                    int pos = commandField.getCaretPosition();
                    String newText = currentText.substring(0, pos) + var + currentText.substring(pos);
                    commandField.setText(newText);
                    commandField.setCaretPosition(pos + var.length());
                    commandField.requestFocus();
                });
                panel.add(button);
            }

            return panel;
        }

        private JPanel createEmojiPalette() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
            panel.setBorder(JBUI.Borders.empty(0, 0, 5, 0));

            String[] emojis = {"💻", "🚀", "⚙️", "🔧", "📝", "🔍", "🌐", "📦",
                    "🎯", "✅", "❌", "⚡", "🔥", "💡", "🔔", "📌"};

            for (String emoji : emojis) {
                JButton button = new JButton(emoji);
                button.setPreferredSize(new Dimension(32, 28));
                button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                button.setFocusable(false);
                button.addActionListener(e -> iconField.setText(emoji));
                panel.add(button);
            }

            return panel;
        }

        private void browseDirectory() {
            String projectPath = project != null ? project.getBasePath() : System.getProperty("user.home");
            File startingDir = new File(projectPath);

            FileChooserFactory fileChooserFactory = FileChooserFactory.getInstance();
            VirtualFile baseDir = VirtualFileManager.getInstance().findFileByUrl("file://" + startingDir.getAbsolutePath());

            if (baseDir != null) {
                VirtualFile selected = fileChooserFactory.createFileChooser(
                        new FileSaverDescriptor("Select Working Directory", "Choose the working directory for this command"),
                        project,
                        baseDir
                ).save(null, baseDir, "");

                if (selected != null) {
                    workingDirField.setText(selected.getPath());
                }
            }
        }

        @Override
        protected void doOKAction() {
            if (validateInput()) {
                if (originalConfig == null) {
                    resultConfig = new ShellCommandConfig();
                    resultConfig.setId(UUID.randomUUID().toString());
                } else {
                    resultConfig = originalConfig;
                }

                resultConfig.setTitle(titleField.getText().trim());
                resultConfig.setCommand(commandField.getText().trim());
                resultConfig.setWorkingDir(workingDirField.getText().trim());
                resultConfig.setIcon(iconField.getText().trim());
                resultConfig.setEnabled(enabledCheckBox.isSelected());

                super.doOKAction();
            }
        }

        private boolean validateInput() {
            if (titleField.getText().trim().isEmpty()) {
                Messages.showErrorDialog(project, "Please enter a title for the command.", "Validation Error");
                return false;
            }
            if (commandField.getText().trim().isEmpty()) {
                Messages.showErrorDialog(project, "Please enter a command to execute.", "Validation Error");
                return false;
            }
            return true;
        }

        public ShellCommandConfig getConfig() {
            return resultConfig;
        }
    }

    /**
     * Abstract adapter for popup menu handling.
     */
    private abstract static class PopupMenuAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e.getComponent(), e.getX(), e.getY());
            }
        }

        public abstract void invokePopup(Component component, int x, int y);

        private void showPopup(Component component, int x, int y) {
            invokePopup(component, x, y);
        }
    }
}
