package sqlrunner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import sqlrunner.flatfileimport.BasicDataType;
import dbtools.SQLPSParam;

/**
 * @author lolling.jan
 * editiert eien Parameter
 * Das Panel enthält nebeneinander den Namen, eine Auswahlbox für den BasicType und ein Eingabefeld für den Inhalt
 * Bei Datumsfeldern wird die globale Einstellung des SimpleDateFormat verwendet.
 */
public final class SQLPSParamEditor extends JPanel implements DocumentListener {

    private static final long serialVersionUID = 1L;
    private SQLPSParam parameter;
    private JLabel     labelName;
    private JComboBox<BasicDataType>  cbType;
    private JTextField textFieldValue;
    static ArrayList<BasicDataType>   basicTypes = new ArrayList<BasicDataType>();  //  @jve:decl-index=0:
    private JCheckBox jCheckBoxOutParam = null;

    public SQLPSParamEditor(SQLPSParam parameter) {
        super();
        this.parameter = parameter;
        if (basicTypes.isEmpty()) {
            basicTypes.add(BasicDataType.DOUBLE);
            basicTypes.add(BasicDataType.CHARACTER);
            basicTypes.add(BasicDataType.DATE);
        }
        initComponents();
        if (parameter.getBasicType() != -1) {
            setRecommendedType(parameter.getBasicType());
        }
    }

    public void initComponents() {
        GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
        gridBagConstraints3.insets = new Insets(0, 2, 0, 0);
        gridBagConstraints3.gridy = 0;
        gridBagConstraints3.gridx = 3;
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints2.gridx = 2;
        gridBagConstraints2.gridy = 0;
        gridBagConstraints2.ipadx = 51;
        gridBagConstraints2.ipady = 3;
        gridBagConstraints2.weightx = 1.0;
        gridBagConstraints2.insets = new Insets(0, 2, 0, 1);
        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.fill = GridBagConstraints.NONE;
        gridBagConstraints1.gridx = 1;
        gridBagConstraints1.gridy = 0;
        gridBagConstraints1.ipadx = 1;
        gridBagConstraints1.ipady = 1;
        gridBagConstraints1.weightx = 0.0;
        gridBagConstraints1.insets = new Insets(0, 2, 0, 1);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 1;
        gridBagConstraints.ipady = 1;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.gridx = 0;
        setLayout(new GridBagLayout());
        labelName = new JLabel();
        if (parameter.getName() != null) {
            labelName.setText(String.valueOf(parameter.getIndex()) + ": " + parameter.getName());
        } else {
            labelName.setText(String.valueOf(parameter.getIndex()));
        }
        labelName.setBackground(Main.info);
        cbType = new JComboBox<BasicDataType>();
        for (int i = 0; i < basicTypes.size(); i++) {
            cbType.addItem(basicTypes.get(i));
        }
        textFieldValue = new JTextField();
        this.add(labelName, gridBagConstraints);
        this.add(cbType, gridBagConstraints1);
        this.add(textFieldValue, gridBagConstraints2);
        this.add(getJCheckBoxOutParam(), gridBagConstraints3);
        textFieldValue.getDocument().addDocumentListener(this);
    }
    
    public String getParameterName() {
        return parameter.getName();
    }

    public int getBasicType() {
        if (cbType.getSelectedItem() != null) {
            return ((BasicDataType) cbType.getSelectedItem()).getId();
        } else {
            return -1;
        }
    }

    public SQLPSParam getParameter() {
        return parameter;
    }

    public void setRecommendedValue(String value) {
        textFieldValue.setText(value);
    }
   
    public void setRecommendedOutputState(boolean output) {
        jCheckBoxOutParam.setSelected(output);
    }

    public void setRecommendedType(int type) {
    	BasicDataType basicType = null;
        for (int i = 0; i < cbType.getItemCount(); i++) {
            basicType = (BasicDataType) cbType.getItemAt(i);
            if (basicType.getId() == type) {
                cbType.setSelectedIndex(i);
                break;
            }
        }
    }

    public String getValueText() {
        return textFieldValue.getText();
    }
    
    public boolean isOutParam() {
        return jCheckBoxOutParam.isSelected();
    }

    public void reset() {
        textFieldValue.setText(null);
        cbType.setSelectedIndex(-1);
    }

    private void test() {
        if (getBasicType() != BasicDataType.DATE.getId()) {
            String text = textFieldValue.getText();
            if (text != null && text.length() > 0) {
                char c;
                for (int i = 0; i < text.length(); i++) {
                    c = text.charAt(i);
                    if (Character.isLetter(c)) {
                        setRecommendedType(BasicDataType.CHARACTER.getId());
                        break;
                    }
                }
            }
        }
    }
    
    public void changedUpdate(DocumentEvent e) {
        test();
    }

    public void insertUpdate(DocumentEvent e) {
        test();
    }

    public void removeUpdate(DocumentEvent e) {
        test();
    }
    
    @Override
    public String toString() {
        return labelName.getText();
    }

    /**
     * This method initializes jCheckBoxOutParam	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getJCheckBoxOutParam() {
        if (jCheckBoxOutParam == null) {
            jCheckBoxOutParam = new JCheckBox();
            jCheckBoxOutParam.setText("output");
        }
        return jCheckBoxOutParam;
    }
    
}
