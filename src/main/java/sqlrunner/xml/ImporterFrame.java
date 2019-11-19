package sqlrunner.xml;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.xml.sax.SAXException;

import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import sqlrunner.LongRunningAction;
import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.datamodel.SQLDataModel;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.dbext.DatabaseExtension;
import sqlrunner.dbext.DatabaseExtensionFactory;
import sqlrunner.flatfileimport.Importer;
import sqlrunner.flatfileimport.gui.ImportProgressPanel;
import sqlrunner.resources.ApplicationIcons;
import sqlrunner.swinghelper.WindowHelper;

/**
 * @author jan
 *
 * GUI für die Konfiguraton und Durchfährung des XML-Importes
 */
public final class ImporterFrame extends JFrame implements ActionListener {
    
    private static final Logger logger = Logger.getLogger(ImporterFrame.class);

    private static final long serialVersionUID = 1L;
    // enthält alle Tabellen und Views des aktuellen Datenbanknutzes
    private JTabbedPane      jTabbedPane              = null;
    private JPanel           mainPanel                = null;
    private ImportProgressPanel importProgressPanel   = null; 
    private JPanel           panelSchema              = null;
    private JLabel           labelSchema              = null;
    private JTextField       jTextFieldSchema         = null;
    private JPanel jContentPane = null;
    private transient TableTableModel tableModel               = null;
    private JTable          jTable                   = null;
    private JScrollPane     scrollPaneList           = null;
    private JPanel          panelTop                 = null;
    private JPanel          panelTableButtons        = null;
    private JPanel          panelTables              = null;
    private final JButton         buttonReload             = new JButton();
    private final JButton         buttonRemove             = new JButton();
    private final JButton         buttonUp                 = new JButton();
    private final JButton         buttonDown               = new JButton();
    private JPanel                panelFile                = null;
    private final JTextField      textFieldTargetDirectory = new JTextField();
    private final JButton         buttonOpenFile           = new JButton();
    private final JCheckBox       checkBoxDeleteBefore     = new JCheckBox();
    private final JCheckBox       checkBoxUpdateEnabled    = new JCheckBox();
    private final JCheckBox       checkBoxTestOnly         = new JCheckBox();
    private final JLabel          jLabelFile               = new JLabel();
    private JPanel                panelButton              = null;
    private final JButton         buttonStart              = new JButton();
    private final JButton         buttonInterrupt          = new JButton();
    private final JButton         buttonClose              = new JButton();
    private final StatusBar       status                   = new StatusBar();
    private transient DatabaseSession       session = null;
    private ConnectionDescription cd;
    private transient XMLImporter           importer = null;
    static final String           fileSeparator            = "/"; //$NON-NLS-1$
    private transient SQLDataModel          sqlDataModel;

    /**
     * This is the default constructor
     */
    public ImporterFrame(MainFrame mainFrame) {
        super();
        logger.debug("ImporterFrame is opening...");
        initialize();
        pack();
        this.session = new DatabaseSession();
        this.cd = mainFrame.getDatabase().getDatabaseSession().getConnectionDescription();
        this.session.setConnectionDescription(cd);
        this.session.connect();
        if (session.isConnected()) {
            status.infoActionLabel.setText("CONN"); //$NON-NLS-1$
            status.infoActionLabel.setToolTipText(cd.toString());
        }
        DatabaseExtension ext = DatabaseExtensionFactory.getDatabaseExtension(cd);
        sqlDataModel = new SQLDataModel(cd);
        setTitle(Messages.getString("ImporterFrame.title") + " " + cd.toString()); //$NON-NLS-1$ //$NON-NLS-2$
        WindowHelper.locateWindowAtMiddle(mainFrame, this);
        setVisible(true);
    }

    @Override
    public void setVisible(boolean visible) {
        if (isShowing() == false) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    private void initialize() {
    	this.setContentPane(getJContentPane());
        setTitle(Messages.getString("ImporterFrame.title")); //$NON-NLS-1$
    }
    
    private JPanel getJContentPane() {
    	if (jContentPane == null) {
    		jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 0;
            gb.fill = GridBagConstraints.BOTH;
            gb.weightx = 1;
            gb.weighty = 1;
            jContentPane.add(getJTabbedPane(), gb);
            gb.gridx = 0;
            gb.gridy = 1;
            gb.fill = GridBagConstraints.HORIZONTAL;
            gb.weightx = 1;
            gb.weighty = 0;
            gb.anchor = GridBagConstraints.SOUTH;
            jContentPane.add(status, gb);
    	}
    	return jContentPane;
    }
    
    private JTabbedPane getJTabbedPane() {
        if (jTabbedPane == null) {
            jTabbedPane = new JTabbedPane();
            jTabbedPane.add(Messages.getString("ImporterFrame.mainpanel"), getMainPanel());
            jTabbedPane.add(Messages.getString("ImporterFrame.progresspanel"), getImportProgressPanel());
        }
        return jTabbedPane;
    }
    
    private JPanel getMainPanel() {
        if (mainPanel == null) {
            mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 0;
            gb.fill = GridBagConstraints.BOTH;
            gb.weightx = 1.0;
            gb.weighty = 1.0;
            mainPanel.add(getPanelTop(), gb);
            gb.gridx = 0;
            gb.gridy =10;
            gb.fill = GridBagConstraints.HORIZONTAL;
            gb.weightx = 1.0;
            gb.weighty = 0.0;
            mainPanel.add(getPanelButton(), gb);
        }
        return mainPanel;
    }
    
    private ImportProgressPanel getImportProgressPanel() {
        if (importProgressPanel == null) {
            importProgressPanel = new ImportProgressPanel();
        }
        return importProgressPanel;
    }
    
    private JPanel getPanelButton() {
    	if (panelButton == null) {
    		panelButton = new JPanel();
            buttonStart.setText(Messages.getString("ImporterFrame.start")); //$NON-NLS-1$
            buttonStart.addActionListener(this);
            checkBoxTestOnly.setText(Messages.getString("ImporterFrame.testonly")); //$NON-NLS-1$
            buttonInterrupt.setText(Messages.getString("ImporterFrame.cancel")); //$NON-NLS-1$
            buttonInterrupt.addActionListener(this);
            buttonInterrupt.setEnabled(false);
            buttonClose.setText(Messages.getString("ImporterFrame.close")); //$NON-NLS-1$
            buttonClose.addActionListener(this);
            panelButton.add(buttonStart);
            panelButton.add(checkBoxTestOnly);
            panelButton.add(buttonInterrupt);
            panelButton.add(buttonClose);
    	}
    	return panelButton;
    }
    
    private JPanel getPanelFile() {
    	if (panelFile == null) {
    		panelFile = new JPanel();
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(2, 2, 2, 2);
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.gridx = 2;
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.gridy = 1;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.gridwidth = 3;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.insets = new Insets(2, 2, 2, 2);
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.gridy = 2;
            gridBagConstraints3.anchor = GridBagConstraints.WEST;
            gridBagConstraints3.gridwidth = 3;
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.insets = new Insets(2, 2, 2, 2);
            gridBagConstraints4.gridy = 0;
            gridBagConstraints4.anchor = GridBagConstraints.EAST;
            gridBagConstraints4.gridx = 0;
            buttonOpenFile.addActionListener(this);
            buttonOpenFile.setIcon(ApplicationIcons.OPEN_GIF); //$NON-NLS-1$
            checkBoxDeleteBefore.setText(Messages.getString("ImporterFrame.deletedatasetsbefore")); //$NON-NLS-1$
            checkBoxUpdateEnabled.setText(Messages.getString("ImporterFrame.updateenabled")); //$NON-NLS-1$
            jLabelFile.setHorizontalAlignment(SwingConstants.LEFT);
            jLabelFile.setText(Messages.getString("ImporterFrame.directory")); //$NON-NLS-1$
            panelFile.setBorder(new TitledBorder(
                    BorderFactory.createEtchedBorder(Color.white, new Color(142, 142, 142)),
                    Messages.getString("ImporterFrame.sourcedir"))); //$NON-NLS-1$
            panelFile.setLayout(new GridBagLayout());
            panelFile.add(textFieldTargetDirectory, gridBagConstraints);
            panelFile.add(buttonOpenFile, gridBagConstraints1);
            panelFile.add(checkBoxDeleteBefore, gridBagConstraints2);
            panelFile.add(checkBoxUpdateEnabled, gridBagConstraints3);
            panelFile.add(jLabelFile, gridBagConstraints4);
    	}
    	return panelFile;
    }
    
    private JPanel getPanelTop() {
    	if (panelTop == null) {
    		panelTop = new JPanel();
            panelTop.setLayout(new GridBagLayout());
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 0;
            gb.fill = GridBagConstraints.HORIZONTAL;
            gb.weightx = 1;
            panelTop.add(getPanelSchema(), gb);
            gb.gridy = 1;
            panelTop.add(getPanelFile(), gb);
            gb.gridy = 2;
            gb.fill = GridBagConstraints.BOTH;
            gb.weighty = 1;
            panelTop.add(getPanelTables(), gb);
    	}
    	return panelTop;
    }
    
    private JPanel getPanelSchema() {
    	if (panelSchema == null) {
    		panelSchema = new JPanel();
    		panelSchema.setLayout(new GridBagLayout());
    		GridBagConstraints constraint1 = new GridBagConstraints();
    		constraint1.anchor = GridBagConstraints.EAST;
    		constraint1.gridx = 0;
    		constraint1.gridy = 0;
    		constraint1.insets = new Insets(5, 5, 2, 2);
    		GridBagConstraints constraint2 = new GridBagConstraints();
    		constraint2.anchor = GridBagConstraints.CENTER;
    		constraint2.gridx = 1;
    		constraint2.gridy = 0;
    		constraint2.fill = GridBagConstraints.HORIZONTAL;
    		constraint2.weightx = 1.0;
    		constraint2.insets = new Insets(5, 2, 2, 5);
    		labelSchema = new JLabel();
    		labelSchema.setText("Schema");
    		jTextFieldSchema = new JTextField();
    		panelSchema.add(labelSchema, constraint1);
    		panelSchema.add(jTextFieldSchema, constraint2);
    	}
    	return panelSchema;
    }
    
    private JPanel getPanelTables() {
    	if (panelTables == null) {
    		panelTables = new JPanel();
            panelTables.setLayout(new GridBagLayout());
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 0;
            gb.fill = GridBagConstraints.BOTH;
            gb.weightx = 1;
            gb.weighty = 1;
            panelTables.add(getScrollPaneList(), gb);
            gb.gridx = 1;
            gb.gridy = 0;
            gb.fill = GridBagConstraints.NONE;
            gb.weightx = 0;
            gb.weighty = 0;
            panelTables.add(getPanelTableButtons(), gb);
            panelTables.setBorder(new TitledBorder(
                    BorderFactory.createEtchedBorder(Color.white, new Color(142, 142, 142)),
                    Messages.getString("ImporterFrame.tables"))); //$NON-NLS-1$
    	}
    	return panelTables;
    }
    
    private JScrollPane getScrollPaneList() {
    	if (scrollPaneList == null) {
    		scrollPaneList = new JScrollPane();
            scrollPaneList.setViewportView(getJTable());
    	}
    	return scrollPaneList;
    }
    
    private JTable getJTable() {
    	if (jTable == null) {
    		jTable = new JTable();
            jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    		jTable.setModel(getTableModel());
    	}
    	return jTable;
    }
    
    private TableTableModel getTableModel() {
    	if (tableModel == null) {
    		tableModel = new TableTableModel();
    	}
    	return tableModel;
    }
    
    private JPanel getPanelTableButtons() {
    	if (panelTableButtons == null) {
    		panelTableButtons = new JPanel();
            GridBagConstraints gridBagConstraints41 = new GridBagConstraints();
            gridBagConstraints41.gridy = 3;
            gridBagConstraints41.anchor = GridBagConstraints.WEST;
            gridBagConstraints41.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints41.gridx = 0;
            GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
            gridBagConstraints31.gridy = 2;
            gridBagConstraints31.anchor = GridBagConstraints.WEST;
            gridBagConstraints31.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints31.gridx = 0;
            GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
            gridBagConstraints21.gridy = 1;
            gridBagConstraints21.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints21.gridx = 0;
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.gridy = 0;
            gridBagConstraints11.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints11.gridx = 0;
            buttonReload.setText(Messages.getString("ImporterFrame.loadtables")); //$NON-NLS-1$
            buttonReload.addActionListener(this);
            buttonRemove.setText(Messages.getString("ImporterFrame.remove")); //$NON-NLS-1$
            buttonRemove.addActionListener(this);
            buttonUp.setIcon(ApplicationIcons.UP_GIF); //$NON-NLS-1$
            buttonUp.addActionListener(this);
            buttonDown.setIcon(ApplicationIcons.DOWN_GIF); //$NON-NLS-1$
            buttonDown.addActionListener(this);
            panelTableButtons.setLayout(new GridBagLayout());
            panelTableButtons.add(buttonReload, gridBagConstraints11);
            panelTableButtons.add(buttonRemove, gridBagConstraints21);
            panelTableButtons.add(buttonUp, gridBagConstraints31);
            panelTableButtons.add(buttonDown, gridBagConstraints41);
        }
    	return panelTableButtons;
    }

    @Override
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonOpenFile) {
            // neue Datei öffnen
            final JFileChooser chooser = new JFileChooser();
            final String directory = Main.getUserProperty(
                    "IMPORT_DATAFILE_DIR", //$NON-NLS-1$
                    System.getProperty("user.home")); //$NON-NLS-1$
            File previousDir = new File(directory);
            File parentDir = previousDir.getParentFile();
            if (parentDir != null) {
                chooser.setCurrentDirectory(parentDir);
                chooser.setSelectedFile(previousDir);
            }
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle(Messages.getString("ImporterFrame.choosesourcedir")); //$NON-NLS-1$
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // Note: source for ExampleFileFilter can be found in FileChooserDemo,
            // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
            final int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                final File f = chooser.getSelectedFile();
                Main.setUserProperty("IMPORT_DATAFILE_DIR", f.toString()); //$NON-NLS-1$
                textFieldTargetDirectory.setText(f.getAbsolutePath());
                if (tableModel.getRowCount() > 0) {
                    status.messageLabel.setText(Messages.getString("ImporterFrame.findmatchingxmlfiles")); //$NON-NLS-1$
                    lookupXmlFiles();
                    status.messageLabel.setText(Messages.getString("ImporterFrame.ready")); //$NON-NLS-1$
                }
            }
        } else if (e.getSource() == buttonInterrupt) {
            if ((importer != null) && importer.isAlive()) {
                importer.interrupt();
            }
        } else if (e.getSource() == buttonStart) {
            if ((textFieldTargetDirectory.getText() != null) && (textFieldTargetDirectory.getText().trim().length() > 0)) {
                importer = new XMLImporter(session, textFieldTargetDirectory.getText().trim());
                jTabbedPane.setSelectedIndex(1);
                getImportProgressPanel().setImporter(importer);
                getImportProgressPanel().startMonitoring();
                importer.start();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Messages.getString("ImporterFrame.choosesourcedir"), //$NON-NLS-1$
                        Messages.getString("ImporterFrame.startimport"), //$NON-NLS-1$
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (e.getSource() == buttonReload) {
            final MetaDataLoader loader = new MetaDataLoader(sqlDataModel, tableModel, jTextFieldSchema.getText());
            loader.start();
        } else if (e.getSource() == buttonRemove) {
            tableModel.removeSelected();
        } else if (e.getSource() == buttonUp) {
            tableModel.moveUp(jTable.getSelectedRow());
        } else if (e.getSource() == buttonDown) {
            tableModel.moveDown(jTable.getSelectedRow());
        } else if (e.getSource() == buttonClose) {
            close();
        }
    }
    
    public boolean close() {
        if (importer != null && importer.isRunning()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    Messages.getString("ImporterFrame.continueclosequestion"), //$NON-NLS-1$
                    Messages.getString("MainFrame.closequestiontitle"), //$NON-NLS-1$
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                importer.abort();
            } else {
                return false;
            }
        }
        if (session != null) {
            session.close();
        }
        dispose();
        return true;
    } 

    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()) {
            case WindowEvent.WINDOW_CLOSING: {
                if (close() == false) {
                    break;
                }
            }
            default:
                super.processWindowEvent(e);
        }
    }
    
    /**
     * sieht nach ob in dem gewählten Verzeichnis für die 
     * Tabellen namensgleiche XML-Files vorhanden sind
     * und setzt in den ImportConfigurationen die Dateinamen
     */
    private void lookupXmlFiles() {
        ImportDescription conf = null;
        final String directory = textFieldTargetDirectory.getText();
        File xmlFile = null;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            conf = tableModel.getImportDescriptionAt(i);
            xmlFile = new File(directory + fileSeparator + conf.getTable().getName() + ".xml"); //$NON-NLS-1$
            if (xmlFile.exists()) {
                conf.setXmlFile(xmlFile);
            } else {
            	xmlFile = new File(directory + fileSeparator + conf.getTable().getAbsoluteName() + ".xml"); //$NON-NLS-1$
            }
            if (xmlFile.exists()) {
                conf.setXmlFile(xmlFile);
            }
        }
        tableModel.fireTableRowsUpdated(0, tableModel.getRowCount());
    }

    static class StatusBar extends JPanel {

        private static final long serialVersionUID = 1L;
        public JLabel    messageLabel            = new FixedLabel();
        public JLabel    infoActionLabel         = new FixedLabel();
        static final int INFO_ACTION_WIDTH       = 60;
        static final int STATUS_HEIGHT           = 25;

        public StatusBar() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            messageLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            messageLabel.setForeground(Color.black);
            infoActionLabel.setPreferredSize(new Dimension(INFO_ACTION_WIDTH, STATUS_HEIGHT));
            infoActionLabel.setOpaque(true);
            infoActionLabel.setText("DISC"); //$NON-NLS-1$
            infoActionLabel.setToolTipText(Messages.getString("ImporterFrame.notconnected")); //$NON-NLS-1$
            infoActionLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            infoActionLabel.setForeground(Color.black);
            add(messageLabel);
            add(infoActionLabel);
            messageLabel.addPropertyChangeListener(new MeldungPropertyChangeListener());
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final int meldungBreite = this.getWidth() - INFO_ACTION_WIDTH;
            messageLabel.setPreferredSize(new Dimension(meldungBreite, STATUS_HEIGHT));
            remove(messageLabel); // Label entfernen
            add(messageLabel, null, 0); // neu hinzufügen, damit neu eingepasst
            doLayout(); // damit wird die Änderung sofort wirksam !
        }

        private static class FixedLabel extends JLabel {

            private static final long serialVersionUID = 1L;

            public FixedLabel() {
                super();
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

        }

        private class MeldungPropertyChangeListener implements PropertyChangeListener {

            @Override
			public void propertyChange(PropertyChangeEvent evt) {
                messageLabel.setToolTipText(messageLabel.getText());
            }
        }

    }
    
    public void scrollTableToCell(int row) {
        final int y = row * jTable.getRowHeight();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
			public void run() {
                scrollPaneList.getVerticalScrollBar().setValue(y);
            }
            }); // muss verzögert ausgeführt werden !
    }


    private class TableTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private final Vector<ImportDescription> impDescs = new Vector<ImportDescription>();

        @Override
		public int getRowCount() {
            return impDescs.size();
        }

        @Override
		public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Messages.getString("ImporterFrame.table"); //$NON-NLS-1$
                case 1:
                    return Messages.getString("ImporterFrame.xmlfile"); //$NON-NLS-1$
                default:
                    return ""; //$NON-NLS-1$
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                default:
                    return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /**
         *
         * @param rowIndex -
         * @param columnIndex
         * @return
         */
        @Override
		public Object getValueAt(int rowIndex, int columnIndex) {
            final ImportDescription impDesc = impDescs.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return impDesc.getTable().getName();
                case 1: {
                    if (impDesc.getXmlFile() != null) {
                        return impDesc.getXmlFile().getName();
                    } else {
                        return ""; //$NON-NLS-1$
                    }
                }
                default:
                    return null;
            }
        }

        public ImportDescription getImportDescriptionAt(int rowIndex) {
            return impDescs.get(rowIndex);
        }

        public void add(ImportDescription desc) {
            impDescs.add(desc);
            fireTableRowsInserted(impDescs.size(), impDescs.size());
        }

        public void add(int pos, ImportDescription desc) {
            impDescs.add(pos, desc);
            fireTableRowsInserted(pos, pos);
        }

        public void moveUp(int currPos) {
            if (currPos != -1) {
                final ImportDescription desc = impDescs.get(currPos);
                if (currPos > 0) {
                    removeAt(currPos);
                    currPos--;
                    add(currPos, desc);
                    jTable.setRowSelectionInterval(currPos, currPos);
                    scrollTableToCell(currPos);
                }
            }
        }

        public void moveDown(int currPos) {
            if (currPos != -1) {
                final ImportDescription desc = impDescs.get(currPos);
                if (currPos < impDescs.size() - 1) {
                    removeAt(currPos);
                    currPos++;
                    add(currPos, desc);
                    jTable.setRowSelectionInterval(currPos, currPos);
                    scrollTableToCell(currPos);
                }
            }
        }

        public void removeAt(int index) {
            impDescs.remove(index);
            fireTableRowsDeleted(index, index);
        }

        public void removeSelected() {
            final int[] indexArray = jTable.getSelectedRows();
            for (int i = 0; i < indexArray.length; i++) {
                removeAt(indexArray[i]);
            }
        }

        public void clear() {
            final int size = impDescs.size();
            impDescs.clear();
            fireTableRowsDeleted(0, size);
        }

    }

    private class MetaDataLoader extends Thread {

        private SQLDataModel    sqlDataModel;
        private TableTableModel tableModel;
        private String          currSchemaName;

        MetaDataLoader(final SQLDataModel sqlDataModel, final TableTableModel tableModel, String currSchemaName) {
            this.sqlDataModel = sqlDataModel;
            this.tableModel = tableModel;
            this.currSchemaName = currSchemaName;
        }

        @Override
        public void run() {
            logger.info("load metadata...");
            status.infoActionLabel.setBackground(Color.red);
            status.messageLabel.setText(Messages.getString("ImporterFrame.deleteoldlist")); //$NON-NLS-1$
            if (logger.isDebugEnabled()) {
            	logger.debug("clear previous list..."); 
            }
            tableModel.clear();
            status.messageLabel.setText(Messages.getString("ImporterFrame.loadschemas")); //$NON-NLS-1$
            if (logger.isDebugEnabled()) {
            	logger.debug("load catalogs+schemas...");
            }
            sqlDataModel.loadCatalogs();
            status.messageLabel.setText(Messages.getString("ImporterFrame.38"));
            if (logger.isDebugEnabled()) {
                logger.debug("load tables...");
            }
            final SQLSchema sqlSchema = sqlDataModel.getSchema(currSchemaName);
            sqlDataModel.loadTables(sqlSchema);
            /* it takes to much time for all tables
            if (logger.isDebugEnabled()) {
            	logger.debug("sort tables..."); //$NON-NLS-1$
            }
            SQLCodeGenerator.completeTableAndColumnsAndSort(sqlSchema);
            if (logger.isDebugEnabled()) {
            	logger.debug("show tables..."); 
            }
            */
            SQLTable sqlTable = null;
            ImportDescription impDesc = null;
            for (int i = 0; i < sqlSchema.getTableCount(); i++) {
                sqlTable = sqlSchema.getTableAt(i);
                if (sqlTable.getType().equals(SQLTable.TYPE_VIEW) == false) {
                    impDesc = new ImportDescription();
                    impDesc.setTable(sqlTable);
                    tableModel.add(impDesc);
                    if (logger.isDebugEnabled()) {
                    	logger.debug("add table "+impDesc); //$NON-NLS-1$
                    }
                }
            }
            if ((textFieldTargetDirectory.getText() != null) && (textFieldTargetDirectory.getText().length() > 1)) {
                status.messageLabel.setText(Messages.getString("ImporterFrame.findmatchingxmlfiles")); //$NON-NLS-1$
                if (logger.isDebugEnabled()) {
                	logger.debug("find matching XML files..."); //$NON-NLS-1$
                }
                lookupXmlFiles();
            }
            status.messageLabel.setText(Messages.getString("ImporterFrame.ready")); //$NON-NLS-1$
            if (logger.isDebugEnabled()) {
            	logger.debug("ready"); //$NON-NLS-1$
            }
            status.infoActionLabel.setBackground(Color.lightGray);
        }

    }

    final private class XMLImporter extends Thread implements Importer {

        private Logger importLogger = null;
        private DatabaseSession session;
        private TableImportHandler importHandle = null;
        private String currentAction = null;
        private long countAll = 0;
        private long startTime;
        private long stopTime;
        private int statusCode = Importer.NOT_STARTED;
        private String logFileName = null;
        private FileAppender fileAppender = null;
        private String sourceFileBaseDir = null;

        public XMLImporter(DatabaseSession session, String sourceFileBaseDir) {
            this.session = session;
            this.sourceFileBaseDir = sourceFileBaseDir;
        }

        private FileAppender createFileAppender(String baseDir) throws IOException {
            if (logger.isDebugEnabled()) {
                logger.debug("createFileAppender fileName=" + baseDir);
            }
            SimpleDateFormat sdfLocal = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss"); 
            logFileName = baseDir + "/xml-import-" + sdfLocal.format(new java.util.Date()) + ".log";
            FileAppender appender = new FileAppender();
            appender.setFile(logFileName, false, true, 8000);
            final PatternLayout layout = new PatternLayout();
            layout.setConversionPattern("%d %-5p %m%n");
            appender.setLayout(layout);
            appender.setImmediateFlush(true);
            return appender;
        }
        
        private void closeFileAppender() {
            if (fileAppender != null) {
                fileAppender.close();
                if (importLogger != null) {
                    importLogger.removeAppender(fileAppender);
                }
            }
        }

        private void setupLocalLoggerWithFileAppender(String baseDir) throws IOException {
            fileAppender = createFileAppender(baseDir);
            setupLocalLogger(baseDir).addAppender(fileAppender);
        }

        private Logger setupLocalLogger(String baseDir) {
            File f = new File(baseDir);
            importLogger = Logger.getLogger(getClass().getName() + "-" +f.getName());
            return importLogger;
        }

        @Override
        public void run() {
            getRootPane().putClientProperty("windowModified", Boolean.TRUE); //$NON-NLS-1$
            logger.debug("xml import is starting...");
            reset();
            try {
                setupLocalLoggerWithFileAppender(sourceFileBaseDir);
                session.setLogger(importLogger);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            disableEvents(AWTEvent.WINDOW_EVENT_MASK);
            final LongRunningAction lra = new LongRunningAction() {

                @Override
				public String getName() {
                    return "XML Import";
                }

                @Override
				public void cancel() {

                }

                @Override
				public boolean canBeCanceled() {
                    return false;
                }

            };
            MainFrame.addLongRunningAction(lra);
            try {
                buttonStart.setEnabled(false);
                buttonInterrupt.setEnabled(true);
                buttonClose.setEnabled(false);
                checkBoxUpdateEnabled.setEnabled(false);
                checkBoxDeleteBefore.setEnabled(false);
                checkBoxTestOnly.setEnabled(false);
                status.infoActionLabel.setBackground(Color.red);
                if (checkBoxDeleteBefore.isSelected()) {
                    for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
                        if (isInterrupted()) {
                            break;
                        }
                        ImportDescription impDesc = tableModel.getImportDescriptionAt(i);
                        File xmlFile = impDesc.getXmlFile();
                        if ((xmlFile != null) && xmlFile.exists()) {
                            if (checkBoxTestOnly.isSelected() == false) {
                                currentAction = Messages.getString("ImporterFrame.statusdelete") + impDesc.getTable().getName() + "..."; //$NON-NLS-1$ //$NON-NLS-2$
                                importLogger.info("delete datasets from table " + impDesc.getTable().getName()); //$NON-NLS-1$
                                session.executeUpdate("delete from " + impDesc.getTable().getName()); //$NON-NLS-1$
                                if (session.isSuccessful() == false) {
                                    session.rollback();
                                    importLogger.error("delete all datasets for table " + (impDesc.getTable()).getName() + " failed:" + session.getLastErrorMessage());
                                } else {
                                    session.commit();
                                }
                            } else {
                                currentAction = Messages.getString("ImporterFrame.testOnly") + ' ' + impDesc.getTable().getName();
                            }
                        }
                    }
                }
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (isInterrupted()) {
                        break;
                    }
                    ImportDescription impDesc = tableModel.getImportDescriptionAt(i);
                    File xmlFile = impDesc.getXmlFile();
                    if ((xmlFile != null) && xmlFile.exists()) {
                        if (processImport(impDesc) == false) {
                            break;
                        }
                    }
                }
                buttonStart.setEnabled(true);
                buttonInterrupt.setEnabled(false);
                buttonClose.setEnabled(true);
                checkBoxUpdateEnabled.setEnabled(true);
                checkBoxDeleteBefore.setEnabled(true);
                checkBoxTestOnly.setEnabled(true);
                status.infoActionLabel.setBackground(Color.lightGray);
            } finally {
                MainFrame.removeLongRunningAction(lra);
            }
            enableEvents(AWTEvent.WINDOW_EVENT_MASK);
            logger.debug("xml import are finished");
            closeFileAppender();
            getRootPane().putClientProperty("windowModified", Boolean.FALSE); //$NON-NLS-1$
        }

        public boolean processImport(ImportDescription impDesc) {
            stopTime = 0;
            countAll = 0;
            boolean ok = true;
            importLogger.info("#### import file " + impDesc.getXmlFile().getAbsolutePath() + " in table " + impDesc.getTable());
            try {
                final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
                final TableCountImportHandler counterHandle = new TableCountImportHandler(impDesc);
                counterHandle.setLogger(importLogger);
                importHandle = new TableImportHandler(
                        session,
                        impDesc,
                        checkBoxUpdateEnabled.isSelected(),
                        checkBoxDeleteBefore.isSelected(),
                        checkBoxTestOnly.isSelected());
                importHandle.setLogger(importLogger);
                if (impDesc.getXmlFile() != null) {
                    if (importProgressPanel.isMonitorRunning() == false) {
                        importProgressPanel.startMonitoring();
                    }
                    currentAction = Messages.getString("ImporterFrame.countdatasets") + " " + impDesc.getTable().getName(); //$NON-NLS-1$ //$NON-NLS-2$
                    BufferedInputStream is1 = new BufferedInputStream(new FileInputStream(impDesc.getXmlFile()), 100000);
                    saxParser.parse(is1, counterHandle);
                    is1.close();
                    countAll = counterHandle.getCountCurrDatasets();
                    importLogger.info("file contains " + countAll + " datasets");
                    currentAction = Messages.getString("ImporterFrame.startimport") + " " + impDesc.getTable().getName(); //$NON-NLS-1$ //$NON-NLS-2$
                    startTime = System.currentTimeMillis();
                    BufferedInputStream is2 = new BufferedInputStream(new FileInputStream(impDesc.getXmlFile()), 100000);
                    saxParser.parse(is2, importHandle);
                    is2.close();
                }
            } catch (IOException e1) {
                ok = false;
                statusCode = Importer.FATALS;
                importLogger.error(e1);
            } catch (ParserConfigurationException e) {
                ok = false;
                statusCode = Importer.FATALS;
                importLogger.error(e);
            } catch (SAXStoppedException e) {
                importLogger.info("stop performed " + e.getMessage());
                statusCode = Importer.FATALS;
                ok = false;
            } catch (SAXException e) {
                ok = false;
                statusCode = Importer.FATALS;
                importLogger.error(e);
            } catch (FactoryConfigurationError e) {
                ok = false;
                statusCode = Importer.FATALS;
                importLogger.error(e);
            }
            stopTime = System.currentTimeMillis();
            return ok;
        }

        @Override
		public long getCountMaxInput() {
            return countAll;
        }

        @Override
		public long getCountCurrInput() {
            if (importHandle != null) {
                return importHandle.getCountCurrDatasets();
            } else {
                return 0;
            }
        }

        @Override
		public long getCountInserts() {
            if (importHandle != null) {
                return importHandle.getCountDatasetInserted();
            } else {
                return 0;
            }
        }

        @Override
		public long getCountUpdates() {
            if (importHandle != null) {
                return importHandle.getCountDatasetUpdated();
            } else {
                return 0;
            }
        }

        @Override
		public long getCountIgnored() {
            if (importHandle != null) {
                return importHandle.getCountCurrDatasets() - importHandle.getCountDatasetInserted() - importHandle.getCountDatasetUpdated();
            } else {
                return 0;
            }
        }

        @Override
		public long getStartTime() {
            return startTime;
        }

        @Override
		public long getStopTime() {
            return stopTime;
        }

        @Override
		public boolean isStopped() {
            return stopTime > 0;
        }

        @Override
		public boolean isRunning() {
            return stopTime == 0;
        }

        @Override
		public int getStatusCode() {
            if (importHandle != null) {
                return importHandle.getStatus();
            } else {
                return statusCode;
            }
        }

        @Override
		public void abort() {
            logger.info("abort");
            interrupt();
        }

        @Override
		public String getLogFileName() {
            return null;
        }

        @Override
		public String getCurrentAction() {
            return currentAction;
        }

        public void reset() {
            importHandle = null;
            startTime = 0;
            stopTime = 0;
            countAll = 0;
            currentAction = null;
            statusCode = Importer.NOT_STARTED;
        }

		@Override
		public Object getLastValue(String columnName) {
			// TODO Auto-generated method stub
			return null;
		}

    }

}