package sqlrunner.flatfileimport.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.Logger;

import sqlrunner.Database;
import sqlrunner.LongRunningAction;
import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.CSVFieldTokenizer;
import sqlrunner.flatfileimport.CSVFileDatasetProvider;
import sqlrunner.flatfileimport.DatasetProvider;
import sqlrunner.flatfileimport.FieldDescription;
import sqlrunner.flatfileimport.FieldTokenizer;
import sqlrunner.flatfileimport.FileImporter;
import sqlrunner.flatfileimport.ImportAttributes;
import sqlrunner.flatfileimport.ParserException;
import sqlrunner.resources.ApplicationIcons;
import sqlrunner.swinghelper.WindowHelper;
import sqlrunner.talend.SchemaUtil;

public final class ImportConfiguratorFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(ImportConfiguratorFrame.class);
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JTabbedPane jTabbedPane = null;
    private JButton jButtonStartImport = null;
    private JButton jButtonCancelImport = null;
    private JTextField jTextFieldSourceFileName = null;
    private JTextField jTextFieldSheetName = null;
    private JButton jButtonChooseSourceFile = null;
    private JCheckBox jCheckBoxIgnoreFirstLine = null;
    private JTextField jTextFieldSkipRows = null;
    private JComboBox jComboBoxCharSet = null;
    private JButton jButtonCountLines = null;
    private JTextField jTextFieldCountLines = null;
    private JCheckBox jCheckBoxEnablePreProcessing = null;
    private JComboBox jComboBoxAddPreSQLParameter = null;
    private JTextArea jTextAreaPostProcessingSQL = null;
    private JComboBox jComboBoxAddPostSQLParameter = null;
    private JCheckBox jCheckBoxEnablePostProcessing = null;
    private JTextArea jTextAreaPreProcessingSQL = null;
    private DelimiterConfigPanel delimiterConfigPanel = null;
    private JTextField jTextFieldTableName = null;
    private JTextField jTextFieldBatchSize = null;
    private JButton jButtonCreateNewFieldDescriptions = null;
    private JButton jButtonEditFieldDescription = null;
    private JButton jButtonAddFieldDescription = null;
    private JButton jButtonDeleteFieldDescription = null;
    private JButton jButtonDeleteAllFieldDescriptions = null;
    private JButton jButtonTakeOverHeaderAsFieldName = null;
    private JCheckBox jCheckBoxDeleteAllBefore = null;
    private JCheckBox jCheckBoxInsertEnabled = null;
    private JCheckBox jCheckBoxUpdateEnabled = null;
    private JButton jButtonTestForwards = null;
    private JButton jButtonTestBackwards = null;
    private JTextField jTextFieldCurrentRowNum = null;
    private JLabel jLabelCurrentCountColumns = null;
    private JButton jButtonMoveFieldDescUp = null;
    private JButton jButtonMoveFieldDescDown = null;
    private JTable jTableFieldDescriptions = null;
    private transient DescriptionTableModel descriptionTableModel = null; // @jve:decl-index=0:visual-constraint="5,618"
    private ImportProgressPanel importProgressPanel = null;
    private final Vector<Long> filePointers = new Vector<Long>(); // @jve:decl-index=0:
    private transient Database database = null;
    private transient final FileImporter importer = new FileImporter(); // @jve:decl-index=0:
    private transient Thread counterThread = null;
    private JMenuBar importConfigMenuBar = null;
    private JMenu jMenuFile = null;
    private JMenuItem jMenuItemFileNew = null;
    private JMenuItem jMenuItemFileOpen = null;
    private JMenuItem jMenuItemFileSave = null;
    private JMenuItem jMenuItemFileSaveAs = null;
    private JMenuItem jMenuItemFileSaveAsTalendSchema = null;
    private JMenuItem jMenuItemClose = null;
    private String configFileName;
    private Properties lastConfiguration = new Properties();
    private JCheckBox jCheckBoxTestOnly = null;
    public static final String IMPORT_CONFIG_EXTENSION = ".importconfig";
    public static final String DATASOURCE_EXTENSION = ".csv";
    private File currentDataFile = null;
    private transient FieldTokenizer testFieldParser = null;
    private DatasetProvider testDatasetProvider = null;
    private JCheckBox jCheckBoxHandleAlwaysAsCSVFile;
    private JComboBox jComboBoxSortFieldDescriptions = null;
    
    /**
     * This is the default constructor
     */
    public ImportConfiguratorFrame() {
        initialize();
        lastConfiguration = buildProperties();
    }

    public ImportConfiguratorFrame(Database database) {
        this();
        this.database = database;
    }

    public ImportConfiguratorFrame(Database database, String tableName) {
        this(database);
        if (tableName != null && tableName.trim().length() > 0) {
            createImportDescriptionForTable(tableName);
            jCheckBoxInsertEnabled.setSelected(true);
            jTextFieldBatchSize.setEditable(true);
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent winEvent) {
        switch (winEvent.getID()) {
            case WindowEvent.WINDOW_CLOSING: {
                if (close()) {
                    super.processWindowEvent(winEvent);
                }
                break;
            }
            default:
                super.processWindowEvent(winEvent);
        }
    }
    
    private boolean close() {
        if (jButtonStartImport.isEnabled() == false) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    Messages.getString("ImportConfiguratorFrame.interruptQuestion"),
                    Messages.getString("ImportConfiguratorFrame.close"),
                    JOptionPane.YES_NO_OPTION);  //$NON-NLS-2$
            if (answer == JOptionPane.YES_OPTION) {
            	if (importer != null) {
                    importer.abort();
            	}
            	return true;
            } else {
                return false;
            }
        }
        if (checkSaveConfig()) {
            return true;
        }
        return false;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible == false) {
            if (testDatasetProvider != null) {
                testDatasetProvider.closeDatasetProvider();
            }
        }
        if (!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {
            }
        }
        super.setVisible(visible);
        if (visible) {
            WindowHelper.checkAndCorrectWindowBounds(this);
        }
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setJMenuBar(getImportConfigMenuBar());
        this.setContentPane(getJContentPane());
        this.setTitle(Messages.getString("ImportConfiguratorFrame.title"));
        pack();
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(createJPanelSourceFile(), BorderLayout.NORTH);
            jContentPane.add(getJTabbedPane(), BorderLayout.CENTER);
            jContentPane.add(createJPanelStartStopButtons(), BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes jTabbedPane
     * 
     * @return javax.swing.JTabbedPane
     */
    private JTabbedPane getJTabbedPane() {
        if (jTabbedPane == null) {
            jTabbedPane = new JTabbedPane();
            jTabbedPane.addTab(Messages.getString("ImportConfiguratorFrame.mapping"), null, createJPanelMapping(), null); 
            jTabbedPane.addTab(Messages.getString("ImportConfiguratorFrame.preprocesssql"), null, createJPanelPreProcessSQL(), null); 
            jTabbedPane.addTab(Messages.getString("ImportConfiguratorFrame.postprocesssql"), null, createJPanelPostProcessSQL(), null); 
            jTabbedPane.addTab(Messages.getString("ImportConfiguratorFrame.progress"), null, createJPanelProgress(), null); 
        }
        return jTabbedPane;
    }

    /**
     * This method initializes jPanelMapping
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelMapping() {
        JPanel jPanelMapping = new JPanel();
        jPanelMapping.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.0;
            jPanelMapping.add(getDelimiterConfigPanel(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            JLabel jLabel7 = new JLabel();
            jLabel7.setText(Messages.getString("ImportConfiguratorFrame.tablename")); 
            jPanelMapping.add(createJPanelTableConfig(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            gbc.weighty = 1.0;
            jPanelMapping.add(createJPanelFieldDescriptions(), gbc);
        }
        return jPanelMapping;
    }
    
    private JCheckBox getJCheckBoxHandleFilesAlwaysAsCSV() {
    	if (jCheckBoxHandleAlwaysAsCSVFile == null) {
    		jCheckBoxHandleAlwaysAsCSVFile = new JCheckBox();
    		jCheckBoxHandleAlwaysAsCSVFile.setText(Messages.getString("ImportConfiguratorFrame.alwaysCSV"));
    		jCheckBoxHandleAlwaysAsCSVFile.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					if ((testDatasetProvider instanceof CSVFileDatasetProvider) == false) {
						intitializeTestdataView();
					}
				}
				
			});
    	}
    	return jCheckBoxHandleAlwaysAsCSVFile;
    }

    /**
     * This method initializes jPanelSourceFile
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelSourceFile() {
        JPanel jPanelSourceFile = new JPanel();
        jPanelSourceFile.setLayout(new GridBagLayout());
        int y = 0;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = y;
            gbc.insets = new Insets(2, 5, 2, 2);
            gbc.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel();
            label.setText(Messages.getString("ImportConfiguratorFrame.sourcefilename")); 
            jPanelSourceFile.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = y;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            jPanelSourceFile.add(getJTextFieldSourceFileName(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = y;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelSourceFile.add(getJButtonChooseSourceFile(), gbc);
        }
        y++;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = y;
            gbc.insets = new Insets(2, 5, 2, 2);
            gbc.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel();
            label.setText(Messages.getString("ImportConfiguratorFrame.sheetName")); 
            jPanelSourceFile.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = y;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            jPanelSourceFile.add(getJTextFieldSheetName(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = y;
            gbc.insets = new Insets(2, 5, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel();
            label.setText(Messages.getString("ImportConfiguratorFrame.sheetNameHint")); 
            jPanelSourceFile.add(label, gbc);
        }
        y++;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = y;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 0);
            gbc.gridwidth = 2;
            JLabel label = new JLabel();
            label.setText(Messages.getString("ImportConfiguratorFrame.skipRows"));
            jPanelSourceFile.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = y;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.gridwidth = 1;
            jPanelSourceFile.add(getJTextFieldSkipRows(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = y;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.gridwidth = 1;
            jPanelSourceFile.add(getJCheckBoxIgnoreFirstLine(), gbc);
        }
        y++;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = y;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 5, 2, 2);
            JLabel jLabel = new JLabel();
            jLabel.setText(Messages.getString("ImportConfiguratorFrame.charset")); 
            jPanelSourceFile.add(jLabel, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = y;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelSourceFile.add(getJCheckBoxHandleFilesAlwaysAsCSV(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = y;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridwidth = 2;
            jPanelSourceFile.add(getJComboBoxCharSet(), gbc);
        }
        y++;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = y;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.EAST;
            jPanelSourceFile.add(getJButtonCountLines(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = y;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            jTextFieldCountLines = new JTextField();
            jTextFieldCountLines.setEditable(false);
            jTextFieldCountLines.setText("0"); 
            jPanelSourceFile.add(jTextFieldCountLines, gbc);
        }
        return jPanelSourceFile;
    }

    /**
     * This method initializes jPanelProgress
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelProgress() {
        JPanel jPanelProgress = new JPanel();
        jPanelProgress.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridy = 0;
            jPanelProgress.add(getImportProgressPanel(), gbc);
        }
        return jPanelProgress;
    }

    /**
     * This method initializes jPanelButtom
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelStartStopButtons() {
        JPanel jPanelButtom = new JPanel();
        jPanelButtom.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelButtom.add(getJButtonStartImport(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelButtom.add(getJCheckBoxTestOnly(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelButtom.add(getJButtonCancelImport(), gbc);
        }
        return jPanelButtom;
    }

    private JCheckBox getJCheckBoxTestOnly() {
        if (jCheckBoxTestOnly == null) {
            jCheckBoxTestOnly = new JCheckBox();
            jCheckBoxTestOnly.setText(Messages.getString("ImportConfiguratorFrame.testOnly"));
        }
        return jCheckBoxTestOnly;
    }

    /**
     * This method initializes jButtonStartImport
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonStartImport() {
        if (jButtonStartImport == null) {
            jButtonStartImport = new JButton();
            jButtonStartImport.setText(Messages.getString("ImportConfiguratorFrame.startimport")); 
            jButtonStartImport.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    buttonStart_actionPerformed();
                }
            });
        }
        return jButtonStartImport;
    }

    /**
     * This method initializes jButtonCancelImport
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonCancelImport() {
        if (jButtonCancelImport == null) {
            jButtonCancelImport = new JButton();
            jButtonCancelImport.setEnabled(false);
            jButtonCancelImport.setText(Messages.getString("ImportConfiguratorFrame.cancelimport")); 
            jButtonCancelImport.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (importer != null) {
                        importer.interrupt();
                        jButtonCancelImport.setEnabled(false);
                        jButtonStartImport.setEnabled(true);
                    }
                }
            });
        }
        return jButtonCancelImport;
    }
    
    private void buttonUp_actionPerformed() {
        final int row = jTableFieldDescriptions.getSelectedRow();
        if (row > 0) {
            final FieldDescription movedDesc = descriptionTableModel.getFieldDescription(row);
            final FieldDescription skippedDesc = descriptionTableModel.getFieldDescription(row - 1);
            final int abspos = movedDesc.getAbsPos();
            final int delimpos = movedDesc.getDelimPos();
            movedDesc.setAbsPos(skippedDesc.getAbsPos());
            skippedDesc.setAbsPos(abspos);
            movedDesc.setDelimPos(skippedDesc.getDelimPos());
            skippedDesc.setDelimPos(delimpos);
            descriptionTableModel.removeFieldDescriptionAt(row);
            descriptionTableModel.insertFieldDescriptionAt(movedDesc, row - 1);
            descriptionTableModel.setupFieldDescriptionIndex();
            jTableFieldDescriptions.setRowSelectionInterval(row - 1, row - 1);
            refreshTestData();
        }
    }

    private void buttonDown_actionPerformed() {
        final int row = jTableFieldDescriptions.getSelectedRow();
        if (row < descriptionTableModel.getRowCount() - 1) {
            final FieldDescription movedDesc = descriptionTableModel.getFieldDescription(row);
            final FieldDescription skippedDesc = descriptionTableModel.getFieldDescription(row + 1);
            final int index = movedDesc.getIndex();
            final int abspos = movedDesc.getAbsPos();
            final int delimpos = movedDesc.getDelimPos();
            movedDesc.setIndex(skippedDesc.getIndex());
            skippedDesc.setIndex(index);
            movedDesc.setAbsPos(skippedDesc.getAbsPos());
            skippedDesc.setAbsPos(abspos);
            movedDesc.setDelimPos(skippedDesc.getDelimPos());
            skippedDesc.setDelimPos(delimpos);
            descriptionTableModel.removeFieldDescriptionAt(row);
            descriptionTableModel.insertFieldDescriptionAt(movedDesc, row + 1);
            descriptionTableModel.setupFieldDescriptionIndex();
            jTableFieldDescriptions.setRowSelectionInterval(row + 1, row + 1);
            refreshTestData();
        }
    }

    private void createNewFieldDescriptions() {
        if (database != null && database.getDatabaseSession().isConnected()) {
        	SQLTable table = database.getDataModel().getSQLTable(jTextFieldTableName.getText().trim());
        	if (table != null) {
                final List<FieldDescription> v = database.selectFieldDescriptions(table);
                if (v != null) {
                    descriptionTableModel.setFieldDescriptions(v);
                } else {
                    JOptionPane.showMessageDialog(this,
                            Messages.getString("ImportConfiguratorFrame.tablenotexist"), 
                            Messages.getString("ImportConfiguratorFrame.loadTableStructure"), 
                            JOptionPane.ERROR_MESSAGE);
                }
        	}
        }
        createTestFieldDescriptions();
        refreshTestData();
    }

    private boolean existsFile(String fileName) {
        File f = new File(fileName);
        return f.exists();
    }

    private void buttonStart_actionPerformed() {
        final String message = isDescriptionValid();
        if (message != null) {
            JOptionPane.showMessageDialog(this,
                    message,
                    Messages.getString("ImportConfiguratorFrame.importMessageTitle"), JOptionPane.ERROR_MESSAGE); 
        } else if ((database == null) || database.isConnected() == false) {
            JOptionPane.showMessageDialog(this,
                    Messages.getString("ImportConfiguratorFrame.noconnectioncreated"), 
                    Messages.getString("ImportConfiguratorFrame.importMessageTitle"), 
                    JOptionPane.ERROR_MESSAGE);
        } else if (existsFile(jTextFieldSourceFileName.getText()) == false) {
            JOptionPane.showMessageDialog(this,
                    Messages.getString("ImportConfiguratorFrame.sourcefilenotexists"), 
                    Messages.getString("ImportConfiguratorFrame.importMessageTitle"), 
                    JOptionPane.ERROR_MESSAGE);
        } else {
            importProgressPanel.reset();
            importProgressPanel.setImporter(importer);
            final Thread importThread = new Thread() {

                @Override
                public void run() {
                    final LongRunningAction lra = new LongRunningAction() {

                        public String getName() {
                            return "Import file";
                        }

                        public void cancel() {

                        }

                        public boolean canBeCanceled() {
                            return false;
                        }

                    };
                	MainFrame.addLongRunningAction(lra);
                    jButtonStartImport.setEnabled(false);
                    jMenuItemFileNew.setEnabled(false);
                    jMenuItemFileOpen.setEnabled(false);
                    jButtonCancelImport.setEnabled(true);
                    try {
                        importer.setupLocalLoggerWithFileAppender(jTextFieldSourceFileName.getText().trim());
                        if (importer.connect(database.getDatabaseSession().getConnectionDescription())) {
                            if (importer.initConfig(buildProperties())) {
                                importer.setTestOnly(jCheckBoxTestOnly.isSelected());
                                importer.importData(new File(jTextFieldSourceFileName.getText().trim()));
                                importer.disconnect();
                            } else {
                                JOptionPane.showMessageDialog(
                                    importProgressPanel, 
                                    importer.getLastErrorMessage(), 
                                    "config", 
                                    JOptionPane.ERROR_MESSAGE); 
                            }
                        } else {
                            JOptionPane.showMessageDialog(
                                importProgressPanel, 
                                importer.getLastErrorMessage(), 
                                "connect", 
                                JOptionPane.ERROR_MESSAGE); 
                        }
                    } catch (IOException ioe) {
                        JOptionPane.showMessageDialog(
                            importProgressPanel, 
                            "logger cannot be initiated:" + ioe.getMessage(), 
                            "init importer", 
                            JOptionPane.ERROR_MESSAGE); 
                    } finally {
                        jButtonStartImport.setEnabled(true);
                        jButtonCancelImport.setEnabled(false);
                        jMenuItemFileNew.setEnabled(true);
                        jMenuItemFileOpen.setEnabled(true);
                        MainFrame.removeLongRunningAction(lra);
                    }
                }
            }; // new Thread() {
            importThread.start();
            importProgressPanel.startMonitoring();
            jTabbedPane.setSelectedIndex(jTabbedPane.getTabCount() - 1); // select the last tab
        }
    }

    private boolean isChanged() {
        Properties currentConfig = buildProperties();
        return currentConfig.equals(lastConfiguration) == false;
    }

    /**
     * check if configuration is changed
     * @return true if workflow can continue
     */
    private boolean checkSaveConfig() {
        boolean ok = true;
        if (isChanged()) {
            int answer = JOptionPane.showConfirmDialog(
                this, 
                Messages.getString("ImportConfiguratorFrame.changedMessage"), 
                Messages.getString("ImportConfiguratorFrame.changedTitle"), 
                JOptionPane.YES_NO_CANCEL_OPTION);  //$NON-NLS-2$
            switch (answer) {
                case JOptionPane.YES_OPTION:
                    ok = saveConfigFile(false);
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                    ok = false;
                    break;
                default:
                    ok = false;
            }
        }
        return ok;
    }

    private void chooseAndOpenConfigfile() {
        File f = chooseImportCfgFilename(false);
        if (f != null) {
            if (checkSaveConfig()) {
                openConfigFile(f);
            }
        }
    }
    
    public boolean openConfigFile(File file) {
        return openConfigFile(file.getAbsolutePath());
    }
    
    public boolean openConfigFile(String file) {
        boolean ok = false;
        ok = importer.setImportConfig(file);
        showImportConfigurationFromImporter();
        setConfigFileName(file);
        if (ok == false) {
            JOptionPane.showMessageDialog(this,
                    importer.getLastErrorMessage(),
                    Messages.getString("ImportConfiguratorFrame.openImportConfiguration"), 
                    JOptionPane.ERROR_MESSAGE);
        }
        return ok;
    }

    private void showImportConfigurationFromImporter() {
    	ImportAttributes attr = importer.getImportAttributes();
        jTextFieldTableName.setText(attr.getTableName());
        jTextFieldSheetName.setText(attr.getSheetName());
        delimiterConfigPanel.setDelimiterToken(attr.getDelimiterToken());
        delimiterConfigPanel.setEnclosure(attr.getEnclosure());
        jCheckBoxInsertEnabled.setSelected(attr.isInsertEnabled());
        if (attr.getBatchSize() > 1) {
            jTextFieldBatchSize.setText(String.valueOf(attr.getBatchSize()));
            jTextFieldBatchSize.setEditable(true);
        } else {
            jTextFieldBatchSize.setText(null);
        }
        jCheckBoxUpdateEnabled.setSelected(attr.isUpdateEnabled());
        jCheckBoxDeleteAllBefore.setSelected(attr.isDeleteBeforeImport());
        jCheckBoxIgnoreFirstLine.setSelected(attr.isSkipFirstRow());
        jCheckBoxHandleAlwaysAsCSVFile.setSelected(attr.isHandleFileAlwaysAsCSV());
        NumberFormat nf = NumberFormat.getInstance(new Locale("en_UK"));
        nf.setGroupingUsed(false);
        jTextFieldSkipRows.setText(nf.format(attr.getCountSkipRows()));
        jComboBoxCharSet.setSelectedItem(attr.getCharsetName());
        jTextAreaPreProcessingSQL.setText(attr.getPreProcessSQL());
        jCheckBoxEnablePreProcessing.setSelected(attr.isPreProcessEnabled());
        jTextAreaPostProcessingSQL.setText(attr.getPostProcessSQL());
        jCheckBoxEnablePostProcessing.setSelected(attr.isPostProcessEnabled());
        descriptionTableModel.setFieldDescriptions(importer.getFieldDescriptions());
        filePointers.removeAllElements();
        lastConfiguration = buildProperties();
        showTestData(0);
    }

    private void clearConfiguration() {
        jTextFieldTableName.setText(null);
        jCheckBoxInsertEnabled.setSelected(true);
        jTextFieldBatchSize.setText(null);
        jCheckBoxUpdateEnabled.setSelected(false);
        jCheckBoxDeleteAllBefore.setSelected(false);
        jCheckBoxIgnoreFirstLine.setSelected(false);
        jCheckBoxHandleAlwaysAsCSVFile.setSelected(false);
        jTextFieldSkipRows.setText("0");
        jComboBoxCharSet.setSelectedItem(Main.currentCharSet);
        jTextAreaPreProcessingSQL.setText(null);
        jCheckBoxEnablePreProcessing.setSelected(false);
        jTextAreaPostProcessingSQL.setText(null);
        jCheckBoxEnablePostProcessing.setSelected(false);
        descriptionTableModel.removeAllFieldDescriptions();
        filePointers.removeAllElements();
        this.configFileName = null;
        // create default properties
        lastConfiguration = buildProperties();
        setTitle(Messages.getString("ImportConfiguratorFrame.newTitle")); 
    }

    private void buttonOpenSourceFile_actionPerformed() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(Messages.getString("ImportConfiguratorFrame.chooseSourceFile")); 
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.addChoosableFileFilter(new ImpDataFileFilter());
        File parentDir = null;
        if (configFileName != null) {
            final File cfgFile = new File(configFileName.trim());
            parentDir = cfgFile.getParentFile();
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        } else if (jTextFieldSourceFileName.getText().trim().length() > 0) {
            final File dataFile = new File(jTextFieldSourceFileName.getText().trim());
            chooser.setSelectedFile(dataFile);
            parentDir = dataFile.getParentFile();
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        } else {
            parentDir = new File(Main.getUserProperty("IMPORT_DATAFILE_DIR", System.getProperty("user.home")));  //$NON-NLS-2$
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        }
        final int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File f = chooser.getSelectedFile();
            parentDir = f.getParentFile().getAbsoluteFile();
            Main.setUserProperty("IMPORT_DATAFILE_DIR", parentDir.getAbsolutePath());
            jTextFieldSourceFileName.setText(f.getAbsolutePath());
            jButtonCountLines.setEnabled(true);
            openDataFile(new File(jTextFieldSourceFileName.getText()));
        }
    }

    private void buttonClear_actionPerformed() {
        descriptionTableModel.removeAllFieldDescriptions();
    }

    public void createImportDescriptionForTable(String tablename) {
        jTextFieldTableName.setText(tablename);
        createNewFieldDescriptions();
    }
    
    public void takeOverHeaderAsFieldNames() {
    	int rowCount = descriptionTableModel.getRowCount();
    	for (int i = 0; i < rowCount; i++) {
    		String testdata = (String) descriptionTableModel.getValueAt(i, 5);
    		FieldDescription fd = descriptionTableModel.getFieldDescription(i);
    		if (fd.isDummy()) {
    			fd.setName(testdata);
    			fd.setEnabled(true);
    			fd.setIndex(i);
    		}
    	}
    	descriptionTableModel.fireTableRowsUpdated(0, rowCount - 1);
    }

    /**
     * This method initializes jPanelPostProcessSQL
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelPostProcessSQL() {
        final JPanel jPanelPostProcessSQL = new JPanel();
        jPanelPostProcessSQL.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelPostProcessSQL.add(getJCheckBoxEnablePostProcessing(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelPostProcessSQL.add(getJComboBoxAddPostSQLParameter(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelPostProcessSQL.add(createJScrollPanePostProcessingSQL(), gbc);
        }
        return jPanelPostProcessSQL;
    }
    
    private JComboBox getJComboBoxAddPostSQLParameter() {
        if (jComboBoxAddPostSQLParameter == null) {
            jComboBoxAddPostSQLParameter = new JComboBox();
            setupAddParameterComboBox(jComboBoxAddPostSQLParameter);
            jComboBoxAddPostSQLParameter.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        Object selection = e.getItem();
                        if (selection instanceof ParameterPlaceHolder) {
                            ParameterPlaceHolder ph = (ParameterPlaceHolder) selection;
                            jTextAreaPostProcessingSQL.insert(ph.getCode(), jTextAreaPostProcessingSQL.getCaretPosition());
                        }
                    }
                }
            });
        }
        return jComboBoxAddPostSQLParameter;
    }
    
    private void setupAddParameterComboBoxes() {
        setupAddParameterComboBox(getJComboBoxAddPreSQLParameter());
        setupAddParameterComboBox(getJComboBoxAddPostSQLParameter());
    }
    
    private void setupAddParameterComboBox(JComboBox jComboBox) {
        jComboBox.removeAllItems();
        jComboBox.addItem(Messages.getString("ImportConfiguratorFrame.addparameter"));
        List<FieldDescription> list = descriptionTableModel.getDescriptions();
        for (FieldDescription fieldDescription : list) {
            if (fieldDescription.isDummy() == false) {
                ParameterPlaceHolder ph = new ParameterPlaceHolder();
                ph.setName("default value of Field " + fieldDescription.getName());
                if (fieldDescription.getBasicTypeId() == BasicDataType.CHARACTER.getId() || fieldDescription.getBasicTypeId() == BasicDataType.CLOB.getId()) {
                    ph.setCode("'{field." + fieldDescription.getName() + ".defaultValue}'");
                } else {
                    ph.setCode("{field." + fieldDescription.getName() + ".defaultValue}");
                }
                jComboBox.addItem(ph);
                jComboBox.setSelectedIndex(0);
            }
        }
    }

    private static class ParameterPlaceHolder {
        
        private String name;
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public void setName(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
    }
    
    /**
     * This method initializes jPanelPostProcessSQL
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelPreProcessSQL() {
        JPanel jPanelPreProcessSQL = new JPanel();
        jPanelPreProcessSQL.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelPreProcessSQL.add(getJCheckBoxEnablePreProcessing(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelPreProcessSQL.add(getJComboBoxAddPreSQLParameter(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.fill = GridBagConstraints.BOTH;            
            jPanelPreProcessSQL.add(createJScrollPanePreProcessingSQL(), gbc);
        }
        return jPanelPreProcessSQL;
    }

    private JComboBox getJComboBoxAddPreSQLParameter() {
        if (jComboBoxAddPreSQLParameter == null) {
            jComboBoxAddPreSQLParameter = new JComboBox();
            setupAddParameterComboBox(jComboBoxAddPreSQLParameter);
            jComboBoxAddPreSQLParameter.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        Object selection = e.getItem();
                        if (selection instanceof ParameterPlaceHolder) {
                            ParameterPlaceHolder ph = (ParameterPlaceHolder) selection;
                            jTextAreaPreProcessingSQL.insert(ph.getCode(), jTextAreaPreProcessingSQL.getCaretPosition());
                        }
                    }
                }
            });
        }
        return jComboBoxAddPreSQLParameter;
    }

    /**
     * This method initializes jTextFieldSourceFileName
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getJTextFieldSourceFileName() {
        if (jTextFieldSourceFileName == null) {
            jTextFieldSourceFileName = new JTextField();
            jTextFieldSourceFileName.setEditable(false);
        }
        return jTextFieldSourceFileName;
    }
    
    private JTextField getJTextFieldSheetName() {
    	if (jTextFieldSheetName == null) {
    		jTextFieldSheetName = new JTextField();
    		jTextFieldSheetName.addKeyListener(new KeyListener() {
				
				public void keyTyped(KeyEvent e) {
					if (e.getKeyChar() == KeyEvent.VK_ENTER) {
						setupTestDatasetProvider();
						showTestDataAtRow(0);
					}
				}
				
				public void keyReleased(KeyEvent e) {}
				
				public void keyPressed(KeyEvent e) {}
				
			});
    	}
    	return jTextFieldSheetName;
    }

    /**
     * This method initializes jButtonChooseSourceFile
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonChooseSourceFile() {
        if (jButtonChooseSourceFile == null) {
            jButtonChooseSourceFile = new JButton();
            jButtonChooseSourceFile.setText(Messages.getString("ImportConfiguratorFrame.choosesourcefile")); 
            jButtonChooseSourceFile.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    buttonOpenSourceFile_actionPerformed();
                }
            });
        }
        return jButtonChooseSourceFile;
    }

    /**
     * This method initializes jCheckBoxIgnoreFirstLine
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBoxIgnoreFirstLine() {
        if (jCheckBoxIgnoreFirstLine == null) {
            jCheckBoxIgnoreFirstLine = new JCheckBox();
            jCheckBoxIgnoreFirstLine.setText(Messages.getString("ImportConfiguratorFrame.ignorefirstline")); 
            jCheckBoxIgnoreFirstLine.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    refreshTestData();
                }
                
            });
        }
        return jCheckBoxIgnoreFirstLine;
    }

    private JTextField getJTextFieldSkipRows() {
        if (jTextFieldSkipRows == null) {
            jTextFieldSkipRows = new JTextField();
            jTextFieldSkipRows.setText("0");
            jTextFieldSkipRows.addKeyListener(new KeyListener() {
				
				public void keyTyped(KeyEvent e) {
					if (e.getKeyChar() == KeyEvent.VK_ENTER) {
						refreshTestData();
					}
				}
				
				public void keyReleased(KeyEvent e) {}
				
				public void keyPressed(KeyEvent e) {}
			});
        }
        return jTextFieldSkipRows;
    }

    /**
     * This method initializes jComboBoxCharSet
     * 
     * @return javax.swing.JComboBox
     */
    private JComboBox getJComboBoxCharSet() {
        if (jComboBoxCharSet == null) {
            jComboBoxCharSet = new JComboBox();
            jComboBoxCharSet.setEditable(true);
            final String charSets = Main.getDefaultProperty("CHAR_SETS", 
                    "ISO-8859-1|ISO-8859-15|UTF-8|UTF-16|Cp1252|MacRoman"); 
            final String currentCharSet = Main.getUserProperty("CURR_CHAR_SET"); 
            final String systemCharSet = System.getProperty("file.encoding"); 
            final StringTokenizer st = new StringTokenizer(charSets, "|"); 
            String charSet = null;
            while (st.hasMoreTokens()) {
                charSet = st.nextToken();
                jComboBoxCharSet.addItem(charSet);
                if (currentCharSet != null) {
                    if (charSet.equals(currentCharSet)) {
                        jComboBoxCharSet.setSelectedItem(charSet);
                    }
                } else if (systemCharSet != null) {
                    if (charSet.equals(systemCharSet)) {
                        jComboBoxCharSet.setSelectedItem(charSet);
                    }
                }
            }
            jComboBoxCharSet.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                    	setupTestDatasetProvider();
						showTestDataAtRow(0);
                    }
                }
            });
        }
        return jComboBoxCharSet;
    }
    
    public void handleFile(File file) {
        if (file != null) {
            if (file.getName().toLowerCase().endsWith(IMPORT_CONFIG_EXTENSION)) {
                if (checkSaveConfig()) {
                    openConfigFile(file);
                }
            } else if (importer.isDataFile(file)) {
            	jTextFieldSourceFileName.setText(file.getAbsolutePath());
                openDataFile(file);
            }
        }
    }
    
    public void handleFile(String filePath) {
        if (filePath != null) {
        	handleFile(new File(filePath));
        }
    }
    
    public void handleConfigProperties(String propertiesAsString) {
        if (checkSaveConfig()) {
            boolean ok = false;
            ok = importer.initConfig(propertiesAsString);
            showImportConfigurationFromImporter();
            setConfigFileName(null);
            if (ok == false) {
                JOptionPane.showMessageDialog(this,
                        importer.getLastErrorMessage(),
                        Messages.getString("ImportConfiguratorFrame.openImportConfiguration"), 
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void openDataFile(File file) {
        if (logger.isDebugEnabled()) {
            logger.debug("openDataFile(file=" + file + ")");
        }
        currentDataFile = file;
        intitializeTestdataView();
    }

	private void intitializeTestdataView() {
		new Thread() {
        	public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                	}
                });
                try {
                    if (setupTestDatasetProvider()) {
                        setupTestFieldTokenizer();
                        showTestDataAtRow(0);
                    }
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                    	public void run() {
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    	}
                    });
                }
        	}
        }.start();
	}

    /**
     * This method initializes jButtonCountLines
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonCountLines() {
        if (jButtonCountLines == null) {
            jButtonCountLines = new JButton();
            jButtonCountLines.setEnabled(false);
            jButtonCountLines.setText(Messages.getString("ImportConfiguratorFrame.countlines")); 
            jButtonCountLines.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    countLines();
                }
            });
        }
        return jButtonCountLines;
    }
    
    private void countLines() {
        if (jTextFieldSourceFileName.getText() != null) {
            counterThread = new Thread() {

                @Override
                public void run() {
                	SwingUtilities.invokeLater(new Runnable() {
                		public void run() {
                            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            jButtonCountLines.setEnabled(false);
                		}
                	});
                    long count = 0;
                    try {
                        count = importer.retrieveDatasetCount(new File(jTextFieldSourceFileName.getText().trim()), buildImportAttributes());
                    } catch (Exception ex) {
                        logger.error("countLines failed: " + ex.getMessage(), ex);
                        JOptionPane.showMessageDialog(jTextFieldSourceFileName, ex.getMessage(), "retrieve line count failed", JOptionPane.ERROR_MESSAGE);
                    }
                    final long fcount = count;
                	SwingUtilities.invokeLater(new Runnable() {
                		public void run() {
                            jTextFieldCountLines.setText(String.valueOf(fcount));
                            setCursor(Cursor.getDefaultCursor());
                            jButtonCountLines.setEnabled(true);
                		}
                	});
                }
            };
            counterThread.start();
        }
    }

    /**
     * This method initializes jCheckBoxEnablePostProcessing
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBoxEnablePostProcessing() {
        if (jCheckBoxEnablePostProcessing == null) {
            jCheckBoxEnablePostProcessing = new JCheckBox();
            jCheckBoxEnablePostProcessing.setText(Messages.getString("ImportConfiguratorFrame.enablepostprocessing")); 
        }
        return jCheckBoxEnablePostProcessing;
    }

    /**
     * This method initializes jScrollPanePostProcessingSQL
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane createJScrollPanePostProcessingSQL() {
        JScrollPane jScrollPanePostProcessingSQL = new JScrollPane();
        jScrollPanePostProcessingSQL.setViewportView(getJTextAreaPostProcessingSQL());
        return jScrollPanePostProcessingSQL;
    }

    /**
     * This method initializes jCheckBoxEnablePostProcessing
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBoxEnablePreProcessing() {
        if (jCheckBoxEnablePreProcessing == null) {
            jCheckBoxEnablePreProcessing = new JCheckBox();
            jCheckBoxEnablePreProcessing.setText(Messages.getString("ImportConfiguratorFrame.enablepreprocessing")); 
        }
        return jCheckBoxEnablePreProcessing;
    }

    /**
     * This method initializes jScrollPanePostProcessingSQL
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane createJScrollPanePreProcessingSQL() {
        JScrollPane jScrollPanePreProcessingSQL = new JScrollPane();
        jScrollPanePreProcessingSQL.setViewportView(getJTextAreaPreProcessingSQL());
        return jScrollPanePreProcessingSQL;
    }

    /**
     * This method initializes jTextAreaPostProcessingSQL
     * 
     * @return javax.swing.JTextArea
     */
    private JTextArea getJTextAreaPostProcessingSQL() {
        if (jTextAreaPostProcessingSQL == null) {
            jTextAreaPostProcessingSQL = new JTextArea();
        }
        return jTextAreaPostProcessingSQL;
    }

    /**
     * This method initializes jTextAreaPostProcessingSQL
     * 
     * @return javax.swing.JTextArea
     */
    private JTextArea getJTextAreaPreProcessingSQL() {
        if (jTextAreaPreProcessingSQL == null) {
            jTextAreaPreProcessingSQL = new JTextArea();
        }
        return jTextAreaPreProcessingSQL;
    }

    /**
     * This method initializes delimiterConfigPanel
     * 
     * @return sqlrunner.DelimiterConfigPanel
     */
    private DelimiterConfigPanel getDelimiterConfigPanel() {
        if (delimiterConfigPanel == null) {
            delimiterConfigPanel = new DelimiterConfigPanel();
            delimiterConfigPanel.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    createTestFieldDescriptions();
                    refreshTestData();
                }
            });
        }
        return delimiterConfigPanel;
    }

    /**
     * This method initializes jTextFieldTableName
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getJTextFieldTableName() {
        if (jTextFieldTableName == null) {
            jTextFieldTableName = new JTextField();

        }
        return jTextFieldTableName;
    }
    
    private JTextField getJTextFieldBatchSize() {
    	if (jTextFieldBatchSize == null) {
    		jTextFieldBatchSize = new JTextField();
    		jTextFieldBatchSize.setEditable(false);
    		jTextFieldBatchSize.getDocument().addDocumentListener(new DocumentListener() {
				
				public void removeUpdate(DocumentEvent e) {
					checkBatchSizeInput();
				}
				
				public void insertUpdate(DocumentEvent e) {
					checkBatchSizeInput();
				}
				
				public void changedUpdate(DocumentEvent e) {
					checkBatchSizeInput();
				}
			});
    	}
    	return jTextFieldBatchSize;
    }

    private void checkBatchSizeInput() {
    	String s = jTextFieldBatchSize.getText();
    	if (s != null) {
    		s = s.trim();
    		if (s.isEmpty() ==  false) {
        		try {
        			Integer.parseInt(s);
        		} catch (NumberFormatException nfe) {
        			JOptionPane.showMessageDialog(this, "Invalid value for batch size: it must be an integer!");
        			jTextFieldBatchSize.requestFocusInWindow();
        		}
    		}
    	}
    }
    
    /**
     * This method initializes jPanelFieldConfigButtons
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelFieldConfigButtons() {
        JPanel jPanelFieldConfigButtons = new JPanel();
        jPanelFieldConfigButtons.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(getJButtonCreateNewFieldDescriptions(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(getJButtonAddFieldDescription(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(getJButtonEditFieldDescription(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(getJButtonDeleteFieldDescription(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 4;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(getJButtonDeleteAllFieldDescriptions(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(new JLabel(Messages.getString("ImportConfiguratorFrame.sortFdByLabel")), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(getJComboBoxSortFieldDescriptions(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            jPanelFieldConfigButtons.add(getJButtonTakeOverHeaderAsFieldName(), gbc);
        }
        return jPanelFieldConfigButtons;
    }

    /**
     * This method initializes jButtonCreateNewFieldDescriptions
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonCreateNewFieldDescriptions() {
        if (jButtonCreateNewFieldDescriptions == null) {
            jButtonCreateNewFieldDescriptions = new JButton();
            jButtonCreateNewFieldDescriptions.setText(Messages.getString("ImportConfiguratorFrame.createnewdesc")); 
            jButtonCreateNewFieldDescriptions.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    createNewFieldDescriptions();
                }
            });
        }
        return jButtonCreateNewFieldDescriptions;
    }

    /**
     * This method initializes jButtonEditFieldDescription
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonEditFieldDescription() {
        if (jButtonEditFieldDescription == null) {
            jButtonEditFieldDescription = new JButton();
            jButtonEditFieldDescription.setText(Messages.getString("ImportConfiguratorFrame.editdesc")); 
            jButtonEditFieldDescription.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    buttonEdit_actionPerformed();
                }
            });
        }
        return jButtonEditFieldDescription;
    }

    /**
     * This method initializes jButtonAddFieldDescription
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonAddFieldDescription() {
        if (jButtonAddFieldDescription == null) {
            jButtonAddFieldDescription = new JButton();
            jButtonAddFieldDescription.setText(Messages.getString("ImportConfiguratorFrame.adddesc")); 
            jButtonAddFieldDescription.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    buttonAdd_actionPerformed();
                }
            });
        }
        return jButtonAddFieldDescription;
    }

    /**
     * This method initializes jButtonDeleteFieldDescription
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonDeleteFieldDescription() {
        if (jButtonDeleteFieldDescription == null) {
            jButtonDeleteFieldDescription = new JButton();
            jButtonDeleteFieldDescription.setText(Messages.getString("ImportConfiguratorFrame.deletedesc")); 
            jButtonDeleteFieldDescription.addActionListener(deleteAction);
        }
        return jButtonDeleteFieldDescription;
    }
    
    private AbstractAction deleteAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
            if (jTableFieldDescriptions.getSelectedRow() != -1) {
                final int deletedFDIndex = jTableFieldDescriptions.getSelectedRow();
                descriptionTableModel.removeFieldDescriptionAt(deletedFDIndex);
                // nun den index der FieldDescription aktualisieren
                reorgFieldDescriptionIndex();
            }
        }
        
    };
    
    private AbstractAction moveForwardAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
            showTestData(1);
        }
        
    };
    
    private AbstractAction moveBackwardAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
            showTestData(-1);
        }
        
    };

    /**
     * This method initializes jButtonDeleteAllFieldDescriptions
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonDeleteAllFieldDescriptions() {
        if (jButtonDeleteAllFieldDescriptions == null) {
            jButtonDeleteAllFieldDescriptions = new JButton();
            jButtonDeleteAllFieldDescriptions.setText(Messages.getString("ImportConfiguratorFrame.deletealldesc")); 
            jButtonDeleteAllFieldDescriptions.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    buttonClear_actionPerformed();
                }
            });
        }
        return jButtonDeleteAllFieldDescriptions;
    }
    
    private JButton getJButtonTakeOverHeaderAsFieldName() {
    	if (jButtonTakeOverHeaderAsFieldName == null) {
    		jButtonTakeOverHeaderAsFieldName = new JButton();
    		jButtonTakeOverHeaderAsFieldName.setText(Messages.getString("ImportConfiguratorFrame.takeOverFieldNames"));
    		jButtonTakeOverHeaderAsFieldName.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					takeOverHeaderAsFieldNames();
				}
			});
    	}
    	return jButtonTakeOverHeaderAsFieldName;
    }

    /**
     * This method initializes jPanelTableConfig
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelTableConfig() {
        JPanel jPanelTableConfig = new JPanel();
        jPanelTableConfig.setLayout(new GridBagLayout());
        jPanelTableConfig.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
            Messages.getString("ImportConfiguratorFrame.targetdatabasetable"), 
            TitledBorder.DEFAULT_JUSTIFICATION, 
            TitledBorder.DEFAULT_POSITION, 
            null, 
            null));
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.weightx = 1.0;
            gbc.gridwidth = 3;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            jPanelTableConfig.add(getJTextFieldTableName(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            jPanelTableConfig.add(getJCheckBoxDeleteAllBefore(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            jPanelTableConfig.add(getJCheckBoxInsertEnabled(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel();
            label.setText(Messages.getString("ImportConfiguratorFrame.batchSize"));
            jPanelTableConfig.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            jPanelTableConfig.add(getJTextFieldBatchSize(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            jPanelTableConfig.add(getJCheckBoxUpdateEnabled(), gbc);
        }
        return jPanelTableConfig;
    }

    /**
     * This method initializes jPanelFieldDescriptions
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelFieldDescriptions() {
        JPanel jPanelFieldDescriptions = new JPanel();
        jPanelFieldDescriptions.setLayout(new GridBagLayout());
        jPanelFieldDescriptions.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
            Messages.getString("ImportConfiguratorFrame.fielddescriptions"), 
            TitledBorder.DEFAULT_JUSTIFICATION, 
            TitledBorder.DEFAULT_POSITION, 
            new Font("Lucida Grande", Font.PLAIN, 13), 
            Color.black));
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            jPanelFieldDescriptions.add(createJPanelFieldConfigButtons(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0D;
            jPanelFieldDescriptions.add(createJPanelFieldDetails(), gbc);
        }
        return jPanelFieldDescriptions;
    }

    /**
     * This method initializes jCheckBoxDeleteAllBefore
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBoxDeleteAllBefore() {
        if (jCheckBoxDeleteAllBefore == null) {
            jCheckBoxDeleteAllBefore = new JCheckBox();
            jCheckBoxDeleteAllBefore.setText(Messages.getString("ImportConfiguratorFrame.deleteallpreviousdatasets")); 
        }
        return jCheckBoxDeleteAllBefore;
    }

    /**
     * This method initializes jCheckBoxInsertEnabled
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBoxInsertEnabled() {
        if (jCheckBoxInsertEnabled == null) {
            jCheckBoxInsertEnabled = new JCheckBox();
            jCheckBoxInsertEnabled.setText(Messages.getString("ImportConfiguratorFrame.insertenabled"));
            jCheckBoxInsertEnabled.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					getJTextFieldBatchSize().setEditable(jCheckBoxInsertEnabled.isSelected() && jCheckBoxUpdateEnabled.isSelected() == false);
				}
            	
            });
        }
        return jCheckBoxInsertEnabled;
    }

    /**
     * This method initializes jCheckBoxUpdateEnabled
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBoxUpdateEnabled() {
        if (jCheckBoxUpdateEnabled == null) {
            jCheckBoxUpdateEnabled = new JCheckBox();
            jCheckBoxUpdateEnabled.setText(Messages.getString("ImportConfiguratorFrame.updateenabled")); 
            jCheckBoxUpdateEnabled.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					getJTextFieldBatchSize().setEditable(jCheckBoxInsertEnabled.isSelected() && jCheckBoxUpdateEnabled.isSelected() == false);
				}
            	
            });
        }
        return jCheckBoxUpdateEnabled;
    }

    /**
     * This method initializes jPanelFieldDetails
     * 
     * @return javax.swing.JPanel
     */
    private JPanel createJPanelFieldDetails() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
        	JTextField field = new JTextField();
            field.setEditable(true);
        	field.setText(Messages.getString("ImportConfiguratorFrame.dropConfigPropertiesHere"));
            field.setBorder(new LineBorder(Color.BLACK, 1, true));
            field.setTransferHandler(new ImportConfiguratorTransferHandler(this));
            panel.add(field, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
            panel.add(createTestDataPanel(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0D;
            gbc.gridheight = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(2, 2, 2, 2);
            panel.add(createJScrollPaneFieldDescriptions(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.insets = new Insets(2, 2, 2, 2);
            panel.add(getJButtonMoveFieldDescUp(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.insets = new Insets(2, 2, 2, 2);
            panel.add(getJButtonMoveFieldDescDown(), gbc);
        }
        return panel;
    }
    
    private JPanel createTestDataPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JLabel jLabel = new JLabel();
            jLabel.setText(Messages.getString("ImportConfiguratorFrame.playtest")); 
            jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            panel.add(jLabel, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.insets = new Insets(2, 2, 2, 2);
            panel.add(getJButtonTestBackwards(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 5, 2, 5);
            panel.add(getJTextFieldCurrentRowNum(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 2, 2, 2);
            panel.add(getJButtonTestForwards(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 4;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 10, 2, 5);
            JLabel label = new JLabel();
            label.setText(Messages.getString("ImportConfiguratorFrame.countColumns"));
            panel.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 5;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 5, 2, 5);
            jLabelCurrentCountColumns = new JLabel();
            jLabelCurrentCountColumns.setText("0");
            jLabelCurrentCountColumns.setBorder(new LineBorder(Color.BLACK, 1, true));
            jLabelCurrentCountColumns.setHorizontalAlignment(SwingConstants.CENTER);
            jLabelCurrentCountColumns.setPreferredSize(new Dimension(100,25));
            panel.add(jLabelCurrentCountColumns, gbc);
        }
        return panel;
    }
    
    private JTextField getJTextFieldCurrentRowNum() {
    	if (jTextFieldCurrentRowNum == null) {
            jTextFieldCurrentRowNum = new JTextField();
            jTextFieldCurrentRowNum.setText("1");
            jTextFieldCurrentRowNum.setHorizontalAlignment(SwingConstants.CENTER);
            jTextFieldCurrentRowNum.setPreferredSize(new Dimension(100,25));
            jTextFieldCurrentRowNum.setMinimumSize(jTextFieldCurrentRowNum.getPreferredSize());
            final String actionKey = "readRowAt";
            jTextFieldCurrentRowNum.getActionMap().put(actionKey, this.readDatasetAtRowAction);
            jTextFieldCurrentRowNum.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), actionKey);
    	}
    	return jTextFieldCurrentRowNum;
    }
    
    /**
     * This method initializes jButtonTestForwards
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonTestForwards() {
        if (jButtonTestForwards == null) {
            jButtonTestForwards = new JButton();
            jButtonTestForwards.setIcon(ApplicationIcons.NEXT_GIF); 
            jButtonTestForwards.addActionListener(moveForwardAction);
        }
        return jButtonTestForwards;
    }

    /**
     * This method initializes jButtonTestBackwards
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonTestBackwards() {
        if (jButtonTestBackwards == null) {
            jButtonTestBackwards = new JButton();
            jButtonTestBackwards.setIcon(ApplicationIcons.PREV_GIF); 
            jButtonTestBackwards.addActionListener(moveBackwardAction);
        }
        return jButtonTestBackwards;
    }

    /**
     * This method initializes jButtonMoveFieldDescUp
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonMoveFieldDescUp() {
        if (jButtonMoveFieldDescUp == null) {
            jButtonMoveFieldDescUp = new JButton();
            jButtonMoveFieldDescUp.setIcon(ApplicationIcons.UP_GIF); 
            jButtonMoveFieldDescUp.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    buttonUp_actionPerformed();
                }
                
            });
        }
        return jButtonMoveFieldDescUp;
    }

    /**
     * This method initializes jButtonMoveFieldDescDown
     * 
     * @return javax.swing.JButton
     */
    private JButton getJButtonMoveFieldDescDown() {
        if (jButtonMoveFieldDescDown == null) {
            jButtonMoveFieldDescDown = new JButton();
            jButtonMoveFieldDescDown.setIcon(ApplicationIcons.DOWN_GIF); 
            jButtonMoveFieldDescDown.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    buttonDown_actionPerformed();
                }
                
            });
        }
        return jButtonMoveFieldDescDown;
    }

    /**
     * This method initializes jScrollPaneFieldDescriptions
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane createJScrollPaneFieldDescriptions() {
        JScrollPane jScrollPaneFieldDescriptions = new JScrollPane();
        jScrollPaneFieldDescriptions.setViewportView(getJTableFieldDescriptions());
        jScrollPaneFieldDescriptions.setPreferredSize(new Dimension(400, 300));
        return jScrollPaneFieldDescriptions;
    }

    /**
     * This method initializes jTableFieldDescriptions
     * 
     * @return javax.swing.JTable
     */
    private JTable getJTableFieldDescriptions() {
        if (jTableFieldDescriptions == null) {
            jTableFieldDescriptions = new JTable();
            jTableFieldDescriptions.setBackground(Main.info);
            jTableFieldDescriptions.setModel(getDescriptionTableModel());
            jTableFieldDescriptions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // das verhindert, dass allein durch Mausbewegung Zelleninhalte neu gelesen werden
            jTableFieldDescriptions.setToolTipText(null);
            jTableFieldDescriptions.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            jTableFieldDescriptions.getTableHeader().setReorderingAllowed(false);
            final TableInputListener til = new TableInputListener();
            jTableFieldDescriptions.addMouseListener(til);
            jTableFieldDescriptions.setDefaultRenderer(Object.class, new DescriptionRenderer());
            jTableFieldDescriptions.registerKeyboardAction(
                til,
                "open", 
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), 
                JComponent.WHEN_FOCUSED); 
            jTableFieldDescriptions.registerKeyboardAction(
                moveBackwardAction, 
                "prev", 
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                JComponent.WHEN_FOCUSED);
            jTableFieldDescriptions.registerKeyboardAction(
                moveForwardAction, 
                "next", 
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                JComponent.WHEN_FOCUSED);
            // Spaltenbreite setzen
            // enabled
            jTableFieldDescriptions.getTableHeader().setDefaultRenderer(new TitleRenderer());
            jTableFieldDescriptions.getColumnModel().getColumn(0).setPreferredWidth(8);
            // primary key
            jTableFieldDescriptions.getColumnModel().getColumn(1).setPreferredWidth(8);
            // not null
            jTableFieldDescriptions.getColumnModel().getColumn(2).setPreferredWidth(8);
            // table column
            jTableFieldDescriptions.getColumnModel().getColumn(3).setPreferredWidth(100);
            // description
            jTableFieldDescriptions.getColumnModel().getColumn(4).setPreferredWidth(200);
            // test data
            jTableFieldDescriptions.getColumnModel().getColumn(5).setPreferredWidth(100);
            jTableFieldDescriptions.setDragEnabled(true);
            jTableFieldDescriptions.setTransferHandler(new ImportConfiguratorTransferHandler(this));
        }
        return jTableFieldDescriptions;
    }

    public String getConfigurationPropertiesAsString() throws UnsupportedEncodingException {
    	Properties p = buildProperties();
    	TreeMap<Object, Object> map = new TreeMap<Object, Object>(p);
    	StringBuffer sb = new StringBuffer();
    	for (Map.Entry<Object, Object> entry : map.entrySet()) {
    		sb.append(entry.getKey());
    		sb.append("=");
    		sb.append(entry.getValue() != null ? entry.getValue() : "");
    		sb.append("\n");
    	}
        return sb.toString();
    }

    private long getCountRowsToSkip() throws NumberFormatException {
        if (jTextFieldSkipRows.getText() != null && jTextFieldSkipRows.getText().length() > 0) {
            return Long.parseLong(jTextFieldSkipRows.getText().trim());
        } else {
            return 0;
        }
    }

    private Properties buildProperties() {
    	if (logger.isTraceEnabled()) {
    		logger.trace("buildProperties");
    	}
        Properties props = new Properties();
        ImportAttributes opts = buildImportAttributes();
    	if (logger.isTraceEnabled()) {
    		logger.trace("buildProperties: store attributes");
    	}
        opts.storeInto(props);
    	if (logger.isTraceEnabled()) {
    		logger.trace("buildProperties: store field descriptions");
    	}
        for (int i = 0; i < descriptionTableModel.getRowCount(); i++) {
        	FieldDescription fd = descriptionTableModel.getFieldDescription(i);
        	if (fd.isDummy() == false) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("put into properties: field description " + fd + " with index=" + fd.getIndex());
        		}
        		fd.completeProperties(props);
        	}
        }
        return props;
    }
    
    private ImportAttributes buildImportAttributes() {
        ImportAttributes opts = new ImportAttributes();
        opts.setTableName(jTextFieldTableName.getText());
        opts.setSheetName(jTextFieldSheetName.getText());
        opts.setInsertEnabled(jCheckBoxInsertEnabled.isSelected());
        opts.setUpdateEnabled(jCheckBoxUpdateEnabled.isSelected());
        if (jTextFieldBatchSize.isEditable()) {
        	if (jTextFieldBatchSize.getText().trim().isEmpty() == false) {
            	opts.setBatchSize(Integer.parseInt(jTextFieldBatchSize.getText()));
        	}
        }
        opts.setDeleteBeforeImport(jCheckBoxDeleteAllBefore.isSelected());
        opts.setSkipFirstRow(jCheckBoxIgnoreFirstLine.isSelected());
        opts.setHandleFileAlwaysAsCSV(jCheckBoxHandleAlwaysAsCSVFile.isSelected());
        opts.setCountSkipRows(getCountRowsToSkip());
        opts.setDelimiter(delimiterConfigPanel.getDelimiter());
        opts.setEnclosure(delimiterConfigPanel.getEnclosure());
        opts.setColumnCount(descriptionTableModel.getRowCount());
        opts.setPreProcessSQL(jTextAreaPreProcessingSQL.getText());
        opts.setPreProcessEnabled(jCheckBoxEnablePreProcessing.isSelected());
        opts.setPostProcessSQL(jTextAreaPostProcessingSQL.getText());
        opts.setPostProcessEnabled(jCheckBoxEnablePostProcessing.isSelected());
        opts.setCharsetName((String) jComboBoxCharSet.getSelectedItem());
        return opts;
    }

    /**
     * table renderer to toggle the background color
     * 
     * @author jan
     * 
     */
    static private final class DescriptionRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        public DescriptionRenderer() {
            super();
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(
                JTable table_loc,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int col) {
            if (value != null) {
                setText(value.toString());
            } else {
                setText(""); 
            }
            setFont(table_loc.getFont());
            if (isSelected) {
                if (hasFocus) {
                    setForeground(Color.black);
                    setBackground(Color.white);
                } else {
                    setForeground(table_loc.getSelectionForeground());
                    setBackground(table_loc.getSelectionBackground());
                }
            } else {
                setForeground(table_loc.getForeground());
                if (col == 5) {
                    setBackground(new Color(240, 255, 240));
                } else {
                    setBackground(new Color(255, 255, 225));
                }
            }
            if (hasFocus) {
                setBorder(BorderFactory.createLineBorder(Color.black));
            } else {
                setBorder(null);
            }
            return this;
        }
    }

    static private class TitleRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;
        private final Color colorData = new Color(190, 230, 190);
        private final Color colorDesc = new Color(200, 200, 100);

        public TitleRenderer() {
            super();
            setOpaque(true); // sonst wird der Hintergrund als durchlässig
        // angesehen und Einstellungen zum Hintergrund ignoriert!!
        }

        public Component getTableCellRendererComponent(JTable table_loc,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            setForeground(Color.black);
            if (column == 5) {
                setBackground(colorData);
            } else {
                setBackground(colorDesc);
            }
            setBorder(BorderFactory.createBevelBorder(0)); // 0 = herausgehoben
            setHorizontalAlignment(SwingConstants.CENTER);
            setText((String) value); // hier wird dem Stempel der Aufdruck verpasst
            setFont(new java.awt.Font("Dialog", 0, 12)); 
            if (column == 0) {
                setToolTipText(Messages.getString("ImportConfiguratorFrame.willbeimported")); 
            } else if (column == 1) {
                setToolTipText(Messages.getString("ImportConfiguratorFrame.ispartofprimarykey")); 
            } else {
                setToolTipText((String) value);
            }
            return this;
        }
    } // class TitleRenderer

    /**
     * This method initializes importConfigurationTableModel
     * 
     * @return sqlrunner.flatfileimport.ImportConfigurationTableModel
     */
    private DescriptionTableModel getDescriptionTableModel() {
        if (descriptionTableModel == null) {
            descriptionTableModel = new DescriptionTableModel();
            descriptionTableModel.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                    setupAddParameterComboBoxes();
                }
            });
        }
        return descriptionTableModel;
    }
    
    ArrayList<FieldDescription> getAllFieldDescriptions() {
        return descriptionTableModel.getDescriptions();
    }

    /**
     * This method initializes importProgressPanel
     * 
     * @return sqlrunner.flatfileimport.ImportProgressPanel
     */
    private ImportProgressPanel getImportProgressPanel() {
        if (importProgressPanel == null) {
            importProgressPanel = new ImportProgressPanel();
        }
        return importProgressPanel;
    }

    /**
     * eigener MouseAdapter mit der Fähigkeit Doppelklick zu erkennen
     */
    private class TableInputListener extends MouseAdapter implements ActionListener {

        // hier Reaktion auf Doppelklick festlegen
        protected void fireDoubleClickPerformed() {
            if (jTableFieldDescriptions.getSelectedRow() != -1) {
                buttonEdit_actionPerformed();
            }
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            final Component comp = (Component) me.getSource();
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent me) {
            final Component comp = (Component) me.getSource();
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            // testen ob Kontextmenu erzeugt werden soll
            if (me.getClickCount() == 2) {
                fireDoubleClickPerformed();
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("open")) { 
                fireDoubleClickPerformed();
            }
        }
    }
    
    private boolean setupTestDatasetProvider() {
    	if (currentDataFile != null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("setupTestDatasetProvider");
        	}
        	if (testDatasetProvider != null) {
        		testDatasetProvider.closeDatasetProvider();
        	}
        	ImportAttributes attr = buildImportAttributes();
            try {
            	testDatasetProvider = importer.createDatasetProvider(currentDataFile, true, "csv", attr);
            	return true;
            } catch (Throwable e) {
    			JOptionPane.showMessageDialog(this, e.getMessage(), "Open data file", JOptionPane.ERROR_MESSAGE);
    			return false;
            }
    	} else {
    		return true;
    	}
    }
    
    private void setupTestFieldTokenizer() {
    	if (currentDataFile != null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("setupTestFieldTokenizer");
        	}    	
            testFieldParser = importer.createFieldTokenizer(currentDataFile.getName());
            if (testFieldParser instanceof CSVFieldTokenizer) {
                ((CSVFieldTokenizer) testFieldParser).setDelimiter(delimiterConfigPanel.getDelimiter());
                ((CSVFieldTokenizer) testFieldParser).setEnclosure(delimiterConfigPanel.getEnclosure());
            }
            testFieldParser.setTestMode(true);
            descriptionTableModel.setTokenizer(testFieldParser);
    	}
    }
    
    private boolean checkParser() {
    	if (testDatasetProvider == null && currentDataFile != null) {
        	setupTestDatasetProvider();
        }
    	if (testFieldParser == null) {
        	setupTestFieldTokenizer();
    	}
        if (testFieldParser != null) {
            if (testFieldParser instanceof CSVFieldTokenizer) {
                ((CSVFieldTokenizer) testFieldParser).setDelimiter(delimiterConfigPanel.getDelimiter());
                ((CSVFieldTokenizer) testFieldParser).setEnclosure(delimiterConfigPanel.getEnclosure());
            }
        }
        return testFieldParser != null && testDatasetProvider != null;
    }
    
    private void showTestData(int direction) {
        if (checkParser()) {
            final Object dataset = readDataset(direction);
            testFieldParser.setRowData(dataset);
            createTestFieldDescriptions();
            refreshTestData();
        }
        this.validate();
    }
    
    private void showTestDataAtRow(final long rowNumber) {
        if (checkParser()) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("showTestDataAtRow rowNumber=" + rowNumber);
        	}
        	if (SwingUtilities.isEventDispatchThread()) {
            	Runnable runnable = new Runnable() {
            		public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                        	public void run() {
                                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                jTextFieldCurrentRowNum.setEnabled(false);
                        	}
                        });
                        final Object dataset = readDatasetAtRowNumber(rowNumber);
                        SwingUtilities.invokeLater(new Runnable() {
                        	public void run() {
                                testFieldParser.setRowData(dataset);
                                createTestFieldDescriptions();
                                refreshTestData();
                    			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                jTextFieldCurrentRowNum.setEnabled(true);
                        	}
                        });
            		}
            	};
            	Thread t = new Thread(runnable);
            	t.start();
        	} else {
                SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        jTextFieldCurrentRowNum.setEnabled(false);
                	}
                });
                final Object dataset = readDatasetAtRowNumber(rowNumber);
                SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                        testFieldParser.setRowData(dataset);
                        createTestFieldDescriptions();
                        refreshTestData();
            			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        jTextFieldCurrentRowNum.setEnabled(true);
                	}
                });
        	}
        }
    }

    private void createTestFieldDescriptions() {
        if (checkParser()) {
            try {
                int countColumns = testFieldParser.countDelimitedFields();
                int countExistingFDs = getDescriptionTableModel().getRowCount();
                if (countColumns != countExistingFDs) {
                    deleteDummyFieldDescriptions();
                    getDescriptionTableModel().createTestFieldDescriptions(countColumns);                    
                }
            } catch (ParserException e) {
                logger.error("createTestFieldDescriptions failed: " + e.getMessage(), e);
            }
        }
    }
    
    private void deleteDummyFieldDescriptions() {
        getDescriptionTableModel().removeTestFieldDescriptions();
    }

    private Object readDataset(int direction) {
        Object dataset = null;
    	if (testDatasetProvider != null) {
            try {
            	long rowNumber = testDatasetProvider.getCurrentRowNum();
                if (direction > 0) {
                	rowNumber++;
                } else if (direction < 0) {
                	if (rowNumber > 0) {
                		rowNumber--;
                	}
                }
            	dataset = testDatasetProvider.getDatasetAtRowInTestMode(rowNumber);
                jTextFieldCurrentRowNum.setText(String.valueOf(testDatasetProvider.getCurrentRowNum() + 1));
            } catch (Exception ioe) {
                logger.error("ImportKonfigurator.readDataset: " + ioe.toString(), ioe);
            }
    	}
        return dataset;
    }
    
    private Object readDatasetAtRowNumber(long rowNumber) {
        Object dataset = null; 
    	if (testDatasetProvider != null) {
            try {
            	dataset = testDatasetProvider.getDatasetAtRowInTestMode(rowNumber);
            	jTextFieldCurrentRowNum.setText(String.valueOf(testDatasetProvider.getCurrentRowNum() + 1));
            } catch (Exception ioe) {
                logger.error("ImportKonfigurator.readDatasetAtRowNumber: " + ioe.toString(), ioe); 
            }
    	}
        return dataset;
    }
    
    private AbstractAction readDatasetAtRowAction = new ReadDatasetAtRowAction();
    
    private class ReadDatasetAtRowAction extends AbstractAction {

    	private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			try {
				long rowNumber = Long.parseLong(jTextFieldCurrentRowNum.getText());
				if (rowNumber == 0) {
					rowNumber = 1;
				}
				showTestDataAtRow(rowNumber - 1);
			} catch (Exception ex) {
				logger.warn("readDatasetAtRowAction failed: " + ex.getClass() + ": " + ex.getMessage());
			}
		}
    	
    }

    /**
     * parsed die aktuelle Datenzeile neu und refreshed die Tabelle
     */
    private void refreshTestData() {
        if (testFieldParser != null && testDatasetProvider != null) {
        	checkParser();
            try {
                long rowsBeforeDataLine = getCountRowsToSkip();
                if (jCheckBoxIgnoreFirstLine.isSelected()) {
                    rowsBeforeDataLine++;
                }
                boolean skipConverting = testDatasetProvider.getCurrentRowNum() < rowsBeforeDataLine;
                testFieldParser.parseRawData(descriptionTableModel.getDescriptions(), skipConverting);
                jLabelCurrentCountColumns.setText(String.valueOf(testFieldParser.countDelimitedFields()));
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
            descriptionTableModel.fireTableRowsUpdated(0,
                    descriptionTableModel.getRowCount() - 1);
            validate();
        }
    }

    private void buttonAdd_actionPerformed() {
        final FieldDescription fd = new FieldDescription();
        fd.setIndex(descriptionTableModel.getRowCount());
        if (editFieldDescription(fd)) {
            descriptionTableModel.addFieldDescription(fd);
        }
    }

    private void buttonEdit_actionPerformed() {
        final int row = jTableFieldDescriptions.getSelectedRow();
        final int col = jTableFieldDescriptions.getSelectedColumn();
        if (row >= 0) {
            final FieldDescription fd = descriptionTableModel.getFieldDescription(row);
            if (col < 5) {
                if (editFieldDescription(fd)) {
                    refreshTestData();
                }
            } else {
                new sqlrunner.editor.TextViewer(this,
                        fd.getName(),
                        (descriptionTableModel.getValueAt(row, col)).toString());
            }
        }
    }

    private boolean editFieldDescription(FieldDescription fd) {
        if (fd == null) {
            throw new IllegalArgumentException("fd cannot be null"); 
        }
        DescriptionEditor descriptionEditor = createDescriptionEditor(fd);
        descriptionEditor.setVisible(true);
        // modales Fenster, weiter wenn Dialog geschlossen !
        if (descriptionEditor.isOkPerformed()) {
            descriptionTableModel.fireTableRowsUpdated(jTableFieldDescriptions.getSelectedRow(),
                    jTableFieldDescriptions.getSelectedRow());
            return true;
        } else {
            return false;
        }
    }

    private DescriptionEditor createDescriptionEditor(FieldDescription fd) {
        DescriptionEditor descriptionEditor = new DescriptionEditor(this, fd);
        WindowHelper.locateWindowAtMiddle(this, descriptionEditor);
        return descriptionEditor;
    }

    private void reorgFieldDescriptionIndex() {
        FieldDescription fd;
        for (int i = 0; i < descriptionTableModel.getRowCount(); i++) {
            fd = descriptionTableModel.getFieldDescription(i);
            fd.setIndex(i);
        }
    }

    private File chooseImportCfgFilename(boolean save) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new ImpPropFileFilter());
        if (configFileName != null) {
            final File cfgFile = new File(configFileName.trim());
            chooser.setSelectedFile(cfgFile);
            final File parentDir = cfgFile.getParentFile();
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        } else if (jTextFieldSourceFileName.getText().trim().length() > 0) {
            final File cfgFile = new File(jTextFieldSourceFileName.getText().trim());
            final File parentDir = cfgFile.getParentFile();
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        } else {
            final File parentDir = new File(Main.getUserProperty("IMPORT_CFGFILE_DIR", 
                    System.getProperty("user.home"))); 
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        }
        int returnVal = -1;
        if (save) {
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setDialogTitle(Messages.getString("ImportConfiguratorFrame.chooseSaveImportConfig")); 
            returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (chooser.getFileFilter() instanceof ImpPropFileFilter && f.getName().toLowerCase().endsWith(IMPORT_CONFIG_EXTENSION) == false) { 
                    return new File(f.getAbsolutePath() + IMPORT_CONFIG_EXTENSION); 
                } else {
                    return f;
                }
            }
        } else {
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setDialogTitle(Messages.getString("ImportConfiguratorFrame.chooseOpenImportConfig")); 
            returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (f.exists()) {
                    if (chooser.getFileFilter() instanceof ImpPropFileFilter && f.getName().toLowerCase().endsWith(IMPORT_CONFIG_EXTENSION) == false) { 
                        return new File(f.getAbsolutePath() + IMPORT_CONFIG_EXTENSION); 
                    } else {
                        return f;
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            f.getAbsolutePath() + Messages.getString("ImportConfiguratorFrame.filenotexists"), 
                            Messages.getString("ImportConfiguratorFrame.titleopenimportconfigfile"), 
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return null;
    }

    private File chooseTalendSchemaFilename(boolean save) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new TalendSchemaFileFilter());
        String fileName = jTextFieldTableName.getText() + ".xml";
        if (configFileName != null) {
            final File cfgFile = new File(configFileName.trim());
            chooser.setSelectedFile(new File(cfgFile.getParentFile(), fileName));
            final File parentDir = cfgFile.getParentFile();
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        } else if (jTextFieldSourceFileName.getText().trim().length() > 0) {
            final File cfgFile = new File(jTextFieldSourceFileName.getText().trim());
            chooser.setSelectedFile(new File(cfgFile.getParentFile(), fileName));
            final File parentDir = cfgFile.getParentFile();
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        } else {
            final File parentDir = new File(Main.getUserProperty("IMPORT_CFGFILE_DIR", 
                    System.getProperty("user.home"))); 
            chooser.setSelectedFile(new File(parentDir, fileName));
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
            }
        }
        int returnVal = -1;
        if (save) {
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setDialogTitle(Messages.getString("ImportConfiguratorFrame.saveasTalendSchema")); 
            returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (chooser.getFileFilter() instanceof TalendSchemaFileFilter && f.getName().toLowerCase().endsWith(".xml") == false) { 
                    return new File(f.getAbsolutePath() + ".xml"); 
                } else {
                    return f;
                }
            }
        } else {
        	// TODO implement open later
        }
        return null;
    }

    private void setConfigFileName(String name) {
        this.configFileName = name;
        if (name == null) {
            setTitle(Messages.getString("ImportConfiguratorFrame.titleunknown")); 
        } else {
            setTitle(Messages.getString("ImportConfiguratorFrame.titleconfigsaved") + configFileName); 
            final File parentDir = (new File(name)).getParentFile();
            Main.setUserProperty("IMPORT_CFGFILE_DIR", parentDir.getAbsolutePath()); 
        }
    }

    private String isDescriptionValid() {
        String errorMessage = null;
        if (jTextFieldTableName.getText().trim().length() == 0) {
            errorMessage = Messages.getString("ImportConfiguratorFrame.errortableunknown"); 
        } else if (descriptionTableModel.getRowCount() == 0) {
            errorMessage = Messages.getString("ImportConfiguratorFrame.errornofielddesc"); 
        } else if ((delimiterConfigPanel.getDelimiterToken()).length() == 0) {
            FieldDescription fd;
            for (int i = 0; i < descriptionTableModel.getRowCount(); i++) {
                fd = descriptionTableModel.getFieldDescription(i);
                if ((fd.getPositionType() == FieldDescription.DELIMITER_POSITION) || (fd.getPositionType() == FieldDescription.DELIMITER_POSITION_WITH_LENGTH)) {
                    errorMessage = Messages.getString("ImportConfiguratorFrame.errornodelimiter"); 
                    break;
                }
            }
        }
        if ((jCheckBoxInsertEnabled.isSelected() == false) && (jCheckBoxUpdateEnabled.isSelected() == false)) {
            errorMessage = Messages.getString("ImportConfiguratorFrame.errornoinsertorupdate"); 
        }
        return errorMessage;
    }

    private boolean saveConfigFile(boolean saveas) {
        // File öffnen und Fd abspeichern
        boolean doit = true;
        boolean ok = false;
        if (saveas) {
            File f = chooseImportCfgFilename(true);
            if (f != null) {
                setConfigFileName(f.getAbsolutePath());
                doit = true;
            } else {
                doit = false;
            }
        } else if (configFileName == null) {
            File f = chooseImportCfgFilename(true);
            if (f != null) {
                setConfigFileName(f.getAbsolutePath());
                doit = true;
            } else {
                doit = false;
            }
        }
        if (doit) {
            try {
                final FileWriter fw = new FileWriter(new File(configFileName));
                fw.append(getConfigurationPropertiesAsString());
                fw.flush();
                fw.close();
                // necessary to recognize next changes
                lastConfiguration = buildProperties();
                ok = true;
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(this,
                        ioe.getMessage(),
                        Messages.getString("ImportConfiguratorFrame.messagetitlesaveconfig"), 
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        return ok;
    }

    private boolean saveTalendSchemaFile() {
        boolean doit = true;
        boolean ok = false;
        File f = chooseTalendSchemaFilename(true);
        if (f != null) {
            doit = true;
        } else {
            doit = false;
        }
        if (doit) {
            try {
                ArrayList<FieldDescription> listFd = new ArrayList<FieldDescription>();
                for (int i = 0; i < descriptionTableModel.getRowCount(); i++) {
                    FieldDescription fd = descriptionTableModel.getFieldDescription(i);
                    if (fd.isDummy() == false) {
                        listFd.add(fd);
                    }
                }
                final FileWriter fw = new FileWriter(f);
                SchemaUtil su = new SchemaUtil();
                fw.append(su.getSchemaXMLFromFieldDescriptions(listFd));
                fw.flush();
                fw.close();
                ok = true;
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(this,
                        ioe.getMessage(),
                        Messages.getString("ImportConfiguratorFrame.messagetitlesaveconfig"), 
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        return ok;
    }

    /**
     * This method initializes importConfigMenuBar	
     * 	
     * @return javax.swing.JMenuBar	
     */
    private JMenuBar getImportConfigMenuBar() {
        if (importConfigMenuBar == null) {
            importConfigMenuBar = new JMenuBar();
            importConfigMenuBar.add(getJMenuFile());
        }
        return importConfigMenuBar;
    }

    /**
     * This method initializes jMenuFile	
     * 	
     * @return javax.swing.JMenu	
     */
    private JMenu getJMenuFile() {
        if (jMenuFile == null) {
            jMenuFile = new JMenu();
            jMenuFile.setText(Messages.getString("ImportConfiguratorFrame.file")); 
            jMenuFile.add(getJMenuItemFileNew());
            jMenuFile.add(getJMenuItemFileOpen());
            jMenuFile.add(getJMenuItemFileSave());
            jMenuFile.add(getJMenuItemFileSaveAs());
            jMenuFile.addSeparator();
            jMenuFile.add(getJMenuItemFileSaveAsTalendSchema());
            jMenuFile.addSeparator();
            jMenuFile.add(getJMenuItemClose());
        }
        return jMenuFile;
    }

    /**
     * This method initializes jMenuItemFileNew	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getJMenuItemFileNew() {
        if (jMenuItemFileNew == null) {
            jMenuItemFileNew = new JMenuItem();
            jMenuItemFileNew.setText(Messages.getString("ImportConfiguratorFrame.new")); 
            jMenuItemFileNew.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (checkSaveConfig()) {
                        clearConfiguration();
                    }
                }
            });
        }
        return jMenuItemFileNew;
    }

    /**
     * This method initializes jMenuItemFileOpen	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getJMenuItemFileOpen() {
        if (jMenuItemFileOpen == null) {
            jMenuItemFileOpen = new JMenuItem();
            jMenuItemFileOpen.setText(Messages.getString("ImportConfiguratorFrame.open")); 
            jMenuItemFileOpen.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    chooseAndOpenConfigfile();
                }
            });
        }
        return jMenuItemFileOpen;
    }

    /**
     * This method initializes jMenuItemFileSave	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getJMenuItemFileSave() {
        if (jMenuItemFileSave == null) {
            jMenuItemFileSave = new JMenuItem();
            jMenuItemFileSave.setText(Messages.getString("ImportConfiguratorFrame.save")); 
            jMenuItemFileSave.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    saveConfigFile(false);
                }
            });
        }
        return jMenuItemFileSave;
    }

    /**jMenuItemFileSaveAsTalendSchema
     * This method initializes jMenuItemFileSaveAs	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getJMenuItemFileSaveAs() {
        if (jMenuItemFileSaveAs == null) {
            jMenuItemFileSaveAs = new JMenuItem();
            jMenuItemFileSaveAs.setText(Messages.getString("ImportConfiguratorFrame.saveas")); 
            jMenuItemFileSaveAs.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    saveConfigFile(true);
                }
            });
        }
        return jMenuItemFileSaveAs;
    }
    
    /**jMenuItemFileSaveAsTalendSchema
     * This method initializes jMenuItemFileSaveAs	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getJMenuItemFileSaveAsTalendSchema() {
        if (jMenuItemFileSaveAsTalendSchema == null) {
        	jMenuItemFileSaveAsTalendSchema = new JMenuItem();
        	jMenuItemFileSaveAsTalendSchema.setText(Messages.getString("ImportConfiguratorFrame.saveasTalendSchema")); 
        	jMenuItemFileSaveAsTalendSchema.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                	saveTalendSchemaFile();
                }
            });
        }
        return jMenuItemFileSaveAsTalendSchema;
    }

    /**
     * This method initializes jMenuItemFileSaveAs	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getJMenuItemClose() {
        if (jMenuItemClose == null) {
        	jMenuItemClose = new JMenuItem();
        	jMenuItemClose.setText(Messages.getString("ImportConfiguratorFrame.close"));
        	jMenuItemClose.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                	if (close()) {
                        dispose();
                	}
                }
            });
        }
        return jMenuItemClose;
    }

    private JComboBox getJComboBoxSortFieldDescriptions() {
    	if (jComboBoxSortFieldDescriptions == null) {
    		jComboBoxSortFieldDescriptions = new JComboBox();
    		jComboBoxSortFieldDescriptions.addItem(new FieldDescriptionComparatorByIndex());
    		jComboBoxSortFieldDescriptions.addItem(new FieldDescriptionComparatorByPosition());
    		jComboBoxSortFieldDescriptions.addItem(new FieldDescriptionComparatorByName());
    		jComboBoxSortFieldDescriptions.addItemListener(new ItemListener() {
				
				@SuppressWarnings("unchecked")
				public void itemStateChanged(ItemEvent e) {
					if (e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getStateChange() == ItemEvent.SELECTED) {
						Comparator<FieldDescription> c = (Comparator<FieldDescription>) jComboBoxSortFieldDescriptions.getSelectedItem(); 
						if (c != null) {
							int row = jTableFieldDescriptions.getSelectedRow();
							if (row != -1) {
								FieldDescription fd = descriptionTableModel.getFieldDescription(row);
								descriptionTableModel.sortBy(c);
					            refreshTestData();
					            if (fd != null) {
					            	int newSelectionRow = descriptionTableModel.getRowIndex(fd);
					                jTableFieldDescriptions.setRowSelectionInterval(newSelectionRow, newSelectionRow);
					                Rectangle r = jTableFieldDescriptions.getCellRect(newSelectionRow, 0, false);
					                jTableFieldDescriptions.scrollRectToVisible(r);
					            }
							}
						}
					}
				}
			});
    	}
    	return jComboBoxSortFieldDescriptions;
    }
    
    private static class FieldDescriptionComparatorByPosition implements Comparator<FieldDescription>, Serializable {

		private static final long serialVersionUID = 1L;

		public int compare(FieldDescription o1, FieldDescription o2) {
			if (o1.isDummy() && o2.isDummy() == false) {
				return 1;
			} else if (o1.isDummy() == false && o2.isDummy()) {
				return -1;
			} else if (o1.isDummy() == false && o2.isDummy() == false) {
				if (o1.getPositionType() != o2.getPositionType()) {
					return o1.getPositionType() - o2.getPositionType();
				} else if (o1.getPositionType() == FieldDescription.DELIMITER_POSITION || o1.getPositionType() == FieldDescription.DELIMITER_POSITION_WITH_LENGTH) {
					return o1.getDelimPos() - o2.getDelimPos();
				} else {
					return o1.getAbsPos() - o2.getAbsPos();
				}
			} else {
				return 0;
			}
		}
    
		public String toString() {
			return Messages.getString("ImportConfiguratorFrame.sortFdByPosition");
		}
		
    }

    private static class FieldDescriptionComparatorByName implements Comparator<FieldDescription>, Serializable {

		private static final long serialVersionUID = 1L;

		public int compare(FieldDescription o1, FieldDescription o2) {
			if (o1.isDummy() && o2.isDummy() == false) {
				return 1;
			} else if (o1.isDummy() == false && o2.isDummy()) {
				return -1;
			} else if (o1.isDummy() == false && o2.isDummy() == false) {
				if (o1.getName() != null && o2.getName() != null) {
					return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
				} else {
					return 0;
				}
			} else {
				return 0;
			}
		}
    
		public String toString() {
			return Messages.getString("ImportConfiguratorFrame.sortFdByName");
		}
		
    }
    
    private static class FieldDescriptionComparatorByIndex implements Comparator<FieldDescription>, Serializable {

		private static final long serialVersionUID = 1L;

		public int compare(FieldDescription o1, FieldDescription o2) {
			if (o1.isDummy() && o2.isDummy() == false) {
				return 1;
			} else if (o1.isDummy() == false && o2.isDummy()) {
				return -1;
			} else if (o1.isDummy() == false && o2.isDummy() == false) {
				return o1.getIndex() - o2.getIndex();
			} else {
				return 0;
			}
		}
    
		public String toString() {
			return Messages.getString("ImportConfiguratorFrame.sortFdByIndex");
		}
		
    }

} // @jve:decl-index=0:visual-constraint="10,10"
