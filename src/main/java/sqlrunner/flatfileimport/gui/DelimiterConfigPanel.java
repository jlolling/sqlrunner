package sqlrunner.flatfileimport.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.ItemSelectable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DelimiterConfigPanel extends JPanel implements ItemSelectable {

	private static final long serialVersionUID = 1L;
	private JRadioButton jRadioButtonPredefined = null;
	private JRadioButton jRadioButtonUserDefined = null;
	private JComboBox jComboBoxPredefinedDelimiter = null;
	private JTextField jTextFieldUserDefinedDelimiter = null;
	private ButtonGroup buttonGroupDelimiter;
	private static final String PIPE = "|";
	private static final String SEMICOLON = ";";
	private static final String COMMA = ",";
	private static final String SPACE = "{SPACE}";
	private static final String TAB = "{TAB}";
	private Vector<ItemListener> listenerList = new Vector<ItemListener>();
	private JComboBox jComboBoxEnclosure = null;
	private JLabel jLabel = null;
	/**
	 * This is the default constructor
	 */
	public DelimiterConfigPanel() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
		gridBagConstraints31.gridx = 0;
		gridBagConstraints31.anchor = GridBagConstraints.EAST;
		gridBagConstraints31.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints31.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints31.gridy = 2;
		jLabel = new JLabel();
		jLabel.setText(Messages.getString("DelimiterConfigPanel.0")); //$NON-NLS-1$
		jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
		gridBagConstraints21.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints21.gridy = 2;
		gridBagConstraints21.weightx = 1.0;
		gridBagConstraints21.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints21.gridx = 1;
		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.anchor = GridBagConstraints.WEST;
		gridBagConstraints1.gridy = 0;
		gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints1.gridx = 0;
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints.gridy = 1;
		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
		gridBagConstraints2.gridx = 1;
		gridBagConstraints2.anchor = GridBagConstraints.WEST;
		gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints2.weightx = 1.0;
		gridBagConstraints2.gridy = 0;
		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.gridx = 1;
		gridBagConstraints3.anchor = GridBagConstraints.WEST;
		gridBagConstraints3.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints3.weightx = 1.0;
		gridBagConstraints3.gridy = 1;
		this.setLayout(new GridBagLayout());
		this.add(getJRadioButtonPredefined(), gridBagConstraints1);
		this.add(getJRadioButtonUserDefined(), gridBagConstraints);
		this.add(getJComboBoxPredefinedDelimiter(), gridBagConstraints2);
		this.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED), 
				Messages.getString("DelimiterConfigPanel.fielddelimiter"), //$NON-NLS-1$
				TitledBorder.DEFAULT_JUSTIFICATION,
				TitledBorder.DEFAULT_POSITION, 
				new Font("Lucida Grande", Font.PLAIN, 13), Color.black)); //$NON-NLS-1$
		this.setPreferredSize(new Dimension(240, 110));
		this.add(getJTextFieldUserDefinedDelimiter(), gridBagConstraints3);
		this.add(getJComboBoxEnclosure(), gridBagConstraints21);
		this.add(jLabel, gridBagConstraints31);
		buttonGroupDelimiter = new ButtonGroup();
		buttonGroupDelimiter.add(jRadioButtonPredefined);
		buttonGroupDelimiter.add(jRadioButtonUserDefined);
	}

	public void setEnabled(boolean enabled) {
		jRadioButtonPredefined.setEnabled(enabled);
		jRadioButtonUserDefined.setEnabled(enabled);
		if (jRadioButtonPredefined.isSelected()) {
			jComboBoxPredefinedDelimiter.setEnabled(enabled);
		} else if (jRadioButtonUserDefined.isSelected()) {
			jTextFieldUserDefinedDelimiter.setEnabled(enabled);
		}
		jComboBoxEnclosure.setEnabled(enabled);
	}
	
	/**
	 * This method initializes jRadioButtonPredefined
	 * 
	 * @return javax.swing.JRadioButton
	 */
	private JRadioButton getJRadioButtonPredefined() {
		if (jRadioButtonPredefined == null) {
			jRadioButtonPredefined = new JRadioButton();
			jRadioButtonPredefined.setText(Messages.getString("DelimiterConfigPanel.predefined")); //$NON-NLS-1$
			jRadioButtonPredefined.setSelected(true);
			jRadioButtonPredefined.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
						fireItemChangePerformed();
						if (e.getStateChange() == ItemEvent.SELECTED) {
							jTextFieldUserDefinedDelimiter.setEnabled(false);
							jComboBoxPredefinedDelimiter.setEnabled(true);
						} else {
							jTextFieldUserDefinedDelimiter.setEnabled(true);
							jComboBoxPredefinedDelimiter.setEnabled(false);
						}
					}
				}
			});
		}
		return jRadioButtonPredefined;
	}

	/**
	 * This method initializes jRadioButtonUserDefined
	 * 
	 * @return javax.swing.JRadioButton
	 */
	private JRadioButton getJRadioButtonUserDefined() {
		if (jRadioButtonUserDefined == null) {
			jRadioButtonUserDefined = new JRadioButton();
			jRadioButtonUserDefined.setText(Messages.getString("DelimiterConfigPanel.userdefined")); //$NON-NLS-1$
		}
		return jRadioButtonUserDefined;
	}

	/**
	 * This method initializes jComboBoxPredefinedDelimiter
	 * 
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxPredefinedDelimiter() {
		if (jComboBoxPredefinedDelimiter == null) {
			jComboBoxPredefinedDelimiter = new JComboBox();
	        final Object comp = jComboBoxPredefinedDelimiter.getRenderer();
	        if (comp instanceof JLabel) {
	            ((JLabel) comp).setHorizontalAlignment(JLabel.CENTER);
	        }
			jComboBoxPredefinedDelimiter.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					fireItemChangePerformed();
				}
			});
			jComboBoxPredefinedDelimiter.addItem(PIPE); // 0
			jComboBoxPredefinedDelimiter.addItem(SEMICOLON); // 1
			jComboBoxPredefinedDelimiter.addItem(COMMA); // 2
			jComboBoxPredefinedDelimiter.addItem(SPACE); // 3
			jComboBoxPredefinedDelimiter.addItem(TAB); // 4
		}
		return jComboBoxPredefinedDelimiter;
	}

	/**
	 * This method initializes jTextFieldUserDefinedDelimiter
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldUserDefinedDelimiter() {
		if (jTextFieldUserDefinedDelimiter == null) {
			jTextFieldUserDefinedDelimiter = new JTextField();
			jTextFieldUserDefinedDelimiter.setEnabled(false);
			jTextFieldUserDefinedDelimiter.getDocument().addDocumentListener(
					
					new DocumentListener() {

						public void changedUpdate(DocumentEvent e) {
							fireItemChangePerformed();
						}

						public void insertUpdate(DocumentEvent e) {
							fireItemChangePerformed();
						}

						public void removeUpdate(DocumentEvent e) {
							fireItemChangePerformed();
						}

					});
		}
		return jTextFieldUserDefinedDelimiter;
	}

	public String getDelimiterToken() {
		try {
			if (jRadioButtonPredefined.isSelected()) {
				return (String) jComboBoxPredefinedDelimiter.getSelectedItem();
			} else {
				return jTextFieldUserDefinedDelimiter.getText();
			}
		} catch (NullPointerException npe) {
			return null;
		}
	}

	public String getDelimiter() {
		String delim = getDelimiterToken();
		if ("{SPACE}".equals(delim)) { //$NON-NLS-1$
			delim = " "; //$NON-NLS-1$
		} else if ("{TAB}".equals(delim)) { //$NON-NLS-1$
			delim = "\t"; //$NON-NLS-1$
		}
		return delim;
	}

	public void setDelimiterToken(String delimiterToken) {
		try {
			if (PIPE.equals(delimiterToken)) {
				jRadioButtonPredefined.setSelected(true);
				jComboBoxPredefinedDelimiter.setSelectedIndex(0);
			} else if (SEMICOLON.equals(delimiterToken)) {
				jRadioButtonPredefined.setSelected(true);
				jComboBoxPredefinedDelimiter.setSelectedIndex(1);
			} else if (COMMA.equals(delimiterToken)) {
				jRadioButtonPredefined.setSelected(true);
				jComboBoxPredefinedDelimiter.setSelectedIndex(2);
			} else if (SPACE.equals(delimiterToken)) {
				jRadioButtonPredefined.setSelected(true);
				jComboBoxPredefinedDelimiter.setSelectedIndex(3);
			} else if (TAB.equals(delimiterToken)) {
				jRadioButtonPredefined.setSelected(true);
				jComboBoxPredefinedDelimiter.setSelectedIndex(4);
			} else {
				jRadioButtonUserDefined.setSelected(true);
				jTextFieldUserDefinedDelimiter.setText(delimiterToken);
			}
		} catch (NullPointerException npe) {
			// for VE to be shown correctly
		}
	}

	public void addItemListener(ItemListener l) {
		if (listenerList.contains(l) == false) {
			listenerList.add(l);
		}
	}

	public void removeItemListener(ItemListener l) {
		listenerList.remove(l);
	}

	private void fireItemChangePerformed() {
		final ItemEvent e = new ItemEvent(
				this, 
				ItemEvent.ITEM_STATE_CHANGED,
				getDelimiter(), 
				ItemEvent.SELECTED);
		for (int i = 0; i < listenerList.size(); i++) {
			listenerList.get(i).itemStateChanged(e);
		}
	}

	public Object[] getSelectedObjects() {
		final Object[] oa = new Object[1];
		oa[0] = getDelimiter();
		return oa;
	}

	/**
	 * This method initializes jComboBoxTextCover	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJComboBoxEnclosure() {
		if (jComboBoxEnclosure == null) {
			jComboBoxEnclosure = new JComboBox();
			jComboBoxEnclosure.addItem(""); //$NON-NLS-1$
			jComboBoxEnclosure.addItem("\""); //$NON-NLS-1$
			jComboBoxEnclosure.addItem("'"); //$NON-NLS-1$
			jComboBoxEnclosure.addItem("%"); //$NON-NLS-1$
	        final Object comp = jComboBoxEnclosure.getRenderer();
	        if (comp instanceof JLabel) {
	            ((JLabel) comp).setHorizontalAlignment(JLabel.CENTER);
	        }
	        jComboBoxEnclosure.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					fireItemChangePerformed();
				}
			});
		}
		return jComboBoxEnclosure;
	}
	
	public void setEnclosure(String enclosure) {
		if (enclosure == null) {
			enclosure = ""; //$NON-NLS-1$
		}
		jComboBoxEnclosure.setSelectedItem(enclosure);
	}
	
	public String getEnclosure() {
		String enclosure = (String) jComboBoxEnclosure.getSelectedItem();
		if (enclosure == "") { //$NON-NLS-1$
			enclosure = null;
		}
		return enclosure;
	}

} // @jve:decl-index=0:visual-constraint="10,10"
