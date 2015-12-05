package sqlrunner.generator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import sqlrunner.Main;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;

/**
 * @author  lolling.jan
 */
public final class JavaCreatorFrame extends JFrame {
    
    private static final long serialVersionUID = -5042003376518345983L;
    private javax.swing.JPanel jContentPane          = null;
    private JLabel             labelDirectory        = null;
    private JTextField         textFieldJavaRootDir  = null;
    private JButton            buttonChooseDirectory = null;
    private JProgressBar       progressBar           = null;
    private JButton            buttonStart           = null;
    private JButton            buttonStop            = null;
    private transient JavaCreatorThread  creatorThread;    
	private JTextField jTextFieldPackage = null;
	private JLabel jLabel = null;
    private transient SQLSchema schema = null;

    /**
     * This is the default constructor
     */
    public JavaCreatorFrame(SQLSchema metaModel) {
        super();
        this.schema = metaModel;
        initialize();
    }
    
    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize() {
        this.setResizable(false);
        this.setContentPane(getJContentPane());
        this.setTitle("Java Files Creator Model: " + schema.toString());
        this.setSize(453, 167);
    }

    /**
     * This method initializes jContentPane
     * @return  javax.swing.JPanel
     */
    private javax.swing.JPanel getJContentPane() {
        if (jContentPane == null) {
            GridBagConstraints constraints = new GridBagConstraints();
            jLabel = new JLabel();
            labelDirectory = new JLabel();
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout(new GridBagLayout());
            labelDirectory.setText("Source Root Directory");
            labelDirectory.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            labelDirectory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(labelDirectory, constraints);
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(getTextFieldJavaRootDir(), constraints);
            constraints.gridx = 3;
            constraints.gridy = 0;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(getButtonChooseDirectory(), constraints);
            jLabel.setText("package");
            jLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(jLabel, constraints);
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.gridwidth = 3;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(getJTextFieldPackage(), constraints);
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.gridwidth = 4;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(getProgressBar(), constraints);
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.gridwidth = 2;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(getButtonStart(), constraints);
            constraints.gridx = 2;
            constraints.gridy = 3;
            constraints.gridwidth = 2;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.insets = new Insets(2, 2, 2, 2);
            jContentPane.add(getButtonStop(), constraints);
        }
        return jContentPane;
    }

    /**
     * This method initializes jTextField
     * @return  javax.swing.JTextField
     */
    private JTextField getTextFieldJavaRootDir() {
        if (textFieldJavaRootDir == null) {
            textFieldJavaRootDir = new JTextField();
            textFieldJavaRootDir.setBounds(136, 5, 268, 23);
            String directoryName = Main.getUserProperty("JAVA_SOURCE_ROOT");
            if (directoryName != null) {
                textFieldJavaRootDir.setText(directoryName);
            }
        }
        return textFieldJavaRootDir;
    }

    /**
     * This method initializes jButton
     * @return  javax.swing.JButton
     */
    private JButton getButtonChooseDirectory() {
        if (buttonChooseDirectory == null) {
            buttonChooseDirectory = new JButton();
            buttonChooseDirectory.setBounds(411, 5, 27, 23);
            buttonChooseDirectory.setText("Choose...");
            buttonChooseDirectory.setMnemonic(java.awt.event.KeyEvent.VK_D);
            buttonChooseDirectory.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    openJavaRootChooserDialog();
                }
            });
        }
        return buttonChooseDirectory;
    }

    private void openJavaRootChooserDialog() {
        String directoryName = Main.getUserProperty("JAVA_SOURCE_ROOT");
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (directoryName != null) {
            chooser.setCurrentDirectory(new File(directoryName));
        }
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("Choose Java Source root");
        final int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String javaSourceRoot = chooser.getSelectedFile().getAbsolutePath();
            textFieldJavaRootDir.setText(javaSourceRoot);
        }
    }

    /**
     * This method initializes jProgressBar
     * @return  javax.swing.JProgressBar
     */
    private JProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = new JProgressBar();
            progressBar.setBounds(8, 75, 430, 19);
            progressBar.setStringPainted(true);
        }
        return progressBar;
    }

    /**
     * This method initializes jButton
     * @return  javax.swing.JButton
     */
    private JButton getButtonStart() {
        if (buttonStart == null) {
            buttonStart = new JButton();
            buttonStart.setBounds(8, 107, 268, 25);
            buttonStart.setText("Start Creating Java Files");
            buttonStart.setMnemonic(java.awt.event.KeyEvent.VK_S);
            buttonStart.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    startCreatingJavaFiles1();
                }
            });
        }
        return buttonStart;
    }

    /**
     * This method initializes jButton
     * @return  javax.swing.JButton
     */
    private JButton getButtonStop() {
        if (buttonStop == null) {
            buttonStop = new JButton();
            buttonStop.setBounds(292, 107, 146, 25);
            buttonStop.setText("Cancel");
            buttonStop.setEnabled(false);
            buttonStop.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    cancelCreatingJavaFiles();
                }
            });
        }
        return buttonStop;
    }
    
    private void startCreatingJavaFiles1() {
        if (jTextFieldPackage.getText() == null || jTextFieldPackage.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "package or java source root is null !");
        } else if (textFieldJavaRootDir.getText() == null || textFieldJavaRootDir.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "schema is empty !");
        } else {
            progressBar.setMinimum(0);
            progressBar.setMaximum(schema.getTableCount());
            Main.setUserProperty("JAVA_PACKAGE", jTextFieldPackage.getText().trim());
            Main.setUserProperty("JAVA_SOURCE_ROOT", textFieldJavaRootDir.getText().trim());
            // start creating java code
            creatorThread = new JavaCreatorThread(textFieldJavaRootDir.getText().trim(), jTextFieldPackage.getText().trim());
            creatorThread.start();
        }
    }

    private final class JavaCreatorThread extends Thread {
        
        final String javaSourceRoot;
        final String packageName;
        
        JavaCreatorThread(String javaSourceRoot, String packageName) {
            this.javaSourceRoot = javaSourceRoot;
            this.packageName = packageName;
        }
        
        @Override
        public void run() {
            buttonStart.setEnabled(false);
            buttonStop.setEnabled(true);
            for (int i = 0 ; i < schema.getTableCount(); i++) {
                SQLTable table = schema.getTableAt(i);
                if (isInterrupted() == false) {
                    try {
                        createFile(javaSourceRoot, packageName, table.getName(), JavaCodeGenerator.createJavaClassCode(table, packageName));
                        progressBar.setValue(i + 1);
                    } catch (Exception ex) {
                        showErrorMessage("Failed to create file for table " + table.toString(), ex.getMessage());
                        break;
                    }
                }
            }
            buttonStart.setEnabled(true);
            buttonStop.setEnabled(false);
        }
    }
    
    private void createFile(String sourceRootDir, String packageName, String tableName, String text) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        File f = JavaCodeGenerator.createSourceFile(sourceRootDir, packageName, tableName);
        if (f.getParentFile().exists() == false) {
        	f.getParentFile().mkdirs();
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getAbsoluteFile()), Main.getFileEnoding()));
        bw.write(text);
        bw.close();
    }
    
    private void showErrorMessage(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    private void cancelCreatingJavaFiles() {
        if (creatorThread != null && creatorThread.isAlive()) {
            creatorThread.interrupt();
        }
    }

	/**
     * This method initializes jTextField	
     * @return  javax.swing.JTextField
     */    
	private JTextField getJTextFieldPackage() {
		if (jTextFieldPackage == null) {
			jTextFieldPackage = new JTextField();
            String packageName = Main.getUserProperty("JAVA_PACKAGE");
            jTextFieldPackage.setText(packageName);
			jTextFieldPackage.setBounds(137, 37, 268, 23);
		}
		return jTextFieldPackage;
	}
 
} //  @jve:decl-index=0:visual-constraint="10,10"
