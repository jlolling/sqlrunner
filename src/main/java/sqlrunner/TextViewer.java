package sqlrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sqlrunner.swinghelper.WindowHelper;

public class TextViewer extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JPanel jPanel1=new JPanel();
    private final JScrollPane jScrollPane1=new JScrollPane();
    private final BorderLayout borderLayout1=new BorderLayout();
    private final JTextArea jTextArea1=new JTextArea();

    public TextViewer(JFrame parentFrame, String title, String text) {
        super(parentFrame, title, true);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            WindowHelper.locateWindowAtMiddle(parentFrame, this);
            this.jTextArea1.setText(text);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TextViewer(JDialog parentFrame, String title, String text) {
        super(parentFrame, title, true);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            WindowHelper.locateWindowAtMiddle(parentFrame, this);
            this.jTextArea1.setText(text);
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
        jPanel1.setLayout(borderLayout1);
        jTextArea1.setBackground(Main.info);
        jTextArea1.setLineWrap(true);
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setEditable(false);
        jPanel1.setPreferredSize(new Dimension(400, 100));
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        (this.getContentPane()).add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        (jScrollPane1.getViewport()).add(jTextArea1, null);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
    }

}
