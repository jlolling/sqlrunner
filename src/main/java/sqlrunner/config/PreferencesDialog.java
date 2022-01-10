package sqlrunner.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import sqlrunner.MainFrame;
import sqlrunner.swinghelper.WindowHelper;

public class PreferencesDialog extends JDialog {
    
    private static final Logger           logger               = LogManager.getLogger(PreferencesDialog.class);

    private static final long serialVersionUID = 1L;
    private JTabbedPane jTabbedPane;  //  @jve:decl-index=0:visual-constraint="10,106"
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private PanelConfigDateFormat panelConfigDateFormat;
    private PanelConfigFKFrameMode panelConfigFKFrameMode;
    private PanelConfigTableCell panelConfigTableCellSize;
    private PanelConfigLoolAndFeel panelConfigLoolAndFeel;
    private PanelConfigFramePositioning panelConfigFramePositioning;
    private PanelConfigCharSets panelConfigCharSets;
    private PanelConfigLineSeparator panelConfigLineSeparator;
    private PanelConfigEditorSettings panelConfigEditorFontSettings;
    private PanelConfigDebugMode panelConfigDebugMode;
    private JPanel jPanelButtons;
    private JButton buttonOk;
    private JButton buttonCancel;
    MainFrame mainFrame;

    public PreferencesDialog() {
    	super();
    }

    public PreferencesDialog(MainFrame mainFrame) {
    	super();
        this.mainFrame = mainFrame;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        if(!isShowing()) {
            try {
                this.setLocationByPlatform(!(WindowHelper.isWindowPositioningEnabled()));
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    private void initComponents() throws Exception {
        GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
        gridBagConstraints8.gridx = 0;
        gridBagConstraints8.fill = GridBagConstraints.BOTH;
        gridBagConstraints8.weightx = 1.0D;
        gridBagConstraints8.weighty = 1.0D;
        gridBagConstraints8.gridy = 0;
        GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
        gridBagConstraints7.gridx = 0;
        gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints7.gridy = 2;
        GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
        gridBagConstraints6.gridx = 0;
        gridBagConstraints6.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints6.gridy = 1;
        GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
        gridBagConstraints5.gridx = 0;
        gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints5.gridy = 0;
        GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
        gridBagConstraints4.insets = new Insets(0, 0, 1, 0);
        gridBagConstraints4.gridy = 4;
        gridBagConstraints4.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints4.gridx = 0;
        GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
        gridBagConstraints3.gridx = 0;
        gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints3.gridy = 3;
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints2.gridy = 2;
        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.gridy = 1;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridy = 0;
        setTitle(Messages.getString("PreferencesDialog.title")); //$NON-NLS-1$
        jPanelButtons=new JPanel();
        buttonOk=new JButton();
        buttonCancel=new JButton();
        jTabbedPane = new JTabbedPane();
        (getContentPane()).setLayout(new BorderLayout());
        (getContentPane()).add(jTabbedPane, BorderLayout.CENTER);
        jPanel1 = new JPanel();
        jPanel1.setLayout(new GridBagLayout());
        jPanel2 = new JPanel();
        jPanel2.setLayout(new GridBagLayout());
        jPanel3 = new JPanel();
        jPanel3.setLayout(new GridBagLayout());
        jTabbedPane.add(jPanel1, Messages.getString("PreferencesDialog.1")); //$NON-NLS-1$
        jTabbedPane.add(jPanel2, Messages.getString("PreferencesDialog.2")); //$NON-NLS-1$
        jTabbedPane.add(jPanel3, Messages.getString("PreferencesDialog.3")); //$NON-NLS-1$
        buttonOk.setText(Messages.getString("PreferencesDialog.4")); //$NON-NLS-1$
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buttonOk_actionPerformed(e);
            }
        });
        buttonCancel.setText(Messages.getString("PreferencesDialog.5")); //$NON-NLS-1$
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buttonCancel_actionPerformed(e);
            }
        });
        // allgemeine Einstellungen
        panelConfigLoolAndFeel = new PanelConfigLoolAndFeel();
        panelConfigCharSets = new PanelConfigCharSets();
        panelConfigLineSeparator = new PanelConfigLineSeparator();
        jPanel1.add(panelConfigLoolAndFeel, gridBagConstraints);
        panelConfigFramePositioning = new PanelConfigFramePositioning();
        jPanel1.add(panelConfigCharSets, gridBagConstraints1);
        panelConfigFKFrameMode = new PanelConfigFKFrameMode();
        panelConfigDebugMode = new PanelConfigDebugMode();
        jPanel1.add(panelConfigLineSeparator, gridBagConstraints2);
        jPanel1.add(panelConfigFramePositioning, gridBagConstraints3);
        jPanel1.add(panelConfigDebugMode, gridBagConstraints4);
        panelConfigDateFormat = new PanelConfigDateFormat();
        panelConfigTableCellSize = new PanelConfigTableCell();
        jPanel2.add(panelConfigFKFrameMode, gridBagConstraints5);
        jPanel2.add(panelConfigDateFormat, gridBagConstraints6);
        jPanel2.add(panelConfigTableCellSize, gridBagConstraints7);
        (getContentPane()).add(jPanelButtons, BorderLayout.SOUTH);
        panelConfigEditorFontSettings = new PanelConfigEditorSettings();
        jPanel3.add(panelConfigEditorFontSettings, gridBagConstraints8);
        jPanelButtons.add(buttonCancel, null);
        jPanelButtons.add(buttonOk, null);
    }

    private void buttonOk_actionPerformed(ActionEvent e) {
        sendSavePerformed(null);
        dispose();
    }
    
    private void sendSavePerformed(JComponent comp) {
        if (comp == null) {
            comp = (JPanel) this.getContentPane();
        }
        // Child components cannot be instanceof ConfigurationPanel !
        if (comp instanceof ConfigurationPanel) {
            if (((ConfigurationPanel) comp).isChanged()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(comp.getClass().getName() + ".performChanges()"); //$NON-NLS-1$
                }
                ((ConfigurationPanel) comp).performChanges();
            }
        } else {
            for (int i = 0; i < comp.getComponentCount(); i++) {
                sendSavePerformed((JComponent) comp.getComponent(i));
            }
        }
    }

    private void sendCancelPerformed(JComponent comp) {
        if (comp == null) {
            comp = (JPanel) this.getContentPane();
        }
        // Child components cannot be instanceof ConfigurationPanel !
        if (comp instanceof ConfigurationPanel) {
            if (((ConfigurationPanel) comp).isChanged()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(comp.getClass().getName() + ".cancel()"); //$NON-NLS-1$
                }
                ((ConfigurationPanel) comp).cancel();
            }
        } else {
            for (int i = 0; i < comp.getComponentCount(); i++) {
                sendCancelPerformed((JComponent) comp.getComponent(i));
            }
        }
    }

    private void buttonCancel_actionPerformed(ActionEvent e) {
        sendCancelPerformed(null);
        dispose();
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        switch (e.getKeyCode()){
            case KeyEvent.VK_ESCAPE:
                dispose();
                break;}
    }

}
