package sqlrunner.datamodel.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import sqlrunner.datamodel.SQLObject;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;

public class CountPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JLabel labelInfo = new JLabel();
	private JButton button = new JButton();
	private JTextField textFieldCountResult = new JTextField();
	private JLabel labelDate = new JLabel();
	private SQLObject sqlObject = null;
	private boolean countRunning = false;
	
	public CountPanel() {
		initGui();
		setPreferredSize(new Dimension(220, 120));
		setMaximumSize(getPreferredSize());
	}
	
	private void initGui() {
		setLayout(new GridBagLayout());
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(2, 2, 2, 2);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			add(labelInfo, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.insets = new Insets(2, 2, 2, 2);
			button.setText("Count");
			button.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (countRunning) {
						cancelCount();
					} else {
						if (sqlObject instanceof SQLTable) {
							startCountForTable();
						} else if (sqlObject instanceof SQLSchema) {
							startCountForAllTables();
						}
					}
				}
				
			});
			add(button, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.insets = new Insets(2, 2, 2, 2);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			textFieldCountResult.setEditable(false);
			add(textFieldCountResult, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.insets = new Insets(2, 2, 2, 2);
			JLabel label = new JLabel("Count date:");
			add(label, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.insets = new Insets(2, 2, 2, 2);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			add(labelDate, gbc);
		}
	}
	
	public void setSQLObject(SQLObject object) {
		this.sqlObject = object;
		if (countRunning) {
			cancelCount();
		}
		refreshView();
	}
	
	private void refreshView() {
		if (sqlObject instanceof SQLTable) {
			button.setText("Count");
			button.setEnabled(true);
			SQLTable table = (SQLTable) sqlObject;
			labelInfo.setText(table.getAbsoluteName());
			if (table.getCountDatasets() != null) {
				NumberFormat nf = NumberFormat.getInstance();
				textFieldCountResult.setText(nf.format(table.getCountDatasets()));
				if (table.getCountDate() != null) {
					SimpleDateFormat sdf = new SimpleDateFormat();
					labelDate.setText(sdf.format(table.getCountDate()));
				} else {
					labelDate.setText(null);
				}
			} else {
				textFieldCountResult.setText(null);
				labelDate.setText(null);
			}
		} else if (sqlObject instanceof SQLSchema) {
			labelInfo.setText(((SQLSchema) sqlObject).getName());
			button.setText("Count All");
			button.setEnabled(true);
			textFieldCountResult.setText(null);
			labelDate.setText(null);
		} else {
			labelInfo.setText(sqlObject.toString());
			button.setEnabled(false);
			textFieldCountResult.setText(null);
			labelDate.setText(null);
		}
	}
	
	private void cancelCount() {
		if (countThread != null) {
			countThread.interrupt();
		}
	}
	
	private Thread countThread;
	
	private void startCountForTable() {
		button.setText("Cancel");
		if (sqlObject instanceof SQLTable) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			countThread = new Thread() {
				@Override
				public void run() {
					countRunning = true;
					try {
						SQLTable table = (SQLTable) sqlObject;
						table.refreshCount();
						refreshView();
					} finally {
						button.setText("Count");
						countRunning = false;
						setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}
				
			};
			countThread.start();
		}
	}
		
	private void startCountForAllTables() {
		button.setText("Cancel");
		if (sqlObject instanceof SQLSchema) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			countThread = new Thread() {
				@Override
				public void run() {
					countRunning = true;
					SQLSchema schema = (SQLSchema) sqlObject;
					try {
						for (SQLTable table : schema.getTables()) {
							if (table.isTable()) {
								labelInfo.setText(table.getAbsoluteName());
								table.refreshCount();
								sqlObject = table;
								refreshView();
							}
						}
					} finally {
						button.setText("Count");
						countRunning = false;
						setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}
				
			};
			countThread.start();
		}
	}

}
