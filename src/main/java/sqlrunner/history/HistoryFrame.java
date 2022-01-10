package sqlrunner.history;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.SQLFileFilter;
import sqlrunner.swinghelper.WindowHelper;
import dbtools.SQLStatement;

/**
 * Dialog zur Auswahl der vorhergehenden SQLStatements.
 */
public class HistoryFrame extends JFrame implements ActionListener, ListSelectionListener {

	private static final Logger logger = LogManager.getLogger(HistoryFrame.class);

    private static final long      serialVersionUID      = 1L;
    private final JPanel           jPanel1               = new JPanel();
    private final JPanel           jPanel2               = new JPanel();
    private final JButton          buttonTakeOver        = new JButton();
    private final JButton          buttonDel             = new JButton();
    private final JButton          buttonDetails         = new JButton();
    private final JCheckBox        checkBoxFreese        = new JCheckBox();
    private final JButton          buttonClear           = new JButton();
    private final JButton          buttonClose          = new JButton();
    private final JScrollPane      jScrollPane1          = new JScrollPane();
    private final DefaultListModel<SQLStatement> model                 = new DefaultListModel<SQLStatement>();
    private final JList<SQLStatement>            statementList         = new JList<SQLStatement>(model);
    private SQLStatement           currentSqlStat;
    private DetailDialog           details;
    private final JButton          buttonScript          = new JButton();
    private String                 sqlMessageDelimiter;
    private final JCheckBox        checkBoxNoDouble      = new JCheckBox();
    private final JCheckBox        checkBoxEnableComment = new JCheckBox();
    private final JCheckBox        checkBoxRunImmediate  = new JCheckBox();
    private MainFrame              mainFrame;
    private boolean freezed;

    public HistoryFrame() {
        super(Messages.getString("HistoryView.0")); 
        try {
            initComponents();
            pack();
            sqlMessageDelimiter = Main.getDefaultProperty("HISTORY_VIEW_DELIMITER_MESSAGES", "*#*"); 
            readList();
        } catch (Exception e) {
        	logger.error("HistoryView (init) failed: " + e.getMessage(), e);
        }
    }

    private void initComponents() throws Exception {
        getRootPane().setDefaultButton(buttonTakeOver);
        jPanel1.setLayout(new BorderLayout());
        jPanel2.setLayout(new GridBagLayout());
        statementList.setBackground(SystemColor.info);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        int y = 0;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, 1, 1, 1);
            buttonTakeOver.setText(Messages.getString("HistoryView.3"));
            buttonTakeOver.addActionListener(this);
            buttonTakeOver.setEnabled(false);
            jPanel2.add(buttonTakeOver, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, 1, 1, 1);
            buttonDel.setText(Messages.getString("HistoryView.4"));
            buttonDel.addActionListener(this);
            buttonDel.setEnabled(false);
            jPanel2.add(buttonDel, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, 1, 1, 1);
            buttonDetails.setText(Messages.getString("HistoryView.6"));
            buttonDetails.setEnabled(false);
            buttonDetails.addActionListener(this);
            jPanel2.add(buttonDetails, gbc);
        }
        y++;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(1, 1, 1, 1);
            checkBoxRunImmediate.setText(Messages.getString("HistoryView.21")); 
            checkBoxRunImmediate.setBorder(null);
            checkBoxRunImmediate.setSelected((Main.getUserProperty("HISTORY_RUN_IMMIDATELY", "false")).equals("true"));
            jPanel2.add(checkBoxRunImmediate, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, 1, 1, 1);
            buttonClear.setText(Messages.getString("HistoryView.5")); 
            buttonClear.addActionListener(this);
            jPanel2.add(buttonClear, gbc);
        }
        y++;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(1, 1, 1, 1);
            checkBoxFreese.setBorder(null);
            checkBoxFreese.addActionListener(new ActionListener() {
            	public void actionPerformed(ActionEvent e) {
            		freezed = checkBoxFreese.isSelected();
            	}
            });
            checkBoxFreese.setText(Messages.getString("HistoryView.8")); 
            checkBoxFreese.setSelected((Main.getUserProperty("HISTORY_FREESE", "false")).equals("true"));
            jPanel2.add(checkBoxFreese, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, 1, 1, 1);
            buttonScript.addActionListener(this);
            buttonScript.setText(Messages.getString("HistoryView.12")); 
            jPanel2.add(buttonScript, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, 1, 1, 1);
            checkBoxEnableComment.setText(Messages.getString("HistoryView.17")); 
            checkBoxEnableComment.setSelected((Main.getUserProperty("HISTORY_SCRIPT_WITH_COMMENTS", "true")).equals("true"));
            jPanel2.add(checkBoxEnableComment, gbc);
        }
        y++;
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 0;
            //gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(1, 1, 1, 1);
            checkBoxNoDouble.setText(Messages.getString("HistoryView.13")); 
            checkBoxNoDouble.setSelected((Main.getUserProperty("HISTORY_NO_DOUBLE", "true")).equals("true"));
            jPanel2.add(checkBoxNoDouble, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = y;
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, 1, 1, 1);
            buttonClose.setText(Messages.getString("HistoryView.7")); 
            buttonClose.addActionListener(this);
            jPanel2.add(buttonClose, gbc);
        }
        getContentPane().add(jPanel2, BorderLayout.SOUTH);
        jScrollPane1.setViewportView(statementList);
        statementList.addListSelectionListener(this);
        statementList.addMouseListener(new ListMouseListener());
        statementList.setFont(Main.textFont);
        statementList.setCellRenderer(new SQLListCellRenderer());
        statementList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (buttonTakeOver.isEnabled()) {
                        buttonTakeOver_actionPerformed();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    if (buttonDel.isEnabled()) {
                        buttonDel_actionPerformed();
                    }
                }
            }
        });
    }
    
    public boolean isFreezed() {
    	return freezed;
    }
    
    public void refresh() {
    	if (SwingUtilities.isEventDispatchThread()) {
    		doRefresh();
    	} else {
    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				doRefresh();
    			}
    		});
    	}
    }
    
    private void doRefresh() {
        statementList.repaint();
        if (details != null) {
            details.refresh();
        }
    }

    public void setMainFrame(MainFrame mainFrame_loc) {
        this.mainFrame = mainFrame_loc;
    }

    /**
     * überschreibt die super-Methode, da hier bester Zeitpunkt die Fensterpos
     * zu überprüfen und ruft zur Prüfung der Fensterpos. die Methode correctWindowPos() auf.
     * Die paint-Methode ist nicht geeignet, da das Fenster nur gezeichnet wird, wenn die
     * gesetzten Bounds auch wenigstens teilweise innerhalb des screens!
     * Sollte das Fenster ausserhalb platziert werden wird paint garnicht erst aufgerufen
     * und keine Korrektur erfolgt!
     * @param visible true=sichtbar
     */
    @Override
    public void setVisible(boolean visible) {
        if (!isShowing()) {
            WindowHelper.checkAndCorrectWindowBounds(this);
            if (((statementList.getModel()).getSize() > 0) && (statementList.getSelectedIndex() == -1)) {
                statementList.setSelectedIndex(0);
            }
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    // Window-Ereignisbehandlung
    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()) {
            case WindowEvent.WINDOW_CLOSING:
                cancel();
                break;
            case WindowEvent.WINDOW_ACTIVATED:
                statementList.requestFocus();
                break;
        }
        super.processWindowEvent(e);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                cancel();
                break;
            case KeyEvent.VK_D:
                buttonDetails_actionPerformed();
                break;
            case KeyEvent.VK_DELETE:
                buttonDel_actionPerformed();
                break;
        }
    }

    private void cancel() {
        setVisible(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonTakeOver) {
            buttonTakeOver_actionPerformed();
        } else if (e.getSource() == buttonDel) {
            buttonDel_actionPerformed();
        } else if (e.getSource() == buttonClose) {
            cancel();
        } else if (e.getSource() == buttonClear) {
            clearAll();
        } else if (e.getSource() == buttonDetails) {
            buttonDetails_actionPerformed();
        } else if (e.getSource() == buttonScript) {
            // neue Datei öffnen
            final JFileChooser chooser = new JFileChooser();
            final String lastFile = Main.getUserProperty("HISTORY_BACKUP_FILE"); 
            if (lastFile != null) {
                chooser.setSelectedFile(new File(lastFile));
            } else {
                chooser.setCurrentDirectory(new File(System.getProperty("user.home"))); 
            }
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle(Messages.getString("HistoryView.27")); 
            chooser.addChoosableFileFilter(new SQLFileFilter());
            // Note: source for ExampleFileFilter can be found in FileChooserDemo,
            // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
            final int returnVal = chooser.showSaveDialog(this);
            BufferedWriter bw = null;
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (chooser.getFileFilter() instanceof SQLFileFilter
                        && (!((f.getName()).toLowerCase()).endsWith(".sql"))) { 
                    f = new File(f.getAbsolutePath() + ".sql"); 
                }
                Main.setUserProperty("HISTORY_BACKUP_FILE", f.getAbsolutePath()); 
                try {
                    bw = new BufferedWriter(new FileWriter(f));
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(this, ioe.getMessage(), Messages.getString("HistoryView.31"), JOptionPane.ERROR_MESSAGE);
                }
            }
            if (bw != null) {
                final ListSaver saver = new ListSaver(bw, this);
                saver.start();
            }
        }
    }

    private void buttonTakeOver_actionPerformed() {
        if (mainFrame != null) {
            currentSqlStat = readSQLStatementFromList();
            if (currentSqlStat == null) {
                JOptionPane.showMessageDialog(
                        this,
                        Messages.getString("HistoryView.32"),
                        Messages.getString("HistoryView.33"),
                        JOptionPane.WARNING_MESSAGE);
            } else {
                if (checkBoxRunImmediate.isSelected()) {
                    if (mainFrame.isConnected()) {
                        mainFrame.getDatabase().executeStatement(currentSqlStat);
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                Messages.getString("HistoryView.34"),
                                Messages.getString("HistoryView.35"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    final int[] selectedIndexes = statementList.getSelectedIndices();
                    for (int i = selectedIndexes.length - 1; i >= 0; i--) {
                    	SQLStatement s = (SQLStatement) statementList.getModel().getElementAt(selectedIndexes[i]);
                        mainFrame.insertOrReplaceText(s.getSQL() + ";\n");
                    }
                    mainFrame.setTextSaveEnabled(false);
                }
                mainFrame.toFront();
            }
        }
    }

    private void buttonDel_actionPerformed() {
        final int[] selectedIndexes = statementList.getSelectedIndices();
        for (int i = selectedIndexes.length - 1; i >= 0; i--) {
            model.removeElementAt(selectedIndexes[i]);
        }
    }

    private void buttonDetails_actionPerformed() {
        if (details == null) {
            details = new DetailDialog(this);
        }
        details.setVisible(true);
        details.setStatement(currentSqlStat);
        WindowHelper.locateWindowAtLeftSideWithin(this, details, 0);
        this.requestFocus();
    }

    public void valueChanged(ListSelectionEvent e) {
        final SQLStatement statTemp = readSQLStatementFromList();
        if (statTemp != null) {
            currentSqlStat = statTemp;
            if (details != null) {
                details.setStatement(currentSqlStat);
            }
        }
    }

    private SQLStatement readSQLStatementFromList() {
        final SQLStatement s = ((SQLStatement) statementList.getSelectedValue());
        if (s != null) {
            buttonTakeOver.setEnabled(true);
            buttonDel.setEnabled(true);
            buttonDetails.setEnabled(true);
        }
        return s;
    }

    public void addSQLStatement(SQLStatement sqlStat) {
        addSQLStatement(sqlStat, false);
    }

    /**
     * fügt das Statement der Liste hinzu nur wenn es nicht bereits enthalten ist.
     */
    public void addSQLStatement(final SQLStatement sqlStat, final boolean isScript) {
    	if (checkBoxFreese.isSelected() == false) {
        	if (SwingUtilities.isEventDispatchThread()) {
        		doAddSQLStatement(sqlStat, isScript);
        	} else {
        		SwingUtilities.invokeLater(new Runnable() {
        			public void run() {
        	    		doAddSQLStatement(sqlStat, isScript);
        			}
        		});
        	}
    	}
    }
    
    private void doAddSQLStatement(SQLStatement sqlStat, boolean isScript) {
        if (sqlStat.isHidden() == false) {
            if ((!isScript) || checkBoxNoDouble.isSelected()) {
                for (int i = 0; i < model.size(); i++) {
                    if (((SQLStatement) model.elementAt(i)).equals(sqlStat)) {
                        // altes gleiches Statement entfernen
                        model.removeElementAt(i);
                    }
                } // for (int i=0; i<model.size(); i++)
                model.insertElementAt(sqlStat, 0);
                statementList.getSelectionModel().clearSelection();
                //                statementList.setSelectedIndex(0);
                statementList.setSelectedValue(sqlStat, true);
            } else {
                addBatch(sqlStat, isScript);
            } // if (checkBoxNoDouble.isSelected())
        } else {
        	if (logger.isDebugEnabled()) {
        		logger.debug("doAddSQLStatement: skipped because statement is hidden");
        	}
        }
    }

    public void setFreezed(boolean freese) {
        checkBoxFreese.setSelected(freese);
    }

    public void clearAll() {
        model.removeAllElements();
        currentSqlStat = null;
        buttonTakeOver.setEnabled(false);
        buttonDel.setEnabled(false);
        buttonDetails.setEnabled(false);
    }

    public void addBatch(SQLStatement sqlStat, boolean isScript) {
        if ((!checkBoxFreese.isSelected() && sqlStat.isValid()) && !sqlStat.isHidden()) {
            model.insertElementAt(sqlStat, 0);
            buttonTakeOver.setEnabled(false);
            buttonDel.setEnabled(false);
            if (isScript) {
                statementList.setSelectedIndex(0);
            }
        }
    }

    static class SQLListCellRenderer extends JLabel implements ListCellRenderer {

        private static final long serialVersionUID = 1L;
        private final Color       lightRed         = new Color(255, 190, 190);
        private final Color       lightGreen       = new Color(190, 255, 190);
        private final Color       yellow           = Color.YELLOW;

        SQLListCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            final SQLStatement s = ((SQLStatement) value);
            setText(s.toString());
            if (isSelected) {
                setFont(new Font("Dialog", Font.BOLD, (list.getFont()).getSize())); 
                setBorder(BorderFactory.createLineBorder(Color.black));
            } else {
                setFont(new Font("Dialog", Font.PLAIN, (list.getFont()).getSize())); 
                setBorder(null);
            }
            if (s.isStarted() == false) {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            } else {
                if (s.isRunning()) {
                    setBackground(yellow);
                    setForeground(list.getForeground());
                } else {
                    if (s.isSuccessful()) {
                        setBackground(lightGreen);
                        setForeground(list.getForeground());
                    } else {
                        setBackground(lightRed);
                        setForeground(list.getForeground());
                    }
                }
            }
            setComponentOrientation(list.getComponentOrientation());
            return this;
        }

    }

    /**
     * read Admin-SQLs from File
     * @param File contains the SQLs
     */
    private void readList() {
        String line = null;
        final StringBuffer textBf = new StringBuffer();
        String text;
        boolean inSQL = false;
        SQLStatement stat = null;
        String param = null;
        try {
            final BufferedReader br = new BufferedReader(new FileReader(Main.getSqlHistoryFileName()));
            while ((line = br.readLine()) != null) {
                if ((!line.startsWith("#")) && ((line.trim()).length() > 0)) { 
                    if (line.startsWith(SQLStatement.START_SEQUENCE_FOR_SUMMARY)) { // Beginn einer SQL gefunden
                        if (inSQL) { // einen vorhergehenden Lesevorgang einer SQL beenden
                            stat = new SQLStatement();
                            // den Text in SQL und Message zerlegen
                            text = textBf.toString();
                            final int p0 = text.indexOf(sqlMessageDelimiter);
                            stat.setSQL((text.substring(0, p0)).trim());
                            stat.setMessage(text.substring(p0 + sqlMessageDelimiter.length(), text.length()));
                            stat.parseSummaryStr(param);
                            model.addElement(stat);
                            inSQL = false;
                        } // if (inSQL)
                        param = line.substring(SQLStatement.START_SEQUENCE_FOR_SUMMARY.length(), line.length() - 1);
                        inSQL = true; // ab jetzt neue SQL-Anweisung lesen
                        textBf.setLength(0); // StringBuffer löschen
                    } else { // if (line.startsWith("["))
                        // hier sql und message einlesen
                        textBf.append(line);
                        textBf.append('\n'); 
                    } // if (line.startsWith("["))
                } // if ((line.startsWith("#")==false) && (line.length() > 0))
            } // while ((line = br.readLine()) != null)
            br.close();
            // Am Ende den Rest einsammeln
            if (textBf.length() > 2) {
                stat = new SQLStatement();
                text = textBf.toString();
                final int p0 = text.indexOf(sqlMessageDelimiter);
                if (p0 > 0) {
                    stat.setSQL((text.substring(0, p0)).trim());
                    stat.setMessage(text.substring(p0 + sqlMessageDelimiter.length(), text.length()));
                    stat.parseSummaryStr(param);
                    model.addElement(stat);
                } else {
                    logger.error("WARNING: malformed SQLHistory entry: " + text); 
                }
            }
        } catch (IOException ioe) {
            logger.error("HistoryView.readList:" + ioe.toString()); 
        }
    }

    /**
     * speichert Liste der Verbindung in Datei (INI_FILE_NAME)
     * Diese Methode begrenzt die Anzahl der Einträge, da diese beim nächsten Start neu eingelesen werden.
     * @return true wenn erfolgreich
     */
    public void saveList() {
        try {
            final File iniFile = new File(Main.getSqlHistoryFileName());
            SQLStatement stat;
            final int MAX_LIST_LINE_NUMBER = 1000;
            int countStatements = 0;
            final BufferedWriter bw = new BufferedWriter(new FileWriter(iniFile));
            for (int i = 0; i < model.size(); i++) {
                stat = (SQLStatement) model.getElementAt(i);
                if (stat.getSQL() != null) {
                    bw.write(stat.getSummaryStr()); // als erstes wird hier START_SEQUENCE_FOR_SUMMARY gesetzt
                    bw.newLine();
                    bw.write(stat.getSQL());
                    bw.newLine();
                    bw.write(sqlMessageDelimiter);
                    bw.newLine();
                    bw.write(stat.getMessage());
                    bw.newLine();
                }
                countStatements++;
                if (countStatements > MAX_LIST_LINE_NUMBER) {
                    logger.info(Messages.getString("HistoryView.44")); 
                    break;
                }
            }
            bw.flush();
            bw.close();
        } catch (Exception ioe) {
            logger.error("HistoryView.saveList: " + ioe.toString()); 
        }
    }

    /**
     * diese Klasse speichert die Historie in eine Datei mit zusätzlichen Informationen als SQL-Script und 
     * hat keine Grössenbegrenzung der Anzahl der Einträge
     * @author lolling.jan
     */
    class ListSaver extends Thread {
		/**
		 * Logger for this class
		 */
		private final Logger logger = LogManager.getLogger(ListSaver.class);

        BufferedWriter bw;
        JFrame         window;

        ListSaver(BufferedWriter bw, JFrame window) {
            this.window = window;
            this.bw = bw;
        }

        @Override
        public void run() {
            SQLStatement stat;
            buttonScript.setEnabled(false);
            checkBoxEnableComment.setEnabled(false);
            try {
                bw.write("/* BEGIN generated script from History-View */\n"); 
                bw.newLine();
                for (int i = model.size() - 1; i >= 0; i--) {
                    stat = (SQLStatement) model.getElementAt(i);
                    bw.write(stat.getSQL());
                    bw.write(";"); 
                    if (checkBoxEnableComment.isSelected()) {
                        bw.newLine();
                        bw.write("/*"); 
                        bw.newLine();
                        bw.write(stat.getSummary());
                        bw.newLine();
                        if (stat.getCurrentUrl() != null) {
                            bw.write("URL=");
                            bw.write(stat.getCurrentUrl());
                            bw.newLine();
                            bw.write("USER=");
                            bw.write(stat.getCurrentUser());
                            bw.newLine();
                        }
                        bw.write("*/"); 
                        bw.newLine();
                    } else {
                        bw.newLine();
                    }
                }
                bw.write("/* END generated script from History-View */\n"); 
                bw.flush();
                bw.close();
            } catch (IOException ioe) {
            	logger.error("ListSaver failed: " + ioe.getMessage(), ioe);
                JOptionPane.showMessageDialog(window, ioe.getMessage(), Messages.getString("HistoryView.51"), JOptionPane.ERROR_MESSAGE); 
            }
            buttonScript.setEnabled(true);
            checkBoxEnableComment.setEnabled(true);
        }
    }

    public boolean isNoDoublesAllowed() {
        return checkBoxNoDouble.isSelected();
    }

    public boolean isFreesed() {
        return checkBoxFreese.isSelected();
    }

    public boolean isCommentsInHistoryScriptEnabled() {
        return checkBoxEnableComment.isSelected();
    }

    public boolean isRunImmidatlyEnabled() {
        return checkBoxRunImmediate.isSelected();
    }

    class ListMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent me) {
            if ((!me.isControlDown()) && (me.getClickCount() == 2)) {
                fireDoubleClickPerformed(me);
            }
        }

        protected void fireDoubleClickPerformed(MouseEvent me) {
            buttonTakeOver_actionPerformed();
        }

    }

}
