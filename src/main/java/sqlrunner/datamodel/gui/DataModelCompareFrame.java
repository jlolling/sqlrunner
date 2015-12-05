package sqlrunner.datamodel.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import sqlrunner.datamodel.DatamodelEvent;
import sqlrunner.datamodel.DatamodelListener;
import sqlrunner.datamodel.ModelComparator;
import sqlrunner.datamodel.SQLObject;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.generator.SQLCodeGenerator;
import sqlrunner.resources.ApplicationIcons;

public class DataModelCompareFrame extends JFrame {

	private static final Logger logger = Logger.getLogger(DataModelCompareFrame.class);
	private static final long serialVersionUID = 1L;
	private JTextField jTextFieldReferenceSchemaName;
	private JTextField jTextFieldTargetSchemaName;
	private JButton jButtonChange12;
	private JTextArea jTextAreaLog;
	private JButton jButtonStartCompare;
	private JButton jButtonCreateSQL;
	private SQLObject schema1;
	private SQLObject schema2;
	private Thread compareThread;
	private ModelComparator comparator;
	/**
	 * Create the frame.
	 */
	public DataModelCompareFrame() {
		initGui();
		setTitle(Messages.getString("DataModelCompareFrame.title"));
		setPreferredSize(new Dimension(400,600));
	}

	public void setSchema1(SQLObject schema1) {
		this.schema1 = schema1;
		jTextFieldReferenceSchemaName.setText(schema1.getModel().getName() + "/" + schema1.getName());
	}

	public void setSchema2(SQLObject schema2) {
		this.schema2 = schema2;
		jTextFieldTargetSchemaName.setText(schema2.getModel().getName() + "/" + schema2.getName());
	}

	private void initGui() {
		getContentPane().setLayout(new GridBagLayout());
		int y = 0;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 0, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			JLabel label = new JLabel();
			label.setText(Messages.getString("DataModelCompareFrame.referenceSchemaLabel"));
			getContentPane().add(label, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			getContentPane().add(getJTextFieldReferenceSchemaName(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 0, 5);
			gbc.anchor = GridBagConstraints.CENTER;
			getContentPane().add(getJButtonChange12(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(0, 5, 0, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			JLabel label = new JLabel();
			label.setText(Messages.getString("DataModelCompareFrame.targetSchemaLabel"));
			getContentPane().add(label, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			getContentPane().add(getJTextFieldTargetSchemaName(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 0, 5);
			gbc.anchor = GridBagConstraints.CENTER;
			getContentPane().add(getJButtonStartCompare(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(0, 5, 0, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			JLabel label = new JLabel();
			label.setText(Messages.getString("DataModelCompareFrame.compareLog"));
			getContentPane().add(label, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 1;
			gbc.weighty = 1;
			JScrollPane sp = new JScrollPane();
			sp.setViewportView(getJTextAreaLog());
			getContentPane().add(sp, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = y++;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.anchor = GridBagConstraints.WEST;
			getContentPane().add(getJButtonCreateSQL(), gbc);
		}
	}
	
	private JTextField getJTextFieldReferenceSchemaName() {
		if (jTextFieldReferenceSchemaName == null) {
			jTextFieldReferenceSchemaName = new JTextField();
			jTextFieldReferenceSchemaName.setEditable(false);
		}
		return jTextFieldReferenceSchemaName;
	}
	
	private JTextField getJTextFieldTargetSchemaName() {
		if (jTextFieldTargetSchemaName == null) {
			jTextFieldTargetSchemaName = new JTextField();
			jTextFieldTargetSchemaName.setEditable(false);
		}
		return jTextFieldTargetSchemaName;
	}
	
	private JTextArea getJTextAreaLog() {
		if (jTextAreaLog == null) {
			jTextAreaLog = new JTextArea();
			jTextAreaLog.setWrapStyleWord(true);
			jTextAreaLog.setLineWrap(true);
			jTextAreaLog.setEditable(false);
		}
		return jTextAreaLog;
	}
	
	private JButton getJButtonChange12() {
		if (jButtonChange12 == null) {
			jButtonChange12 = new JButton();
			jButtonChange12.setIcon(ApplicationIcons.UP_DOWN_EXCHANGE_PNG);
			jButtonChange12.setToolTipText(Messages.getString("DataModelCompareFrame.exchange12"));
			jButtonChange12.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (schema1 != null && schema2 != null) {
						SQLObject t = schema2;
						schema2 = schema1;
						schema1 = t;
						jTextFieldReferenceSchemaName.setText(schema1.getModel().getName() + "/" + schema1.getName());
						jTextFieldTargetSchemaName.setText(schema2.getModel().getName() + "/" + schema2.getName());
					}
				}

			});
		}
		return jButtonChange12;
	}
	
	private JButton getJButtonStartCompare() {
		if (jButtonStartCompare == null) {
			jButtonStartCompare = new JButton();
			jButtonStartCompare.setText(Messages.getString("DataModelCompareFrame.startCompare"));
			jButtonStartCompare.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					startCompare();
				}
				
			});
		}
		return jButtonStartCompare;
	}
	
	@Override
	public void setVisible(boolean visible) {
		if (visible == false && compareThread != null && compareThread.isAlive()) {
			compareThread.interrupt();
		}
		super.setVisible(visible);
	}
	
	private void startCompare() {
		if (logger.isDebugEnabled()) {
			logger.debug("startCompare " + schema1 + " with " + schema2);
		}
		if (compareThread != null && compareThread.isAlive()) {
			compareThread.interrupt();
		}
		jButtonStartCompare.setEnabled(false);
		compareThread = new Thread() {
			
			@Override
			public void run() {
    			SwingUtilities.invokeLater(new Runnable() {
    				@Override
    				public void run() {
    					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    					jTextAreaLog.setText("Start...");
    				}
    			});
    			
				comparator = new ModelComparator();
				comparator.setDatamodelListener(new DatamodelListener() {
					
					@Override
					public void eventHappend(final DatamodelEvent event) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								jTextAreaLog.append("\n" + event.getMessage());
							}
							
						});
					}
					
				});
				try {
					if (schema1 instanceof SQLSchema && schema2 instanceof SQLSchema) {
						comparator.compare((SQLSchema) schema1, (SQLSchema) schema2);
					} else if (schema1 instanceof SQLTable && schema2 instanceof SQLTable) {
						comparator.compare((SQLTable) schema1, (SQLTable) schema2);
					}
				} finally {
	    			SwingUtilities.invokeLater(new Runnable() {
	    				@Override
	    				public void run() {
	    					jButtonStartCompare.setEnabled(true);
	    					jTextAreaLog.append("\nFinished.");
	    					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    					jButtonCreateSQL.setEnabled(true);
	    				}
	    			});
				}
			}
			
		};
		compareThread.start();
	}
	
	private JButton getJButtonCreateSQL() {
		if (jButtonCreateSQL == null) {
			jButtonCreateSQL = new JButton();
			jButtonCreateSQL.setText(Messages.getString("DataModelCompareFrame.createSQL"));
			jButtonCreateSQL.setEnabled(false);
			jButtonCreateSQL.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					showSQLCode();
				}
				
			});
		}
		return jButtonCreateSQL;
	}
	
	private void showSQLCode() {
		if (logger.isDebugEnabled()) {
			logger.debug("showSQLCode ");
		}
		jButtonCreateSQL.setEnabled(false);
		Thread t = new Thread() {
			public void run() {
				try {
					jTextAreaLog.setText(SQLCodeGenerator.getInstance().buildSchemaUpdateStatements(comparator));
				} finally {
					jButtonCreateSQL.setEnabled(true);
				}
			}
		};
		t.start();
	}
	
}
