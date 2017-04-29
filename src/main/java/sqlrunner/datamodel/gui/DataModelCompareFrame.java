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
	private SQLObject object1;
	private SQLObject object2;
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

	public void setObject1(SQLObject object1) {
		this.object1 = object1;
		jTextFieldReferenceSchemaName.setText(object1.getModel().getName() + "/" + object1.getName());
	}

	public void setObject2(SQLObject object2) {
		this.object2 = object2;
		jTextFieldTargetSchemaName.setText(object2.getModel().getName() + "/" + object2.getName());
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
					if (object1 != null && object2 != null) {
						SQLObject t = object2;
						object2 = object1;
						object1 = t;
						jTextFieldReferenceSchemaName.setText(object1.getModel().getName() + "/" + object1.getName());
						jTextFieldTargetSchemaName.setText(object2.getModel().getName() + "/" + object2.getName());
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
			logger.debug("startCompare " + object1 + " with " + object2);
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
					if (object1 instanceof SQLSchema && object2 instanceof SQLSchema) {
						comparator.compare((SQLSchema) object1, (SQLSchema) object2);
					} else if (object1 instanceof SQLTable && object2 instanceof SQLTable) {
						comparator.compare((SQLTable) object1, (SQLTable) object2);
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
			@Override
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
