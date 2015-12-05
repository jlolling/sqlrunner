/*
 * Created on 09.07.2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package sqlrunner;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
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

import sqlrunner.editor.SearchReplaceDialog;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.resources.ApplicationIcons;
import sqlrunner.swinghelper.WindowHelper;
import sqlrunner.text.GenericDateUtil;
import sqlrunner.text.StringReplacer;

public final class ValueEditor extends JFrame {

    private static final Logger logger              = Logger.getLogger(ValueEditor.class);

    private static final long   serialVersionUID    = 1L;
    private int                 contentType;
    private int                 row;
    private int                 col;
    private MainFrame           mainFrame;
    private SearchReplaceDialog dlgSeRe;
    private boolean             insertModus         = false;
    private JPanel              jContentPane        = null;
    private JScrollPane         jScrollPane         = null;
    private JTextArea           textArea            = null;
    private JPanel              jPanel              = null;
    private JLabel              labelCounter        = null;
    private JCheckBox           checkBoxLineWrap    = null;
    private JCheckBox           checkBoxViewAsDate  = null;
    private JCheckBox           checkBoxTrim        = null;
    private JButton             buttonSaveInDB      = null;
    private JButton             buttonLoadFile      = null;
    private JButton             buttonSaveInFile    = null;
    private JButton             buttonSearchReplace = null;
    private JComboBox<String>   cbDateFormat        = null;
    
    /**
     * This is the default constructor
     */
    public ValueEditor() {
        super();
        getRootPane().putClientProperty("Window.style", "small");
        initialize();
    }

    public ValueEditor(boolean readOnly, boolean insertModus, MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.insertModus = insertModus;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initialize();
            WindowHelper.locateWindowAtMiddle(mainFrame, this);
            setVisible(true);
            if (readOnly) {
                buttonSaveInDB.setEnabled(false);
                buttonSaveInFile.setEnabled(false);
            }
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
        int x = Integer.parseInt(Main.getUserProperty("CELL_EDITOR_WIDTH", "300"));  
        int y = Integer.parseInt(Main.getUserProperty("CELL_EDITOR_HEIGHT", "150"));  
        if (x < 300) x = 300;
        if (y < 150) y = 150;
        this.setSize(x, y);
        this.setContentPane(getJContentPane());
        this.setTitle(Messages.getString("ValueEditor.title")); 
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
            textArea.setLineWrap(checkBoxLineWrap.isSelected());
            textArea.addKeyListener(new java.awt.event.KeyAdapter() {

                @Override
	            public void keyPressed(KeyEvent e) {
	                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        dispose();
                    }
	            }
	        });
            textArea.getDocument().addDocumentListener(new DocumentListener() {

                public void insertUpdate(DocumentEvent de) {
                    labelCounter.setText(String.valueOf((textArea.getText()).length()));
                }

                public void changedUpdate(DocumentEvent de) {
                    labelCounter.setText(String.valueOf((textArea.getText()).length()));
                }

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
	            gbc.gridy = 0;
	            gbc.gridx = 2;
	            gbc.gridwidth = 2;
	            gbc.anchor = java.awt.GridBagConstraints.WEST;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            jPanel.add(getCheckBoxLineWrap(), gbc);
            }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 0;
	            gbc.gridx = 4;
	            gbc.gridwidth = 1;
	            gbc.anchor = java.awt.GridBagConstraints.WEST;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            jPanel.add(getCheckBoxTrim(), gbc);
            }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 1;
	            gbc.gridx = 0;
	            gbc.gridwidth = 2;
	            gbc.anchor = GridBagConstraints.WEST;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            jPanel.add(getComboBoxDateFormat(), gbc);
	        }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 1;
	            gbc.gridx = 2;
	            gbc.gridwidth = 3;
	            gbc.anchor = GridBagConstraints.WEST;
	            gbc.insets = new Insets(2, 2, 2, 2);
	            jPanel.add(getCheckBoxViewAsDate(), gbc);
	        }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 2;
	            gbc.gridx = 0;
	            gbc.gridwidth = 2;
	            gbc.insets = new java.awt.Insets(2, 2, 2, 2);
	            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
	            jPanel.add(getButtonSaveInDB(), gbc);
            }
            {
            	GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 2;
	            gbc.gridx = 2;
	            gbc.insets = new java.awt.Insets(2, 2, 2, 2);
	            jPanel.add(getButtonLoadFile(), gbc);
            }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 2;
	            gbc.gridx = 3;
	            gbc.insets = new java.awt.Insets(2, 2, 2, 2);
	            jPanel.add(getButtonSaveInFile(), gbc);
            }
            {
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.gridy = 2;
	            gbc.gridx = 4;
	            gbc.anchor = java.awt.GridBagConstraints.WEST;
	            gbc.insets = new java.awt.Insets(2, 2, 2, 2);
	            jPanel.add(getButtonSearchReplace(), gbc);
            }
        }
        return jPanel;
    }

    /**
     * This method initializes jCheckBox	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getCheckBoxLineWrap() {
        if (checkBoxLineWrap == null) {
            checkBoxLineWrap = new JCheckBox();
            checkBoxLineWrap.setText(Messages.getString("ValueEditor.linewrap"));
            checkBoxLineWrap.addActionListener(new java.awt.event.ActionListener() { 
            	public void actionPerformed(java.awt.event.ActionEvent e) {    
                    textArea.setLineWrap(((JCheckBox) e.getSource()).isSelected());
                    textArea.setWrapStyleWord(true);
                    Main.setUserProperty("CELL_EDITOR_LINEWRAP", String.valueOf(checkBoxLineWrap.isSelected()));
            	}
            });
            checkBoxLineWrap.setSelected((Main.getUserProperty("CELL_EDITOR_LINEWRAP", "false")).equals("true"));
        }
        return checkBoxLineWrap;
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
            	public void actionPerformed(java.awt.event.ActionEvent e) {    
                    if (checkBoxViewAsDate.isSelected() == false) {
                        // dann das textuelle Datumsformat in die long-Form wandeln
                        try {
                            java.util.Date date = GenericDateUtil.parseDate(textArea.getText().trim());
                            textArea.setText(String.valueOf(date.getTime()));
                        } catch (ParseException pe) {
                            logger.warn("converting long->Date failed:"+pe.getMessage());
                        }
                    } else {
                        // die long-Form in die Datumsform wandeln
                        try {
                        	long time = Long.parseLong(textArea.getText().trim());
                            java.util.Date date = new java.util.Date(time);
                            SimpleDateFormat sdf = new SimpleDateFormat((String) cbDateFormat.getSelectedItem());
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

    /**
     * This method initializes checkBoxTrim	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getCheckBoxTrim() {
        if (checkBoxTrim == null) {
            checkBoxTrim = new JCheckBox();
            checkBoxTrim.setText(Messages.getString("ValueEditor.trimonsave"));
            checkBoxTrim.setToolTipText(Messages.getString("ValueEditor.removesallspaces")); 
            checkBoxTrim.addActionListener(new java.awt.event.ActionListener() { 
            	public void actionPerformed(java.awt.event.ActionEvent e) {    
                    Main.setUserProperty("CELL_EDITOR_TRIM", String.valueOf(checkBoxTrim.isSelected())); 
            	}
            });
            checkBoxTrim.setSelected((Main.getUserProperty("CELL_EDITOR_TRIM", "false")).equals("true"));   
        }
        return checkBoxTrim;
    }

    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getButtonSaveInDB() {
        if (buttonSaveInDB == null) {
            buttonSaveInDB = new JButton();
            buttonSaveInDB.setMnemonic(java.awt.event.KeyEvent.VK_S);
            buttonSaveInDB.addActionListener(new java.awt.event.ActionListener() { 
            	
                public void actionPerformed(java.awt.event.ActionEvent e) {    
                    buttonSaveInDBActionPerformed();
                }
                
            });
            if (insertModus) {
                buttonSaveInDB.setText(Messages.getString("ValueEditor.writeintable")); 
            } else {
                buttonSaveInDB.setText(Messages.getString("ValueEditor.updateindb")); 
            }
        }
        return buttonSaveInDB;
    }

    private void buttonSaveInDBActionPerformed() {
    	mainFrame.getDatabase().setCurrentCellEditor(this);
        if (contentType == BasicDataType.BINARY.getId()) {
            final String text = textArea.getText().trim();
            if (isByteArray) {
            	byte[] value = hexStringToByteArray(text);
                if (insertModus) {
                    mainFrame.getDatabase().setValueAt(value, row, col);
                    dispose();
                } else {
                	buttonSaveInDB.setEnabled(false);
                	final Object finalValue = value;
                    Thread t = new Thread() {
                    	public void run() {
                            mainFrame.getDatabase().updateValue(finalValue, row, col, ValueEditor.this, cbDateFormat.getSelectedItem());
                        	buttonSaveInDB.setEnabled(true);
                    	}
                    };
                    t.start();
                }
            } else {
                final File f = new File(textArea.getText());
                if (f.exists()) {
                    if (insertModus) {
                        mainFrame.getDatabase().setValueAt(new BinaryDataFile(f.getAbsolutePath()), row, col);
                        dispose();
                    } else {
                    	buttonSaveInDB.setEnabled(false);
                        Thread t = new Thread() {
                        	public void run() {
                            	mainFrame.getDatabase().updateValue(f.getAbsolutePath(), row, col, ValueEditor.this, null);
                            	buttonSaveInDB.setEnabled(true);
                        	}
                        };
                        t.start();
                    }
                } else {
                    mainFrame.showErrorMessage(Messages.getString("ValueEditor.filenotexists"), Messages.getString("ValueEditor.uploadfile"));  
                    toFront();
                }
            }
        } else if (BasicDataType.isStringType(contentType)) {
            final Object text;
            if (checkBoxTrim.isSelected()) {
                text = textArea.getText().trim();
            } else {
                text = textArea.getText();
            }
            if (insertModus) {
                mainFrame.getDatabase().setValueAtLogicalIndexes(row, col, text, cbDateFormat.getSelectedItem());
                dispose();
            } else {
            	buttonSaveInDB.setEnabled(false);
                Thread t = new Thread() {
                	public void run() {
                        mainFrame.getDatabase().updateValue(text, row, col, ValueEditor.this, cbDateFormat.getSelectedItem());
                    	buttonSaveInDB.setEnabled(true);
                	}
                };
                t.start();
            }
        } else { // if ((contentType == Database.BASICTYPE_BINARY) || (contentType == Database.BASICTYPE_BLOB))
            // hier weiter für alle anderen Typen
            final String text = textArea.getText().trim();
            if (insertModus) {
                mainFrame.getDatabase().setValueAt(text, row, col);
                dispose();
            } else {
            	buttonSaveInDB.setEnabled(false);
                Thread t = new Thread() {
                	public void run() {
                        mainFrame.getDatabase().updateValue(text, row, col, ValueEditor.this, cbDateFormat.getSelectedItem());
                    	buttonSaveInDB.setEnabled(true);
                	}
                };
                t.start();
            }
        }
    }
    
    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getButtonLoadFile() {
        if (buttonLoadFile == null) {
            buttonLoadFile = new JButton();
            buttonLoadFile.setIcon(ApplicationIcons.OPEN_GIF); 
            buttonLoadFile.addActionListener(new java.awt.event.ActionListener() { 
            	public void actionPerformed(java.awt.event.ActionEvent e) {    
                    buttonLoadInFileActionPerformed();
            	}
            });
        }
        return buttonLoadFile;
    }
    
    private void buttonLoadInFileActionPerformed() {
        mainFrame.getDatabase().setCurrentCellEditor(this);
        try {
            // Dateidialog öffnen
            final FileDialog openDialog = new FileDialog(this, Messages.getString("ValueEditor.load"), FileDialog.LOAD); 
            final String directory = Main.getUserProperty(
                    "TABLE_FIELD_LOAD_DIR", 
                    System.getProperty("user.home")); 
            openDialog.setDirectory(directory);
            openDialog.setVisible(true);
            final String dir = openDialog.getDirectory();
            final String file = openDialog.getFile();
            if ((dir != null) && (file != null)) {
                Main.setUserProperty("TABLE_FIELD_LOAD_DIR", dir); 
                final File f = new File(dir + file);
                if ((contentType == BasicDataType.BINARY.getId())) {
                    // dann ist content eine Referenz auf einen BinaryInputStream
                    textArea.setText(dir + file);
                } else { // der Inhalt steht bei allen anderen Typen direkt im Textfeld
                    final BufferedReader br = new BufferedReader(new FileReader(f));
                    String line;
                    int pos = 0;
                    textArea.setText(""); 
                    while ((line = br.readLine()) != null) {
                        textArea.insert(line + "\n", pos); 
                        pos = (pos + line.length()) + 1;
                    }
                    br.close();
                } // if ((contentType == Database.BASICTYPE_BINARY) || (contentType == Database.BASICTYPE_BLOB))
            } // if ((dir != null) && (file != null))
        } catch (FileNotFoundException fnfe) {
            System.err.println(fnfe.toString());
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }
    }

    /**
     * This method initializes buttonSaveInFile	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getButtonSaveInFile() {
        if (buttonSaveInFile == null) {
            buttonSaveInFile = new JButton();
            buttonSaveInFile.setIcon(ApplicationIcons.SAVE_GIF); 
            if (insertModus) {
                buttonSaveInFile.setEnabled(false);
            }
            buttonSaveInFile.addActionListener(new java.awt.event.ActionListener() { 
            	public void actionPerformed(java.awt.event.ActionEvent e) {    
                    buttonSaveInFileActionPerformed();
            	}
            });
            buttonSaveInFile.setToolTipText(Messages.getString("ValueEditor.savedatainfile")); 
        }
        return buttonSaveInFile;
    }
    
    private void buttonSaveInFileActionPerformed() {
        mainFrame.getDatabase().setCurrentCellEditor(this);
        try {
            // Dateidialog öffnen
            final FileDialog saveDialog = new FileDialog(this, Messages.getString("ValueEditor.saveas"), FileDialog.SAVE); 
            if ((contentType == BasicDataType.BINARY.getId())) {
                saveDialog.setFile(mainFrame.getDatabase().getTableName()
                        + "_" 
                        + mainFrame.getDatabase().getColumnName(col)
                        + "_" 
                        + String.valueOf(row + 1)
                        + ".bin"); 
            } else {
                saveDialog.setFile(mainFrame.getDatabase().getTableName()
                        + "_" 
                        + mainFrame.getDatabase().getColumnName(col)
                        + "_" 
                        + String.valueOf(row + 1)
                        + ".txt"); 
            }
            final String directory = Main.getUserProperty(
                    "TABLE_FIELD_LOAD_DIR", 
                    System.getProperty("user.home")); 
            saveDialog.setDirectory(directory);
            saveDialog.setVisible(true);
            final String dir = saveDialog.getDirectory();
            final String file = saveDialog.getFile();
            if ((dir != null) && (file != null)) {
                Main.setUserProperty("TABLE_FIELD_LOAD_DIR", dir); 
                final File f = new File(dir + file);
                if ((contentType == BasicDataType.BINARY.getId())) {
                    // dann ist content eine Referenz auf einen BinaryInputStream
                    mainFrame.getDatabase().writeLOBValueInFile(f, row, col, this);
                } else { // der Inhalt steht bei allen anderen Typen direkt im Textfeld
                    final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                    String text = textArea.getText();
                    if (checkBoxTrim.isSelected() && text != null) {
                        text = text.trim();
                    }
                    if (text != null) {
                        bw.write(text);
                        bw.flush();
                    }
                    bw.close();
                } // if ((contentType == Database.BASICTYPE_BINARY) || (contentType == Database.BASICTYPE_BLOB))
            } // if ((dir != null) && (file != null))
        } catch (FileNotFoundException fnfe) {
            System.err.println(fnfe.toString());
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }
    }

    /**
     * This method initializes buttonSearchReplace	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getButtonSearchReplace() {
        if (buttonSearchReplace == null) {
            buttonSearchReplace = new JButton();
            buttonSearchReplace.setIcon(ApplicationIcons.SEARCH_GIF); 
            buttonSearchReplace.addActionListener(new java.awt.event.ActionListener() { 
            	public void actionPerformed(java.awt.event.ActionEvent e) {   
                    buttonSearchReplaceActionPerformed();
            	}
            });
        }
        return buttonSearchReplace;
    }
    
    private void buttonSearchReplaceActionPerformed() {
        textArea.grabFocus();
        if (dlgSeRe == null) {
            dlgSeRe = new SearchReplaceDialog(this, false, textArea);
        }
        if (textArea.getSelectionStart() < textArea.getSelectionEnd()) {
            dlgSeRe.setSearchText(textArea.getSelectedText());
        }
        dlgSeRe.setVisible(true);
    }
    
    private JComboBox<String> getComboBoxDateFormat() {
    	if (cbDateFormat == null) {
    		cbDateFormat = new JComboBox<String>();
    		prepareCbDateFormat();
    	}
    	return cbDateFormat;
    }

    private void prepareCbDateFormat() {
        int i = 0;
        String formatStr;
        while (true) {
            formatStr = Main.getDefaultProperty("DATE_FORMAT_" + String.valueOf(i));
            if (formatStr != null) {
                cbDateFormat.addItem(formatStr);
            } else {
                break; // Schleife abbrechen, wenn keine weiteren Einträge vorhanden
            }
            i++;
        }
    }

    private boolean isByteArray = false;
    /**
     * setzt das anzuzeigende Objekt
     * @param value = Inhalt
     * @param row, col die zugeordnete Koordinate der zelle
     */
    public void setObject(Object value, int row, int col) {
        this.row = row;
        this.col = col;
        isByteArray = false;
        if (mainFrame.getDatabase().isVerticalView()) {
            contentType = mainFrame.getDatabase().getColumnBasicType(mainFrame.getDatabase().convertFromVerticalToLogicalColIndex(row));
        } else {
            contentType = mainFrame.getDatabase().getColumnBasicType(col);
        }
        if (value != null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("Cell value:" + value);
        	}
            if (contentType == BasicDataType.DATE.getId()) {
            	getComboBoxDateFormat().setSelectedItem(MainFrame.getDateFormatMask());
                final SimpleDateFormat sdf = new SimpleDateFormat(MainFrame.getDateFormatMask());
                textArea.setText(sdf.format(value));
            } else if (contentType == BasicDataType.BINARY.getId()) { 
                textArea.setToolTipText(Messages.getString("ValueEditor.binarysavetooltip"));
                if (value instanceof BinaryDataFile) {
                    final String name = ((BinaryDataFile) value).getFilename();
                    if (name != null) {
                        textArea.setText(name);
                    } else {
                        textArea.setText("");
                    }
                } else if (value instanceof byte[]) {
                	isByteArray = true;
                	byte[] byteArray = (byte[]) value;
                	StringBuilder sb = new StringBuilder(byteArray.length * 2);
                	sb.append("x'");
                	for (byte b : byteArray) {
                		sb.append(String.format("%02X", b));
                	}
                	sb.append("'");
                	textArea.setText(sb.toString());
                } else {
                    textArea.setText("");
                }
            } else if (BasicDataType.isStringType(contentType) || BasicDataType.isNumberType(contentType)) {
                checkBoxViewAsDate.setEnabled(true);
                checkBoxTrim.setEnabled(true);
                textArea.setText(StringReplacer.fixLineBreaks(value.toString()));
                labelCounter.setText(String.valueOf((textArea.getText()).length()));
            } else {
                textArea.setText(value.toString());
                labelCounter.setText(String.valueOf((textArea.getText()).length()));
            }
            textArea.setCaretPosition(0);
        } // if (content != null)
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

    public void setUpdateEnabled(boolean enable) {
        buttonSaveInDB.setEnabled(enable);
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

    @Override
    public void dispose() {
        if (dlgSeRe != null) {
            dlgSeRe.dispose();
        }
        Main.setUserProperty("CELL_EDITOR_WIDTH", String.valueOf(this.getWidth())); 
        Main.setUserProperty("CELL_EDITOR_HEIGHT", String.valueOf(this.getHeight())); 
        super.dispose();
    }
    
} //  @jve:decl-index=0:visual-constraint="10,10"
