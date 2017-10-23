package sqlrunner.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.swinghelper.WindowHelper;

public class TextViewer extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JPanel jPanel1=new JPanel();
    private final JScrollPane jScrollPane1=new JScrollPane();
    private final BorderLayout borderLayout1=new BorderLayout();
    private final JTextArea jTextArea1 = new JTextArea();

    public TextViewer(JFrame frame, String title, String text) {
        super(frame, title, true);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            Dimension window = frame.getSize();
            int x=(frame.getLocation().x + (window.width >> 1)) - (this.getSize().width >> 1);
            int y=(frame.getLocation().y + (window.height >> 1)) - (this.getSize().height >> 1);
            setLocation(x, y);
            this.jTextArea1.setText(text);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TextViewer(MainFrame mainFrame, String title, String text, boolean selectAllText) {
        super(mainFrame, title, false);
        try {
            initComponents();
            pack();
            WindowHelper.locateWindowAtMiddle(mainFrame, this);
            this.jTextArea1.setText(text);
            if (selectAllText) {
                selectAllText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TextViewer(MainFrame mainFrame, boolean modal, String title, String text, boolean selectAllText) {
        super(mainFrame, title, modal);
        try {
            initComponents();
            pack();
            WindowHelper.locateWindowAtMiddle(mainFrame, this);
            this.jTextArea1.setText(text);
            if (selectAllText) {
                selectAllText();
            }
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
        jPanel1.setPreferredSize(new Dimension(300, 100));
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        (this.getContentPane()).add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        (jScrollPane1.getViewport()).add(jTextArea1, null);
    }

    public void setToolTipText(String text) {
        jTextArea1.setToolTipText(text);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            setVisible(false);
        }
    }

    public void setText(String text) {
        jTextArea1.setText(text);
    }

    public String getText() {
        return jTextArea1.getText();
    }

    public void setEditable(boolean editable) {
        jTextArea1.setEditable(editable);
    }

    public void selectAllText() {
        jTextArea1.selectAll();
    }

}
