package sqlrunner.fileconverter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.util.Timer;
import java.util.TimerTask;

import sqlrunner.FileOpenChooserPanel;
import sqlrunner.swinghelper.WindowHelper;

public class TextFileConverterFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private FileOpenChooserPanel sourceFileChooser;
	private FileOpenChooserPanel targetFileChooser;
	private JTextField textFieldMaxLinesPerFile;
	private JComboBox comboBoxSourceEncoding;
	private JComboBox comboBoxTargetEncoding;
	private JComboBox comboBoxTargetLineSeparator;
	private JButton buttonStart;
	private JButton buttonCancel;
	private TextFileConverter converter = new TextFileConverter();
	private transient Thread converterThread = null;
	private JLabel labelStatus = new JLabel();
	
	public TextFileConverterFrame() {
		initialize();
		pack();
		WindowHelper.checkAndCorrectWindowBounds(this);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	private void initialize() {
		setTitle(Messages.getString("TextFileConverter.title"));
		getContentPane().setLayout(new GridBagLayout());
		int y = 0;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.EAST;
			getContentPane().add(new JLabel(Messages.getString("TextFileConverter.sourceFile")), gbc);
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
			getContentPane().add(new JLabel(Messages.getString("TextFileConverter.encoding")), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(2,2,2,2);
			getContentPane().add(getSourceEncodingComboBox(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.EAST;
			getContentPane().add(new JLabel(Messages.getString("TextFileConverter.targetFile")), gbc);
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
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.EAST;
			getContentPane().add(new JLabel(Messages.getString("TextFileConverter.maxlines")), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(2,2,2,2);
			getContentPane().add(getTextFieldMaxLinesPerFile(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.EAST;
			getContentPane().add(new JLabel(Messages.getString("TextFileConverter.encoding")), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(2,2,2,2);
			getContentPane().add(getTargetEncodingComboBox(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2,2,2,2);
			gbc.anchor = GridBagConstraints.EAST;
			getContentPane().add(new JLabel(Messages.getString("TextFileConverter.targetLineSeparator")), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new Insets(2,2,2,2);
			getContentPane().add(getTargetLineSeparatorComboBox(), gbc);
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
			sourceFileChooser = new FileOpenChooserPanel(Messages.getString("TextFileConverter.sourceFile"));
			sourceFileChooser.setAsOpenFileChooser();
		}
		return sourceFileChooser;
	}

	private FileOpenChooserPanel getTargetFileChooser() {
		if (targetFileChooser == null) {
			targetFileChooser = new FileOpenChooserPanel(Messages.getString("TextFileConverter.targetFile"));
			targetFileChooser.setAsSaveFileChooser();
		}
		return targetFileChooser;
	}
	
	private JComboBox getSourceEncodingComboBox() {
		if (comboBoxSourceEncoding == null) {
			comboBoxSourceEncoding = new JComboBox();
			comboBoxSourceEncoding.setEditable(true);
			comboBoxSourceEncoding.addItem("UTF-8");
			comboBoxSourceEncoding.addItem("UTF-16");
			comboBoxSourceEncoding.addItem("Cp1252");
			comboBoxSourceEncoding.addItem("ISO-8859-1");
			comboBoxSourceEncoding.addItem("ISO-8859-2");
			comboBoxSourceEncoding.addItem("ISO-8859-3");
			comboBoxSourceEncoding.addItem("ISO-8859-15");
			comboBoxSourceEncoding.addItem("ASCII");
			comboBoxSourceEncoding.addItem("MacRoman");
		}
		return comboBoxSourceEncoding;
	}
	
	private JComboBox getTargetEncodingComboBox() {
		if (comboBoxTargetEncoding == null) {
			comboBoxTargetEncoding = new JComboBox();
			comboBoxTargetEncoding.setEditable(true);
			comboBoxTargetEncoding.addItem("UTF-8");
			comboBoxTargetEncoding.addItem("UTF-16");
			comboBoxTargetEncoding.addItem("Cp1252");
			comboBoxTargetEncoding.addItem("ISO-8859-1");
			comboBoxTargetEncoding.addItem("ISO-8859-2");
			comboBoxTargetEncoding.addItem("ISO-8859-3");
			comboBoxTargetEncoding.addItem("ISO-8859-15");
			comboBoxTargetEncoding.addItem("ASCII");
			comboBoxTargetEncoding.addItem("MacRoman");
		}
		return comboBoxTargetEncoding;
	}

	private JComboBox getTargetLineSeparatorComboBox() {
		if (comboBoxTargetLineSeparator == null) {
			comboBoxTargetLineSeparator = new JComboBox();
			comboBoxTargetLineSeparator.setEditable(false);
			comboBoxTargetLineSeparator.addItem(TextFileConverter.LineSeparator.UNIX);
			comboBoxTargetLineSeparator.addItem(TextFileConverter.LineSeparator.WINDOWS);
		}
		return comboBoxTargetLineSeparator;
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
	
	private JTextField getTextFieldMaxLinesPerFile() {
		if (textFieldMaxLinesPerFile == null) {
			textFieldMaxLinesPerFile = new JTextField();
		}
		return textFieldMaxLinesPerFile;
	}
	
	private long getMaxLineNumberPerFile() {
		String text = textFieldMaxLinesPerFile.getText();
		if (text != null) {
			text = text.trim();
			try {
				return Long.parseLong(text);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Line number invalid", "Check max lines per file", JOptionPane.ERROR_MESSAGE);
				return -1;
			}
		} else {
			return 0;
		}
	}

	private JButton getButtonStart() {
		if (buttonStart == null) {
			buttonStart = new JButton();
			buttonStart.setText(Messages.getString("TextFileConverter.start"));
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
			buttonCancel.setText(Messages.getString("TextFileConverter.cancel"));
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
	
	private void startConversion() {
		if (converterThread != null) {
			converterThread.interrupt();
		}
		final File source = sourceFileChooser.getSelectedFile();
		final File target = targetFileChooser.getSelectedFile();
		final String sourceEncoding = (String) getSourceEncodingComboBox().getSelectedItem();
		final String targetEncoding = (String) getTargetEncodingComboBox().getSelectedItem();
		final String targetLineSeparator = ((TextFileConverter.LineSeparator) getTargetLineSeparatorComboBox().getSelectedItem()).getSeparator();
		converterThread = new Thread() {
			
			@Override
			public void run() {
				converter.reset();
				long maxLinesPerFile = getMaxLineNumberPerFile();
				if (maxLinesPerFile < 0) {
					return;
				}
				converter.setMaxLinesPerFile(maxLinesPerFile);
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						SwingUtilities.invokeLater(new Runnable() {

							public void run() {
								labelStatus.setText(
										Messages.getString("TextFileConverter.convert") + 
										converter.getCurrentLineNumber());
							}
							
						});
					}
					
				}, 0, 1000);
				try {
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							labelStatus.setText(
									Messages.getString("TextFileConverter.convert") + 
									0);
						}
						
					});
					converter.convert(
							source, 
							sourceEncoding, 
							target, 
							targetEncoding, 
							targetLineSeparator);
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							labelStatus.setText(
									Messages.getString("TextFileConverter.convert") + 
									converter.getCurrentLineNumber());
						}
						
					});
				} catch (Exception e) {
					JOptionPane.showMessageDialog(
							getContentPane(), 
							e.getMessage(), 
							Messages.getString("TextFileConverter.title"), 
							JOptionPane.ERROR_MESSAGE);
				}
				timer.cancel();
				labelStatus.setText(
						Messages.getString("TextFileConverter.finished"));
				buttonStart.setEnabled(true);
				buttonCancel.setEnabled(false);
			}
		};
		converterThread.start();
	}
	
}
