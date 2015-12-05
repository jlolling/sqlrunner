package sqlrunner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import sqlrunner.swinghelper.WindowHelper;

public final class TableSearchDialog extends JFrame implements ActionListener {
 
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JPanel panel = null;
    private JLabel     jLabel1               = null;
    private JTextField textFieldSearchText   = null;
    private JCheckBox  checkBoxLatestColumn  = null;
    private JCheckBox  checkBoxViewerStart   = null;
    private JButton buttonSearch             = null;
    private JCheckBox checkBoxCaseSensitive  = null;
    private JButton    buttonCancel          = null;
    private StatusBar        status;
    private MainFrame        mainFrame;
    private boolean          searchNext      = false;
    private JCheckBox  checkBoxStartAt0      = null;
    private boolean          windowIsActivated;

    public TableSearchDialog(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            WindowHelper.checkAndCorrectWindowBounds(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    private void initComponents() throws Exception {
        setTitle(Messages.getString("TableSearchDialog.seatchinallcolumns")); //$NON-NLS-1$
        this.setContentPane(getJContentPane());
        getRootPane().setDefaultButton(buttonSearch);
        setResizable(false);
    }
    
    /**
     * This method initializes jContentPane    
     *     
     * @return javax.swing.JPanel    
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridy = 5;
            gridBagConstraints7.anchor = GridBagConstraints.EAST;
            gridBagConstraints7.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints7.gridx = 2;
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.gridx = 0;
            gridBagConstraints6.gridy = 5;
            gridBagConstraints6.anchor = GridBagConstraints.WEST;
            gridBagConstraints6.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints6.gridwidth = 2;
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.insets = new Insets(2, 20, 2, 2);
            gridBagConstraints5.gridx = 0;
            gridBagConstraints5.gridy = 4;
            gridBagConstraints5.anchor = GridBagConstraints.WEST;
            gridBagConstraints5.gridwidth = 3;
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.insets = new Insets(2, 20, 2, 2);
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.gridy = 3;
            gridBagConstraints4.anchor = GridBagConstraints.WEST;
            gridBagConstraints4.gridwidth = 3;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.insets = new Insets(2, 20, 2, 2);
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.gridy = 2;
            gridBagConstraints3.anchor = GridBagConstraints.WEST;
            gridBagConstraints3.gridwidth = 3;
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.insets = new Insets(2, 20, 2, 2);
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.gridy = 1;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.gridwidth = 3;
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.gridwidth = 2;
            gridBagConstraints1.gridx = 1;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.insets = new Insets(5, 2, 2, 5);
            gridBagConstraints1.weightx = 1.0;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.insets = new Insets(5, 5, 2, 5);
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.gridx = 0;
            jLabel1 = new JLabel();
            jLabel1.setForeground(Color.black);
            jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel1.setText(Messages.getString("TableSearchDialog.searchpattern")); //$NON-NLS-1$
            textFieldSearchText = new JTextField();
            textFieldSearchText.setBackground(Main.info);
            textFieldSearchText.setToolTipText(Messages.getString("TableSearchDialog.tooltip")); //$NON-NLS-1$
            checkBoxLatestColumn = new JCheckBox();
            checkBoxLatestColumn.setText(Messages.getString("TableSearchDialog.searchonlyincurrentcolumn")); //$NON-NLS-1$
            checkBoxLatestColumn.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    checkBoxLatestColumn_itemStateChanged(e);
                }
            });
            checkBoxViewerStart = new JCheckBox();
            checkBoxViewerStart.setText(Messages.getString("TableSearchDialog.opencelleditor")); //$NON-NLS-1$
            buttonSearch = new JButton();
            buttonSearch.setText(Messages.getString("TableSearchDialog.search")); //$NON-NLS-1$
            buttonSearch.setToolTipText(Messages.getString("TableSearchDialog.serachtooltip")); //$NON-NLS-1$
            buttonSearch.addActionListener(this);
            buttonSearch.setActionCommand("search"); //$NON-NLS-1$
            checkBoxCaseSensitive = new JCheckBox();
            checkBoxCaseSensitive.setText(Messages.getString("TableSearchDialog.casesensitive")); //$NON-NLS-1$
            buttonCancel = new JButton();
            buttonCancel.setText(Messages.getString("TableSearchDialog.Close")); //$NON-NLS-1$
            buttonCancel.setToolTipText(Messages.getString("TableSearchDialog.closetooltip")); //$NON-NLS-1$
            buttonCancel.addActionListener(this);
            buttonCancel.setActionCommand("cancel"); //$NON-NLS-1$
            checkBoxStartAt0 = new JCheckBox();
            checkBoxStartAt0.setText(Messages.getString("TableSearchDialog.searchfromfirstline")); //$NON-NLS-1$
            checkBoxStartAt0.setSelected(true);
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            panel.add(jLabel1, gridBagConstraints);
            panel.add(textFieldSearchText, gridBagConstraints1);
            panel.add(checkBoxLatestColumn, gridBagConstraints2);
            panel.add(checkBoxViewerStart, gridBagConstraints3);
            panel.add(checkBoxCaseSensitive, gridBagConstraints4);
            panel.add(checkBoxStartAt0, gridBagConstraints5);
            panel.add(buttonSearch, gridBagConstraints6);
            panel.add(buttonCancel, gridBagConstraints7);
            status = new StatusBar();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(panel, BorderLayout.CENTER);
            jContentPane.add(status, BorderLayout.SOUTH);
        }
        return jContentPane;
    }
    
    @Override
    protected void processWindowEvent(WindowEvent winEvent) {
        switch (winEvent.getID()) {
            case WindowEvent.WINDOW_ACTIVATED:
                textFieldSearchText.selectAll();
                textFieldSearchText.requestFocus();
                windowIsActivated = true;
                break;
            case WindowEvent.WINDOW_DEACTIVATED:
                windowIsActivated = false;
                break;
            default:
                super.processWindowEvent(winEvent);
        }
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (e.getID() == KeyEvent.KEY_PRESSED) { // da aus dem Editor noch die abfallende Flanke des Events weitergereicht wird.
                if (windowIsActivated) {
                    cancel();
                }
            }
        } else if (e.getKeyCode() == KeyEvent.VK_F3) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                buttonSearch_actionPerformed();
            }
        } else {
            super.processKeyEvent(e);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonCancel) {
            cancel();
        } else if (e.getSource() == buttonSearch) {
            buttonSearch_actionPerformed();
        }
    }

    private void buttonSearch_actionPerformed() {
        // ab der aktuellen Zeile anfang zu suchen
        status.infoAction.setText("BUSY"); //$NON-NLS-1$
        boolean founded = false;
        this.setEnabled(false);
        if (mainFrame.getDatabase() != null) {
            if (mainFrame.getDatabase().size() > 0) {
                if (checkBoxStartAt0.isSelected()) {
                    mainFrame.resultTable.setRowSelectionInterval(0, 0);
                    mainFrame.scrollTableToCell(0);
                    searchNext = false;
                }
                final String muster = (textFieldSearchText.getText()).trim();
                if (muster.length() > 0) {
                    if (searchNext) {
                        founded = mainFrame.getDatabase().findNextValue(
                                muster,
                                checkBoxCaseSensitive.isSelected(),
                                checkBoxLatestColumn.isSelected());
                    } else {
                        founded = mainFrame.getDatabase().findValue(
                                muster,
                                checkBoxCaseSensitive.isSelected(),
                                checkBoxLatestColumn.isSelected());
                    }
                    if (founded) {
                        // gefunden
                        // nun die position angeben
                        final int row = mainFrame.resultTable.getSelectedRow();
                        final int col = mainFrame.resultTable.getSelectedColumn();
                        status.meldung.setText(Messages.getString("TableSearchDialog.fondinfield") //$NON-NLS-1$
                                + mainFrame.getDatabase().getColumnName(col)
                                + Messages.getString("TableSearchDialog.foundinline") //$NON-NLS-1$
                                + String.valueOf(row + 1));
                        mainFrame.scrollTableToCell(row);
                        mainFrame.resultTable.requestFocus();
                        this.toFront();
                        if (checkBoxViewerStart.isSelected()) {
                            mainFrame.showEditor(row, col);
                        }
                        buttonSearch.setText(Messages.getString("TableSearchDialog.searchnext")); //$NON-NLS-1$
                        checkBoxStartAt0.setSelected(false);
                        searchNext = true;
                    } else {
                        status.meldung.setText(Messages.getString("TableSearchDialog.status_notfound")); //$NON-NLS-1$
                        checkBoxStartAt0.setSelected(true);
                        buttonSearch.setText(Messages.getString("TableSearchDialog.search")); //$NON-NLS-1$
                        searchNext = false;
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            Messages.getString("TableSearchDialog.messagedefinesearchpattern"), //$NON-NLS-1$
                            Messages.getString("TableSearchDialog.search"), //$NON-NLS-1$
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Messages.getString("TableSearchDialog.message_nodata"), //$NON-NLS-1$
                        Messages.getString("TableSearchDialog.search"), //$NON-NLS-1$
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, Messages.getString("TableSearchDialog.message_nodata"), Messages.getString("TableSearchDialog.search"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
        }
        this.setEnabled(true);
        status.infoAction.setText("READY"); //$NON-NLS-1$
        doLayout(); // to ensure all button text are visible
    }

    private void cancel() {
        searchNext = false;
        buttonSearch.setText(Messages.getString("TableSearchDialog.search")); //$NON-NLS-1$
        setVisible(false);
    }

    public String getSearchString() {
        return (textFieldSearchText.getText()).trim();
    }

    public boolean searchInSelectedColumn() {
        return checkBoxLatestColumn.isSelected();
    }

    public boolean caseSensitive() {
        return checkBoxCaseSensitive.isSelected();
    }

    public boolean requestViewer() {
        return checkBoxViewerStart.isSelected();
    }

    static class StatusBar extends JPanel {

        private static final long serialVersionUID = 1L;
        public JLabel meldung            = new FixedLabel();
        public JLabel infoAction         = new FixedLabel();
        static final int     INFO_ACTION_BREITE = 60;
        static final int     HOEHE              = 25;

        public StatusBar() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            meldung.setBorder(BorderFactory.createLoweredBevelBorder());
            meldung.setForeground(Color.black);
            infoAction.setPreferredSize(new Dimension(INFO_ACTION_BREITE, HOEHE));
            infoAction.setOpaque(true);
            infoAction.setText("IDLE"); //$NON-NLS-1$
            infoAction.setBorder(BorderFactory.createLoweredBevelBorder());
            infoAction.setForeground(Color.black);
            add(meldung);
            add(infoAction);
            // die zu erwartenden Property-Änderungen sind die setText()
            // Aufrufe, dann soll der ToolTip aktualisiert werden,
            // da dieser den Textinhalt voll anzeigen kann, auch wenn das Label
            // den Platz dafür nicht hat
            meldung.addPropertyChangeListener(new MeldungPropertyChangeListener());
            // eine bislang sinnvolle Nutzung der Spalte InfoAction
            // ist die Anzeige, ob Bak-Dateien erstellt werden oder nicht
        }

        // wird beim erstmaligen Darstellen , bei jeder FormÄnderung
        // und bei Aufrufen von repaint durchlaufen
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final int meldungBreite = this.getWidth() - INFO_ACTION_BREITE;
            meldung.setPreferredSize(new Dimension(meldungBreite, HOEHE));
            remove(meldung); // Label entfernen
            add(meldung, null, 0); // neu hinzufügen, damit neu eingepasst
            doLayout(); // damit wird die Änderung sofort wirksam !
        }

        // diese Klasse sichert, dass die Label auch in einer festen Groesse
        // dargestellt werden
        static class FixedLabel extends JLabel {

            private static final long serialVersionUID = 1L;

            public FixedLabel(String text) {
                super(text);
            }

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
        class MeldungPropertyChangeListener implements PropertyChangeListener {

            public void propertyChange(PropertyChangeEvent evt) {
                meldung.setToolTipText(meldung.getText());
            }
        }

    }

    private void checkBoxLatestColumn_itemStateChanged(ItemEvent e) {
        if (checkBoxLatestColumn.isSelected()) {
            final String name = mainFrame.getDatabase().getSelectedColumnName();
            if (name != null) {
                setTitle(Messages.getString("TableSearchDialog.seartchincolumn") + name); //$NON-NLS-1$
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Messages.getString("TableSearchDialog.selectcolumn"), //$NON-NLS-1$
                        Messages.getString("TableSearchDialog.Advise"), //$NON-NLS-1$
                        JOptionPane.INFORMATION_MESSAGE);
                checkBoxLatestColumn.setSelected(false);
            }
        } else {
            setTitle(Messages.getString("TableSearchDialog.titlesearchinallcolumns")); //$NON-NLS-1$
        }
    }

}
