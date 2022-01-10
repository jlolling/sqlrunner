package sqlrunner.talend;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import sqlrunner.FileOpenChooserPanel;
import sqlrunner.flatfileimport.DatasetProvider;
import sqlrunner.flatfileimport.FieldTokenizer;
import sqlrunner.flatfileimport.FileImporter;
import sqlrunner.swinghelper.WindowHelper;

public class SchemaImportFrame extends JFrame {

	private static final Logger logger = LogManager.getLogger(SchemaImportFrame.class);
	private static final long serialVersionUID = 1L;
	private FileOpenChooserPanel sourceFileChooser;
	private FileOpenChooserPanel targetFileChooser;
	private JButton buttonStart;
	private JButton buttonCancel;
	private transient Thread converterThread = null;
	private JLabel labelStatus = new JLabel();
	
	public SchemaImportFrame() {
		initialize();
		pack();
		WindowHelper.checkAndCorrectWindowBounds(this);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	private void initialize() {
		setTitle(Messages.getString("SchemaImportFrame.title"));
		getContentPane().setLayout(new GridBagLayout());
		int y = 0;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.EAST;
			getContentPane().add(new JLabel(Messages.getString("SchemaImportFrame.sourceFile")), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			getContentPane().add(getSourceFileChooser(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.EAST;
			getContentPane().add(new JLabel(Messages.getString("SchemaImportFrame.targetFile")), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			getContentPane().add(getTargetFileChooser(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(2,2,2,2);
			getContentPane().add(getPanelButtons(), gbc);
		}
	}
	
	private FileOpenChooserPanel getSourceFileChooser() {
		if (sourceFileChooser == null) {
			sourceFileChooser = new FileOpenChooserPanel(Messages.getString("SchemaImportFrame.sourceFile"));
			sourceFileChooser.setAsOpenFileChooser();
		}
		return sourceFileChooser;
	}

	private FileOpenChooserPanel getTargetFileChooser() {
		if (targetFileChooser == null) {
			targetFileChooser = new FileOpenChooserPanel(Messages.getString("SchemaImportFrame.targetFile"));
			targetFileChooser.setAsSaveFileChooser();
		}
		return targetFileChooser;
	}
	
	private JPanel getPanelButtons() {
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(400, 50));
		panel.setLayout(new GridBagLayout());
		int y = 0;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.CENTER;
			panel.add(getLabelStatus(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.weightx = 1;
			gbc.anchor = GridBagConstraints.EAST;
			gbc.insets = new Insets(2,2,2,2);
			panel.add(getButtonStart(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.weightx = 1;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(2,2,2,2);
			panel.add(getButtonCancel(), gbc);
		}
		return panel;
	}
	
	private JLabel getLabelStatus() {
		if (labelStatus == null) {
			labelStatus = new JLabel();
			labelStatus.setHorizontalAlignment(JLabel.CENTER);
			labelStatus.setText("not started");
		}
		return labelStatus;
	}
	
	private JButton getButtonStart() {
		if (buttonStart == null) {
			buttonStart = new JButton();
			buttonStart.setText(Messages.getString("SchemaImportFrame.start"));
			buttonStart.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					startConversion();
					buttonCancel.setEnabled(true);
					buttonStart.setEnabled(false);
				}
				
			});
		}
		return buttonStart;
	}

	private JButton getButtonCancel() {
		if (buttonCancel == null) {
			buttonCancel = new JButton();
			buttonCancel.setEnabled(false);
			buttonCancel.setText(Messages.getString("SchemaImportFrame.cancel"));
			buttonCancel.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					cancelConversion();
					buttonCancel.setEnabled(false);
					buttonStart.setEnabled(true);
				}
				
			});
		}
		return buttonCancel;
	}
	
	private void cancelConversion() {
		if (converterThread != null) {
			converterThread.interrupt();
		}
	}
	
	private Properties loadImportConfig() {
        Properties importConfig = new Properties();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("load look and feels from archive");
            }
	        final Object dummy = new Object();
	        String res = "/" + SchemaImportFrame.class.getPackage().getName().replace(".", "/") + "/talend_schema.importconfig";
	        logger.debug("Load import config from:" + res);
            final InputStream is = dummy.getClass().getResourceAsStream(res);
            importConfig.load(is);
            is.close();
        } catch (Exception e) {
            logger.error("loadImportConfig: " + e.getMessage(), e);
        }
        return importConfig;
	}
	
	private Column buildColumn(FieldTokenizer ft) throws Exception {
		Column column = new Column();
		Object value = null;
		int index = -1;
		try {
			// 0 name
			value = ft.getData(++index);
			column.setName((String) value);
			// 1 key
			value = ft.getData(++index);
			if (value != null) {
				column.setKey((Boolean) value);
			}
			// 2 nullable
			value = ft.getData(++index);
			if (value != null) {
				column.setNullable((Boolean) value);
			}
			// 3 datatype
			value = ft.getData(++index);
			column.setDataType((String) value);
			// 4 length
			value = ft.getData(++index);
			if (value != null) {
				column.setLength(((Number) value).intValue());
			}
			// 5 precision
			value = ft.getData(++index);
			if (value != null) {
				column.setPrecision(((Number) value).intValue());
			}
			// 6 pattern
			value = ft.getData(++index);
			column.setPattern((String) value);
			// 7 dbtype
			value = ft.getData(++index);
			column.setDbType((String) value);
			// 8 comment
			value = ft.getData(++index);
			column.setComment((String) value);
		} catch (Throwable t) {
			throw new Exception("Failed to extract field index:" + index + " value:" + value + " failure:" + t.getMessage(), t);
		}
		return column;
	}
	
	private void startConversion() {
		if (converterThread != null) {
			converterThread.interrupt();
		}
		final File source = sourceFileChooser.getSelectedFile();
		final File target = targetFileChooser.getSelectedFile();
		if (source != null && source.canRead() && target != null) {
			converterThread = new Thread() {
				
				@Override
				public void run() {
                    int lineNumber = 0;
					try {
						FileImporter fi = new FileImporter();
						fi.setupLoggerAsStaticClassLogger();
						fi.initConfig(loadImportConfig());
						final DatasetProvider dp = fi.createDatasetProvider(source, true, "xls", fi.getImportAttributes());
						FieldTokenizer ft = dp.createParser();
						ft.setFieldDescriptions(fi.getFieldDescriptions());
						List<Column> list = new ArrayList<Column>();
						Timer timer = new Timer();
						timer.schedule(new TimerTask() {

							@Override
							public void run() {
								SwingUtilities.invokeLater(new Runnable() {

									public void run() {
										
										labelStatus.setText(
												Messages.getString("SchemaImportFrame.convert") + 
												(dp != null ? dp.getCurrentRowNum() : 0));
									}
									
								});
							}
							
						}, 0, 1000);
						boolean firstLoop = true;
	                    long currentRowsToSkip = fi.getImportAttributes().getCountSkipRows();
						while (true) {
							lineNumber++;
							Object rawdata = dp.getNextDataset();
	                        if (fi.getImportAttributes().getCountSkipRows() > 0 && currentRowsToSkip > 0) {
	                            currentRowsToSkip--;
	                            continue;
	                        }
	                        if (fi.getImportAttributes().isSkipFirstRow() && firstLoop) {
	                            firstLoop = false;
	                            continue;
	                        }
							if (rawdata == null) {
								break;
							}
							if (Thread.currentThread().isInterrupted()) {
								break;
							}
							if (ft.parseRawData(rawdata) == false) {
								break;
							}
							list.add(buildColumn(ft));
						}
						timer.cancel();
						SchemaUtil su = new SchemaUtil();
						su.writeSchemaFile(target, list);
					} catch (Exception e) {
						String message = "Import Schema file in line:" + lineNumber + " failed:" + e.getMessage();
						logger.error(message, e);
						JOptionPane.showMessageDialog(
								getContentPane(), 
								message, 
								Messages.getString("SchemaImportFrame.title"), 
								JOptionPane.ERROR_MESSAGE);
					}
					labelStatus.setText(
							Messages.getString("SchemaImportFrame.finished"));
					buttonStart.setEnabled(true);
					buttonCancel.setEnabled(false);
				}
			};
			converterThread.start();
		}
	}
	
}
