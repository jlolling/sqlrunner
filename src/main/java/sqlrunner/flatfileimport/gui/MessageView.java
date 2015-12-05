package sqlrunner.flatfileimport.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import sqlrunner.flatfileimport.FileImporter;
import sqlrunner.swinghelper.WindowHelper;

public class MessageView extends JFrame {

    private static final long serialVersionUID = 1L;
    private final JScrollPane jScrollPane1=new JScrollPane();
    private final JTextArea textArea = new JTextArea();
    private final BorderLayout borderLayout1=new BorderLayout();
    private final JPanel jPanel1=new JPanel();
    private final JButton buttonClose=new JButton();
    private final JButton buttonAbort=new JButton();
    private transient FileImporter importer;
    private boolean enabled       = true;

    public MessageView() {
        try {
            getRootPane().putClientProperty("Window.style", "small");
            jbInit();
            pack();
            Dimension screen = this.getToolkit().getScreenSize();
            // neue position genau in der Mitte des Bildschirmes !
            int x = (screen.width >> 1) - (this.getSize().width >> 1);
            int y = (screen.height >> 1) - (this.getSize().height >> 1);
            setLocation(x, y);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFlatFileImportObject(FileImporter importer_loc) {
        this.importer = importer_loc;
    }

    public JTextComponent getTextComponent() {
        return textArea;
    }

    public void setVisible(boolean visible) {
        if(!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    private void jbInit() throws Exception {
        setTitle("Import-Message-View");
        this.getContentPane().setLayout(borderLayout1);
        buttonClose.setText("Schliessen");
        buttonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buttonClose_actionPerformed(e);
            }
        });
        buttonAbort.setText("Import abbrechen");
        buttonAbort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buttonAbort_actionPerformed(e);
            }
        });
        this.getContentPane().add(jScrollPane1, BorderLayout.CENTER);
        this.getContentPane().add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(buttonAbort, null);
        jPanel1.add(buttonClose, null);
        jScrollPane1.setPreferredSize(new Dimension(400, 400));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        jScrollPane1.getViewport().add(textArea, null);
    }

    // Window-Ereignisbehandlung
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()){
            case WindowEvent.WINDOW_CLOSING:
                if (enabled) {
                    cancel();
                    super.processWindowEvent(e);
                }
                break;
            default:
                super.processWindowEvent(e);}
    }

    public void setEnabled(boolean enabled_loc) {
        this.enabled = enabled_loc;
        this.buttonAbort.setEnabled(!enabled_loc);
        this.buttonClose.setEnabled(enabled_loc);
    }

    private void cancel() {
        dispose(); // nicht mehr sichtbar machen
    }

    private void buttonAbort_actionPerformed(ActionEvent e) {
        importer.interrupt();
        setEnabled(true);
    }

    private void buttonClose_actionPerformed(ActionEvent e) {
        cancel();
    }

}
