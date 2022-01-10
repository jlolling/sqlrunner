package sqlrunner;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dbtools.DatabaseSessionPoolInfoPanel;
import sqlrunner.log4jpanel.Log4JPanel;
import sqlrunner.log4jpanel.LogPanel;
import sqlrunner.swinghelper.WindowHelper;

/**
 * Klasse für das Info-Dialogfenster
 * mit F5 kann die Information zum Speicher aktualisiert werden
 * Der Dialog ist nicht modal
 * die ReleaseNotes werden aus dem Archiv alv.jar gelesen
 * und in der TextArea dargestellt (schreibgeschützt)
 */
public final class AboutDialog extends JDialog implements ActionListener {

    private static final Logger logger = LogManager.getLogger(AboutDialog.class);
    private static final long serialVersionUID = 1L;
    private JButton buttonCancel;
    private long freeMem;
    private long totalMem;
    private transient Runtime rt = null;
    private JPanel jPanelInfo;
    private JLabel jLabelRAMfree;
    private JLabel jLabelRAMtotal;
    private Timer updateTimer;
    private JPanel jContentPane = null;
    private DatabaseSessionPoolInfoPanel poolInfoPanel;

    public AboutDialog() {
        super();
        getRootPane().putClientProperty("Window.style", "small");
        initComponents();
    }

    public AboutDialog(JFrame parent) {
        super(parent);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            rt = Runtime.getRuntime();
            initComponents();
            pack();
        } catch (Exception e) {
            System.err.println("AboutDialog: error in constructor"); //$NON-NLS-1$
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
            cancel();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (isShowing() == false) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {
            }
        }
        if (visible) {
            updateTimer = new javax.swing.Timer(1000, new java.awt.event.ActionListener() {
                // wenn Timeout erreicht ...
                public void actionPerformed(ActionEvent e) {
                    refreshMemoryView();
                }
            });
            updateTimer.setRepeats(true); // immer wieder
            updateTimer.setInitialDelay(2000);
            updateTimer.start(); // Timer starten
            poolInfoPanel.startRefreshing();
        } else {
            if (updateTimer != null && updateTimer.isRunning()) {
                updateTimer.stop();
                updateTimer = null;
            }
            poolInfoPanel.stopRefreshing();
        }
        super.setVisible(visible);
    }

    private void initComponents() {
        this.setContentPane(getJContentPane());
        setTitle("Info"); //$NON-NLS-1$
        getRootPane().setDefaultButton(getJButtonClose());
        setPreferredSize(new Dimension(600, 500));
    }

    // Window-Ereignisbehandlung
    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()) {
            case WindowEvent.WINDOW_CLOSING:
                cancel();
                break;
            case WindowEvent.WINDOW_ACTIVATED:
                refreshMemoryView();
        }
        super.processWindowEvent(e);
    }

    private String loadTextResource(String fname) throws IOException {
        String ret = null;
        try {
            InputStream is = getClass().getResourceAsStream("/" + fname); //$NON-NLS-1$
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8"); //$NON-NLS-1$
                StringBuffer sb = new StringBuffer();
                char[] buffer = new char[1024];
                int length = 0;
                while ((length = isr.read(buffer)) > 0) {
                    sb.append(buffer, 0, length);
                }
                isr.close();
                is.close();
                ret = sb.toString();
            }
        } catch (Exception e) {
            logger.error("loadTextResource: failed: " + e.getMessage(), e); //$NON-NLS-1$
        }
        return ret;
    }

    void cancel() {
        updateTimer.stop();
        updateTimer = null;
        dispose();
    }

    private void refreshMemoryView() {
        totalMem = rt.totalMemory() / 1024;
        freeMem = rt.freeMemory() / 1024;
        long maxMem = rt.maxMemory() / 1024;
        System.out.println("total=" + totalMem);
        System.out.println("maxMemory=" + (rt.maxMemory() / 1024));
        System.out.println("used=" + (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1024));
        System.out.println("total-free=used=" + (totalMem - freeMem));
        System.out.println("Divide from maxMem used=" + (((double) (totalMem - freeMem)) / ((double) maxMem)));
        System.out.println("percentage from maxMem used=" + Math.round((((double) (totalMem - freeMem)) / ((double) maxMem)) * 100));
        jLabelRAMfree.setText(freeMem + " kbyte");
        jLabelRAMtotal.setText(totalMem + " kbyte");
    }

    /**
     * This method initializes jContentPane    
     *     
     * @return javax.swing.JPanel    
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.fill = GridBagConstraints.BOTH;
                jContentPane.add(getJTabbedPane(), gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.insets = new Insets(2, 2, 2, 2);
                jContentPane.add(getJButtonClose(), gbc);
            }
        }
        return jContentPane;
    }
    
    private JButton getJButtonClose() {
        if (buttonCancel == null) {
            buttonCancel = new JButton();
            buttonCancel.setText(Messages.getString("AboutDialog.close")); //$NON-NLS-1$
            buttonCancel.setActionCommand("cancel"); //$NON-NLS-1$
            buttonCancel.addActionListener(this);
        }
        return buttonCancel;
    }
    
    private JTabbedPane jTabbedPane = null;

    private JTabbedPane getJTabbedPane() {
        if (jTabbedPane == null) {
            jTabbedPane = new JTabbedPane();
            jTabbedPane.add(Messages.getString("AboutDialog.info"), getJPanelInfo());
            jTabbedPane.add(Messages.getString("AboutDialog.releasenotes"), getScrollPaneReleaseNotes());
            jTabbedPane.add(Messages.getString("AboutDialog.systemproperties"), getScrollPaneSystemProperties());
            jTabbedPane.add(Messages.getString("AboutDialog.uidefaults"), getScrollPaneLnFProperties());
            jTabbedPane.add(Messages.getString("AboutDialog.poolinfo"), getDatabasePoolInfoPanel());
            jTabbedPane.add(Messages.getString("AboutDialog.loginfo"), LogPanel.getInstance());
            jTabbedPane.add(Messages.getString("AboutDialog.log4j"), new Log4JPanel());
        }
        return jTabbedPane;
    }

    private JPanel getDatabasePoolInfoPanel() {
        if (poolInfoPanel == null) {
            poolInfoPanel = new DatabaseSessionPoolInfoPanel();
        }
        return poolInfoPanel;
    }
    
	private JScrollPane getScrollPaneSystemProperties() {
        JScrollPane sp = new JScrollPane();
        JTextArea jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        jTextArea.setLineWrap(true);
        jTextArea.setWrapStyleWord(true);
        @SuppressWarnings({ "rawtypes", "unchecked" })
		TreeMap<String, String> propTreeMap = new TreeMap(System.getProperties());
        for (Entry<String, String> entry: propTreeMap.entrySet()) {
            jTextArea.append(entry.getKey() + "=" + entry.getValue() + "\n");
        }
        sp.setViewportView(jTextArea);
        jTextArea.setCaretPosition(0);
        return sp;
    }

	private JScrollPane getScrollPaneLnFProperties() {
        JScrollPane sp = new JScrollPane();
        JTextArea jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        jTextArea.setLineWrap(true);
        jTextArea.setWrapStyleWord(true);
        try {
	        UIDefaults uid = UIManager.getDefaults();
			TreeMap<String, Object> propTreeMap = new TreeMap<String, Object>();
			for (Entry<Object, Object> entry : uid.entrySet()) {
				propTreeMap.put(entry.getKey().toString(), entry.getValue());
			}
	        for (Entry<String, Object> entry : propTreeMap.entrySet()) {
	            jTextArea.append(entry.getKey() + "=" + entry.getValue() + "\n");
	        }
        } catch (Exception e) {
        	logger.error("show l&f defaults failed:" + e.getMessage(), e);
        }
        sp.setViewportView(jTextArea);
        jTextArea.setCaretPosition(0);
        return sp;
    }

	private JScrollPane getScrollPaneReleaseNotes() {
        JScrollPane jScrollPane = new JScrollPane();
        JTextArea jTextAreaReleaseNotes = new JTextArea();
        jTextAreaReleaseNotes.setEditable(false);
        try {
            jTextAreaReleaseNotes.setText(loadTextResource("ReleaseNotes.txt"));
        } catch (IOException ex) {
            logger.error("loading releasenotes text failed: " + ex.getMessage(), ex);
        }
        jTextAreaReleaseNotes.setCaretPosition(0);
        jScrollPane.setViewportView(jTextAreaReleaseNotes);
        return jScrollPane;
    }

    private JPanel getJPanelInfo() {
        if (jPanelInfo == null) {
            jPanelInfo = new JPanel();
            jPanelInfo.setBorder(BorderFactory.createEtchedBorder());
            jPanelInfo.setLayout(new GridBagLayout());
            int y = 0;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.version"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Main.VERSION);
                jPanelInfo.add(jLabel, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.author"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText("Jan Lolling");
                jPanelInfo.add(jLabel, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.ramfree"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                jLabelRAMfree = new JLabel();
                jPanelInfo.add(jLabelRAMfree, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.ramtotal"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                jLabelRAMtotal = new JLabel();
                jPanelInfo.add(jLabelRAMtotal, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.javaversion"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(System.getProperty("java.version"));
                jPanelInfo.add(jLabel, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.screensize"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                StringBuilder text = new StringBuilder();
                text.append(WindowHelper.getAllScreenWidth());
                text.append(" x ");
                text.append(WindowHelper.getAllScreenHeight());
                if (WindowHelper.getScreenCount() > 1) {
                	text.append(" ( ");
                	Rectangle[] screens = WindowHelper.getScreensBounds();
                	for (Rectangle r : screens) {
                		text.append("[");
                		text.append(r.x);
                		text.append(",");
                		text.append(r.y);
                		text.append(",");
                		text.append(r.width);
                		text.append(",");
                		text.append(r.height);
                		text.append("] ");
                	}
                	text.append(")");
                }
                jLabel.setText(text.toString());
                jPanelInfo.add(jLabel, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.codebase"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Main.getCodebase()); //$NON-NLS-1$
                jPanelInfo.add(jLabel, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.configdir"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Main.getWorkDirectory()); //$NON-NLS-1$
                jPanelInfo.add(jLabel, gbc);
            }
            y++;
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("AboutDialog.processinfo"));
                jPanelInfo.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = y;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(ManagementFactory.getRuntimeMXBean().getName()); //$NON-NLS-1$
                jPanelInfo.add(jLabel, gbc);
            }
        }
        return jPanelInfo;
    }
    
}
