package sqlrunner.flatfileimport.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import sqlrunner.Main;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.FieldDescription;
import sqlrunner.swinghelper.WindowHelper;

public class DescriptionEditor extends JDialog {

    private static final long serialVersionUID = 1L;
    private JPanel jContentPane;
    private JComboBox cbPositionType;
    private JTextField textFieldColumnName;
    private JLabel labelIndex;
    private JLabel jLabelDefaultValue;
    private JCheckBox checkboxEnabled;
    private JCheckBox checkboxIsPrimaryKey;
    private JComboBox cbBasicType;
    private JComboBox comboboxDateFormat;
    private JComboBox comboBoxLocale;
    private JTextField textFieldDelimPos;
    private JTextField textFieldAbsPos;
    private JTextField textFieldLength;
    private JButton buttonOk;
    private JButton buttonCancel;
    private transient FieldDescription fd = null;  //  @jve:decl-index=0:
    private JLabel labelRegelText;
    private boolean okPerformed;
    static String tooltipTextDate = "<HTML><BODY>" //$NON-NLS-1$
        + Messages.getString("DescriptionEditor.1") //$NON-NLS-1$
        + Messages.getString("DescriptionEditor.2") //$NON-NLS-1$
        + Messages.getString("DescriptionEditor.3") //$NON-NLS-1$
        + Messages.getString("DescriptionEditor.4") //$NON-NLS-1$
        + Messages.getString("DescriptionEditor.5") //$NON-NLS-1$
        + Messages.getString("DescriptionEditor.6") //$NON-NLS-1$
        + Messages.getString("DescriptionEditor.7") //$NON-NLS-1$
        + "</BODY></HTML>"; //$NON-NLS-1$  //  @jve:decl-index=0:
    private JTextField textFieldDefaultValue;
    private JComboBox cbAlternativeFieldDescription;
    private JCheckBox jCheckBoxAggregateValues = null;
    private JCheckBox jCheckBoxNotNull = null;
    private JCheckBox jCheckBoxIgnoreDatasetIfInvalid = null;
    private JTextField jTextFieldGeneratorStartValue = null;
    private JTextField jTextFieldRegex = null;
    private JCheckBox jCheckBoxTrim;
    private ImportConfiguratorFrame configuratorFrame;

    public DescriptionEditor(ImportConfiguratorFrame configuratorFrame, FieldDescription desc) {
        super(configuratorFrame, true);
        this.fd = desc;
        this.configuratorFrame = configuratorFrame;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            loadDescription(fd);
            pack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DescriptionEditor(ImportConfiguratorFrame configuratorFrame) {
        super(configuratorFrame, true);
        this.configuratorFrame = configuratorFrame;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // Dummy für den Designer

    @Override
    public void setVisible(boolean visible) {
        if (!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {
            }
        }
        super.setVisible(visible);
    }

    private void initComponents() throws Exception {
        setTitle(Messages.getString("DescriptionEditor.newfielddesc")); //$NON-NLS-1$
        setContentPane(getJContentPane());
        getRootPane().setDefaultButton(buttonOk);
        initComboBoxes();
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
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 5;
                labelIndex = new JLabel();
                labelIndex.setBackground(new Color(255, 255, 240));
                labelIndex.setText(Messages.getString("DescriptionEditor.chronlogicalorder")); //$NON-NLS-1$
                labelIndex.setFont(new java.awt.Font("Dialog", 0, 12)); //$NON-NLS-1$
                labelIndex.setForeground(Color.black);
                labelIndex.setBorder(BorderFactory.createEtchedBorder());
                labelIndex.setOpaque(true);
                labelIndex.setHorizontalAlignment(SwingConstants.CENTER);
                jContentPane.add(labelIndex, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 3;
                checkboxEnabled = new JCheckBox();
                checkboxEnabled.setOpaque(false);
                checkboxEnabled.setText(Messages.getString("DescriptionEditor.enablefield")); //$NON-NLS-1$
                jContentPane.add(checkboxEnabled, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.anchor = GridBagConstraints.EAST;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.tablefield")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 2;
                gbc.gridx = 1;
                gbc.gridy = 2;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 10);
                textFieldColumnName = new JTextField();
                textFieldColumnName.setToolTipText(Messages.getString("DescriptionEditor.fieldintargettable")); //$NON-NLS-1$
                jContentPane.add(textFieldColumnName, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 3;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 5;
                checkboxIsPrimaryKey = new JCheckBox();
                checkboxIsPrimaryKey.setOpaque(false);
                checkboxIsPrimaryKey.setText(Messages.getString("DescriptionEditor.unique")); //$NON-NLS-1$
                jContentPane.add(checkboxIsPrimaryKey, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 4;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 10, 2, 0);
                gbc.gridwidth = 2;
                jContentPane.add(getJCheckBoxNotNull(), gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 2;
                gbc.gridy = 4;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 10, 2, 0);
                gbc.gridwidth = 2;
                jContentPane.add(getJCheckBoxIgnoreDatasetIfInvalid(), gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 5;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridwidth = 1;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.datatype1"));
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 1;
                gbc.gridy = 5;
                gbc.weightx = 1.0;
                gbc.gridwidth = 2;
                gbc.insets = new Insets(2, 2, 2, 10);
                cbBasicType = new JComboBox();
                cbBasicType.setToolTipText(Messages.getString("DescriptionEditor.datatype")); //$NON-NLS-1$
                cbBasicType.addItemListener(new java.awt.event.ItemListener() {

                    public void itemStateChanged(ItemEvent e) {
                        cbBasicType_itemStateChanged(e);
                    }
                });
                jContentPane.add(cbBasicType, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 6;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 3;
                jContentPane.add(getJCheckBoxAggregateValues(), gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 7;
                gbc.anchor = GridBagConstraints.EAST;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.format")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 2;
                gbc.gridx = 1;
                gbc.gridy = 7;
                gbc.weightx = 1.0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 2, 2, 10);
                comboboxDateFormat = new JComboBox();
                comboboxDateFormat.setEditable(true);
                jContentPane.add(comboboxDateFormat, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 8;
                gbc.anchor = GridBagConstraints.EAST;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.locale")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 2;
                gbc.gridx = 1;
                gbc.gridy = 8;
                gbc.weightx = 1.0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 2, 2, 10);
                comboBoxLocale = new JComboBox();
                comboBoxLocale.setEditable(true);
                jContentPane.add(comboBoxLocale, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridx = 0;
                gbc.gridy = 9;
                gbc.insets = new Insets(2, 10, 2, 2);
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.alternativefield")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 1;
                gbc.gridy = 9;
                gbc.gridwidth = 2;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 10);
                cbAlternativeFieldDescription = new JComboBox();
                jContentPane.add(cbAlternativeFieldDescription, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 10;
                gbc.anchor = GridBagConstraints.EAST;
                jLabelDefaultValue = new JLabel();
                jLabelDefaultValue.setText(Messages.getString("DescriptionEditor.defaultvalue")); //$NON-NLS-1$
                jContentPane.add(jLabelDefaultValue, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 1;
                gbc.gridy = 10;
                gbc.gridwidth = 2;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 10);
                textFieldDefaultValue = new JTextField();
                jContentPane.add(textFieldDefaultValue, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 11;
                gbc.anchor = GridBagConstraints.EAST;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.positioning")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 2;
                gbc.gridx = 1;
                gbc.gridy = 11;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 10);
                cbPositionType = new JComboBox();
                cbPositionType.setToolTipText(Messages.getString("DescriptionEditor.kindofselection")); //$NON-NLS-1$
                cbPositionType.addItemListener(new java.awt.event.ItemListener() {

                    public void itemStateChanged(ItemEvent e) {
                        cbPositionType_itemStateChanged(e);
                    }
                });
                jContentPane.add(cbPositionType, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 12;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridwidth = 2;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.fromcountdelimiter")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 2;
                gbc.gridy = 12;
                gbc.weightx = 1.0;
                gbc.gridwidth = 1;
                gbc.insets = new Insets(2, 2, 2, 10);
                textFieldDelimPos = new JTextField();
                textFieldDelimPos.setEnabled(false);
                jContentPane.add(textFieldDelimPos, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 13;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridwidth = 2;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.absolutecharpos")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 2;
                gbc.gridy = 13;
                gbc.insets = new Insets(2, 2, 2, 10);
                gbc.weightx = 1.0;
                textFieldAbsPos = new JTextField();
                textFieldAbsPos.setEnabled(false);
                jContentPane.add(textFieldAbsPos, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 14;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridwidth = 2;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.readnumberofchars")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 2;
                gbc.gridy = 14;
                gbc.weightx = 1.0;
                gbc.gridwidth = 1;
                gbc.insets = new Insets(2, 2, 2, 10);
                textFieldLength = new JTextField();
                textFieldLength.setEnabled(false);
                jContentPane.add(textFieldLength, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 15;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridwidth = 2;
                JLabel jLabel = new JLabel();
                jLabel.setText(Messages.getString("DescriptionEditor.0")); //$NON-NLS-1$
                jContentPane.add(jLabel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 2;
                gbc.gridy = 15;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(2, 2, 2, 10);
                jContentPane.add(getJTextFieldGeneratorStartValue(), gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = 16;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 2;
                gbc.insets = new Insets(2, 2, 2, 10);
                jContentPane.add(getJCheckBoxTrim(), gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 17;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 0);
                JLabel jLabel3 = new JLabel();
                jLabel3.setText("Regex");
                jContentPane.add(jLabel3, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 1;
                gbc.gridy = 17;
                gbc.weightx = 1.0;
                gbc.gridwidth = 2;
                gbc.insets = new Insets(2, 2, 2, 10);
                jContentPane.add(getJTextFieldRegex(), gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 18;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 3;
                labelRegelText = new JLabel();
                labelRegelText.setBackground(new Color(255, 255, 240));
                labelRegelText.setFont(new java.awt.Font("Dialog", 0, 12)); //$NON-NLS-1$
                labelRegelText.setForeground(Color.black);
                labelRegelText.setBorder(BorderFactory.createEtchedBorder());
                labelRegelText.setOpaque(true);
                labelRegelText.setToolTipText(Messages.getString("DescriptionEditor.summary")); //$NON-NLS-1$
                labelRegelText.setText("    "); //$NON-NLS-1$
                labelRegelText.setHorizontalAlignment(SwingConstants.CENTER);
                jContentPane.add(labelRegelText, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 20, 5, 2);
                gbc.gridx = 0;
                gbc.gridy = 19;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridwidth = 1;
                buttonCancel = new JButton();
                buttonCancel.setText(Messages.getString("DescriptionEditor.cancel")); //$NON-NLS-1$
                buttonCancel.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        buttonCancel_actionPerformed(e);
                    }
                });
                jContentPane.add(buttonCancel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 5, 20);
                gbc.gridx = 1;
                gbc.gridy = 19;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridwidth = 2;
                buttonOk = new JButton();
                buttonOk.setText(Messages.getString("DescriptionEditor.accept")); //$NON-NLS-1$
                buttonOk.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        buttonOk_actionPerformed(e);
                    }
                });
                jContentPane.add(buttonOk, gbc);
            }
        }
        return jContentPane;
    }

    private void setupComboBoxAlternativeField() {
        cbAlternativeFieldDescription.removeAllItems();
        cbAlternativeFieldDescription.addItem(Messages.getString("DescriptionEditor.noalternativefield"));
        for (int i = 0, max = configuratorFrame.getAllFieldDescriptions().size(); i < max; i++) {
            FieldDescription temp = configuratorFrame.getAllFieldDescriptions().get(i);
            if (temp.equals(this.fd) == false) {
                cbAlternativeFieldDescription.addItem(temp);
            }
        }
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
    }

    private void cancel() {
        dispose();
    }

    /**
     * Achtung in der Methode cbBasicType_itmeStateChanged wird auf Bezug
     * genommen auf die Reihenfolge der Items ! Änderungen müssen dort
     * nachvollzogen werden
     */
    private void initComboBoxes() {
        cbBasicType.addItem(BasicDataType.CHARACTER);
        cbBasicType.addItem(BasicDataType.DATE);
        cbBasicType.addItem(BasicDataType.DOUBLE);
        cbBasicType.addItem(BasicDataType.INTEGER);
        cbBasicType.addItem(BasicDataType.LONG);
        cbBasicType.addItem(BasicDataType.BOOLEAN);
        cbBasicType.addItem("--");
        cbBasicType.addItem(BasicDataType.CLOB);
        cbBasicType.addItem(BasicDataType.ROWID);
        cbBasicType.addItem("--");
        cbBasicType.addItem(BasicDataType.SQLEXP);
        // important ! the follwoing int values must correspondent with contains in FieldDescriptions
        cbPositionType.addItem(FieldDescription.getPositioningTypeName(0));
        cbPositionType.addItem(FieldDescription.getPositioningTypeName(1));
        cbPositionType.addItem(FieldDescription.getPositioningTypeName(2));
        cbPositionType.addItem(FieldDescription.getPositioningTypeName(3));
        cbPositionType.addItem(FieldDescription.getPositioningTypeName(4));
        cbPositionType.addItem(FieldDescription.getPositioningTypeName(5));
        setupComboBoxAlternativeField();
        prepareComboBoxFormatForDate();
        prepareComboBoxLocale();
    }

    private void prepareComboBoxFormatForDate() {
        int i = 0;
        String formatStr;
        comboboxDateFormat.removeAllItems();
        while (true) {
            formatStr = Main.getDefaultProperty("DATE_FORMAT_" + String.valueOf(i)); //$NON-NLS-1$
            if (formatStr != null) {
                comboboxDateFormat.addItem(formatStr);
            } else {
                break; // Schleife abbrechen, wenn keine weiteren Einträge
            // vorhanden
            }
            i++;
        }
        comboboxDateFormat.setToolTipText(tooltipTextDate);
    }

    private void prepareComboBoxLocale() {
        comboBoxLocale.removeAllItems();
        Locale[] locales = NumberFormat.getAvailableLocales();
        Arrays.sort(locales, new Comparator<Locale>() {

            public int compare(final Locale o1, final Locale o2) {
                return o1.toString().compareTo(o2.toString());
            }

        });
        for (int i = 0; i < locales.length; i++) {
            comboBoxLocale.addItem(locales[i]);
        }
        comboBoxLocale.setSelectedItem(new Locale("en_US"));
        comboBoxLocale.setToolTipText("select location for your numbers");
    }

    public void setFieldDescription(FieldDescription fd_loc) {
        this.fd = fd_loc;
        loadDescription(fd_loc);
    }

    private void loadDescription(FieldDescription fd_loc) {
        if (fd_loc == null) {
            throw new IllegalArgumentException("fd_loc cannot be null"); //$NON-NLS-1$
        }
        textFieldColumnName.setText(fd_loc.getName());
        if (fd_loc.isDummy()) {
        	labelIndex.setText(" ");
        } else {
            labelIndex.setText(Messages.getString("DescriptionEditor.willbereadas") + String.valueOf(fd_loc.getIndex() + 1) + Messages.getString("DescriptionEditor.readed")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        checkboxEnabled.setSelected(fd_loc.isEnabled());
        checkboxIsPrimaryKey.setSelected(fd_loc.isPartOfPrimaryKey());
        cbBasicType.setSelectedItem(BasicDataType.getBasicDataType(fd_loc.getBasicTypeId()));
        setComboboxDateFormatItem(fd_loc.getFieldFormat());
        setComboboxLocaleItem(fd_loc.getLocale());
        if (fd_loc.getDefaultValue() != null) {
            textFieldDefaultValue.setText(fd_loc.getDefaultValue());
        } else {
            textFieldDefaultValue.setText(""); //$NON-NLS-1$
        }
        cbPositionType.setSelectedIndex(fd_loc.getPositionType());
        if (fd_loc.getDelimPos() != -1) {
            textFieldDelimPos.setText(String.valueOf(fd_loc.getDelimPos()));
        }
        if (fd_loc.getAbsPos() != -1) {
            textFieldAbsPos.setText(String.valueOf(fd_loc.getAbsPos()));
        }
        textFieldLength.setText(String.valueOf(fd_loc.getLength()));
        labelRegelText.setText(fd_loc.getExtractionDescription());
        jCheckBoxAggregateValues.setSelected(fd_loc.isAggregateNumberValues());
        if (fd.getName() != null) {
            setTitle(Messages.getString("DescriptionEditor.importfielddesc") + fd.getName()); //$NON-NLS-1$
        } else {
            setTitle(Messages.getString("DescriptionEditor.importfielddesc")); //$NON-NLS-1$
        }
        jCheckBoxNotNull.setSelected(fd_loc.isNullEnabled() == false);
        jCheckBoxIgnoreDatasetIfInvalid.setSelected(fd_loc.isIgnoreDatasetIfInvalid());
        jTextFieldGeneratorStartValue.setText(fd_loc.getGeneratorStartValue());
        jCheckBoxTrim.setSelected(fd_loc.isTrimRequired());
        jTextFieldRegex.setText(fd.getRegex());
        if (fd_loc.getAlternativeFieldDescription() != null) {
            cbAlternativeFieldDescription.setSelectedItem(fd_loc.getAlternativeFieldDescription());
        } else {
            cbAlternativeFieldDescription.setSelectedIndex(0);
        }
    }

    private void setComboboxDateFormatItem(String format) {
        if (format != null) {
            int i;
            for (i = 0; i < comboboxDateFormat.getItemCount(); i++) {
                if (format.equals(comboboxDateFormat.getItemAt(i).toString())) {
                    comboboxDateFormat.setSelectedIndex(i);
                    break;
                }
            }
            if (i == comboboxDateFormat.getItemCount()) {
                // nicht gefunden
                // neu hinzufügen
                comboboxDateFormat.addItem(format);
                comboboxDateFormat.setSelectedItem(format);
            }
        }
    }

    private void setComboboxLocaleItem(Locale locale) {
        comboBoxLocale.setSelectedItem(locale);
    }

    private void buttonCancel_actionPerformed(ActionEvent e) {
        okPerformed = false;
        cancel();
    }

    private Locale getSelectedLocale() {
        Object o = comboBoxLocale.getSelectedItem();
        if (o instanceof Locale) {
            return (Locale) o;
        } else if (o instanceof String) {
            return new Locale((String) o);
        } else {
            return null;
        }
    }

    private void buttonOk_actionPerformed(ActionEvent e) {
        // fill description with new edited configuration
        fd.setName(textFieldColumnName.getText().trim());
        fd.setDefaultValue(textFieldDefaultValue.getText());
        fd.setEnabled(checkboxEnabled.isSelected());
        fd.setIsPartOfPrimaryKey(checkboxIsPrimaryKey.isSelected());
        BasicDataType type = (BasicDataType) cbBasicType.getSelectedItem();
        if (type != null) {
            fd.setBasicTypeId(type.getId());
        }
        fd.setAggregateNumberValues(jCheckBoxAggregateValues.isSelected());
        final Object item = comboboxDateFormat.getSelectedItem();
        if (item != null) {
            fd.setFormat(item.toString().trim());
        }
        fd.setLocale(getSelectedLocale());
        fd.setPositionType(cbPositionType.getSelectedIndex());
        switch (fd.getPositionType()) {
            case FieldDescription.ABSOLUTE_POSITION:
                if (textFieldAbsPos.getText().length() > 0) {
                    fd.setAbsPos(Integer.parseInt(textFieldAbsPos.getText()));
                }
                if (textFieldLength.getText().length() > 0) {
                    fd.setLength(Integer.parseInt(textFieldLength.getText()));
                }
                break;
            case FieldDescription.RELATIVE_POSITION:
                if (textFieldLength.getText().length() > 0) {
                    fd.setLength(Integer.parseInt(textFieldLength.getText()));
                }
                break;
            case FieldDescription.DELIMITER_POSITION_WITH_LENGTH:
                if (textFieldLength.getText().length() > 0) {
                    fd.setLength(Integer.parseInt(textFieldLength.getText()));
                }
                if (textFieldDelimPos.getText().length() > 0) {
                    fd.setDelimPos(Integer.parseInt(textFieldDelimPos.getText()));
                }
                break;
            case FieldDescription.DELIMITER_POSITION:
                if (textFieldDelimPos.getText().length() > 0) {
                    fd.setDelimPos(Integer.parseInt(textFieldDelimPos.getText()));
                }
                break;
        }
        fd.setNullEnabled(jCheckBoxNotNull.isSelected() == false);
        fd.setIgnoreDatasetIfInvalid(jCheckBoxIgnoreDatasetIfInvalid.isSelected());
        fd.setGeneratorStartValue(jTextFieldGeneratorStartValue.getText());
        fd.setFilterRegex(jTextFieldRegex.getText());
        fd.setTrimRequired(jCheckBoxTrim.isSelected());
        Object test = cbAlternativeFieldDescription.getSelectedItem();
        if (test instanceof FieldDescription) {
            fd.setAlternativeFieldDescriptionName(((FieldDescription) test).getName());
            fd.setAlternativeFieldDescription((FieldDescription) test);
        } else {
            fd.setAlternativeFieldDescriptionName(null);
            fd.setAlternativeFieldDescription(null);
        }
        if (fd.validate()) {
            okPerformed = true;
            cancel();
        } else {
            JOptionPane.showMessageDialog(this,
                fd.getErrorMessage(),
                Messages.getString("DescriptionEditor.checkdesc"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
        }
    }

    public boolean isOkPerformed() {
        return okPerformed;
    }

    private void cbPositionType_itemStateChanged(ItemEvent e) {
        if (cbPositionType.getSelectedItem() != null) {
            cbPositionType.setToolTipText(cbPositionType.getSelectedItem().toString());
        }
        switch (cbPositionType.getSelectedIndex()) {
            case FieldDescription.ABSOLUTE_POSITION:
                textFieldAbsPos.setEnabled(true);
                textFieldLength.setEnabled(true);
                textFieldDelimPos.setEnabled(false);
                jTextFieldRegex.setEnabled(true);
                jCheckBoxTrim.setEnabled(true);
                jTextFieldGeneratorStartValue.setEnabled(false);
                break;
            case FieldDescription.RELATIVE_POSITION:
                textFieldLength.setEnabled(true);
                textFieldAbsPos.setEnabled(false);
                textFieldDelimPos.setEnabled(false);
                jTextFieldRegex.setEnabled(true);
                jCheckBoxTrim.setEnabled(true);
                jTextFieldGeneratorStartValue.setEnabled(false);
                break;
            case FieldDescription.DELIMITER_POSITION_WITH_LENGTH:
                textFieldLength.setEnabled(true);
                textFieldDelimPos.setEnabled(true);
                textFieldAbsPos.setEnabled(false);
                jTextFieldRegex.setEnabled(true);
                jCheckBoxTrim.setEnabled(true);
                jTextFieldGeneratorStartValue.setEnabled(false);
                break;
            case FieldDescription.DELIMITER_POSITION:
                textFieldDelimPos.setEnabled(true);
                textFieldLength.setEnabled(false);
                textFieldAbsPos.setEnabled(false);
                jTextFieldRegex.setEnabled(true);
                jCheckBoxTrim.setEnabled(true);
                jTextFieldGeneratorStartValue.setEnabled(false);
                break;
            case FieldDescription.AUTO_GENERATED:
                textFieldDelimPos.setEnabled(false);
                textFieldLength.setEnabled(false);
                textFieldAbsPos.setEnabled(false);
                jTextFieldRegex.setEnabled(false);
                jCheckBoxTrim.setEnabled(false);
                jTextFieldGeneratorStartValue.setEnabled(true);
                break;
            case FieldDescription.FIX_VALUE:
                textFieldDelimPos.setEnabled(false);
                textFieldLength.setEnabled(false);
                textFieldAbsPos.setEnabled(false);
                jTextFieldRegex.setEnabled(false);
                jCheckBoxTrim.setEnabled(false);
                jTextFieldGeneratorStartValue.setEnabled(false);
                break;
        }
    }

    private void cbBasicType_itemStateChanged(ItemEvent e) {
        BasicDataType type = (BasicDataType) cbBasicType.getSelectedItem();
        if (type == BasicDataType.DATE) {
            jLabelDefaultValue.setText(Messages.getString("DescriptionEditor.defaultvalue"));
            comboboxDateFormat.setEnabled(true);
            comboboxDateFormat.setToolTipText(tooltipTextDate);
            comboBoxLocale.setEnabled(true);
            jCheckBoxTrim.setEnabled(true);
            jTextFieldRegex.setEnabled(true);
            jCheckBoxAggregateValues.setEnabled(false);
            cbAlternativeFieldDescription.setEnabled(true);
            cbPositionType.setEnabled(true);
        } else if (BasicDataType.isNumberType(type)) {
            jLabelDefaultValue.setText(Messages.getString("DescriptionEditor.defaultvalue"));
            comboboxDateFormat.setEnabled(false);
            comboBoxLocale.setEnabled(true);
            jCheckBoxTrim.setEnabled(true);
            jTextFieldRegex.setEnabled(true);
            jCheckBoxAggregateValues.setEnabled(true);
            cbAlternativeFieldDescription.setEnabled(true);
            cbPositionType.setEnabled(true);
        } else if (type == BasicDataType.BOOLEAN) {
            jLabelDefaultValue.setText(Messages.getString("DescriptionEditor.defaultvalue"));
            comboboxDateFormat.setEnabled(false);
            comboBoxLocale.setEnabled(false);
            jCheckBoxTrim.setEnabled(true);
            jTextFieldRegex.setEnabled(true);
            jCheckBoxAggregateValues.setEnabled(false);
            cbAlternativeFieldDescription.setEnabled(true);
            cbPositionType.setEnabled(true);
        } else if (type == BasicDataType.SQLEXP) {
            jLabelDefaultValue.setText(Messages.getString("DescriptionEditor.sqlterm"));
            comboboxDateFormat.setEnabled(false);
            comboboxDateFormat.setToolTipText(null);
            comboBoxLocale.setEnabled(false);
            jCheckBoxTrim.setEnabled(false);
            jTextFieldRegex.setEnabled(false);
            jCheckBoxAggregateValues.setEnabled(false);
            cbAlternativeFieldDescription.setEnabled(false);
            cbPositionType.setEnabled(false);
        } else {
            jLabelDefaultValue.setText(Messages.getString("DescriptionEditor.defaultvalue"));
            comboboxDateFormat.setEnabled(false);
            comboboxDateFormat.setToolTipText(null);
            comboBoxLocale.setEnabled(false);
            jCheckBoxTrim.setEnabled(true);
            jTextFieldRegex.setEnabled(true);
            jCheckBoxAggregateValues.setEnabled(false);
            cbAlternativeFieldDescription.setEnabled(true);
            cbPositionType.setEnabled(true);
        }
    }

    /**
     * This method initializes jCheckBoxAggregateValues
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getJCheckBoxAggregateValues() {
        if (jCheckBoxAggregateValues == null) {
            jCheckBoxAggregateValues = new JCheckBox();
            jCheckBoxAggregateValues.setText(Messages.getString("DescriptionEditor.aggregatevalues")); //$NON-NLS-1$
        }
        return jCheckBoxAggregateValues;
    }

    /**
     * This method initializes jCheckBoxNotNull	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getJCheckBoxNotNull() {
        if (jCheckBoxNotNull == null) {
            jCheckBoxNotNull = new JCheckBox();
            jCheckBoxNotNull.setText(Messages.getString("DescriptionEditor.notnull")); //$NON-NLS-1$
        }
        return jCheckBoxNotNull;
    }

    /**
     * This method initializes jCheckBoxNotNull	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getJCheckBoxIgnoreDatasetIfInvalid() {
        if (jCheckBoxIgnoreDatasetIfInvalid == null) {
        	jCheckBoxIgnoreDatasetIfInvalid = new JCheckBox();
        	jCheckBoxIgnoreDatasetIfInvalid.setText(Messages.getString("DescriptionEditor.ignoreDatasetIfInvalid")); //$NON-NLS-1$
        }
        return jCheckBoxIgnoreDatasetIfInvalid;
    }

    /**
     * This method initializes jTextFieldGeneratorStartValue	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getJTextFieldGeneratorStartValue() {
        if (jTextFieldGeneratorStartValue == null) {
            jTextFieldGeneratorStartValue = new JTextField();
        }
        return jTextFieldGeneratorStartValue;
    }

    /**
     * This method initializes jTextFieldRegex	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getJTextFieldRegex() {
        if (jTextFieldRegex == null) {
            jTextFieldRegex = new JTextField();
        }
        return jTextFieldRegex;
    }

    private JCheckBox getJCheckBoxTrim() {
        if (jCheckBoxTrim == null) {
            jCheckBoxTrim = new JCheckBox();
            jCheckBoxTrim.setText(Messages.getString("DescriptionEditor.trim"));
        }
        return jCheckBoxTrim;
    }
}  //  @jve:decl-index=0:visual-constraint="10,10"
