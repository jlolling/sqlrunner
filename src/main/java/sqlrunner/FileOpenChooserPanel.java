package sqlrunner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import sqlrunner.resources.ApplicationIcons;

public class FileOpenChooserPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private String title;
	private File previousFile;
	private JTextField fileTextField;
	private JButton jButtonChooseFile;
	private JFileChooser chooser = new JFileChooser();
	
	public FileOpenChooserPanel(String title) {
		this.title = title;
		initialize();
	}
	
	public void setAsOpenFileChooser() {
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
	}
	
	public void setAsSaveFileChooser() {
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
	}

	public void removeAllFileFilters() {
		chooser.resetChoosableFileFilters();
	}
	
	public void addFileFilter(javax.swing.filechooser.FileFilter filter) {
		chooser.addChoosableFileFilter(filter);
	}
	
	public void setPreviousFile(File f) {
		this.previousFile = f;
	}
	
	public File getSelectedFile() {
		return new File(fileTextField.getText());
	}
	
	public void setTextFieldEditable(boolean editable) {
		getTextField().setEditable(editable);
	}
	
	public void clearSelectedFile() {
		getTextField().setText(null);
	}
	
	public void reset() {
		previousFile = null;
		getTextField().setText(null);
		chooser.resetChoosableFileFilters();
	}
	
	private void initialize() {
		this.setLayout(new GridBagLayout());
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(2, 2, 2, 2);
			add(getTextField(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.insets = new Insets(2, 2, 2, 2);
			add(getJButton(), gbc);
		}
	}
	
	private JTextField getTextField() {
		if (fileTextField == null) {
			fileTextField = new JTextField();
		}
		return fileTextField;
	}
	
	private JButton getJButton() {
		if (jButtonChooseFile == null) {
			jButtonChooseFile = new JButton(ApplicationIcons.OPEN_GIF);
			jButtonChooseFile.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					openFileDialog();
				}
				
			});
		}
		return jButtonChooseFile;
	}

    public void openFileDialog() {
        if (previousFile != null) { 
            chooser.setSelectedFile(previousFile);
        }
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(title);
        int returnVal;
        if (chooser.getDialogType() == JFileChooser.OPEN_DIALOG) {
        	returnVal = chooser.showOpenDialog(this);
        } else {
        	returnVal = chooser.showSaveDialog(this);
        }
        if (returnVal == JFileChooser.APPROVE_OPTION) {
        	File file = chooser.getSelectedFile();
        	if (file != null) {
            	getTextField().setText(file.getAbsolutePath());
        	}
        }
    }

}
