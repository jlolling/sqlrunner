package sqlrunner.talend;

import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import sqlrunner.MainFrame;
import sqlrunner.swinghelper.WindowHelper;

public class ContextConfigFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private ContextTableModel model = new ContextTableModel();
	private JTable table = null;
	private JCheckBox cbReplaceAll = null;
	private JLabel status = new JLabel();
	private MainFrame mainFrame;
	
	public ContextConfigFrame(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
		initialize();
		pack();
		setVisible(true);
		WindowHelper.locateWindowAtMiddle(mainFrame, this);
		WindowHelper.checkAndCorrectWindowBounds(this);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	private void initialize() {
		setTitle("Talend Context Configuration");
		getContentPane().setLayout(new GridBagLayout());
		{
			JButton buttomLoadFromCode = new JButton();
			buttomLoadFromCode.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					mainFrame.setupContextResolver();
					model.setData(MainFrame.getContextVarResolver().getContextVars(), false);
					setupStatus();
				}
			});
			buttomLoadFromCode.setText("(Re)Load from current editor");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			getContentPane().add(buttomLoadFromCode, gbc);
		}
		{
			JButton buttonLoadFromContextFile = new JButton();
			buttonLoadFromContextFile.setText("Load Context Properties...");
			buttonLoadFromContextFile.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					FileDialog fd = new FileDialog(ContextConfigFrame.this, "Load Talend Context File", FileDialog.LOAD);
					fd.setFilenameFilter(new FilenameFilter() {
						
						@Override
						public boolean accept(File dir, String name) {
							if (name != null && name.toLowerCase().contains("properties")) {
								return true;
							} else {
								return false;
							}
						}
						
					});
					fd.setVisible(true);
					String dir = fd.getDirectory();
					String file = fd.getFile();
			        if (file != null) {
			            final File f = new File(dir, file);
			            try {
			            	if (cbReplaceAll.isSelected()) {
			            		MainFrame.getContextVarResolver().clear();
			            	}
							MainFrame.getContextVarResolver().initContextVars(f.getAbsolutePath());
							model.setData(MainFrame.getContextVarResolver().getContextVars(), cbReplaceAll.isSelected());
							ContextConfigFrame.this.setTitle(file);
							setupStatus();
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(ContextConfigFrame.this, e1.getMessage(), "Load Talend Contect File failed", JOptionPane.ERROR_MESSAGE);
						}
			        }
					
				}
				
			});
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 1;
			getContentPane().add(buttonLoadFromContextFile, gbc);
		}
		{
			cbReplaceAll = new JCheckBox("Replace all");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 1;
			getContentPane().add(cbReplaceAll, gbc);
		}
		{
			model.setData(MainFrame.getContextVarResolver().getContextVars(), false);
			table = new JTable(model);
			JScrollPane sp = new JScrollPane(table);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			gbc.weighty = 1;
			getContentPane().add(sp, gbc);
			setupStatus();
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 3;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			getContentPane().add(status, gbc);
		}
	}
	
	private void setupStatus() {
		status.setText(model.getRowCount() + " variables");
	}

}
