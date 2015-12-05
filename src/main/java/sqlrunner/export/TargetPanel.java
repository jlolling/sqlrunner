package sqlrunner.export;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import sqlrunner.CSVFileFilter;
import sqlrunner.Main;
import sqlrunner.SQLFileFilter;
import sqlrunner.XLSFileFilter;
import sqlrunner.XLSXFileFilter;
import sqlrunner.resources.ApplicationIcons;

public class TargetPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JRadioButton jRadioButtonExportInFile = null;
	private JTextField jTextFieldFileName = null;
	private JButton jButtonChooseFile = null;
	private JCheckBox jCheckBoxSplit = null;
	private JTextField jTextFieldCountDatasets = null;
	private JRadioButton jRadioButtonExportInEditor = null;
	private JCheckBox jCheckBoxLimitDatasetsInEditor = null;
	private JTextField jTextFieldLimitDatasetsInEditor = null;
	private ButtonGroup buttonGroup = new ButtonGroup(); // @jve:decl-index=0:
	private File exportFile = null;
	private boolean showEditorAsTarget = false;
	private SQLFileFilter sqlFileFilter = null;  //  @jve:decl-index=0:
	private CSVFileFilter csvFileFilter = null;  //  @jve:decl-index=0:
	private XLSFileFilter xlsFileFilter = null;
	private XLSXFileFilter xlsxFileFilter = null;

	/**
	 * This is the default constructor
	 */
	public TargetPanel(boolean showEditorAsTarget) {
		super();
		this.showEditorAsTarget = showEditorAsTarget;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setLayout(new GridBagLayout());
		this.setBorder(
				BorderFactory.createTitledBorder(
					BorderFactory.createEtchedBorder(
							EtchedBorder.RAISED),
							Messages.getString("TargetPanel.6"),
							TitledBorder.DEFAULT_JUSTIFICATION, 
							TitledBorder.DEFAULT_POSITION, 
							new Font("Lucida Grande", Font.PLAIN, 13),
							Color.black)); 
		int y = 0;
		if (showEditorAsTarget) {
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = y;
				gbc.insets = new Insets(2, 5, 2, 2);
				gbc.anchor = GridBagConstraints.WEST;
				gbc.gridwidth = 3;
				this.add(getJRadioButtonExportInFile(), gbc);
			}
			y++;
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2, 20, 2, 2);
			gbc.anchor = GridBagConstraints.EAST;
			JLabel jLabel = new JLabel();
			jLabel.setText(Messages.getString("TargetPanel.0")); //$NON-NLS-1$
			this.add(jLabel, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.gridwidth = 2;
			gbc.weightx = 1.0;
			gbc.insets = new Insets(2, 2, 2, 2);
			this.add(getJTextFieldFileName(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 3;
			gbc.gridy = y;
			gbc.insets = new Insets(0, 2, 2, 2);
			this.add(getJButtonChooseFile(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(2, 20, 2, 0);
			gbc.anchor = GridBagConstraints.WEST;
			this.add(getJCheckBoxSplit(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx = 2;
			gbc.gridy = y;
			gbc.weightx = 1.0;
			gbc.insets = new Insets(2, 2, 2, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridwidth = 2;
			this.add(getJTextFieldCountDatasets(), gbc);
		}
		y++;
		if (showEditorAsTarget) {
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = y;
				gbc.insets = new Insets(15, 5, 2, 2);
				gbc.gridwidth = 4;
				gbc.anchor = GridBagConstraints.WEST;
				this.add(getJRadioButtonExportInEditor(), gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = y;
				gbc.insets = new Insets(4, 20, 2, 2);
				gbc.anchor = GridBagConstraints.WEST;
				gbc.gridwidth = 2;
				this.add(getJCheckBoxLimitDatasetsInEditor(), gbc);
			}
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridx = 2;
				gbc.gridy = y;
				gbc.weightx = 1.0;
				gbc.insets = new Insets(4, 2, 2, 5);
				gbc.anchor = GridBagConstraints.WEST;
				gbc.gridwidth = 2;
				this.add(getJTextFieldLimitDatasetsInEditor(), gbc);
			}
			buttonGroup.add(getJRadioButtonExportInFile());
			buttonGroup.add(getJRadioButtonExportInEditor());
		} else {
			jTextFieldFileName.setEnabled(true);
			jButtonChooseFile.setEnabled(true);
			jCheckBoxSplit.setEnabled(true);
			jTextFieldCountDatasets.setEnabled(true);
		}
	}

	/**
	 * This method initializes jRadioButtonExportInFile
	 * 
	 * @return javax.swing.JRadioButton
	 */
	private JRadioButton getJRadioButtonExportInFile() {
		if (jRadioButtonExportInFile == null) {
			jRadioButtonExportInFile = new JRadioButton();
			jRadioButtonExportInFile.setText(Messages.getString("TargetPanel.1")); //$NON-NLS-1$
			jRadioButtonExportInFile.addItemListener(new java.awt.event.ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					if (jRadioButtonExportInFile.isSelected()) {
						if (jRadioButtonExportInEditor != null) {
							jRadioButtonExportInEditor.setSelected(false);
							jTextFieldLimitDatasetsInEditor.setEnabled(false);
							jCheckBoxLimitDatasetsInEditor.setEnabled(false);
						}
						jTextFieldFileName.setEnabled(true);
						jButtonChooseFile.setEnabled(true);
						jCheckBoxSplit.setEnabled(true);
						jTextFieldCountDatasets.setEnabled(true);
					}
				}

			});
		}
		return jRadioButtonExportInFile;
	}

	/**
	 * This method initializes jTextFieldFileName
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldFileName() {
		if (jTextFieldFileName == null) {
			jTextFieldFileName = new JTextField();
			jTextFieldFileName.setEnabled(false);
		}
		return jTextFieldFileName;
	}
	
	private void setupExportFile() {
		if (jTextFieldFileName.getText() != null && jTextFieldFileName.getText().trim().length() > 0) {
			exportFile = new File(jTextFieldFileName.getText());
		}
	}

	/**
	 * This method initializes jButtonChooseFile
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonChooseFile() {
		if (jButtonChooseFile == null) {
			jButtonChooseFile = new JButton();
			jButtonChooseFile.setEnabled(false);
			jButtonChooseFile.setIcon(ApplicationIcons.OPEN_GIF); //$NON-NLS-1$
			jButtonChooseFile.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					setupExportFile();
					final JFileChooser chooser = new JFileChooser();
					if (exportFile != null) {
						if (exportFile.getParentFile() != null) {
							chooser.setCurrentDirectory(exportFile.getParentFile());
						}
					} else {
						final String directory = Main.getUserProperty("EXPORT_DATAFILE_DIR", System.getProperty("user.home")); //$NON-NLS-1$ //$NON-NLS-2$
						chooser.setCurrentDirectory(new File(directory));
					}
					chooser.setDialogType(JFileChooser.OPEN_DIALOG);
					chooser.setMultiSelectionEnabled(false);
					chooser.setDialogTitle(Messages.getString("TargetPanel.9")); //$NON-NLS-1$
					if (xlsFileFilter != null) {
						chooser.addChoosableFileFilter(xlsFileFilter);
					}
					if (xlsxFileFilter != null) {
						chooser.addChoosableFileFilter(xlsxFileFilter);
					}
					if (csvFileFilter != null) {
						chooser.addChoosableFileFilter(csvFileFilter);
					}
					if (sqlFileFilter != null) {
						chooser.addChoosableFileFilter(sqlFileFilter);
					}
					final int returnVal = chooser.showSaveDialog(jButtonChooseFile);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File f = chooser.getSelectedFile();
						if (chooser.getFileFilter() instanceof SQLFileFilter
								&& (f.getName().toLowerCase().endsWith(".sql") == false)) {
							f = new File(f.getAbsolutePath() + ".sql");
						} else {
							String fileName = f.getName();
							int point = fileName.lastIndexOf(".");
							String extension = null;
							if (point > 0) {
								extension = fileName.toLowerCase().substring(point + 1); 
							}
							if (chooser.getFileFilter() instanceof CSVFileFilter) {
								if (f.getName().toLowerCase().endsWith(".csv") == false && extension == null) {
									f = new File(f.getAbsolutePath() + ".csv");
								}
							} else if (chooser.getFileFilter() instanceof XLSFileFilter) {
								if (f.getName().toLowerCase().endsWith(".xls") == false && extension == null) {
									f = new File(f.getAbsolutePath() + ".xls");
								}
							} else if (chooser.getFileFilter() instanceof XLSXFileFilter) {
								if (f.getName().toLowerCase().endsWith(".xlsx") == false && extension == null) {
									f = new File(f.getAbsolutePath() + ".xlsx");
								}
							}
						}
						Main.setUserProperty("EXPORT_DATAFILE_DIR", f.getParentFile().getAbsolutePath());
						jTextFieldFileName.setText(f.getAbsolutePath());
						exportFile = f;
					}
				}

			});
		}
		return jButtonChooseFile;
	}

	/**
	 * This method initializes jCheckBoxSplit
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxSplit() {
		if (jCheckBoxSplit == null) {
			jCheckBoxSplit = new JCheckBox();
			jCheckBoxSplit.setEnabled(false);
			jCheckBoxSplit.setText(Messages.getString("TargetPanel.3")); //$NON-NLS-1$
		}
		return jCheckBoxSplit;
	}

	/**
	 * This method initializes jTextFieldCountDatasets
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldCountDatasets() {
		if (jTextFieldCountDatasets == null) {
			jTextFieldCountDatasets = new JTextField();
			jTextFieldCountDatasets.setEnabled(false);
			jTextFieldCountDatasets.getDocument().addDocumentListener(new DocumentListener() {

				public void changedUpdate(DocumentEvent e) {
					checkOption();
				}

				public void insertUpdate(DocumentEvent e) {
					checkOption();
			    }

				public void removeUpdate(DocumentEvent e) {
					checkOption();
				}
				
				private void checkOption() {
					try {
						int count = Integer.parseInt(jTextFieldCountDatasets.getText());
						if (count == 0) {
							jCheckBoxSplit.setSelected(false);
						} else {
							jCheckBoxSplit.setSelected(true);
						}
					} catch (Exception e) {
						jCheckBoxSplit.setSelected(false);
					}
				}
				
			});
		}
		return jTextFieldCountDatasets;
	}

	/**
	 * This method initializes jRadioButtonExportInEditor
	 * 
	 * @return javax.swing.JRadioButton
	 */
	private JRadioButton getJRadioButtonExportInEditor() {
		if (jRadioButtonExportInEditor == null) {
			jRadioButtonExportInEditor = new JRadioButton();
			jRadioButtonExportInEditor.setText(Messages.getString("TargetPanel.4")); //$NON-NLS-1$
			jRadioButtonExportInEditor.addItemListener(new java.awt.event.ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					if (jRadioButtonExportInEditor.isSelected()) {
						jRadioButtonExportInFile.setSelected(false);
						jTextFieldLimitDatasetsInEditor.setEnabled(true);
						jTextFieldFileName.setEnabled(false);
						jButtonChooseFile.setEnabled(false);
						jCheckBoxSplit.setEnabled(false);
						jCheckBoxLimitDatasetsInEditor.setEnabled(true);
						jTextFieldCountDatasets.setEnabled(false);
					}
				}

			});
		}
		return jRadioButtonExportInEditor;
	}

	/**
	 * This method initializes jCheckBoxLimitDatasetsInEditor
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxLimitDatasetsInEditor() {
		if (jCheckBoxLimitDatasetsInEditor == null) {
			jCheckBoxLimitDatasetsInEditor = new JCheckBox();
			jCheckBoxLimitDatasetsInEditor.setEnabled(false);
			jCheckBoxLimitDatasetsInEditor.setText(Messages.getString("TargetPanel.5")); //$NON-NLS-1$
		}
		return jCheckBoxLimitDatasetsInEditor;
	}

	/**
	 * This method initializes jTextFieldLimitDatasetsInEditor
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldLimitDatasetsInEditor() {
		if (jTextFieldLimitDatasetsInEditor == null) {
			jTextFieldLimitDatasetsInEditor = new JTextField();
			jTextFieldLimitDatasetsInEditor.setEnabled(false);
			jTextFieldLimitDatasetsInEditor.getDocument().addDocumentListener(new DocumentListener() {

				public void changedUpdate(DocumentEvent e) {
					checkOption();
				}

				public void insertUpdate(DocumentEvent e) {
					checkOption();
			    }

				public void removeUpdate(DocumentEvent e) {
					checkOption();
				}
				
				private void checkOption() {
					try {
						int count = Integer.parseInt(jTextFieldLimitDatasetsInEditor.getText());
						if (count == 0) {
							jCheckBoxLimitDatasetsInEditor.setSelected(false);
						} else {
							jCheckBoxLimitDatasetsInEditor.setSelected(true);
						}
					} catch (Exception e) {
						jCheckBoxLimitDatasetsInEditor.setSelected(false);
					}
				}
				
			});
		}
		return jTextFieldLimitDatasetsInEditor;
	}

	public boolean isExportInFileSelected() {
		return jRadioButtonExportInFile.isSelected();
	}

	public void setExportToFile(boolean toFile) {
		if (toFile) {
			jRadioButtonExportInFile.doClick();
		} else {
			if (showEditorAsTarget == false) {
				throw new IllegalStateException(Messages.getString("TargetPanel.17")); //$NON-NLS-1$
			}
			jRadioButtonExportInEditor.doClick();
		}
	}

	public String getFileName() {
		String fileName = jTextFieldFileName.getText();
		if (fileName != null) {
			fileName = fileName.trim();
		}
		return fileName;
	}

	public boolean hasFileName() {
		if (jTextFieldFileName.getText() != null
				&& jTextFieldFileName.getText().trim().length() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public void setFocusToFileName() {
		jTextFieldFileName.requestFocusInWindow();
	}

	public boolean isCountDatasetsLimited() {
		return jCheckBoxLimitDatasetsInEditor.isSelected();
	}

	public long getMaxLimitDatasetInSplittedFiles() {
		try {
			return Long.parseLong(jTextFieldCountDatasets.getText());
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean shouldFileSplitted() {
		return jCheckBoxSplit.isSelected();
	}

	public void setFileShouldSplitted(boolean split) {
		if (split) {
			if (jCheckBoxSplit.isSelected() == false) {
				jCheckBoxSplit.doClick();
			}
		} else {
			if (jCheckBoxSplit.isSelected()) {
				jCheckBoxSplit.doClick();
			}
		}
	}

	public void setCountDatasetsLimited(boolean limited) {
		if (limited) {
			if (jCheckBoxLimitDatasetsInEditor.isSelected() == false) {
				jCheckBoxLimitDatasetsInEditor.doClick();
			}
		} else {
			if (jCheckBoxLimitDatasetsInEditor.isSelected()) {
				jCheckBoxLimitDatasetsInEditor.doClick();
			}
		}
	}

	public void setFile(File file) {
		if (file != null) {
			jTextFieldFileName.setText(file.getAbsolutePath());
			exportFile = file;
		} else {
			jTextFieldFileName.setText(null);
		}
	}

	public File getFile() {
		setupExportFile();
		return exportFile;
	}

	public boolean isShowEditorAsTarget() {
		return showEditorAsTarget;
	}

	public void setShowEditorAsTarget(boolean showEditorAsTarget) {
		this.showEditorAsTarget = showEditorAsTarget;
	}
	
	public void enableFileTypeSQL(boolean enable) {
		if (enable) {
			sqlFileFilter = new SQLFileFilter();
		} else {
			sqlFileFilter = null;
		}
	}
	
	public void enableFileTypeCSV(boolean enable) {
		if (enable) {
			csvFileFilter = new CSVFileFilter();
		} else {
			csvFileFilter = null;
		}
	}

	public void enableFileTypeXLS(boolean enable) {
		if (enable) {
			xlsFileFilter = new XLSFileFilter();
			xlsxFileFilter = new XLSXFileFilter();
		} else {
			xlsFileFilter = null;
			xlsxFileFilter = null;
		}
	}


} // @jve:decl-index=0:visual-constraint="10,10"
