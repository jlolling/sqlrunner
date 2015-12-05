package sqlrunner.editor;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;

import sqlrunner.swinghelper.WindowHelper;

/**
 *  Gehe zur Zeile... Dialog
 */
public class GotoLineDialog extends JDialog implements ActionListener, KeyListener {

	private final Logger logger = Logger.getLogger(GotoLineDialog.class);
    private static final long serialVersionUID = 1L;
    private JTextField textFieldIndex;
    private JButton buttonJumpToLine;
    private JButton buttonJumpToPos;
    private JLabel jLabelResult;
    private JButton buttonClose;
    private int destLine;
    private JTextComponent editor;

    public GotoLineDialog(Frame frame, String title, JTextComponent editor) {
        super(frame, title, false);
        this.editor = editor;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
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
        this.setResizable(false);
        getContentPane().setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(10, 5, 2, 2);
            JLabel jLabel = new JLabel();
            jLabel.setText(Messages.getString("GotoLineDialog.label"));
            getContentPane().add(jLabel, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 2, 2, 10);
            textFieldIndex = new JTextField();
            textFieldIndex.addKeyListener(this);
            textFieldIndex.requestFocus();
            getContentPane().add(textFieldIndex, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 1;
            gbc.gridx = 0;
            gbc.insets = new Insets(10, 2, 2, 10);
            gbc.anchor = GridBagConstraints.WEST;
            buttonJumpToLine = new JButton();
            buttonJumpToLine.setText(Messages.getString("GotoLineDialog.gotoline"));
            buttonJumpToLine.setActionCommand("gotoline");
            buttonJumpToLine.addActionListener(this);
            getContentPane().add(buttonJumpToLine, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 1;
            gbc.gridx = 1;
            gbc.insets = new Insets(10, 2, 2, 10);
            gbc.anchor = GridBagConstraints.EAST;
            buttonJumpToPos = new JButton();
            buttonJumpToPos.setText(Messages.getString("GotoLineDialog.gotoposition"));
            buttonJumpToPos.setActionCommand("gotoposition");
            buttonJumpToPos.addActionListener(this);
            getContentPane().add(buttonJumpToPos, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.insets = new Insets(2, 2, 10, 2);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 2;
            jLabelResult = new JLabel();
            jLabelResult.setText(" ");
            getContentPane().add(jLabelResult, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 2;
            gbc.gridx = 1;
            gbc.insets = new Insets(2, 2, 10, 10);
            gbc.anchor = GridBagConstraints.EAST;
            buttonClose = new JButton();
            buttonClose.setText(Messages.getString("GotoLineDialog.cancel"));
            buttonClose.setActionCommand("close");
            buttonClose.addActionListener(this);
            getContentPane().add(buttonClose, gbc);
        }
        this.getRootPane().setDefaultButton(buttonJumpToLine);
    }

    public void actionPerformed(ActionEvent e) {
        if ((e.getActionCommand()).equals("gotoline")) {
            if (jumpToLine()) {
                close();
            }
        }
        if ((e.getActionCommand()).equals("gotoposition")) {
            if (jumpToPosition()) {
                close();
            }
        }
        if ((e.getActionCommand()).equals("close")) {
            close();
        }
    }

    private void editLineNumber_keyPressed(KeyEvent e) {
        switch (e.getKeyCode()){
            case KeyEvent.VK_ENTER: {
                if (!jumpToLine()) {
                    break;
                }
            }
            case KeyEvent.VK_ESCAPE:
                close();
            }
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()){
            case WindowEvent.WINDOW_CLOSING:
                close();
                break;
            case WindowEvent.WINDOW_ACTIVATED: {
                textFieldIndex.requestFocus();
                textFieldIndex.selectAll();
                jLabelResult.setText(" "); //$NON-NLS-1$
                break;
            }}
        super.processWindowEvent(e);
    }

    private void close() {
        setVisible(false);
    }
    
    private boolean jumpToPosition() {
    	try {
        	int pos = Integer.parseInt(textFieldIndex.getText());
        	int selectionStart = editor.getSelectionStart();
        	int selectionEnd = editor.getSelectionEnd();
        	if (selectionStart < selectionEnd) {
        		pos = pos + selectionStart;
        	}
    		editor.setCaretPosition(pos);
        	return true;
    	} catch (Exception e) {
    		logger.error(e);
    	}
    	return false;
    }

    private boolean jumpToLine() {
        boolean ok = false;
        final Document doc = editor.getDocument();
        if (doc.getLength() == 0) {
            jLabelResult.setForeground(Color.red);
            jLabelResult.setText(Messages.getString("GotoLineDialog.emptydoc"));
        } else {
            buttonJumpToLine.setEnabled(false);
            // umsetzen der Eingabe in Integer
            try {
                destLine = Integer.parseInt(textFieldIndex.getText()) - 1;
                if (destLine < 0) {
                    throw new NumberFormatException("0"); //$NON-NLS-1$
                }
                final Element root = doc.getDefaultRootElement();
                // bei PlainDocument entspricht eine Textzeile einem Element !!
                // die Elemente sind baumartig am root-Element aufgehängt
                final Element elem = root.getElement(destLine);
                if (elem == null) {
                    jLabelResult.setForeground(Color.red);
                    jLabelResult.setText(Messages.getString("GotoLineDialog.linenotexist"));
                    textFieldIndex.selectAll();
                    textFieldIndex.requestFocus();
                } else {
                    final int p0 = elem.getStartOffset();
                    editor.setCaretPosition(p0);
                    jLabelResult.setForeground(Color.black);
                    jLabelResult.setText(Messages.getString("GotoLineDialog.atpos") + " " + p0 + Messages.getString("GotoLineDialog.founded")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    ok = true;
                }
            } catch (NumberFormatException nfe) {
                jLabelResult.setForeground(Color.red);
                jLabelResult.setText(Messages.getString("GotoLineDialog.invalidvalue") + " " + nfe.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
                textFieldIndex.selectAll();
                textFieldIndex.requestFocus();
            } finally {
                buttonJumpToLine.setEnabled(true);
            }
        }
        return ok;
    }

    public void keyTyped(KeyEvent e) {
    // nothing to do
    }

    public void keyPressed(KeyEvent e) {
        if (e.getSource() == textFieldIndex) {
            editLineNumber_keyPressed(e);
        }
    }

    public void keyReleased(KeyEvent e) {
    // nothing to do
    }
}
