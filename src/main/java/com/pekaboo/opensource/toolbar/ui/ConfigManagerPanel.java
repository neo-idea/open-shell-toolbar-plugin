package com.pekaboo.opensource.toolbar.ui;

import com.intellij.openapi.Disposable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTable;
import com.intellij.util.ui.JBUI;
import com.pekaboo.opensource.toolbar.model.ShellCommandConfig;
import com.pekaboo.opensource.toolbar.service.ToolbarConfigService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Panel for managing shell command configurations.
 * Provides a table view with add/edit/delete/move operations and import/export functionality.
 */
public class ConfigManagerPanel implements Disposable {

    private final Project project;
    private final ToolbarConfigService configService;
    private JPanel mainPanel;
    private JBTable configTable;
    private ConfigTableModel tableModel;
    private final List<ShellCommandConfig> configs = new ArrayList<>();

    // Column indices
    private static final int COL_ICON = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_COMMAND = 2;
    private static final int COL_WORKING_DIR = 3;
    private static final int COL_ENABLED = 4;

    public ConfigManagerPanel(@NotNull Project project) {
        this.project = project;
        this.configService = ToolbarConfigService.getInstance();
        loadConfigs();
        initializeUI();
    }

    /**
     * Loads configurations from the service.
     */
    private void loadConfigs() {
        configs.clear();
        configs.addAll(configService.getConfigs());
    }

    /**
     * Reloads configurations from the service and refreshes the table.
     */
    public void refresh() {
        loadConfigs();
        if (tableModel != null) {
            tableModel.fireTableDataChanged();
        }
    }

    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        tableModel = new ConfigTableModel();
        configTable = new JBTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // Set row height for better emoji visibility
                setRowHeight(30);
                return c;
            }
        };

        // Configure table appearance
        configTable.setRowHeight(30);
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configTable.setShowGrid(false);
        configTable.setIntercellSpacing(new Dimension(0, 0));
        configTable.getTableHeader().setReorderingAllowed(false);

        // Configure column widths
        configureTableColumns();

        // Add double-click listener for editing
        configTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedConfig();
                }
            }
        });

        // Create toolbar decorator
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(configTable)
                .setAddAction(this::addConfig)
                .setEditAction(this::editSelectedConfig)
                .setRemoveAction(this::removeSelectedConfig)
                .setMoveUpAction(this::moveConfigUp)
                .setMoveDownAction(this::moveConfigDown)
                .addExtraAction(new ImportAction())
                .addExtraAction(new ExportAction());

        // Add border to toolbar
        decorator.setToolbarBorder(JBUI.Borders.customLine(new JBColor.Gray(200), 0, 0, 1, 0));

        // Create scroll pane
        JBScrollPane scrollPane = new JBScrollPane(configTable);
        scrollPane.setBorder(JBUI.Borders.empty());

        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(decorator.createPanel(), BorderLayout.NORTH);
        mainPanel.setPreferredSize(JBUI.size(600, 400));
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // Add selection listener
        configTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    // Selection changed - can be used for context-sensitive actions
                }
            }
        });
    }

    /**
     * Configures table column widths.
     */
    private void configureTableColumns() {
        TableColumnModel columnModel = configTable.getColumnModel();

        // Icon column - narrow
        columnModel.getColumn(COL_ICON).setPreferredWidth(50);
        columnModel.getColumn(COL_ICON).setMaxWidth(60);

        // Title column - wider
        columnModel.getColumn(COL_TITLE).setPreferredWidth(150);

        // Command column - widest
        columnModel.getColumn(COL_COMMAND).setPreferredWidth(200);

        // Working Dir column - medium
        columnModel.getColumn(COL_WORKING_DIR).setPreferredWidth(150);

        // Enabled column - narrow
        columnModel.getColumn(COL_ENABLED).setPreferredWidth(60);
        columnModel.getColumn(COL_ENABLED).setMaxWidth(70);
    }

    /**
     * Adds a new configuration.
     */
    private void addConfig(ActionEvent e) {
        AddEditConfigDialog dialog = new AddEditConfigDialog(project);
        if (dialog.showAndGet()) {
            ShellCommandConfig newConfig = dialog.getConfig();
            configs.add(newConfig);
            configService.addConfig(newConfig);
            tableModel.fireTableRowsInserted(configs.size() - 1, configs.size() - 1);
        }
    }

    /**
     * Edits the selected configuration.
     */
    private void editSelectedConfig(ActionEvent e) {
        editSelectedConfig();
    }

    /**
     * Edits the selected configuration.
     */
    private void editSelectedConfig() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        ShellCommandConfig selectedConfig = configs.get(selectedRow);
        AddEditConfigDialog dialog = new AddEditConfigDialog(project, selectedConfig);
        if (dialog.showAndGet()) {
            ShellCommandConfig updatedConfig = dialog.getConfig();
            configs.set(selectedRow, updatedConfig);
            configService.updateConfig(updatedConfig);
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
        }
    }

    /**
     * Removes the selected configuration.
     */
    private void removeSelectedConfig(ActionEvent e) {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        ShellCommandConfig selectedConfig = configs.get(selectedRow);
        String title = selectedConfig.getTitle() != null ? selectedConfig.getTitle() : "this command";

        int result = Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete \"" + title + "\"?",
                "Confirm Delete",
                Messages.getQuestionIcon()
        );

        if (result == Messages.YES) {
            configs.remove(selectedRow);
            configService.removeConfig(selectedConfig.getId());
            tableModel.fireTableRowsDeleted(selectedRow, selectedRow);

            // Select the row after the deleted one if available
            if (configs.size() > 0) {
                int newSelection = Math.min(selectedRow, configs.size() - 1);
                configTable.setRowSelectionInterval(newSelection, newSelection);
            }
        }
    }

    /**
     * Moves the selected config up.
     */
    private void moveConfigUp(ActionEvent e) {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow <= 0) {
            return;
        }

        // Swap in the list
        ShellCommandConfig temp = configs.get(selectedRow);
        configs.set(selectedRow, configs.get(selectedRow - 1));
        configs.set(selectedRow - 1, temp);

        // Update service - need to replace all since order changed
        updateAllConfigs();

        tableModel.fireTableRowsUpdated(selectedRow - 1, selectedRow);
        configTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
    }

    /**
     * Moves the selected config down.
     */
    private void moveConfigDown(ActionEvent e) {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= configs.size() - 1) {
            return;
        }

        // Swap in the list
        ShellCommandConfig temp = configs.get(selectedRow);
        configs.set(selectedRow, configs.get(selectedRow + 1));
        configs.set(selectedRow + 1, temp);

        // Update service - need to replace all since order changed
        updateAllConfigs();

        tableModel.fireTableRowsUpdated(selectedRow, selectedRow + 1);
        configTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
    }

    /**
     * Updates all configurations in the service (used for reorder operations).
     */
    private void updateAllConfigs() {
        // Clear and re-add all configs to update the service
        List<ShellCommandConfig> currentConfigs = new ArrayList<>(configs);
        configService.clearAllConfigs();
        for (ShellCommandConfig config : currentConfigs) {
            configService.addConfig(config);
        }
    }

    /**
     * Returns the main panel.
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * Checks if configurations have been modified.
     */
    public boolean isModified() {
        // For simplicity, we assume modifications occur when actions are performed
        // In a more complex implementation, you'd track the original state
        return false;
    }

    /**
     * Applies changes (already applied immediately in this implementation).
     */
    public void apply() {
        // Changes are applied immediately, but we can reload to ensure consistency
        refresh();
    }

    /**
     * Resets to the service state.
     */
    public void reset() {
        refresh();
    }

    @Override
    public void dispose() {
        // Cleanup if needed
    }

    /**
     * Table model for configurations.
     */
    private class ConfigTableModel extends AbstractTableModel {

        private final String[] COLUMN_NAMES = {"Icon", "Title", "Command", "Working Dir", "Enabled"};

        @Override
        public int getRowCount() {
            return configs.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == COL_ENABLED) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= configs.size()) {
                return null;
            }

            ShellCommandConfig config = configs.get(rowIndex);

            return switch (columnIndex) {
                case COL_ICON -> config.getIcon() != null ? config.getIcon() : "💻";
                case COL_TITLE -> config.getTitle() != null ? config.getTitle() : "";
                case COL_COMMAND -> config.getCommand() != null ? config.getCommand() : "";
                case COL_WORKING_DIR -> config.getWorkingDir() != null ? config.getWorkingDir() : "";
                case COL_ENABLED -> config.isEnabled();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= configs.size()) {
                return;
            }

            ShellCommandConfig config = configs.get(rowIndex);

            if (columnIndex == COL_ENABLED && value instanceof Boolean) {
                config.setEnabled((Boolean) value);
                configService.updateConfig(config);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COL_ENABLED;
        }
    }

    /**
     * Action for importing configurations from a JSON file.
     */
    private class ImportAction extends AnAction {
        public ImportAction() {
            super("Import", "Import configurations from JSON", AllIcons.Actions.Download);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(
                    true,   // choose files
                    false,  // choose folders
                    false,  // choose jars
                    false,  // choose jars as files
                    false,  // choose jar contents
                    false   // choose multiple
            );
            descriptor.setTitle("Import Configurations");
            descriptor.setDescription("Select a JSON file to import configurations from");

            VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
            if (file != null) {
                String filePath = file.getCanonicalPath();
                if (filePath != null && configService.importFromJson(filePath)) {
                    Messages.showInfoMessage(
                            project,
                            "Successfully imported configurations from " + file.getName(),
                            "Import Successful"
                    );
                    refresh();
                } else {
                    Messages.showErrorDialog(
                            project,
                            "Failed to import configurations. Please check the file format.",
                            "Import Failed"
                    );
                }
            }
        }
    }

    /**
     * Action for exporting configurations to a JSON file.
     */
    private class ExportAction extends AnAction {
        public ExportAction() {
            super("Export", "Export configurations to JSON", AllIcons.Actions.Upload);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(
                    false,  // choose files
                    true,   // choose folders
                    false,  // choose jars
                    false,  // choose jars as files
                    false,  // choose jar contents
                    false   // choose multiple
            );
            descriptor.setTitle("Export Configurations");
            descriptor.setDescription("Select a location to save the configurations");

            VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
            if (file != null) {
                String filePath = file.getCanonicalPath();
                if (filePath != null) {
                    if (!filePath.endsWith(".json")) {
                        filePath += "/shell-toolbar-config.json";
                    }

                    if (configService.exportToJson(filePath)) {
                        Messages.showInfoMessage(
                                project,
                                "Successfully exported configurations to " + new File(filePath).getName(),
                                "Export Successful"
                        );
                    } else {
                        Messages.showErrorDialog(
                                project,
                                "Failed to export configurations. Please check the file path.",
                                "Export Failed"
                        );
                    }
                }
            }
        }
    }
}
