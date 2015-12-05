package sqlrunner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import sqlrunner.swinghelper.WindowHelper;

public final class AdminSQLConfigDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private final JPanel        jPanel1           = new JPanel();
    private final JScrollPane   jScrollPaneValues = new JScrollPane();
    private final JPanel        jPanelButtons     = new JPanel();
    private final JButton       buttonCancel      = new JButton();
    private final JButton       buttonOk          = new JButton();
    private final JPanel        panelParameters   = new JPanel() {

        private static final long serialVersionUID = 1L;
        Insets insets = new Insets(0, 4, 0, 0);

        @Override
        public Insets getInsets() {
            return insets;
        }
    };
    private StringBuffer        sql;
    private String              sqltemplate;
    private final static String START_LIMITER     = "{";
    private final static String END_LIMITER       = "}";
    private Vector<ParameterInput>              parameters        = new Vector<ParameterInput>();
    static final int            OK                = 0;
    static final int            CANCEL            = 1;
    private int                 returnCode        = CANCEL;

    public AdminSQLConfigDialog(JFrame parent, String comment, String sqltemplate) {
        super(parent, comment, true);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            jbInit();
            pack();
            Dimension screen = (this.getToolkit()).getScreenSize();
            // neue position genau in der Mitte des Bildschirmes !
            int x = (screen.width >> 1) - (this.getSize().width >> 1);
            int y = (screen.height >> 1) - (this.getSize().height >> 1);
            setLocation(x, y);
            setSQLTemplate(sqltemplate);
            setVisible(true);
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

    /**
     * initialisiert alle Komponenten
     */
    private void jbInit() throws Exception {
        (this.getRootPane()).setDefaultButton(buttonOk);
        buttonCancel.setText("Abbrechen");
        buttonCancel.addActionListener(this);
        buttonOk.setText("OK");
        buttonOk.addActionListener(this);
        jPanel1.setPreferredSize(new Dimension(300, 200));
        jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.Y_AXIS));
        panelParameters.setBackground(SystemColor.control);
        jPanel1.add(jScrollPaneValues);
        panelParameters.setLayout(new BoxLayout(panelParameters, BoxLayout.Y_AXIS));
        jScrollPaneValues.setViewportView(panelParameters);
        jPanelButtons.add(buttonOk, null);
        jPanelButtons.add(buttonCancel, null);
        this.getContentPane().add(jPanel1, BorderLayout.CENTER);
        this.getContentPane().add(jPanelButtons, BorderLayout.SOUTH);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
    }

    private void addParameterInput(String name, int p0, int p1) {
        final ParameterInput pi = new ParameterInput(name, p0, p1);
        parameters.addElement(pi);
        panelParameters.add(pi);
    }

    public void setSQLTemplate(String sqltemplate_loc) {
        this.sqltemplate = sqltemplate_loc;
        this.sql = new StringBuffer();
        parameters = new Vector<ParameterInput>();
        // Zerlegen des templates und nach zu ersetzenden Passagen suchen
        boolean fertig = false;
        int p0;
        int p1 = 0;
        while (!fertig) {
            p0 = sqltemplate_loc.indexOf(START_LIMITER, p1);
            if (p0 != -1) {
                p1 = sqltemplate_loc.indexOf(END_LIMITER, p0);
                if (p1 != -1) {
                    addParameterInput(sqltemplate_loc.substring(p0 + 1, p1), p0, p1 + 1);
                    p1 = p0 + 1; // neuen Suchbeginn festlegen
                }
            } else {
                fertig = true;
            }
        }
    }

    public String getSQL() {
        ParameterInput pi0 = null;
        ParameterInput pi1;
        // den Teil bis zum ersten Parameter zusammensetzen
        if (parameters.size() > 0) {
            pi0 = parameters.elementAt(0);
            sql.append(sqltemplate.substring(0, pi0.getStartIndex()));
        }
        for (int i = 0; i < parameters.size(); i++) {
            pi0 = parameters.elementAt(i);
            sql.append(pi0.getValue());
            if (i + 1 < parameters.size()) { // ist ein weitere Parameter vorhanden ?
                pi1 = parameters.elementAt(i + 1);
                // zwischenraum hinzufügen
                sql.append(sqltemplate.substring(pi0.getEndIndex(), pi1.getStartIndex()));
            }
        }
        // das Ende hinzufügen
        if (pi0 != null) {
            sql.append(sqltemplate.substring(pi0.getEndIndex(), sqltemplate.length()));
        }
        return sql.toString();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonCancel) {
            returnCode = CANCEL;
            dispose();
        } else if (e.getSource() == buttonOk) {
            // die Parametertemplates ersetzen und komplette SQL in sql speichern
            returnCode = OK;
            dispose();
        }
    }

    public int getReturnCode() {
        return returnCode;
    }

    static class ParameterInput extends JPanel {

        private static final long serialVersionUID = 1L;
        private JLabel     label;
        private JTextField textField;
        private int        p0;
        private int        p1;

        ParameterInput(String paramName, int p0, int p1) {
            this.p0 = p0;
            this.p1 = p1;
            //            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setLayout(null);
            label = new JLabel();
            label.setForeground(Color.black);
            label.setHorizontalAlignment(JLabel.RIGHT);
            label.setBounds(2, 2, 100, 25);
            label.setText(paramName);
            label.setToolTipText(paramName);
            textField = new JTextField();
            textField.setBounds(106, 2, 160, 25);
            textField.setBackground(SystemColor.info);
            add(label);
            add(textField);
            setPreferredSize(new Dimension((4 + label.getBounds().width) + textField.getBounds().width, 29));
        }

        public String getParamName() {
            return label.getText();
        }

        public int getStartIndex() {
            return p0;
        }

        public int getEndIndex() {
            return p1;
        }

        public String getValue() {
            return (textField.getText()).trim();
        }

    } // end of class ParameterInput

}
