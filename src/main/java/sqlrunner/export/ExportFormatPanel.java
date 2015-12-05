package sqlrunner.export;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.flatfileimport.gui.DelimiterConfigPanel;

public class ExportFormatPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JRadioButton rbInsertExport = null;
	private JTextField textFieldDateFormat = null;
	private JRadioButton rbFlatFileExport = null;
	private JCheckBox checkBoxHeader = null;
	private JCheckBox checkBoxComma = null;
	private DelimiterConfigPanel delimiterConfigPanel = null;
	private ButtonGroup buttonGroup = new ButtonGroup();
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
	public static final String FLAT_FILE_SELECTED = "flat";
	public static final String INSERT_SQL_SELECTED = "inserts";

	/**
	 * This is the default constructor
	 */
	public ExportFormatPanel() {
		super();
		initialize();
	}

	@Override
	public void setEnabled(boolean enabled) {
		rbInsertExport.setEnabled(enabled);
		if (rbInsertExport.isSelected()) {
			textFieldDateFormat.setEditable(enabled);
		} else {
			checkBoxHeader.setEnabled(enabled);
			checkBoxComma.setEnabled(enabled);
			delimiterConfigPanel.setEnabled(enabled);
		}
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
								Messages.getString("ExportFormatPanel.5"), 
								TitledBorder.DEFAULT_JUSTIFICATION, 
								TitledBorder.DEFAULT_POSITION, 
								null, 
								null));
		int y = 0;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(2, 5, 2, 2);
			add(getRbInsertExport(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.anchor = GridBagConstraints.EAST;
			gbc.insets = new Insets(2, 20, 2, 2);
			gbc.gridwidth = 2;
			JLabel jLabel = new JLabel();
			jLabel.setText(Messages.getString("ExportFormatPanel.0")); //$NON-NLS-1$
			this.add(jLabel, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 2;
			gbc.gridy = y;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 1.0;
			gbc.insets = new Insets(2, 2, 2, 2);
			this.add(getTextFieldDateFormat(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(15, 5, 2, 2);
			this.add(getRbFlatFileExport(), gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = y;
			gbc.gridheight = 3;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0D;
			gbc.insets = new Insets(15, 2, 2, 2);
			gbc.gridwidth = 2;
			this.add(getDelimiterConfigPanel(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.insets = new Insets(2, 15, 2, 2);
			gbc.anchor = GridBagConstraints.WEST;
			this.add(getCheckBoxHeader(), gbc);
		}
		y++;
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = y;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.insets = new Insets(2, 15, 2, 2);
			this.add(getCheckBoxComma(), gbc);
		}
		buttonGroup.add(getRbFlatFileExport());
		buttonGroup.add(getRbInsertExport());
	}

	/**
	 * This method initializes rbInsertExport	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getRbInsertExport() {
		if (rbInsertExport == null) {
			rbInsertExport = new JRadioButton();
			rbInsertExport.setText(Messages.getString("ExportFormatPanel.1")); //$NON-NLS-1$
			rbInsertExport.addItemListener(new java.awt.event.ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					if (rbInsertExport.isSelected()) {
						rbFlatFileExport.setSelected(false);
						textFieldDateFormat.setEnabled(true);
						checkBoxComma.setEnabled(false);
						checkBoxHeader.setEnabled(false);
						delimiterConfigPanel.setEnabled(false);
					}
					fireActionPerformed();
				}
				
			});
		}
		return rbInsertExport;
	}

	/**
	 * This method initializes textFieldDateFormat	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTextFieldDateFormat() {
		if (textFieldDateFormat == null) {
			textFieldDateFormat = new JTextField();
			textFieldDateFormat.setEnabled(false);
		}
		return textFieldDateFormat;
	}

	/**
	 * This method initializes rbFlatFileExport	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getRbFlatFileExport() {
		if (rbFlatFileExport == null) {
			rbFlatFileExport = new JRadioButton();
			rbFlatFileExport.setText(Messages.getString("ExportFormatPanel.2")); //$NON-NLS-1$
			rbFlatFileExport.addItemListener(new java.awt.event.ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					if (rbFlatFileExport.isSelected()) {
						rbInsertExport.setSelected(false);
						textFieldDateFormat.setEnabled(false);
						checkBoxComma.setEnabled(true);
						checkBoxHeader.setEnabled(true);
						delimiterConfigPanel.setEnabled(true);
					}
					fireActionPerformed();
				}
				
			});
		}
		return rbFlatFileExport;
	}

	/**
	 * This method initializes checkBoxHeader	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getCheckBoxHeader() {
		if (checkBoxHeader == null) {
			checkBoxHeader = new JCheckBox();
			checkBoxHeader.setText(Messages.getString("ExportFormatPanel.3")); //$NON-NLS-1$
			checkBoxHeader.setEnabled(false);
		}
		return checkBoxHeader;
	}

	/**
	 * This method initializes checkBoxComma	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getCheckBoxComma() {
		if (checkBoxComma == null) {
			checkBoxComma = new JCheckBox();
			checkBoxComma.setText(Messages.getString("ExportFormatPanel.4")); //$NON-NLS-1$
			checkBoxComma.setEnabled(false);
		}
		return checkBoxComma;
	}

	/**
	 * This method initializes delimiterConfigPanel	
	 * 	
	 * @return sqlrunner.flatfileimport.DelimiterConfigPanel	
	 */
	private DelimiterConfigPanel getDelimiterConfigPanel() {
		if (delimiterConfigPanel == null) {
			delimiterConfigPanel = new DelimiterConfigPanel();
			delimiterConfigPanel.setEnabled(false);
		}
		return delimiterConfigPanel;
	}
	
	public boolean isExportInCSVFileSelected() {
		return rbFlatFileExport.isSelected();
	}
	
	public void setExportInCSVFileselected(boolean toFile) {
		if (toFile) {
			rbFlatFileExport.doClick();
		} else {
			rbInsertExport.doClick();
		}
	}
	
	public boolean isWithHeaderSelected() {
		return checkBoxHeader.isSelected();
	}
	
	public void setWithHeaderSelected(boolean withHeader) {
		checkBoxHeader.setSelected(withHeader);
	}
	
	public boolean isReplacePointWithCommaSelected() {
		return checkBoxComma.isSelected();
	}
	
	public void setReplaceWithCommaSelected(boolean withComma) {
		checkBoxComma.setSelected(withComma);
	}
	
	public String getDelimiter() {
		return delimiterConfigPanel.getDelimiter();
	}
	
	public String getDelimiterToken() {
		return delimiterConfigPanel.getDelimiterToken();
	}
	
	public String getEnclosure() {
		return delimiterConfigPanel.getEnclosure();
	}
	
	public String getDateSQLExpression() {
		return textFieldDateFormat.getText();
	}
	
	public void setDateSQLExpression(String sql) {
		textFieldDateFormat.setText(sql);
	}
	
	public void setDelimiterToken(String token) {
		delimiterConfigPanel.setDelimiterToken(token);
	}
	
	public void addActionListener(ActionListener l) {
		listeners.add(l);
	}
	
	public void removeActionListener(ActionListener l) {
		listeners.remove(l);
	}
	
	private void fireActionPerformed() {
		ActionEvent e = null;
		if (rbFlatFileExport.isSelected()) {
			e = new ActionEvent(this, 0, FLAT_FILE_SELECTED);
		} else {
			e = new ActionEvent(this, 0, INSERT_SQL_SELECTED);
		}
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).actionPerformed(e);
		}
	}
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
