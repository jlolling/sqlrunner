package sqlrunner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sqlrunner.swinghelper.WindowHelper;

public class DBMessageDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private final JPanel jPanelText=new JPanel();
    private final JPanel jPanelButtons=new JPanel();
    private final JScrollPane jScrollPaneText=new JScrollPane();
    private final JTextArea textArea=new JTextArea();
    private final JButton buttonContinue=new JButton();
    private final JButton buttonCancel=new JButton();
    private final JCheckBox checkBoxIgnoreErrors=new JCheckBox();
    public static final int CONTINUE             = 0;
    public static final int INGORE_ERRORS        = 1;
    public static final int CANCEL               = 2;
    private int returnCode           = -1;

    public DBMessageDialog(JFrame parent, String message, String title) {
        super(parent, title, true);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            WindowHelper.locateWindowAtMiddle(parent, this);
            textArea.setText(message);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DBMessageDialog(JFrame parent, String message, String title, boolean enableContinue) {
        super(parent, title, true);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            Dimension screen = getToolkit().getScreenSize();
            int x=(screen.width >> 1) - (this.getSize().width >> 1);
            int y=(screen.height >> 1) - (this.getSize().height >> 1);
            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }
            setLocation(x, y);
            buttonContinue.setEnabled(enableContinue);
            checkBoxIgnoreErrors.setEnabled(enableContinue);
            textArea.setText(message);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if(!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    private void initComponents() throws Exception {
        final int width=Integer.parseInt(Main.getUserProperty("DB_MESSAGE_WIDTH","300"));
        final int height=Integer.parseInt(Main.getUserProperty("DB_MESSAGE_HEIGHT","200"));
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = 1;
        gridBagConstraints2.gridwidth = 2;
        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints1.gridy = 0;
        gridBagConstraints1.gridx = 1;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        jPanelText.setPreferredSize(new Dimension(width, height));
        jPanelButtons.setPreferredSize(new Dimension(300, 75));
        jPanelButtons.setLayout(new GridBagLayout());
        jPanelText.setLayout(new BorderLayout());
        textArea.setLineWrap(true);
        textArea.setBackground(new Color(255, 255, 223));
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setFont(new Font("Dialog", 0, 14));
        buttonContinue.setText(Messages.getString("DBMessageDialog.continue"));
        buttonContinue.addActionListener(this);
        buttonCancel.setText(Messages.getString("DBMessageDialog.cancel"));
        buttonCancel.addActionListener(this);
        checkBoxIgnoreErrors.setText(Messages.getString("DBMessageDialog.ignorenexterrors"));
        getContentPane().add(jPanelText, BorderLayout.CENTER);
        jPanelText.add(jScrollPaneText, BorderLayout.CENTER);
        jPanelButtons.add(buttonContinue, gridBagConstraints);
        jPanelButtons.add(checkBoxIgnoreErrors, gridBagConstraints1);
        jPanelButtons.add(buttonCancel, gridBagConstraints2);
        jScrollPaneText.getViewport().add(textArea, null);
        getContentPane().add(jPanelButtons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(buttonCancel);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            returnCode = CANCEL;
            setVisible(false);
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (checkBoxIgnoreErrors.isSelected()) {
                returnCode = INGORE_ERRORS;
            } else {
                returnCode = CONTINUE;
            }
            setVisible(false);
        }
    }

    public void cancel() {
        setVisible(false);
        dispose();
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonContinue) {
            if (checkBoxIgnoreErrors.isSelected()) {
                returnCode = INGORE_ERRORS;
            } else {
                returnCode = CONTINUE;
            }
        } else if (e.getSource() == buttonCancel) {
            returnCode = CANCEL;
        }
        setVisible(false);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if ((jPanelText.getSize().width > 300) && (jPanelText.getSize().height > 200)) {
            // paint wird schon vor dem ersten jbInit aufgerufen und da darf noch kein Wert gesetzt werden
            Main.setUserProperty("DB_MESSAGE_WIDTH", String.valueOf(jPanelText.getSize().width)); 
            Main.setUserProperty("DB_MESSAGE_HEIGHT", String.valueOf(jPanelText.getSize().height)); 
        } else {
            Main.setUserProperty("DB_MESSAGE_WIDTH", "300");  
            Main.setUserProperty("DB_MESSAGE_HEIGHT", "200");  
        }
    }

}
