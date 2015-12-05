package sqlrunner.xml;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sqlrunner.swinghelper.WindowHelper;

/**
 * @author lolling.jan
 */
public class ExportQueryEditor extends JDialog {

    private static final long serialVersionUID = 1L;

    private javax.swing.JPanel jContentPane      = null;

    private JPanel jPanelEditor      = null;
    private JPanel jPanel            = null;
    private JScrollPane jScrollPane       = null;
    private JTextArea jTextAreaSQL      = null;
    private JButton jButtonOk         = null;
    private JButton jButtonCancel     = null;
    static final int RETURNCODE_CANCEL = 1;
    static final int RETURNCODE_OK     = 0;
    private int returnCode        = RETURNCODE_CANCEL;

    /**
     * This is the default constructor
     */
    public ExportQueryEditor(JFrame parent) {
        super(parent, true);
        initialize();
        pack();
        WindowHelper.locateWindowAtMiddle(parent, this);
    }

    public void setText(String text) {
        jTextAreaSQL.setText(text);
    }

    public String getText() {
        return jTextAreaSQL.getText();
    }

    public int getReturnCode() {
        return returnCode;
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

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle("Edit Export-Query");
        this.setContentPane(getJContentPane());
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new java.awt.BorderLayout());
            jContentPane.add(getJPanelEditor(), java.awt.BorderLayout.CENTER);
            jContentPane.add(getJPanel(), java.awt.BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes jPanelEditor	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanelEditor() {
        if (jPanelEditor == null) {
            jPanelEditor = new JPanel();
            jPanelEditor.setLayout(new BorderLayout());
            jPanelEditor.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
        }
        return jPanelEditor;
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.add(getJButtonOk(), null);
            jPanel.add(getJButtonCancel(), null);
        }
        return jPanel;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getJTextAreaSQL());
            jScrollPane.setPreferredSize(new java.awt.Dimension(400, 100));
        }
        return jScrollPane;
    }

    /**
     * This method initializes jTextAreaSQL	
     * 	
     * @return javax.swing.JTextArea	
     */
    private JTextArea getJTextAreaSQL() {
        if (jTextAreaSQL == null) {
            jTextAreaSQL = new JTextArea();
            jTextAreaSQL.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        }
        return jTextAreaSQL;
    }

    /**
     * This method initializes jButtonOk	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButtonOk() {
        if (jButtonOk == null) {
            jButtonOk = new JButton();
            jButtonOk.setText("Ok");
            jButtonOk.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    returnCode = RETURNCODE_OK;
                    setVisible(false);
                }
            });
        }
        return jButtonOk;
    }

    /**
     * This method initializes jButtonCancel	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButtonCancel() {
        if (jButtonCancel == null) {
            jButtonCancel = new JButton();
            jButtonCancel.setText("Abbrechen");
            jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    returnCode = RETURNCODE_CANCEL;
                    setVisible(false);
                }
            });
        }
        return jButtonCancel;
    }

}
