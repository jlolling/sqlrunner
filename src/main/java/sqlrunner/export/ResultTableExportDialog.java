package sqlrunner.export;

import java.awt.AWTEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import sqlrunner.Database;
import sqlrunner.LongRunningAction;
import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.swinghelper.WindowHelper;

public class ResultTableExportDialog extends JDialog {

	private static final Logger logger = Logger.getLogger(ResultTableExportDialog.class);
	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private ExportFormatPanel exportFormatPanel = null;
	private TargetPanel targetPanel = null;
	private JPanel jPanelButtons = null;
	private JButton jButtonStart = null;
	private JButton jButtonClose = null;
	private MainFrame mainFrame = null;

	/**
	 * @param mainFrame
	 */
	public ResultTableExportDialog(MainFrame mainFrame) {
		super(mainFrame);
		this.mainFrame = mainFrame;
		initialize();
		pack();
        WindowHelper.checkAndCorrectWindowBounds(this);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setTitle(Messages.getString("ResultTableExportDialog.0")); //$NON-NLS-1$
		this.setContentPane(getJContentPane());
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
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 1.0D;
				gbc.weighty = 1.0D;
				gbc.gridy = 0;
				jContentPane.add(getExportFormatPanel(), gbc);
			}
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 1.0D;
				gbc.weighty = 1.0D;
				gbc.gridy = 1;
				jContentPane.add(getTargetPanel(), gbc);
			}
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.weightx = 1.0D;
				gbc.weighty = 0.0D;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridy = 2;
				jContentPane.add(getJPanelButtons(), gbc);
			}
		}
		return jContentPane;
	}

	/**
	 * This method initializes exportFormatPanel	
	 * 	
	 * @return sqlrunner.export.ExportFormatPanel	
	 */
	private ExportFormatPanel getExportFormatPanel() {
		if (exportFormatPanel == null) {
			exportFormatPanel = new ExportFormatPanel();
			exportFormatPanel.setDateSQLExpression(
					Main.getUserProperty(
	                "SQL_STRING_DATE_CONVERSION",  //$NON-NLS-1$
	                "to_date(\'<STRING>\',\'dd.MM.yy HH24:MI\')")); //$NON-NLS-1$
			exportFormatPanel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals(ExportFormatPanel.FLAT_FILE_SELECTED)) {
						getTargetPanel().enableFileTypeCSV(true);
						getTargetPanel().enableFileTypeXLS(true);
						getTargetPanel().enableFileTypeSQL(false);
					} else {
						getTargetPanel().enableFileTypeCSV(false);
						getTargetPanel().enableFileTypeXLS(false);
						getTargetPanel().enableFileTypeSQL(true);
					}
				}
			});
		}
		return exportFormatPanel;
	}

	/**
	 * This method initializes targetPanel	
	 * 	
	 * @return sqlrunner.export.TargetPanel	
	 */
	private TargetPanel getTargetPanel() {
		if (targetPanel == null) {
			targetPanel = new TargetPanel(true);
			
		}
		return targetPanel;
	}

	/**
	 * This method initializes jPanelButtons	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelButtons() {
		if (jPanelButtons == null) {
			jPanelButtons = new JPanel();
			jPanelButtons.setLayout(new GridBagLayout());
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.insets = new Insets(5, 5, 5, 5);
				gbc.gridy = 0;
				jPanelButtons.add(getJButtonStart(), gbc);
			}
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 1;
				gbc.insets = new Insets(5, 5, 5, 5);
				gbc.gridy = 0;
				jPanelButtons.add(getJButtonClose(), gbc);
			}
		}
		return jPanelButtons;
	}

	/**
	 * This method initializes jButtonStart	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonStart() {
		if (jButtonStart == null) {
			jButtonStart = new JButton();
			jButtonStart.setText(Messages.getString("ResultTableExportDialog.1")); //$NON-NLS-1$
			jButtonStart.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					startExport();
				}
				
			});
		}
		return jButtonStart;
	}
	
	private void startExport() {
        Main.setUserProperty("SQL_STRING_DATE_CONVERSION", exportFormatPanel.getDateSQLExpression()); //$NON-NLS-1$
        final LongRunningAction lra = new LongRunningAction() {

            public String getName() {
                return "Export result table";
            }

            public void cancel() {
                
            }

            public boolean canBeCanceled() {
                return false;
            }

        };
        MainFrame.addLongRunningAction(lra);
        try {
            jButtonStart.setEnabled(false);
            jButtonClose.setEnabled(false);
            disableEvents(AWTEvent.WINDOW_EVENT_MASK);
            int exportType;
            if (exportFormatPanel.isExportInCSVFileSelected()) {
                exportType = Database.CSV_FORMAT;
            } else {
                exportType = Database.INSERT_FORMAT;
            }
            if (targetPanel.isExportInFileSelected()) {
                boolean ok = false;
                if (targetPanel.hasFileName() == false) {
                    JOptionPane.showMessageDialog(
                            this,
                            Messages.getString("ResultTableExportDialog.3"), 
                            Messages.getString("ResultTableExportDialog.4"),
                            JOptionPane.INFORMATION_MESSAGE);
                    ok = false;
                    targetPanel.setFocusToFileName();
                } else {
                    ok = true;
                }
                if (ok) {
                    if (targetPanel.getFile().exists()) {
                        // Nachfragen ob überschrieben werden soll
                        final int answer = JOptionPane.showConfirmDialog(
                        		this,
                        		Messages.getString("ResultTableExportDialog.5"),
                        		Messages.getString("ResultTableExportDialog.6"),
                        		JOptionPane.YES_NO_OPTION,
                        		JOptionPane.INFORMATION_MESSAGE);
                        if (answer == JOptionPane.YES_OPTION) {
                            ok = true;
                        } else {
                            ok = false;
                        }
                    } else {
                        ok = true;
                    }
                }
                if (ok) {
                    // in mehrere Files ?
                    if (targetPanel.shouldFileSplitted()) {
                        try {
                            if (mainFrame.getDatabase().exportTableInMultipleFiles(
                                    targetPanel.getFile(), 
                                    exportFormatPanel.getDelimiter(), 
                                    exportFormatPanel.getEnclosure(),
                                    exportType, 
                                    exportFormatPanel.getDateSQLExpression(),
                                    targetPanel.getMaxLimitDatasetInSplittedFiles(),
                                    exportFormatPanel.isWithHeaderSelected(),
                                    exportFormatPanel.isReplacePointWithCommaSelected())) {
                                setVisible(false);
                            }
                        } catch (NumberFormatException nfe) {
                        	logger.error("startExport failed:" + nfe.getMessage(), nfe);
                            JOptionPane.showMessageDialog(
                                    this,
                                    Messages.getString("ResultTableExportDialog.7"),
                                    Messages.getString("ResultTableExportDialog.8"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                    	File targetFile = targetPanel.getFile();
                    	if (targetFile.getName().toLowerCase().endsWith(".xls") || targetFile.getName().toLowerCase().endsWith(".xlsx")) {
                    		exportToSpreadsheetFile(targetFile, exportFormatPanel.isWithHeaderSelected());
                    	} else {
	                        if (mainFrame.getDatabase().exportTableInCSVFile(
	                        		targetFile,
	                                exportFormatPanel.getDelimiter(),
	                                exportFormatPanel.getEnclosure(),
	                                exportType,
	                                exportFormatPanel.getDateSQLExpression(),
	                                exportFormatPanel.isWithHeaderSelected(),
	                                exportFormatPanel.isReplacePointWithCommaSelected())) {
	                            setVisible(false);
	                        }
                    	}
                    }
                }
            } else { // if (rbExportInFile.isSelected())
                try {
                    mainFrame.insertOrReplaceText(mainFrame.getDatabase().exportTableToString(
                            exportFormatPanel.getDelimiter(),
                            exportFormatPanel.getEnclosure(),
                            exportType,
                            exportFormatPanel.getDateSQLExpression(),
                            exportFormatPanel.isWithHeaderSelected(),
                            exportFormatPanel.isReplacePointWithCommaSelected()));
                    setVisible(false);
                } catch (NumberFormatException nfe) {
                	logger.error("startExport failed:" + nfe.getMessage(), nfe);
                    JOptionPane.showMessageDialog(
                            this,
                            Messages.getString("ResultTableExportDialog.9"), //$NON-NLS-1$
                            Messages.getString("ResultTableExportDialog.10"), //$NON-NLS-1$
                            JOptionPane.ERROR_MESSAGE);
                }
            } // if (rbExportInFile.isSelected())
            jButtonStart.setEnabled(true);
            jButtonClose.setEnabled(true);
            enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        } finally {
            MainFrame.removeLongRunningAction(lra);
        }
		
	}

	private void exportToSpreadsheetFile(File f, boolean withHeader) {
		try {
			mainFrame.getDatabase().exportTableToSpreadSheetFile(f, withHeader);
		} catch (Exception e) {
			logger.error("exportToSpreadsheetFile file=" + f.getAbsolutePath() + " failed:" + e.getMessage(), e);
			JOptionPane.showMessageDialog(this, "Export failed", e.getMessage(), JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * This method initializes jButtonClose	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonClose() {
		if (jButtonClose == null) {
			jButtonClose = new JButton();
			jButtonClose.setText(Messages.getString("ResultTableExportDialog.11")); //$NON-NLS-1$
			jButtonClose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
		}
		return jButtonClose;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
