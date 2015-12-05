package sqlrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import sqlrunner.swinghelper.WindowHelper;
import dbtools.SQLPSParam;
import dbtools.SQLStatement;

/**
 * @author lolling.jan
 * Dieser Dialog konfiguriert die Parameter eines Prepared Statement
 */
public final class SQLPSParamConfigurer extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    public static final int   OK               = 0;
    public static final int   CANCEL           = 1;
    private int               returnCode       = CANCEL;
    private JScrollPane       scrollPaneParams;
    private JPanel            panelButtons;
    private JButton           buttonCancel;
    private JButton           buttonReset;
    private JButton           buttonOk;
    private final List<SQLPSParamEditor>      paramEditorList     = new Vector<SQLPSParamEditor>();  //  @jve:decl-index=0:
    private transient SQLStatement psStat = null;
    private MainFrame         mainFrame;
    private static final HashMap<String, ValueType>     paramValueMap    = new HashMap<String, ValueType>();
    private JPanel jContentPane = null;
    private JButton jButtonSort = null;
    private JPanel jPanelParameters = null;

    public SQLPSParamConfigurer(MainFrame mainFrame, SQLStatement ps) {
        super(mainFrame);
        getRootPane().putClientProperty("Window.style", "small");
        setModal(true);
        this.mainFrame = mainFrame;
        psStat = ps;
        initComponents();
        setSQLPreparedStatement(ps);
        pack();
    }

    private void correctPosition() {
        WindowHelper.locateWindowAtMiddle(mainFrame, this);
    }

    private void initComponents() {
        panelButtons = new JPanel();
        buttonOk = new JButton("OK");
        buttonOk.addActionListener(this);
        buttonReset = new JButton("Clear all values");
        buttonReset.addActionListener(this);
        buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(this);
        panelButtons.add(buttonOk);
        panelButtons.add(buttonReset);
        panelButtons.add(buttonCancel);
        panelButtons.add(getJButtonSort(), null);
        scrollPaneParams = new JScrollPane();
        scrollPaneParams.setViewportView(getJPanelParameters());
        setTitle("Statement " + psStat.getIndex());
        this.setContentPane(getJContentPane());
    }

    public void setSQLPreparedStatement(SQLStatement ps) {
        // nun die Eingabefelder für die Parameter erzeugen
        SQLPSParam param = null;
        SQLPSParamEditor paramEditor = null;
        paramEditorList.clear();
        ValueType vt = null;
        for (int i = 0; i < ps.getParams().size(); i++) {
            param = ps.getParams().get(i);
            paramEditor = new SQLPSParamEditor(param);
            if (param.getName() != null) {
                vt = paramValueMap.get(param.getName());
            } else {
                vt = paramValueMap.get(String.valueOf(param.getIndex()));
            }
            if (vt != null) {
                paramEditor.setRecommendedValue(vt.value);
                paramEditor.setRecommendedType(vt.type);
                paramEditor.setRecommendedOutputState(vt.output);
            }
            paramEditorList.add(paramEditor);
        }
        setupParamEditors(paramEditorList);
        doLayout();
    }

    public int getReturnCode() {
        return returnCode;
    }

    public SQLStatement getSQLPreparedStatement() {
        return psStat;
    }

    private void setupParameter() {
        SQLPSParamEditor paramEditor = null;
        SQLPSParam param = null;
        ValueType vt = null;
        for (int i = 0; i < paramEditorList.size(); i++) {
            paramEditor = paramEditorList.get(i);
            param = paramEditor.getParameter();
            param.setBasicType(paramEditor.getBasicType());
            param.setOutParam(paramEditor.isOutParam());
            if (param.isOutParam()) {
                param.setValue(null);
            } else {
                param.setValue(paramEditor.getValueText());
            }
            // die gesetzten Werte merken
            vt = new ValueType();
            vt.type = param.getBasicType();
            vt.value = param.getValue();
            vt.output = param.isOutParam();
            if (param.getName() != null) {
                paramValueMap.put(param.getName(), vt);
            } else {
                paramValueMap.put(String.valueOf(param.getIndex()), vt);
            }
        }
    }
    
    private void sortParameterByName() {
        Collections.sort(paramEditorList, new ParamEditorComparator());
        setupParamEditors(paramEditorList);
    }
    
    private void setupParamEditors(List<SQLPSParamEditor> editorList) {
        int preferredHeight = 0;
        SQLPSParamEditor paramEditor = null;
        jPanelParameters.removeAll();
        for (int i = 0; i < editorList.size(); i++) {
            paramEditor = editorList.get(i);
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
            jPanelParameters.add(paramEditor, gridBagConstraints);
            preferredHeight = (preferredHeight + paramEditor.getPreferredSize().height) + 8;
        }
        if (preferredHeight > 600) {
            preferredHeight = 600;
        }
        scrollPaneParams.setPreferredSize(new Dimension(450, preferredHeight));
        // wichtig damit die Maus nicht einzelne Pixel schiebt. Die Schrittweite ist nun ein Eingabefeld
        scrollPaneParams.getVerticalScrollBar().setUnitIncrement(paramEditor.getPreferredSize().height);
        pack();
        correctPosition();
    }
    
    private static final class ParamEditorComparator implements Comparator<SQLPSParamEditor>, Serializable {

		private static final long serialVersionUID = 1L;

		public int compare(SQLPSParamEditor e1, SQLPSParamEditor e2) {
            return e1.getParameterName().compareTo(e2.getParameterName());
        }
        
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == buttonOk) {
            setupParameter();
            returnCode = OK;
            setVisible(false);
        } else if (ae.getSource() == buttonReset) {
            for (int i = 0; i < paramEditorList.size(); i++) {
                paramEditorList.get(i).reset();
            }
        } else if (ae.getSource() == buttonCancel) {
            returnCode = CANCEL;
            setVisible(false);
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            returnCode = CANCEL;
        }
        if (!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    static final class ValueType {

        public String value;
        public int    type;
        public boolean output;
        
    }

    /**
     * This method initializes jContentPane	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(scrollPaneParams, BorderLayout.CENTER);
            jContentPane.add(panelButtons, BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes jButtonSort	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButtonSort() {
        if (jButtonSort == null) {
            jButtonSort = new JButton();
            jButtonSort.setText("Sortieren");
            jButtonSort.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    sortParameterByName();
                }
            });
        }
        return jButtonSort;
    }

    /**
     * This method initializes jPanelParameters	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanelParameters() {
        if (jPanelParameters == null) {
            jPanelParameters = new JPanel();
            jPanelParameters.setLayout(new GridBagLayout());
        }
        return jPanelParameters;
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"
