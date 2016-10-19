/*
 * Created on 09.07.2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package sqlrunner.datetool;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

import sqlrunner.Main;
import sqlrunner.swinghelper.WindowHelper;

public final class DateConverter extends JFrame {

    private static final Logger logger              = Logger.getLogger(DateConverter.class);

    private static final long   serialVersionUID    = 1L;
    private int                 row;
    private int                 col;
    private JPanel              jContentPane        = null;
    private JScrollPane         jScrollPane         = null;
    private JTextArea           textArea            = null;
    private JPanel              jPanel              = null;
    private JLabel              labelCounter        = null;
    private JCheckBox           checkBoxViewAsDate  = null;
    private JComboBox<String>   cbDateFormat        = null;
    
    public DateConverter() {
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setContentPane(getJContentPane());
        this.setTitle(Messages.getString("ValueEditor.title")); 
        pack();
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
            // das Panel zuerst hinzufügen da die TextArea auf Elemente dieses Panels zurückgreift !
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridx = 0;
	            gbc.gridy = 1;
	            jContentPane.add(getJPanel(), gbc);
            }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.fill = GridBagConstraints.BOTH;
	            gbc.gridy = 0;
	            gbc.weightx = 1.0;
	            gbc.weighty = 2.0D;
	            gbc.gridx = 0;
	            jContentPane.add(getJScrollPane(), gbc);
            }
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
            jScrollPane.setPreferredSize(new Dimension(390, 50));
            jScrollPane.setViewportView(getTextArea());
        }
        return jScrollPane;
    }

    /**
     * This method initializes jTextArea	
     * 	
     * @return javax.swing.JTextArea	
     */
    private JTextArea getTextArea() {
        if (textArea == null) {
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.addKeyListener(new java.awt.event.KeyAdapter() {

                @Override
	            public void keyPressed(KeyEvent e) {
	                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        dispose();
                    }
	            }
	        });
            textArea.getDocument().addDocumentListener(new DocumentListener() {

                @Override
				public void insertUpdate(DocumentEvent de) {
                    labelCounter.setText(String.valueOf((textArea.getText()).length()));
                }

                @Override
				public void changedUpdate(DocumentEvent de) {
                    labelCounter.setText(String.valueOf((textArea.getText()).length()));
                }

                @Override
				public void removeUpdate(DocumentEvent de) {
                    labelCounter.setText(String.valueOf((textArea.getText()).length()));
                }

            });
        }
        return textArea;
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
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.anchor = GridBagConstraints.EAST;
	            gbc.gridy = 0;
	            gbc.gridx = 0;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            JLabel jLabel = new JLabel();
	            jLabel.setText(Messages.getString("ValueEditor.countchars"));
	            jPanel.add(jLabel, gbc);
            }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 0;
	            gbc.gridx = 1;
	            gbc.fill = GridBagConstraints.HORIZONTAL;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            gbc.anchor = GridBagConstraints.WEST;
	            labelCounter = new JLabel();
	            labelCounter.setText("0");
	            jPanel.add(labelCounter, gbc);
            }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 1;
	            gbc.gridx = 0;
	            gbc.anchor = GridBagConstraints.WEST;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            gbc.weightx = 2;
	            gbc.fill = GridBagConstraints.HORIZONTAL;
	            jPanel.add(getComboBoxDateFormat(), gbc);
	        }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 1;
	            gbc.gridx = 1;
	            gbc.anchor = GridBagConstraints.EAST;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            jPanel.add(getCheckBoxViewAsDate(), gbc);
	        }
        }
        return jPanel;
    }

    /**
     * This method initializes checkBox	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getCheckBoxViewAsDate() {
        if (checkBoxViewAsDate == null) {
            checkBoxViewAsDate = new JCheckBox();
            checkBoxViewAsDate.setText(Messages.getString("ValueEditor.interpretasdate"));
            checkBoxViewAsDate.addActionListener(new java.awt.event.ActionListener() { 
            	
            	@Override
            	public void actionPerformed(java.awt.event.ActionEvent e) {    
                    String format = (String) cbDateFormat.getSelectedItem();
                    System.out.println("Format:" + format);
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    if (checkBoxViewAsDate.isSelected() == false) {
                        // dann das textuelle Datumsformat in die long-Form wandeln
                        try {
                        	String text = textArea.getText().trim();
                            java.util.Date date = null;
                        	if (text == null || text.isEmpty()) {
                        		date = new Date();
                        	} else {
                        		date = sdf.parse(text);
                        	}
                            textArea.setText(String.valueOf(date.getTime()));
                        } catch (ParseException pe) {
                            logger.warn("converting long->Date failed:"+pe.getMessage());
                        }
                    } else {
                        // die long-Form in die Datumsform wandeln
                        try {
                        	String text = textArea.getText().trim();
                        	Date date = null;
                        	if (text == null || text.isEmpty()) {
                        		date = new Date();
                        	} else {
                            	long time = Long.parseLong(textArea.getText().trim());
                            	date = new java.util.Date(time);
                        	}
                            textArea.setText(sdf.format(date));
                        } catch (NumberFormatException nfe) {
                            logger.warn("converting Date->long failed:"+nfe.getMessage());
                        }
                    }
                    labelCounter.setText(String.valueOf((textArea.getText()).length()));
            	}
            });
            checkBoxViewAsDate.setToolTipText(Messages.getString("ValueEditor.showeditlongasdate")); 
        }
        return checkBoxViewAsDate;
    }
    
    private JComboBox<String> getComboBoxDateFormat() {
    	if (cbDateFormat == null) {
    		cbDateFormat = new JComboBox<String>();
    		cbDateFormat.setEditable(true);
    		prepareCbDateFormat();
    	}
    	return cbDateFormat;
    }

    private void prepareCbDateFormat() {
        int i = 0;
        String formatStr;
        while (true) {
        	cbDateFormat.addItem("yyyy-MM-dd HH:mm:ss.SSS");
            formatStr = Main.getDefaultProperty("DATE_FORMAT_" + String.valueOf(i));
            if (formatStr != null) {
                cbDateFormat.addItem(formatStr);
            } else {
                break;
            }
            i++;
        }
    }

    public static byte[] hexStringToByteArray(String s) {
    	s = s.replace(" ", "").replace("\n", "").trim();
    	if (s.startsWith("x'") && s.endsWith("'")) {
    		s = s.replace("x'", "").replace("'", "");
    	}
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public int getColumnIndex() {
        return col;
    }

    public int getRowIndex() {
        return row;
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        if (visible == false) {
            Main.setUserProperty("CELL_EDITOR_WIDTH", String.valueOf(this.getWidth())); 
            Main.setUserProperty("CELL_EDITOR_HEIGHT", String.valueOf(this.getHeight())); 
        }
        super.setVisible(visible);
    }

    
} //  @jve:decl-index=0:visual-constraint="10,10"
