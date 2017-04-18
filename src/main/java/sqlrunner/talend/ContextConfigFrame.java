package sqlrunner.talend;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;

import sqlrunner.MainFrame;
import sqlrunner.swinghelper.WindowHelper;

public class ContextConfigFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JButton buttomLoadFromCode = null;
	private JButton buttonLoadFromContextFile = null;
	private ContextTableModel model = new ContextTableModel();
	private JTable table = null;
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
			buttomLoadFromCode = new JButton();
			buttomLoadFromCode.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					mainFrame.setupContextResolver();
					model.setData(mainFrame.getContextVarResolver().getContextVars());
				}
			});
			buttomLoadFromCode.setText("(Re)Load from current editor");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 1;
			getContentPane().add(buttomLoadFromCode, gbc);
		}
		{
			buttonLoadFromContextFile = new JButton();
			buttonLoadFromContextFile.setText("Load Context Properties...");
			buttonLoadFromContextFile.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser();
			        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			        chooser.setMultiSelectionEnabled(false);
			        chooser.setDialogTitle("Load Talend Context File"); 
			        chooser.addChoosableFileFilter(new FileFilter() {
						
						@Override
						public String getDescription() {
							return "Properties";
						}
						
						@Override
						public boolean accept(File f) {
							if (f != null && f.getAbsolutePath().endsWith(".properties")) {
								return true;
							} else {
								return false;
							}
						}
					});
			        final int returnVal = chooser.showOpenDialog(ContextConfigFrame.this);
			        // hier weiter wenn der modale FileDialog geschlossen wurde
			        if (returnVal == JFileChooser.APPROVE_OPTION) {
			            final File f = chooser.getSelectedFile();
			            try {
							MainFrame.getContextVarResolver().initContextVars(f.getAbsolutePath());
						} catch (IOException e1) {
							mainFrame.showErrorMessage(e1.getMessage(), "Load Talend Contect File failed");
						}
			        }
					
				}
				
			});
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 2;
			getContentPane().add(buttonLoadFromContextFile, gbc);
		}
		{
			model.setData(mainFrame.getContextVarResolver().getContextVars());
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
		}
	}

}
