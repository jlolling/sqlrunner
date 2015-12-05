package sqlrunner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.Document;

import sqlrunner.swinghelper.WindowHelper;

/**
 * kleines Fenster um die sonst nur im öffnen-Dialog sichtbaren Dateiinfos
 * sichtbar zu machen
 */
public final class FileInfo extends JDialog implements ActionListener, KeyListener {

    private static final long serialVersionUID = 1L;
    private File currentFile = null;
    private long lastModified = 0;
    private long lastLoaded = 0;
    private Document currentDocument = null;

    public FileInfo(MainFrame mainFrame) {
        super(mainFrame, "File info", true); // modaler Dialog
        currentFile = mainFrame.getCurrentFile();
        lastModified = mainFrame.getCurrentFileLastModified();
        lastLoaded = mainFrame.getCurrentFileLoaded();
        currentDocument = mainFrame.getDocument();
        addKeyListener(this);
        setContentPane(getJContentPane());
        pack();
        WindowHelper.locateWindowAtMiddle(mainFrame, this);
        setVisible(true);
    }

    private JPanel getJContentPane() {
        SimpleDateFormat sdf = new SimpleDateFormat(MainFrame.getDateFormatMask());
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 0;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(Messages.getString("FileInfo.path"));
            label.setHorizontalAlignment(JLabel.RIGHT);
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 1;
            gb.gridy = 0;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.WEST;
            JTextField textField = new JTextField();
            textField.setEditable(false);
            if (currentFile != null) {
                textField.setText(currentFile.getAbsolutePath());
                textField.setSelectionStart(0);
                textField.setSelectionEnd(textField.getText().length());
                textField.requestFocusInWindow();
            } else {
                textField.setText(Messages.getString("FileInfo.notsaved"));
            }
            contentPane.add(textField, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 1;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(Messages.getString("FileInfo.loadedat"));
            label.setHorizontalAlignment(JLabel.RIGHT);
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 1;
            gb.gridy = 1;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel();
            if (lastLoaded > 0) {
                label.setText(sdf.format(new Date(lastLoaded)));
            } else {
                label.setText(null);
            }
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 2;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(Messages.getString("FileInfo.modifiedat"));
            label.setHorizontalAlignment(JLabel.RIGHT);
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 1;
            gb.gridy = 2;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel();
            if (lastModified > 0) {
                label.setText(sdf.format(new Date(Main.getLastModified(currentFile))));
            } else {
                label.setText(null);
            }
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 3;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(Messages.getString("FileInfo.filesize"));
            label.setHorizontalAlignment(JLabel.RIGHT);
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 1;
            gb.gridy = 3;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel();
            if (currentFile != null) {
                label.setText(String.valueOf(currentFile.length()));
            } else {
                label.setText(null);
            }
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 4;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(Messages.getString("FileInfo.countlines"));
            label.setHorizontalAlignment(JLabel.RIGHT);
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 1;
            gb.gridy = 4;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel();
            if (currentDocument != null) {
                int countLines = currentDocument.getDefaultRootElement().getElementIndex(currentDocument.getLength() + 1);
                label.setText(String.valueOf(countLines));
            } else {
                label.setText(null);
            }
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 0;
            gb.gridy = 5;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(Messages.getString("FileInfo.countchars"));
            label.setHorizontalAlignment(JLabel.RIGHT);
            contentPane.add(label, gb);
        }
        {
            GridBagConstraints gb = new GridBagConstraints();
            gb.gridx = 1;
            gb.gridy = 5;
            gb.insets = new Insets(2, 2, 2, 2);
            gb.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel();
            if (currentDocument != null) {
                int countChars = currentDocument.getLength();
                label.setText(String.valueOf(countChars));
            } else {
                label.setText(null);
            }
            contentPane.add(label, gb);
        }
        return contentPane;
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

    // Window-Ereignisbehandlung
    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()){
            case WindowEvent.WINDOW_CLOSING:
                cancel();
                break;}
        super.processWindowEvent(e);
    }

    private void cancel() {
        dispose();
    }

    public void actionPerformed(ActionEvent e) {
        if ((e.getActionCommand()).equals("close")) {
            cancel();
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            cancel();
        }
    }

    public void keyTyped(KeyEvent e) {} // Dummy für KeyListener

    public void keyReleased(KeyEvent e) {} // Dummy für KeyListener

}
