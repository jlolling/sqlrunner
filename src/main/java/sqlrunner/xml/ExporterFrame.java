package sqlrunner.xml;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.LogManager;

import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import sqlrunner.DBMessageDialog;
import sqlrunner.LongRunningAction;
import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.base64.Base64;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.dbext.DatabaseExtension;
import sqlrunner.dbext.DatabaseExtensionFactory;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.log4jpanel.Log4J2Util;
import sqlrunner.resources.ApplicationIcons;
import sqlrunner.swinghelper.WindowHelper;

/**
 * Pro Tabelle wird eine XML-Datei erstellt.
 * Im Dataiauswahldialog wird das Verzeichnis für die Dateiablage ausgewählt.
 * @author jan
 * Das Format der XML-Dateien:
 * <table1>
 *   <row>
 *     <column1>
 *       content
 *     </column1>
 *     <column2>
 *       content
 *     </column2>
 *   </row>
 * </table1>
 *
 */
public final class ExporterFrame extends JFrame implements ActionListener {
	
    private static final Logger    logger                   = LogManager.getLogger(ExporterFrame.class);
    private static final long      serialVersionUID         = 1L;
    private ExporterFrame          self;
    private final List<String>     schemas                  = new ArrayList<String>();
    private final List<String>     tables                   = new ArrayList<String>();
    private LargeListModel    listModel                = null;
    private JList<ExportDescription>  list             = null;
    private JScrollPane      scrollPaneList           = null;
    private JPanel           panelSchema              = null;
    private JTextField       jTextFieldSchema         = null;
    private JTextField       jTextFieldFetchSize      = null;
    private JPanel           panelTop                 = null;
    private JPanel           panelTableButtons        = null;
    private JPanel           panelTables              = null;
    private JButton          buttonReload             = null;
    private JButton          buttonRemoveSelected     = null;
    private JButton          buttonRemoveUnselected   = null;
    private JButton          buttonEditSQL            = null;
    private JPanel           panelFile                = null;
    private JTextField       textFieldTargetDirectory = null;
    private JButton          buttonOpenFile           = null;
    private JCheckBox        checkboxBase64Coding     = null;
    private JCheckBox        checkBoxFormatXMLOutput  = null;      
    private JPanel           panelButton              = null;
    private JButton          buttonStart              = null;
    private JButton          buttonInterrupt          = null;
    private JButton          buttonClose              = null;
    private final StatusBar        status                   = new StatusBar();
    private transient XMLExporter  exporter = null;
    private File                   targetDirectory;
    private ConnectionDescription  cd;
    private ExportQueryEditor      queryEditor;
    private JPanel jContentPane = null;
    private boolean enableBinary = false;

    public ExporterFrame(ConnectionDescription cd, Window parent) {
        this.self = this;
        setTitle(Messages.getString("ExporterFrame.title")); //$NON-NLS-1$
        try {
            initComponents();
            pack();
            this.cd = cd;
            WindowHelper.locateWindowAtMiddle(parent, this);
            setVisible(true);
            DatabaseExtensionFactory.getDatabaseExtension(cd);
            SimpleMetaReader reader = new SimpleMetaReader(cd, jTextFieldSchema.getText());
            reader.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean close() {
        if (exporter != null && exporter.isRunning()) {
                	int answer = JOptionPane.showConfirmDialog(
                            this, 
                            Messages.getString("ExporterFrame.interruptQuestion"), 
                            Messages.getString("ExporterFrame.close"), 
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                exporter.abort();
            } else {
                return false;
            }
        }
        dispose();
        return true;
    } 

    @Override
    protected void processWindowEvent(WindowEvent winEvent) {
        switch (winEvent.getID()) {
            case WindowEvent.WINDOW_CLOSING: {
                if (close() == false) {
                    break;
                }
            }
            default:
                super.processWindowEvent(winEvent);
        }
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

    private void initComponents() {
        this.setContentPane(getJContentPane());
    }
    
    private JPanel getJContentPane() {
    	if (jContentPane == null) {
    		jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            {
                GridBagConstraints gb = new GridBagConstraints();
                gb.gridx = 0;
                gb.gridy = 0;
                gb.fill = GridBagConstraints.BOTH;
                gb.weightx = 1;
                gb.weighty = 1;
                jContentPane.add(getPanelTop(), gb);
            }
            {
                GridBagConstraints gb = new GridBagConstraints();
                gb.gridx = 0;
                gb.gridy = 1;
                gb.fill = GridBagConstraints.NONE;
                gb.weightx = 0;
                gb.weighty = 0;
                jContentPane.add(getPanelButton(), gb);
            }
            {
                GridBagConstraints gb = new GridBagConstraints();
                gb.gridx = 0;
                gb.gridy = 2;
                gb.fill = GridBagConstraints.HORIZONTAL;
                gb.weightx = 1;
                gb.weighty = 0;
                gb.anchor = GridBagConstraints.SOUTH;
                jContentPane.add(status, gb);
            }
    	}
    	return jContentPane;
    }
    
    private JPanel getPanelFile() {
    	if (panelFile == null) {
    		panelFile = new JPanel();
            panelFile.setBorder(new TitledBorder(
                    BorderFactory.createEtchedBorder(Color.white, new Color(142, 142, 142)),
                    Messages.getString("ExporterFrame.exporttarget"))); //$NON-NLS-1$
            panelFile.setPreferredSize(new Dimension(390, 100));
            panelFile.setLayout(new GridBagLayout());
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 0;
                JLabel label = new JLabel();
                label.setBackground(Color.black);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setText(Messages.getString("ExporterFrame.directory")); //$NON-NLS-1$
                panelFile.add(label, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.weightx = 1.0;
                gbc.gridwidth = 2;
                gbc.insets = new Insets(2, 2, 2, 2);
                textFieldTargetDirectory = new JTextField();
                panelFile.add(textFieldTargetDirectory, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 3;
                gbc.gridy = 0;
                buttonOpenFile = new JButton();
                buttonOpenFile.addActionListener(this);
                buttonOpenFile.setIcon(ApplicationIcons.OPEN_GIF); //$NON-NLS-1$
                panelFile.add(buttonOpenFile, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 2;
                checkboxBase64Coding = new JCheckBox();
                checkboxBase64Coding.setText(Messages.getString("ExporterFrame.encodeinbase64")); //$NON-NLS-1$
                panelFile.add(checkboxBase64Coding, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 2;
                gbc.gridy = 1;
                gbc.gridwidth = 2;
                checkBoxFormatXMLOutput = new JCheckBox();
                checkBoxFormatXMLOutput.setText(Messages.getString("ExporterFrame.formatxml"));
                panelFile.add(checkBoxFormatXMLOutput, gbc);
            }
    	}
    	return panelFile;
    }
    
    private JPanel getPanelButton() {
    	if (panelButton == null) {
    		panelButton = new JPanel();
    		buttonStart = new JButton();
            buttonStart.setText(Messages.getString("ExporterFrame.start")); //$NON-NLS-1$
            buttonStart.addActionListener(this);
            panelButton.add(buttonStart);
    		buttonInterrupt = new JButton();
            buttonInterrupt.setText(Messages.getString("ExporterFrame.cancel")); //$NON-NLS-1$
            buttonInterrupt.addActionListener(this);
            buttonInterrupt.setEnabled(false);
            panelButton.add(buttonInterrupt);
    		buttonClose = new JButton();
            buttonClose.setText(Messages.getString("ExporterFrame.close")); //$NON-NLS-1$
            buttonClose.addActionListener(this);
            panelButton.add(buttonClose);
    	}
    	return panelButton;
    }
    
    private JPanel getPanelTop() {
    	if (panelTop == null) {
    		panelTop = new JPanel();
    		panelTop.setLayout(new BorderLayout());
    		panelTop.add(getPanelSchema(), BorderLayout.NORTH);
            panelTop.add(getPanelTables(), BorderLayout.CENTER);
            panelTop.add(getPanelFile(), BorderLayout.SOUTH);
    	}
    	return panelTop;
    }
    
    private JPanel getPanelSchema() {
    	if (panelSchema == null) {
    		panelSchema = new JPanel();
    		panelSchema.setLayout(new GridBagLayout());
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel label = new JLabel();
                label.setText("Schema");
                panelSchema.add(label, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                jTextFieldSchema = new JTextField();
                panelSchema.add(jTextFieldSchema, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel label = new JLabel();
                label.setText("fetchSize");
                panelSchema.add(label, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.gridx = 1;
                gbc.gridy = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                jTextFieldFetchSize = new JTextField();
                jTextFieldFetchSize.setText("1000");
                panelSchema.add(jTextFieldFetchSize, gbc);
            }
    	}
    	return panelSchema;
    }
    
    private JPanel getPanelTables() {
    	if (panelTables == null) {
    		panelTables = new JPanel();
            panelTables.setPreferredSize(new java.awt.Dimension(182, 160));
            panelTables.setLayout(new BorderLayout());
            panelTables.add(getScrollPaneList(), BorderLayout.CENTER);
            panelTables.add(getPanelTableButtons(), BorderLayout.EAST);
            panelTables.setBorder(new TitledBorder(
                    BorderFactory.createEtchedBorder(Color.white, new Color(142, 142, 142)),
                    Messages.getString("ExporterFrame.tables"))); //$NON-NLS-1$
    	}
    	return panelTables;
    }
    
    private JPanel getPanelTableButtons() {
    	if (panelTableButtons == null) {
    		panelTableButtons = new JPanel();
            panelTableButtons.setLayout(new GridBagLayout());
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.WEST;
                buttonReload             = new JButton();
                buttonReload.setText(Messages.getString("ExporterFrame.loadtables")); //$NON-NLS-1$
                buttonReload.addActionListener(this);
                panelTableButtons.add(buttonReload, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.WEST;
                buttonRemoveSelected     = new JButton();
                buttonRemoveSelected.setText(Messages.getString("ExporterFrame.removeselected")); //$NON-NLS-1$
                buttonRemoveSelected.addActionListener(this);
                panelTableButtons.add(buttonRemoveSelected, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 2, 2, 2);
                buttonRemoveUnselected   = new JButton();
                buttonRemoveUnselected.setText(Messages.getString("ExporterFrame.removenotselected")); //$NON-NLS-1$
                buttonRemoveUnselected.addActionListener(this);
                panelTableButtons.add(buttonRemoveUnselected, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 3;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 2, 2, 2);
                buttonEditSQL            = new JButton();
                buttonEditSQL.setText(Messages.getString("ExporterFrame.editsql")); //$NON-NLS-1$
                buttonEditSQL.addActionListener(this);
                panelTableButtons.add(buttonEditSQL, gbc);
            }
    	}
    	return panelTableButtons;
    }
    
    private JScrollPane getScrollPaneList() {
    	if (scrollPaneList == null) {
    		scrollPaneList = new JScrollPane();
            scrollPaneList.setViewportView(getList());
    	}
    	return scrollPaneList;
    }
    
    private LargeListModel getListModel() {
    	if (listModel == null) {
    		listModel = new LargeListModel();
    	}
    	return listModel;
    }
    
    private JList<ExportDescription> getList() {
    	if (list == null) {
    		list = new JList<ExportDescription>(getListModel());
    	}
    	return list;
    }

	@Override
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonOpenFile) {
            // neue Datei öffnen
            final JFileChooser chooser = new JFileChooser();
            if (targetDirectory != null) {
                if (targetDirectory.getParentFile() != null) {
                    chooser.setCurrentDirectory(targetDirectory.getParentFile());
                }
            } else {
                final String directory = Main.getUserProperty(
                        "EXPORT_DATAFILE_DIR", //$NON-NLS-1$
                        System.getProperty("user.home")); //$NON-NLS-1$
                File f = new File(directory);
                if (f.getParentFile() != null) {
                    f = f.getParentFile();
                }
                chooser.setCurrentDirectory(f);
            }
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle(Messages.getString("ExporterFrame.chooseexporttarget")); //$NON-NLS-1$
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // Note: source for ExampleFileFilter can be found in FileChooserDemo,
            // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
            final int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                final File f = chooser.getSelectedFile();
                Main.setUserProperty("EXPORT_DATAFILE_DIR", f.toString()); //$NON-NLS-1$
                textFieldTargetDirectory.setText(f.getAbsolutePath());
            }
        } else if (e.getSource() == buttonInterrupt) {
            if (exporter != null && exporter.isAlive()) {
                exporter.interrupt();
            }
        } else if (e.getSource() == buttonStart) {
            if (textFieldTargetDirectory.getText() != null && ((textFieldTargetDirectory.getText()).length() > 0)) {
                exporter = new XMLExporter(
                        this,
                        cd,
                        textFieldTargetDirectory.getText(),
                        checkboxBase64Coding.isSelected());
                exporter.start();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Messages.getString("ExporterFrame.chooseexporttarget"), //$NON-NLS-1$
                        Messages.getString("ExporterFrame.startexport"), //$NON-NLS-1$
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (e.getSource() == buttonReload) {
            listModel.clear();
            new SimpleMetaReader(cd, jTextFieldSchema.getText()).start();
        } else if (e.getSource() == buttonRemoveSelected) {
			final List<ExportDescription> selectedItems = list.getSelectedValuesList();
            for (int i = 0; i < selectedItems.size(); i++) {
                listModel.removeElement(selectedItems.get(i));
            }
        } else if (e.getSource() == buttonRemoveUnselected) {
            final List<ExportDescription> selectedItems = list.getSelectedValuesList();
            final Object[] allItems = listModel.toArray();
            boolean toDelete = false;
            for (int i = 0; i < allItems.length; i++) {
                toDelete = true;
                for (int j = 0; j < selectedItems.size(); j++) {
                    if (selectedItems.get(i) == allItems[i]) {
                        toDelete = false;
                    }
                }
                if (toDelete) {
                    listModel.removeElement(allItems[i]);
                }
            }
        } else if (e.getSource() == buttonEditSQL) {
            final ExportDescription selectedExpDesc = (list.getSelectedValue());
            if (selectedExpDesc != null) {
                queryEditor = new ExportQueryEditor(this);
                queryEditor.setText(selectedExpDesc.getSql());
                queryEditor.setVisible(true);
                // der Dialog ist modal
                if (queryEditor.getReturnCode() == ExportQueryEditor.RETURNCODE_OK) {
                    selectedExpDesc.setSql(queryEditor.getText());
                    list.repaint();
                }
            }
        } else if (e.getSource() == buttonClose) {
            close();
        }
    }

    static class StatusBar extends JPanel {

        private static final long serialVersionUID   = 1L;
        public JLabel             messageLabel            = new FixedLabel();
        public JLabel             infoLabel         = new FixedLabel();
        final static int          INFO_ACTION_BREITE = 60;
        final static int          HOEHE              = 25;

        public StatusBar() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            messageLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            messageLabel.setForeground(Color.black);
            infoLabel.setPreferredSize(new Dimension(INFO_ACTION_BREITE, HOEHE));
            infoLabel.setOpaque(true);
            infoLabel.setText("DISC"); //$NON-NLS-1$
            infoLabel.setToolTipText(Messages.getString("ExporterFrame.notconnected")); //$NON-NLS-1$
            infoLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            infoLabel.setForeground(Color.black);
            add(messageLabel);
            add(infoLabel);
            // die zu erwartenden Property-Änderungen sind die setText()
            // Aufrufe, dann soll der ToolTip aktualisiert werden,
            // da dieser den Textinhalt voll anzeigen kann, auch wenn das Label
            // den Platz dafür nicht hat
            messageLabel.addPropertyChangeListener(new MeldungPropertyChangeListener());
            // eine bislang sinnvolle Nutzung der Spalte InfoAction
            // ist die Anzeige, ob Bak-Dateien erstellt werden oder nicht
        }

        // wird beim erstmaligen Darstellen , bei jeder FormÄnderung
        // und bei Aufrufen von repaint durchlaufen
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final int meldungBreite = this.getWidth() - INFO_ACTION_BREITE;
            messageLabel.setPreferredSize(new Dimension(meldungBreite, HOEHE));
            remove(messageLabel); // Label entfernen
            add(messageLabel, null, 0); // neu hinzufügen, damit neu eingepasst
            doLayout(); // damit wird die Änderung sofort wirksam !
        }

        // diese Klasse sichert, dass die Label auch in einer festen Groesse
        // dargestellt werden
        static private class FixedLabel extends JLabel {

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

        // Klasse stellt fest wenn Text in Meldung sich geaedert hat
        // und setzt als Tooltip den Inhalt des Textes
        private class MeldungPropertyChangeListener implements PropertyChangeListener {

            @Override
			public void propertyChange(PropertyChangeEvent evt) {
                messageLabel.setToolTipText(messageLabel.getText());
            }
        }

    }

    /**
     * diese Klasse liest alle Tabellen und Views des users
     * und speichert diese im Vector tables.
     * @author jan
     */
    private class SimpleMetaReader extends Thread {
    	
        private final Logger    logger = LogManager.getLogger(SimpleMetaReader.class);
        private String          user;
        private DatabaseSession session;

        SimpleMetaReader(ConnectionDescription cd, String schema) {
            this.user = schema;
            session = new DatabaseSession();
            session.setConnectionDescription(cd);
            session.connect();
        }

        @Override
        public void run() {
            buttonReload.setEnabled(false);
            status.infoLabel.setBackground(Color.red);
            final Connection conn = session.getConnection();
            if (conn != null) {
                try {
                    if (conn.isClosed() == false) {
                        final DatabaseMetaData dbmd = conn.getMetaData();
                        if (dbmd != null) {
                            String schema = null;
                            // Schemas einlesen
                            schemas.clear();
                            status.messageLabel.setText(Messages.getString("ExporterFrame.loadschemas")); //$NON-NLS-1$
                            final ResultSet rs = dbmd.getSchemas();
                            if (rs != null) {
                                while (rs.next()) {
                                    schema = rs.getString("TABLE_SCHEM"); //$NON-NLS-1$
                                    schemas.add(schema);
                                }
                                rs.close();
                            }
                            // das aktuelle Schema anhand des usernames erkennen
                            for (int i = 0; i < schemas.size(); i++) {
                                schema = schemas.get(i);
                                if (user.equalsIgnoreCase(schema)) {
                                    break;
                                } else {
                                    schema = null;
                                }
                            }
                            if (schema == null) {
                                schema = ""; // irgendwas muss es ja sein  //$NON-NLS-1$
                            }
                            // Tabellen einlesen
                            String table = null;
                            String type = null;
                            tables.clear();
                            ExportDescription exportDesc = null;
                            final ArrayList<ExportDescription> list = new ArrayList<ExportDescription>();
                            final ResultSet rs1 = dbmd.getTables(null, schema, null, null);
                            if (rs1 != null) {
                                while (rs1.next()) {
                                    table = rs1.getString("TABLE_SCHEM") + "." + rs1.getString("TABLE_NAME"); //$NON-NLS-1$
                                    if (table.indexOf("BIN$") == -1) { //$NON-NLS-1$
                                        type = rs1.getString("TABLE_TYPE"); //$NON-NLS-1$
                                        if (SQLTable.TYPE_VIEW.equalsIgnoreCase(type) == false) {
                                            // keine Views !
                                            exportDesc = new ExportDescription();
                                            exportDesc.setTableName(table);
                                            list.add(exportDesc);
                                            if (logger.isDebugEnabled()) {
                                                logger.debug("add table "+table); //$NON-NLS-1$
                                            }
                                        }
                                    }
                                }
                                rs1.close();
                            }
                            // nun die Liste als ganzen in das ListModel einfügen
                            listModel.addElements(list);
                        }
                    }
                } catch (SQLException sqle) {
                    logger.error(sqle);
                }
            }
            session.close();
            buttonReload.setEnabled(true);
            status.infoLabel.setBackground(Color.lightGray);
        }

    } // class SimpleMetaReader

    static private class ExportDescription {

        private String tableName;
        private String sql;
        private String defaultSql;

        public String getSql() {
            if (sql == null) {
                return defaultSql;
            } else {
                return sql;
            }
        }

        public void setSql(String sql_loc) {
            if (sql_loc != null) {
                sql_loc = sql_loc.trim();
                if (sql_loc.equals(defaultSql) == false && sql_loc.length() > 10) {
                    this.sql = sql_loc;
                } else {
                    this.sql = null;
                }
            } else {
                this.sql = null;
            }
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName_loc) {
            this.tableName = tableName_loc;
            defaultSql = "select * from " + tableName_loc; //$NON-NLS-1$
        }

        @Override
        public String toString() {
            if (sql != null) {
                return "* " + tableName; //$NON-NLS-1$
            } else {
                return tableName;
            }
        }
        
    }

    /**
     * diese Klasse exportiert die Tabellendaten in die XML-Dateien
     * @author jan
     */
    private class XMLExporter extends Thread {

        private Logger exportLogger = null;
        private JFrame           frame;
        private boolean          refreshNow         = false;
        private DatabaseSession  exportSession;
        private String           targetDirectory;
        private boolean          codeStringInBase64 = false;
        private long stopTime = 0;
        private String logFileName = null;
        private FileAppender fileAppender = null;
        boolean ignoreErrors = false;

        /**
         * Instanziert den ExporterThread
         * @param frame Referenz auf den Frame um Statusausgaben zu relalisieren
         * @param session bereits verbundene DatenbankSession
         * @param targetDirectory target directory
         * @param codeStringInBase64 true wenn Strings in BASE64 kodiert werden sollen
         * werden.
         */
        public XMLExporter(JFrame frame,
                ConnectionDescription cd,
                String targetDirectory,
                boolean codeStringInBase64) {
            this.frame = frame;
            int fetchSize = 100;
            try {
                fetchSize = Integer.parseInt(jTextFieldFetchSize.getText());
            } catch (Exception e) {
            	fetchSize = 100;
            	jTextFieldFetchSize.setText(String.valueOf(fetchSize));
            }
            ConnectionDescription cdExport = cd.clone();  
            cdExport.setDefaultFetchSize(fetchSize);
            cdExport.setAutoCommit(false);
            this.exportSession = new DatabaseSession(cdExport);
            this.targetDirectory = targetDirectory;
            this.codeStringInBase64 = codeStringInBase64;
        }

        private FileAppender createFileAppender(String baseDir) throws IOException {
            if (logger.isDebugEnabled()) {
                logger.debug("createFileAppender fileName=" + baseDir);
            }
            SimpleDateFormat sdfLocal = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss"); 
            logFileName = baseDir + "/xml-export-" + sdfLocal.format(new java.util.Date()) + ".log";
            FileAppender appender = Log4J2Util.createFileAppender(logFileName);
            return appender;
        }
        
        private void closeFileAppender() {
            if (fileAppender != null) {
                fileAppender.stop();
                Log4J2Util.clearAppenders(exportLogger, fileAppender.getName());
            }
        }

        private void setupLocalLoggerWithFileAppender(String baseDir) throws IOException {
            fileAppender = createFileAppender(baseDir);
            Log4J2Util.addAppender(setupLocalLogger(baseDir), fileAppender);
        }

        private Logger setupLocalLogger(String baseDir) {
            File f = new File(baseDir);
            exportLogger = LogManager.getLogger(getClass().getName() + "-" +f.getName());
            return exportLogger;
        }

        private void setRefreshNow() {
            refreshNow = true;
        }
        
        private void setIgnoreErrors(boolean ignore) {
        	this.ignoreErrors = ignore;
        }
        
        private boolean isIgnoreErrors() {
        	return ignoreErrors;
        }

        @Override
        public void run() {
            try {
                stopTime = 0;
                try {
                    setupLocalLoggerWithFileAppender(targetDirectory);
                    exportSession.setLogger(exportLogger);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                disableEvents(AWTEvent.WINDOW_EVENT_MASK);
                final LongRunningAction lra = new LongRunningAction() {

                    @Override
					public String getName() {
                        return "XML Export";
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
                    buttonInterrupt.setEnabled(true);
                    buttonStart.setEnabled(false);
                    buttonReload.setEnabled(false);
                    buttonRemoveSelected.setEnabled(false);
                    buttonClose.setEnabled(false);
                    checkboxBase64Coding.setEnabled(false);
                    buttonOpenFile.setEnabled(false);
                    textFieldTargetDirectory.setEnabled(false);
                    buttonRemoveUnselected.setEnabled(false);
                    status.infoLabel.setBackground(Color.red);
                    final int refreshTime = 2000;
                    final javax.swing.Timer timer = new javax.swing.Timer(refreshTime, new java.awt.event.ActionListener() {

                        @Override
						public void actionPerformed(ActionEvent e) {
                            setRefreshNow();
                        }

                    });
                    timer.setRepeats(true); // immer wieder
                    timer.start();
                    for (int t = 0; t < listModel.size(); t++) {
                        if (isInterrupted()) {
                            break;
                        }
                        ExportDescription exportDesc = (ExportDescription) listModel.get(t);
                        String tableName = exportDesc.getTableName();
                        // run export
                        // each table will be exported in its own file
                        XMLWriter xmlWriter = null;
                        status.messageLabel.setText("select " + tableName); //$NON-NLS-1$
                        String sql = exportDesc.getSql();
                        if (sql == null) {
                            sql = "select * from " + tableName; //$NON-NLS-1$
                        }
                        final ResultSet rs = exportSession.executeQuery(sql);
                        if (exportSession.isSuccessful() == false) {
                            if (isIgnoreErrors()) {
                                continue;
                            } else {
                                final DBMessageDialog md = new DBMessageDialog(
                                        frame,
                                        exportSession.getLastErrorMessage(),
                                        "execute Query"); //$NON-NLS-1$
                                if (md.getReturnCode() == DBMessageDialog.CANCEL) {
                                    break;
                                } else if (md.getReturnCode() == DBMessageDialog.INGORE_ERRORS) {
                                    setIgnoreErrors(true);
                                }
                            }
                        } else {
                            try {
                                xmlWriter = new XMLWriter(
                                		new FileOutputStream(
                                				new File(targetDirectory, tableName + ".xml"))); //$NON-NLS-1$
                                // write the document header
                                xmlWriter.setFormatOutput(checkBoxFormatXMLOutput.isSelected());
                                xmlWriter.writeXMLBegin();
                                xmlWriter.writeTagBegin(tableName.toLowerCase(), null);
                            } catch (IOException ioe) {
                                exportLogger.error("XMLExport failed: " + ioe.toString()); //$NON-NLS-1$
                                new DBMessageDialog(frame, ioe.toString(), "create export file"); //$NON-NLS-1$
                                break;
                            }
                            if (rs != null) {
                                try {
                                    final ResultSetMetaData rsmd = rs.getMetaData();
                                    final int cols = rsmd.getColumnCount();
                                    Object[] resultRow = new Object[cols];
                                    int[] types = new int[cols];
                                    // read column name from meta data
                                    String[] columnNames = new String[cols];
                                    for (int i = 0; i < cols; i++) {
                                        columnNames[i] = rsmd.getColumnName(i + 1).toLowerCase();
                                        types[i] = BasicDataType.getBasicTypeByTypes(rsmd.getColumnType(i + 1));
                                    }
                                    int rownum = 0;
                                    while (rs.next()) {
                                        if (isInterrupted()) {
                                            status.messageLabel.setText(Messages.getString("ExporterFrame.abortexport") //$NON-NLS-1$
                                                    + String.valueOf(rownum)
                                                    + " " //$NON-NLS-1$
                                                    + Messages.getString("ExporterFrame.datasetsexported")); //$NON-NLS-1$
                                            break;
                                        } else {
                                            xmlWriter.writeTagBegin("row", null); //$NON-NLS-1$
                                            for (int i = 0; i < cols; i++) {
                                                resultRow[i] = null;
                                                // build a result row dependend on type
                                            	if (BasicDataType.isDateType(types[i])) {
                                            		resultRow[i] = rs.getTimestamp(i + 1);
                                            	} else if (BasicDataType.isNumberType(types[i])) {
                                            		resultRow[i] = rs.getString(i + 1);
                                            	} else if (BasicDataType.CLOB.getId() == types[i]) {
                                                    if (rs.getObject(i + 1) != null) {
                                                        final Clob clob = rs.getClob(i + 1);
                                                        resultRow[i] = clob.getSubString(1, (int) clob.length());
                                                    }
                                            	} else if (BasicDataType.isStringType(types[i])) {
                                            		resultRow[i] = rs.getString(i + 1);
                                            	} else if (BasicDataType.BINARY.getId() == types[i]) {
                                                    if (rs.getObject(i + 1) != null) {
                                                        resultRow[i] = rs.getBinaryStream(i + 1);
                                                    }
                                            	}
                                            } // for (int i = 0; i < cols; i++)
                                            processDataSet(xmlWriter, resultRow, columnNames, types);
                                            xmlWriter.writeTagEnd();
                                            rownum++;
                                            if (refreshNow) {
                                                refreshNow = false;
                                                status.messageLabel.setText(tableName
                                                        + ": " //$NON-NLS-1$
                                                        + String.valueOf(rownum)
                                                        + " " //$NON-NLS-1$
                                                        + Messages.getString("ExporterFrame.datasetsexported")); //$NON-NLS-1$
                                            }
                                        } // if (isInterrupted() == false)
                                    } // while (rs.next())
                                    if (isInterrupted()) {
                                        status.messageLabel.setText(tableName
                                                + ": " //$NON-NLS-1$
                                                + String.valueOf(rownum)
                                                + " " //$NON-NLS-1$
                                                + Messages.getString("ExporterFrame.datasetsexportedabort")); //$NON-NLS-1$
                                    } else {
                                        status.messageLabel.setText(tableName
                                                + ": " //$NON-NLS-1$
                                                + String.valueOf(rownum)
                                                + " " //$NON-NLS-1$
                                                + Messages.getString("ExporterFrame.datasetsexportready")); //$NON-NLS-1$
                                    }
                                } catch (SQLException sqle) {
                                    JOptionPane.showMessageDialog(
                                            self,
                                            sqle.getMessage(),
                                            Messages.getString("ExporterFrame.readqueryresult"), //$NON-NLS-1$
                                            JOptionPane.ERROR_MESSAGE);
                                } catch (IOException ioe) {
                                    JOptionPane.showMessageDialog(
                                            self,
                                            ioe.getMessage(),
                                            Messages.getString("ExporterFrame.writequery"), //$NON-NLS-1$
                                            JOptionPane.ERROR_MESSAGE);
                                } finally {
                                    try {
                                        if (rs != null) {
                                            rs.close();
                                        }
                                    } catch (SQLException sqle) {
                                        sqle.printStackTrace();
                                    }
                                }
                            } else {
                                status.messageLabel.setText(Messages.getString("ExporterFrame.Errornoresultforquery")); //$NON-NLS-1$
                            } // if (rs != null)
                        } // else if (session.isSuccessful() == false)
                        if (xmlWriter != null) {
                            try {
                                xmlWriter.writeTagEnd();
                                xmlWriter.close();
                            } catch (IOException e) {
                                exportLogger.error("write document end tag failed: " + e.getMessage(), e);
                                JOptionPane.showMessageDialog(
                                        self,
                                        e.getMessage(),
                                        Messages.getString("ExporterFrame.writequery"), //$NON-NLS-1$
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } // for (int t = 0; t < listModel.size(); t++)
                    timer.stop();
                    status.infoLabel.setText("DISC"); //$NON-NLS-1$
                    status.infoLabel.setToolTipText(Messages.getString("ExporterFrame.notconnected")); //$NON-NLS-1$
                    status.infoLabel.setBackground(new Color(204, 204, 204));
                    buttonInterrupt.setEnabled(false);
                    buttonStart.setEnabled(true);
                    buttonReload.setEnabled(true);
                    buttonRemoveSelected.setEnabled(true);
                    buttonClose.setEnabled(true);
                    checkboxBase64Coding.setEnabled(true);
                    buttonOpenFile.setEnabled(true);
                    textFieldTargetDirectory.setEnabled(true);
                    buttonRemoveUnselected.setEnabled(true);
                    status.infoLabel.setBackground(Color.lightGray);
                } finally {
                    MainFrame.removeLongRunningAction(lra);
                }
                enableEvents(AWTEvent.WINDOW_EVENT_MASK);
                stopTime = System.currentTimeMillis();
            } finally {
                closeFileAppender();
                if (exportSession != null) {
                    exportSession.close();
                }
            }
        }

        private void processDataSet(
                XMLWriter xmlWriter, 
                Object[] resultRow, 
                String[] columnNames,
                int[] types) throws IOException {
            final String[][] attributes = new String[1][2];
            for (int i = 0; i < columnNames.length; i++) {
                Object content = resultRow[i];
                if (content != null) {
                	if (BasicDataType.isDateType(types[i])) {
                        xmlWriter.writeTagBegin(columnNames[i], null);
                        if (content instanceof Timestamp) {
                            xmlWriter.writeTextContent(String.valueOf(((Timestamp) content).getTime()));
                        } else if (content instanceof java.sql.Time) {
                            xmlWriter.writeTextContent(String.valueOf(((java.sql.Time) content).getTime()));
                        }
                        xmlWriter.writeTagEnd();
                	} else if (BasicDataType.isNumberType(types[i])) {
                        xmlWriter.writeTagBegin(columnNames[i], null);
                        if (content != null) {
                            xmlWriter.writeTextContent(content.toString());
                        }
                        xmlWriter.writeTagEnd();
                	} else if (BasicDataType.CLOB.getId() == types[i]) {
                        attributes[0][0] = "code"; //$NON-NLS-1$
                        attributes[0][1] = "base64"; //$NON-NLS-1$
                        xmlWriter.writeTagBegin(columnNames[i], attributes);
                        if (content != null) {
                            xmlWriter.writeCDATAContent(Base64.toString(
                                    Base64.encode(((String) content).getBytes(Base64.DEFAULT_CHARSET)),
                                    true));
                        }
                        xmlWriter.writeTagEnd();
                	} else if (BasicDataType.isStringType(types[i])) {
                        if (codeStringInBase64) {
                            attributes[0][0] = "code"; //$NON-NLS-1$
                            attributes[0][1] = "base64"; //$NON-NLS-1$
                            xmlWriter.writeTagBegin(columnNames[i], attributes);
                            if (content != null) {
                                xmlWriter.writeCDATAContent(Base64.toString(
                                        Base64.encode(((String) content).getBytes(Base64.DEFAULT_CHARSET)),
                                        true));
                            }
                        } else {
                            xmlWriter.writeTagBegin(columnNames[i], null);
                            if (content != null) {
                                xmlWriter.writeTextContent((String) content);
                            }
                        }
                        xmlWriter.writeTagEnd();
                	} else if (enableBinary && BasicDataType.BINARY.getId() == types[i]) {
                         attributes[0][0] = "code"; //$NON-NLS-1$
                         attributes[0][1] = "base64"; //$NON-NLS-1$
                         xmlWriter.writeTagBegin(columnNames[i], attributes);
                         if (content != null) {
                             final byte[] buffer = new byte[Base64.TWENTYFOURBITGROUP];
                             int length = -1;
                             final StringBuffer sb = new StringBuffer();
                             final InputStream is = (InputStream) content;
                             while ((length = is.read(buffer)) != -1) {
                                 sb.append(Base64.toString(Base64.encode(buffer, length)));
                             }
                             is.close();
                             xmlWriter.writeCDATAContent(sb.toString());
                         }
                         xmlWriter.writeTagEnd();
                	}
                }
            }
        }

        public boolean isRunning() {
            return stopTime == 0;
        }

        public void abort() {
            interrupt();
        }

    } // class ExporterThread

    /**
     * @author lolling.jan
     */
    public class LargeListModel extends AbstractListModel<ExportDescription> {

        private static final long serialVersionUID = 1L;
        private List<ExportDescription> delegate = new ArrayList<ExportDescription>();

        /**
         * Returns the number of components in this list.
         * <p>
         * This method is identical to <code>size</code>, which implements the 
         * <code>List</code> interface defined in the 1.2 Collections framework.
         * This method exists in conjunction with <code>setSize</code> so that
         * <code>size</code> is identifiable as a JavaBean property.
         *
         * @return  the number of components in this list
         * @see #size()
         */
        @Override
		public int getSize() {
            return delegate.size();
        }

        /**
         * Returns the component at the specified index.
         * <blockquote>
         * <b>Note:</b> Although this method is not deprecated, the preferred
         *    method to use is <code>get(int)</code>, which implements the 
         *    <code>List</code> interface defined in the 1.2 Collections framework.
         * </blockquote>
         * @param      index   an index into this list
         * @return     the component at the specified index
         * @exception  ArrayIndexOutOfBoundsException  if the <code>index</code> 
         *             is negative or greater than the current size of this 
         *             list
         * @see #get(int)
         */
        @Override
		public ExportDescription getElementAt(int index) {
            return delegate.get(index);
        }

        /**
         * Returns the number of components in this list.
         *
         * @return  the number of components in this list
         * @see Vector#size()
         */
        public int size() {
            return delegate.size();
        }

        /**
         * Tests whether this list has any components.
         *
         * @return  <code>true</code> if and only if this list has 
         *          no components, that is, its size is zero;
         *          <code>false</code> otherwise
         * @see Vector#isEmpty()
         */
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        /**
         * Tests whether the specified object is a component in this list.
         *
         * @param   elem   an object
         * @return  <code>true</code> if the specified object 
         *          is the same as a component in this list
         * @see Vector#contains(Object)
         */
        public boolean contains(Object elem) {
            return delegate.contains(elem);
        }

        /**
         * Searches for the first occurrence of <code>elem</code>.
         *
         * @param   elem   an object
         * @return  the index of the first occurrence of the argument in this
         *          list; returns <code>-1</code> if the object is not found
         * @see Vector#indexOf(Object)
         */
        public int indexOf(Object elem) {
            return delegate.indexOf(elem);
        }

        /**
         * Returns the index of the last occurrence of <code>elem</code>.
         *
         * @param   elem   the desired component
         * @return  the index of the last occurrence of <code>elem</code>
         *          in the list; returns <code>-1</code> if the object is not found
         * @see Vector#lastIndexOf(Object)
         */
        public int lastIndexOf(Object elem) {
            return delegate.lastIndexOf(elem);
        }

        /**
         * add elements from a list
         * @param objectList
         */
        public void addElements(ArrayList<ExportDescription> objectList) {
            if (objectList != null && objectList.isEmpty() == false) {
                int index = delegate.size();
                delegate.addAll(objectList);
                fireIntervalAdded(this, index, delegate.size() - 1);
            }
        }

        /**
         * Removes the first (lowest-indexed) occurrence of the argument 
         * from this list.
         *
         * @param   obj   the component to be removed
         * @return  <code>true</code> if the argument was a component of this
         *          list; <code>false</code> otherwise
         * @see Vector#removeElement(Object)
         */
        public boolean removeElement(Object obj) {
            int index = indexOf(obj);
            boolean rv = delegate.remove(obj);
            if (index >= 0) {
                fireIntervalRemoved(this, index, index);
            }
            return rv;
        }

        /**
         * Returns a string that displays and identifies this
         * object's properties.
         *
         * @return a String representation of this object
         */
        @Override
        public String toString() {
            return delegate.toString();
        }

        /* The remaining methods are included for compatibility with the
         * Java 2 platform Vector class.
         */

        /**
         * Returns the element at the specified position in this list.
         * <p>
         * Throws an <code>ArrayIndexOutOfBoundsException</code>
         * if the index is out of range
         * (<code>index &lt; 0 || index &gt;= size()</code>).
         *
         * @param index index of element to return
         */
        public Object get(int index) {
            return delegate.get(index);
        }

        /**
         * Replaces the element at the specified position in this list with the
         * specified element.
         * <p>
         * Throws an <code>ArrayIndexOutOfBoundsException</code>
         * if the index is out of range
         * (<code>index &lt; 0 || index &gt;= size()</code>).
         *
         * @param index index of element to replace
         * @param element element to be stored at the specified position
         * @return the element previously at the specified position
         */
        public ExportDescription set(int index, ExportDescription element) {
        	ExportDescription rv = delegate.remove(index);
            delegate.add(index, element);
            fireContentsChanged(this, index, index);
            return rv;
        }

        /**
         * Inserts the specified element at the specified position in this list.
         * <p>
         * Throws an <code>ArrayIndexOutOfBoundsException</code> if the
         * index is out of range
         * (<code>index &lt; 0 || index &gt; size()</code>).
         *
         * @param index index at which the specified element is to be inserted
         * @param element element to be inserted
         */
        public void add(int index, ExportDescription element) {
            delegate.add(index, element);
            fireIntervalAdded(this, index, index);
        }

        /**
         * Removes the element at the specified position in this list.
         * Returns the element that was removed from the list.
         * <p>
         * Throws an <code>ArrayIndexOutOfBoundsException</code>
         * if the index is out of range
         * (<code>index &lt; 0 || index &gt;= size()</code>).
         *
         * @param index the index of the element to removed
         */
        public ExportDescription remove(int index) {
        	ExportDescription rv = delegate.get(index);
            delegate.remove(index);
            fireIntervalRemoved(this, index, index);
            return rv;
        }

        /**
         * Removes all of the elements from this list.  The list will
         * be empty after this call returns (unless it throws an exception).
         */
        public void clear() {
            int index1 = delegate.size() - 1;
            delegate.clear();
            if (index1 >= 0) {
                fireIntervalRemoved(this, 0, index1);
            }
        }

        /**
         * Deletes the components at the specified range of indexes.
         * The removal is inclusive, so specifying a range of (1,5)
         * removes the component at index 1 and the component at index 5,
         * as well as all components in between.
         * <p>
         * Throws an <code>ArrayIndexOutOfBoundsException</code>
         * if the index was invalid.
         * Throws an <code>IllegalArgumentException</code> if
         * <code>fromIndex &gt; toIndex</code>.
         *
         * @param      fromIndex the index of the lower end of the range
         * @param      toIndex   the index of the upper end of the range
         * @see    #remove(int)
         */
        public void removeRange(int fromIndex, int toIndex) {
            for (int i = toIndex; i >= fromIndex; i--) {
                delegate.remove(i);
            }
            fireIntervalRemoved(this, fromIndex, toIndex);
        }
        
        public ExportDescription[] toArray() {
        	ExportDescription[] array = new ExportDescription[delegate.size()];
        	for (int i = 0; i < delegate.size(); i++) {
        		array[i] = delegate.get(i);
        	}
        	return array;
        }

    }

    
}