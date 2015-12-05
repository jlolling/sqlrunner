package sqlrunner.base64;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;

import sqlrunner.Main;

/**
 * @author lolling.jan
 */
public class Base64Viewer extends JFrame {

    private static final long serialVersionUID = 1L; 
    private static final Logger logger = Logger.getLogger(Base64Viewer.class);
    private javax.swing.JPanel jContentPane = null;
    private JScrollPane jScrollPane = null;
    private JTextArea jTextArea = null;
    private JPanel jPanel = null;
    private JButton jButtonEncode = null;
    private JButton jButtonDecode = null;
    private JCheckBox jCheckBoxBinaryData = null;
    private JComboBox<String> jComboBoxCharSet = null;

    /**
     * This is the default constructor
     */
    public Base64Viewer() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle("Base64 transcoder");
        this.setContentPane(getJContentPane());
        this.pack();
        this.setVisible(true);
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
            jContentPane.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
            jContentPane.add(getJPanel(), java.awt.BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getJTextArea());
            jScrollPane.setPreferredSize(new Dimension(400, 100));
        }
        return jScrollPane;
    }

    /**
     * This method initializes jTextArea	
     * 	
     * @return javax.swing.JTextArea	
     */
    private JTextArea getJTextArea() {
        if (jTextArea == null) {
            jTextArea = new JTextArea();
            jTextArea.setLineWrap(true);
        }
        return jTextArea;
    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.setLayout(new GridBagLayout());
            GridBagConstraints gb = new GridBagConstraints();
            gb.insets = new Insets(2, 2, 2, 2);
            gb.gridy = 0;
            gb.gridx = 0;
            jPanel.add(getJButtonEncode(), gb);
            gb.gridy = 0;
            gb.gridx = 1;
            jPanel.add(getJComboBoxCharSet(), gb);
            gb.gridy = 0;
            gb.gridx = 2;
            jPanel.add(getJButtonDecode(), gb);
            gb.gridy = 0;
            gb.gridx = 3;
            jPanel.add(getJCheckBoxBinaryData(), gb);
        }
        return jPanel;
    }

    private JComboBox<String> getJComboBoxCharSet() {
        if (jComboBoxCharSet == null) {
            jComboBoxCharSet = new JComboBox<String>();
            jComboBoxCharSet.setEditable(true);
            jComboBoxCharSet.addItem("UTF-8");
            jComboBoxCharSet.addItem("UTF-16");
            jComboBoxCharSet.addItem("ISO-8859-1");
            jComboBoxCharSet.addItem("ISO-8859-15");
            jComboBoxCharSet.addItem("Cp1251");
            jComboBoxCharSet.addItem("MacRoman");
        }
        return jComboBoxCharSet;
    }

    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButtonEncode() {
        if (jButtonEncode == null) {
            jButtonEncode = new JButton();
            jButtonEncode.setText("encode");
            jButtonEncode.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (jCheckBoxBinaryData.isSelected()) {
                        readBinaryData();
                    } else {
                        // den text in Base64 transformieren
                        String text = cleanBase64String(jTextArea.getText());
                        if (text != null && text.length() > 0) {
                            try {
                                jTextArea.setText(Base64.toString(Base64.encode(text.getBytes((String) jComboBoxCharSet.getSelectedItem())),
                                    true));
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
        return jButtonEncode;
    }

    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButtonDecode() {
        if (jButtonDecode == null) {
            jButtonDecode = new JButton();
            jButtonDecode.setText("decode");
            jButtonDecode.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (jCheckBoxBinaryData.isSelected()) {
                        writeBinaryData();
                    } else {
                        String text = jTextArea.getText();
                        if (text != null && text.length() > 0) {
                            try {
                                jTextArea.setText(Base64.getText(text, (String) jComboBoxCharSet.getSelectedItem()));
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
        return jButtonDecode;
    }

    private void writeBinaryData() {
        String base64EncodedData = cleanBase64String(jTextArea.getText());
        if (base64EncodedData != null && base64EncodedData.length() > 0) {
            final JFileChooser chooser = new JFileChooser();
            final String fileName = Main.getUserProperty("BASE64_FILE", System.getProperty("user.home"));
            chooser.setCurrentDirectory(new File(fileName).getParentFile());
            chooser.setSelectedFile(new File(fileName));
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle("decoded base64 data");
            final int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                final File f = chooser.getSelectedFile();
                Main.setUserProperty("BASE64_FILE", f.getAbsolutePath());
                byte[] binaryData = Base64.decode(base64EncodedData.getBytes());
                if (logger.isDebugEnabled()) {
                    logger.debug("decode and write file " + f.getAbsolutePath() + " length=" + binaryData.length);
                }
                try {
                    FileOutputStream fout = new FileOutputStream(f);
                    fout.write(binaryData);
                    fout.close();
                } catch (IOException e) {
                    logger.error("writeBinaryData failed: " + e.getMessage(), e);
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Error while writing file", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "no base64 data in text field", "decode", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void readBinaryData() {
        final JFileChooser chooser = new JFileChooser();
        final String fileName = Main.getUserProperty("BASE64_FILE", System.getProperty("user.home"));
        chooser.setCurrentDirectory(new File(fileName).getParentFile());
        chooser.setSelectedFile(new File(fileName));
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("encode base64 data");
        final int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File f = chooser.getSelectedFile();
            Main.setUserProperty("BASE64_FILE", f.getAbsolutePath());
            long filesize = f.length();
            if (filesize < Integer.MAX_VALUE) {
                if (logger.isDebugEnabled()) {
                    logger.debug("read und encode file " + f.getAbsolutePath() + " length=" + filesize);
                }
                byte[] binaryData = new byte[(int) filesize];
                try {
                    FileInputStream fin = new FileInputStream(f);
                    int nbytes = fin.read(binaryData);
                    fin.close();
                    byte[] base64EncodedData = Base64.encode(binaryData, nbytes);
                    jTextArea.setText(Base64.toString(base64EncodedData, true));
                } catch (IOException e) {
                    logger.error("readBinaryData failed: " + e.getMessage(), e);
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Error while reading", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "File size to large.\nThis fnction can handle only files with a size until " + Integer.MAX_VALUE + " bytes.", "Read file and encode", JOptionPane.INFORMATION_MESSAGE);
            }
        }
	}
	
	private static String cleanBase64String(String base64EncodedText) {
	    StringBuffer sb = new StringBuffer(base64EncodedText.length());
	    char c;
	    for (int i = 0; i < base64EncodedText.length(); i++) {
	        c = base64EncodedText.charAt(i);
	        if (c != '\n' && c != ' ' && c != '\t' && c != '\r') {
	            sb.append(c);
	        }
	    }
	    return sb.toString();
	}
	
	/**
	 * This method initializes jCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */    
	private JCheckBox getJCheckBoxBinaryData() {
		if (jCheckBoxBinaryData == null) {
			jCheckBoxBinaryData = new JCheckBox();
			jCheckBoxBinaryData.setText("binary data");
		}
		return jCheckBoxBinaryData;
	}
    
}
