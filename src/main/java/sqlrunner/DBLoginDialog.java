package sqlrunner;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import sqlrunner.text.StringReplacer;
import sqlrunner.swinghelper.WindowHelper;
import dbtools.ConnectionDescription;
import dbtools.DatabaseType;
import dbtools.URLElement;

public final class DBLoginDialog extends JDialog implements ListSelectionListener {

    private static final long serialVersionUID = 1L;
    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(DBLoginDialog.class.getName());
    private JButton buttonLogin;
    private JButton buttonSave;
    private JButton buttonDel;
    private JButton buttonClose;
    private JButton buttonNew;
    private JButton buttonSort;
    private JScrollPane jScrollPaneConnectionList;
    private DefaultListModel<ConnectionDescription> listModel;
    private JList<ConnectionDescription> list;
    private JComboBox<DatabaseType> comboBoxType;
    private ConnectionDescription desc;
    private List<ConnectionDescription> selectedListValues;
    private ConnectionDescription selectedDesc;
    static public final int LOGIN_ACTION_PERFORMED = 0;
    static public final int CANCEL_ACTION_PERFORMED = 1;
    private int returnCode;
    private JScrollPane jScrollPaneInputFields;
    private JPanel panelElements;
    private List<URLComponent> urlComponents;
    private JCheckBox cbStorePasswd;
    private boolean inChooseFromListTransaction = false;
    private JTextField textFieldProperties;
    private JTextField textFieldComment;
    private JCheckBox checkBoxProductive;
    private JCheckBox checkBoxShowComment;                                                          // kennzeichent, dass Auswahlprozesss aus Liste momentan erfolgt
    private JCheckBox checkBoxExtendsUrlWidthProperties;
    private JPanel jContentPane = null;
    private JScrollPane jScrollPaneInitSQL = null;
    private JTextArea jTextAreaInitSQL = null;
    private JCheckBox jCheckBoxRunInitSQL = null;
    private JTextField jTextFieldFetchSize = null;

    public DBLoginDialog(MainFrame mainFrame, String title) {
        super(mainFrame, title, true);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            initComponents();
            loadConnectionList();
            pack();
            WindowHelper.locateWindowAtMiddle(mainFrame, this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            cancel();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            buttonLogin_actionPerformed();
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()) {
            case WindowEvent.WINDOW_CLOSING:
                cancel();
                break;
        }
        super.processWindowEvent(e);
    }

    private void initComponents() throws Exception {
        this.setContentPane(getJContentPane());
        // wenn die Liste Einträge beinhaltet, dann den Focus auf die Liste setzen
        if ((list.getModel()).getSize() > 0) {
            list.requestFocus();
            list.setSelectedIndex(0);
        } else {
            createURLComponents((DatabaseType) comboBoxType.getSelectedItem());
        }
        setResizable(true);
        getRootPane().setDefaultButton(buttonLogin);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && (list.getModel().getSize() > 0)) {
            list.requestFocus();
            list.setSelectedIndex(0);
        }
        if (isShowing() == false) {
            try {
                this.setLocationByPlatform(WindowHelper.isWindowPositioningEnabled() == false);
            } catch (NoSuchMethodError e) {
            	//could happend, but is no problem
            }
        }
        super.setVisible(visible);
    }

    private void loadComboBoxType() {
        for (int i = 0; i < ConnectionDescription.getDatabaseTypes().size(); i++) {
            comboBoxType.addItem(ConnectionDescription.getDatabaseTypes().get(i));
        }
    }

    /**
     * lädt Liste der Verbindung aus Datei (INI_FILE_NAME)
     * @return true wenn erfolgreich
     */
    private boolean loadConnectionList() {
        boolean ok = false;
        try {
            final File iniFile = new File(Main.getFileNameForConnectionList());
            if (iniFile.exists()) {
                final BufferedReader fr = new BufferedReader(new FileReader(iniFile));
                String line;
                listModel.removeAllElements();
                while ((line = fr.readLine()) != null) {
                    listModel.addElement(new ConnectionDescription(line));
                }
                list.setModel(listModel);
                fr.close();
                ok = true;
            }
        } catch (Exception ioe) {
            if (logger.isDebugEnabled()) {
                logger.debug("DBLogin.loadList: " + ioe.toString()); //$NON-NLS-1$
            }
        }
        return ok;
    }

    /**
     * speichert Liste der Verbindung in Datei (INI_FILE_NAME)
     * @return true wenn erfolgreich
     */
    private boolean saveList() {
        boolean ok = false;
        try {
            final File iniFile = new File(Main.getFileNameForConnectionList());
            if (logger.isDebugEnabled()) {
                logger.debug("DBLoginDialog.saveList: in Datei: " + Main.getFileNameForConnectionList() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            final BufferedWriter bw = new BufferedWriter(new FileWriter(iniFile));
            for (int i = 0; i < listModel.size(); i++) {
                bw.write(((ConnectionDescription) listModel.getElementAt(i)).getParamStr());
                bw.newLine();
            }
            bw.flush();
            bw.close();
            ok = true;
            if (logger.isDebugEnabled()) {
                logger.debug(Messages.getString("DBLoginDialog.readydot")); //$NON-NLS-1$
            }
        } catch (Exception ioe) {
            logger.warn("DBLogin.saveList: " + ioe.toString() + "exception: " + ioe); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return ok;
    }

    /**
     * schliesst das Fenster
     */
    public void cancel() {
        returnCode = CANCEL_ACTION_PERFORMED;
        setVisible(false);
    }

    public ConnectionDescription getConnectionDescription() {
        return desc;
    }

    private void buttonLogin_actionPerformed() {
        final DatabaseType dt = ((DatabaseType) comboBoxType.getSelectedItem());
        desc = new ConnectionDescription();
        fillConnectionDescription(desc, dt);
        addToListModel(desc);
        saveList();
        returnCode = LOGIN_ACTION_PERFORMED;
        setVisible(false);
    }

    private void buttonSave_actionPerformed() {
        final DatabaseType dt = ((DatabaseType) comboBoxType.getSelectedItem());
        desc = (ConnectionDescription) list.getSelectedValue();
        if (desc != null) {
        	fillConnectionDescription(desc, dt);
            list.repaint();
        }
    }

    private void fillConnectionDescription(ConnectionDescription desc, DatabaseType dt) {
        desc.setDatabaseType(dt);
        desc.setURLElements(getURLElementsFromComponents());
        desc.setStorePasswdEnabled(cbStorePasswd.isSelected());
        desc.setProductive(getProductive());
        desc.setComment(getCommentText());
        desc.setDefaultFetchSize(getDefaultFetchSize());
        desc.setInitSQL(getInitSQL());
        desc.setPropertiesString(getPropertiesText());
        desc.createURL();
    }
    
    public void setInitialConnectionDescription(ConnectionDescription cd) {
        fillFormular(cd);
        list.clearSelection();
    }

    private void addToListModel(ConnectionDescription desc_loc) {
        // Liste von doppelten Einträgen befreien
        for (int i = 0; i < listModel.size(); i++) {
            if (((ConnectionDescription) listModel.elementAt(i)).equals(desc_loc)) {
                listModel.removeElementAt(i);
            }
        }
        listModel.insertElementAt(desc_loc, 0);
    }

    private void buttonDel_actionPerformed() {
        if (selectedListValues != null) {
            // rückwärts vorgehen, sonst stimmen die IDs nicht mehr !
            for (int i = selectedListValues.size() - 1; i >= 0; i--) {
                listModel.removeElement(selectedListValues.get(i));
            }
        }
    }
    
    private void buttonSort_actionPerformed() {
    	final List<ConnectionDescription> listItems = new ArrayList<ConnectionDescription>();
    	for (int i = 0, n = listModel.getSize(); i < n; i++) {
    		ConnectionDescription listItem = (ConnectionDescription) listModel.elementAt(i);
    		if (listItem != null) {
    			listItems.add(listItem);
    		}
    	}
    	Collections.sort(listItems, new Comparator<Object>() {

			public int compare(Object o1, Object o2) {
				String s1 = o1.toString().toLowerCase();
				String s2 = o2.toString().toLowerCase();
				return s1.compareTo(s2);
			}
    		
		});
    	listModel.clear();
    	for (ConnectionDescription listItem : listItems) {
    		listModel.addElement(listItem);
    	}
    }

    private void buttonClose_actionPerformed() {
        desc = null;
        saveList();
        cancel();
    }

    public int getReturnAction() {
        return returnCode;
    }

    public void disposeDialog() {
        dispose();
    }

    private void showSelectedConnectionDescPropertiesInTextWindow() {
        if (selectedDesc != null) {
            new TextViewer(this, Messages.getString("DBLoginDialog.propertiesforconnection"), selectedDesc.getDatabasePropertyString()); //$NON-NLS-1$
        }
    }

	public void valueChanged(ListSelectionEvent e) {
        selectedListValues = list.getSelectedValuesList();
        if (selectedListValues != null) {
            if (selectedListValues.size() == 1) {
                selectedDesc = (ConnectionDescription) list.getSelectedValue();
                fillFormular(selectedDesc);
            }
        }
    }

    private void fillFormular(ConnectionDescription descNew) {
        if (descNew != null) {
            desc = descNew;
            inChooseFromListTransaction = true;
            comboBoxType.setSelectedItem(desc.getDatabaseType());
            if (desc != null) { // muss abgefangen werden, da sonst Probleme beim löschen !
                createURLComponents(desc);
                textFieldComment.setText(desc.getComment());
                textFieldProperties.setText(desc.getPropertiesString());
                int value = desc.getDefaultFetchSize();
                if (value != -1) {
                    jTextFieldFetchSize.setText(String.valueOf(value));
                } else {
                    jTextFieldFetchSize.setText(null);
                }
                if (desc.getInitSQL() != null) {
                    StringReplacer sr = new StringReplacer(desc.getInitSQL() );
                    sr.replace(";", ";\n");
                    jTextAreaInitSQL.setText(sr.getResultText().trim());
                } else {
                    jTextAreaInitSQL.setText(null);
                }
                checkBoxProductive.setSelected(desc.isProductive());
            }
            inChooseFromListTransaction = false;
        }
    }

    private void list_keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            if (list.getSelectedIndex() != -1) {
                listModel.removeElement(list.getSelectedValue());
            }
        }
    }

    private class ListMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.isPopupTrigger()) {
            } else {
                if (me.getClickCount() == 2) {
                    fireDoubleClickPerformed(me);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger()) {
                createListContextMenu(me.getX(), me.getY());
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger()) {
                createListContextMenu(me.getX(), me.getY());
            }
        }

        protected void fireDoubleClickPerformed(MouseEvent me) {
            buttonLogin_actionPerformed();
        }
    }

    private void createListContextMenu(int mouseXPos, int mouseYPos) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem mi = new JMenuItem(Messages.getString("DBLoginDialog.showpropertiesinwindow")); //$NON-NLS-1$
        mi.setActionCommand("showPropertiesInTextWindow"); //$NON-NLS-1$
        mi.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
	            showSelectedConnectionDescPropertiesInTextWindow();
			}
		});
        popup.add(mi);

        // Menü anzeigen
        // sicherstellen, dass es noch vollstÄndig am Bildschirm zu sehen ist
        final Dimension popupSize = popup.getPreferredSize();
        // Mausposition ist position bezogen auf Scrollpane!
        // absolute Mausposition errechnen
        final int yLoc = mouseYPos; // bei y ist es immer sinnvoll hier nach unten zu orientieren
        final int absMouseXPos = mouseXPos - jScrollPaneConnectionList.getHorizontalScrollBar().getValue() + getLocation().x;
        int xLoc;
        if (absMouseXPos + popupSize.width > getToolkit().getScreenSize().width) {
            xLoc = mouseXPos - popupSize.width;
        } else {
            xLoc = mouseXPos;
        }
        popup.show(list, xLoc, yLoc);
    }

    private void comboBoxType_itemStateChanged() {
        if (!inChooseFromListTransaction) {
            list.clearSelection();
            textFieldComment.setText(null);
            textFieldProperties.setText(null);
            jTextAreaInitSQL.setText(null);
            createURLComponents((DatabaseType) comboBoxType.getSelectedItem());
        }
    }

    private void createURLComponents(DatabaseType dt) {
        if (dt != null) {
            int rows = dt.getURLElements().size();
            final int remainder = rows % 2;
            rows = (rows >> 1) + remainder;
            URLElement element;
            URLComponent comp;
            urlComponents = new ArrayList<URLComponent>();
            JPanel panel = getPanelElements();
            panel.removeAll();
            panel.setLayout(new GridLayout(rows, 2, 0, 0));
            for (int i = 0; i < dt.getURLElements().size(); i++) {
                element = dt.getURLElements().get(i);
                comp = new URLComponent(element);
                urlComponents.add(comp);
                if (element.getName().equalsIgnoreCase(URLElement.USER_NAME)) {
                    panel.add(comp, 0);
                } else if (element.getName().equalsIgnoreCase(URLElement.PASSWORD_NAME)) {
                    panel.add(comp, 1);
                } else {
                    panel.add(comp);
                }
            }
            checkBoxProductive.setSelected(false);
            jScrollPaneInputFields.validate();
            jScrollPaneInputFields.repaint();
        }
    }

    private void createURLComponents(ConnectionDescription desc_loc) {
        if (desc_loc != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("DBLoginDialog.createURLComponents from Description:" + desc_loc); //$NON-NLS-1$
            }
            int rows = desc_loc.getURLElements().size();
            final int remainder = rows % 2;
            rows = (rows >> 1) + remainder;
            URLElement element;
            URLComponent comp;
            urlComponents = new ArrayList<URLComponent>();
            JPanel panel = getPanelElements();
            panel.removeAll();
            panel.setLayout(new GridLayout(rows, 2, 0, 0));
            for (int i = 0; i < desc_loc.getURLElements().size(); i++) {
                element = desc_loc.getURLElements().get(i);
                comp = new URLComponent(element);
                urlComponents.add(comp);
                if (element.getName().equalsIgnoreCase(URLElement.USER_NAME)) {
                    panel.add(comp, 0);
                } else if (element.getName().equalsIgnoreCase(URLElement.PASSWORD_NAME)) {
                    panel.add(comp, 1);
                } else {
                    panel.add(comp);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("add URLComponent:" + comp); //$NON-NLS-1$
                }
            }
            jScrollPaneInputFields.validate();
            jScrollPaneInputFields.repaint();
        }
    }

    private ArrayList<URLElement> getURLElementsFromComponents() {
        final ArrayList<URLElement> urlElements = new ArrayList<URLElement>();
        URLComponent urlComponent;
        for (int i = 0; i < urlComponents.size(); i++) {
            urlComponent = (URLComponent) urlComponents.get(i);
            urlElements.add(new URLElement(urlComponent.getLabelText(), urlComponent.getValue()));
        }
        return urlElements;
    }

    public String getCommentText() {
        if ((textFieldComment.getText() == null) || (textFieldComment.getText().trim().length() == 0)) {
            return ""; //$NON-NLS-1$
        } else {
            String comment = textFieldComment.getText();
            if (comment.indexOf('|') != -1) {
                return comment.replace('|', ' ');
            } else {
                return comment;
            }
        }
    }

    private int getDefaultFetchSize() {
        if ((jTextFieldFetchSize.getText() == null) || (jTextFieldFetchSize.getText().trim().length() == 0)) {
            return -1; //$NON-NLS-1$
        } else {
            return Integer.parseInt(jTextFieldFetchSize.getText());
        }
    }

    private String getInitSQL() {
        if (jTextAreaInitSQL.getText() == null || jTextAreaInitSQL.getText().trim().length() == 0) {
            return ""; //$NON-NLS-1$
        } else {
            StringReplacer sr = new StringReplacer(jTextAreaInitSQL.getText());
            sr.replace("\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
            return sr.getResultText();
        }
    }
    
    private boolean getProductive() {
    	return checkBoxProductive.isSelected();
    }

    private String getPropertiesText() {
        if ((textFieldProperties.getText() == null) || (textFieldProperties.getText().trim().length() == 0)) {
            return "";
        } else {
            return textFieldProperties.getText();
        }
    }

    /**
     * Klasse kapselt einen zu editierenden Bestandteil einer URL
     * Der Bestandteil wird dargestellt mir einem Label welches den Namen anzeigt
     * und einem Textfeld, welches den Wert entgegennimmt
     */
    private final class URLComponent extends JPanel {

        private static final long serialVersionUID = 1L;
        private JLabel label;
        private JTextField textField;
        private JPasswordField passwdField;
        private boolean isPasswordField = false;

        URLComponent(URLElement element) {
            label = new JLabel();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            label.setFont(new Font("Dialog", Font.PLAIN, 11)); //$NON-NLS-1$
            label.setForeground(Color.black);
            label.setText(element.getName());
            label.setToolTipText(element.getName());
            label.setHorizontalAlignment(JLabel.RIGHT);
            label.setPreferredSize(new Dimension(90, 30));
            label.setMaximumSize(new Dimension(90, 30));
            label.setMinimumSize(new Dimension(90, 30));
            add(label);
            if (element.getName().equalsIgnoreCase(URLElement.PASSWORD_NAME)) {
                passwdField = new JPasswordField();
                passwdField.setBackground(Main.info);
                passwdField.setPreferredSize(new Dimension(200, 30));
                passwdField.setMaximumSize(new Dimension(200, 30));
                passwdField.setMinimumSize(new Dimension(200, 30));
                passwdField.setText(element.getValue());
                add(passwdField);
                isPasswordField = true;
            } else {
                textField = new JTextField();
                textField.setBackground(Main.info);
                textField.setText(element.getValue());
                textField.setPreferredSize(new Dimension(200, 30));
                textField.setMaximumSize(new Dimension(200, 30));
                textField.setMinimumSize(new Dimension(200, 30));
                add(textField);
            }
        }

        String getLabelText() {
            return label.getText();
        }

        String getValue() {
            if (isPasswordField) {
                return String.valueOf(passwdField.getPassword());
            } else {
                return textField.getText().trim();
            }
        }

        @Override
        public String toString() {
            return getClass().getName() + " label=" + getLabelText() + " value=" + getValue(); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void checkBoxShowComment_itemStateChanged() {
        ConnectionDescription.setShowCommentAsDefault(checkBoxShowComment.isSelected());
        list.repaint();
        if (checkBoxShowComment.isSelected()) {
            Main.setUserProperty("LOGIN_SHOW_COMMENT", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            Main.setUserProperty("LOGIN_SHOW_COMMENT", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void checkBoxExtendsUrlWidthProperties_itemStateChanged() {
        ConnectionDescription.setExtendsUrlWithProperties(checkBoxExtendsUrlWidthProperties.isSelected());
        list.repaint();
        if (checkBoxExtendsUrlWidthProperties.isSelected()) {
            Main.setUserProperty("LOGIN_PROPERTIES_IN_URL", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            Main.setUserProperty("LOGIN_PROPERTIES_IN_URL", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void checkBoxRunInitSQL_itemStateChanged() {
        ConnectionDescription.setRunInitialSQLOnConnect(jCheckBoxRunInitSQL.isSelected());
        if (jCheckBoxRunInitSQL.isSelected()) {
            Main.setUserProperty("RUN_INIT_SQL", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            Main.setUserProperty("RUN_INIT_SQL", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        }
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
            // must be initialized before createPanelConnectionDetails !
            jContentPane.add(createPanelListConnections(), BorderLayout.CENTER);
            jContentPane.add(createPanelConnectionDetails(), BorderLayout.NORTH);
            jContentPane.add(createPanelButtons(), BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    private JPanel getPanelElements() {
        if (panelElements == null) {
            panelElements = new JPanel();
        }
        return panelElements;
    }

    private JPanel createPanelAdditionConnectionDetails() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        p.setMinimumSize(new Dimension(200, 75));
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.gridy = 0;
            gbc.ipadx = 0;
            gbc.ipady = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridx = 0;
            JLabel label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setText("Properties"); //$NON-NLS-1$
            p.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(2, 0, 2, 0);
            textFieldProperties = new JTextField();
            textFieldProperties.setBackground(Main.info);
            textFieldProperties.setToolTipText("key1=value1;key2=value2..."); //$NON-NLS-1$
            p.add(textFieldProperties, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.gridy = 0;
            gbc.ipadx = 0;
            gbc.ipady = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 2;
            checkBoxExtendsUrlWidthProperties = new JCheckBox();
            checkBoxExtendsUrlWidthProperties.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    checkBoxExtendsUrlWidthProperties_itemStateChanged();
                }
            });
            checkBoxExtendsUrlWidthProperties.setText(Messages.getString("DBLoginDialog.embeddinurl")); //$NON-NLS-1$
            checkBoxExtendsUrlWidthProperties.setSelected((Main.getUserProperty("LOGIN_PROPERTIES_IN_URL", "false")).equals("true")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            p.add(checkBoxExtendsUrlWidthProperties, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.gridy = 2;
            gbc.ipadx = 0;
            gbc.ipady = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.gridx = 0;
            JLabel label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setText(Messages.getString("DBLoginDialog.comment")); //$NON-NLS-1$
            p.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(2, 0, 2, 0);
            textFieldComment = new JTextField();
            textFieldComment.setBackground(Main.info);
            textFieldComment.setToolTipText(Messages.getString("DBLoginDialog.tooltipcleartextforconnection")); //$NON-NLS-1$
            p.add(textFieldComment, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.gridy = 2;
            gbc.ipadx = 0;
            gbc.ipady = -4;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 2;
            checkBoxShowComment = new JCheckBox();
            checkBoxShowComment.setText(Messages.getString("DBLoginDialog.showinlist")); //$NON-NLS-1$
            checkBoxShowComment.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
                        checkBoxShowComment_itemStateChanged();
                    }
                }
            });
            checkBoxShowComment.setSelected((Main.getUserProperty("LOGIN_SHOW_COMMENT", "false")).equals("true")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            p.add(checkBoxShowComment, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.gridy = 3;
            JLabel label = new JLabel();
            label.setText("initial SQL"); //$NON-NLS-1$
            p.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridy = 3;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.gridx = 1;
            p.add(getJScrollPaneInitSQL(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.gridy = 3;
            p.add(getJCheckBoxRunInitSQL(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            JLabel label = new JLabel();
            label.setText("fetchsize");
            p.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(2, 0, 0, 0);
            gbc.gridx = 1;
            p.add(getJTextFieldFetchSize(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 1;
            gbc.insets = new Insets(2, 5, 2, 5);
            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.WEST;
            p.add(getCheckBoxProductive(), gbc);
        }
        return p;
    }

    private JPanel createPanelConnectionType() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        p.setPreferredSize(new java.awt.Dimension(503, 35));
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(10, 5, 10, 0);
            gbc.gridx = 0;
            JLabel label = new JLabel();
            label.setForeground(Color.black);
            label.setText(Messages.getString("DBLoginDialog.type")); //$NON-NLS-1$
            label.setHorizontalTextPosition(javax.swing.SwingConstants.TRAILING);
            label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            p.add(label, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 5, 10, 5);
            p.add(getComboBoxTypes(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 0, 10, 5);
            gbc.gridx = 2;
            p.add(getJButtonNew(), gbc);
        }
        return p;
    }
    
    private JComboBox<DatabaseType> getComboBoxTypes() {
    	if (comboBoxType == null) {
            comboBoxType = new JComboBox<DatabaseType>();
            loadComboBoxType();
            comboBoxType.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
                        comboBoxType_itemStateChanged();
                    }
                }
            });
    	}
    	return comboBoxType;
    }
    
    private JButton getJButtonNew() {
    	if (buttonNew == null) {
            buttonNew = new JButton();
            buttonNew.setText(Messages.getString("DBLoginDialog.new"));
            buttonNew.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
		            comboBoxType_itemStateChanged();
				}
			});
    	}
    	return buttonNew;
    }

    private JPanel createPanelConnectionDetails() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        p.setPreferredSize(new Dimension(650, 300));
        p.setMinimumSize(p.getPreferredSize());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.ipadx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridy = 0;
            p.add(createPanelConnectionType(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridy = 1;
            gbc.ipadx = 0;
            gbc.ipady = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 0;
            jScrollPaneInputFields = new JScrollPane();
            jScrollPaneInputFields.setPreferredSize(new Dimension(200, 100));
            jScrollPaneInputFields.setViewportView(getPanelElements());
            p.add(jScrollPaneInputFields, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.ipadx = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0D;
            gbc.gridy = 2;
            p.add(createPanelAdditionConnectionDetails(), gbc);
        }
        return p;
    }

    private JList<ConnectionDescription> getListConnections() {
        if (list == null) {
            listModel = new DefaultListModel<ConnectionDescription>();
            list = new JList<ConnectionDescription>(listModel);
            list.setFont(new Font("Dialog", Font.PLAIN, 12));
            list.addListSelectionListener(this);
            list.setBackground(Main.info);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.addMouseListener(new ListMouseListener());
            list.addKeyListener(new java.awt.event.KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    list_keyPressed(e);
                }
            });
        }
        return list;
    }

    private JPanel createPanelListConnections() {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.setPreferredSize(new Dimension(650, 200));
        p.setMinimumSize(new Dimension(650, 50));
        jScrollPaneConnectionList = new JScrollPane();
        jScrollPaneConnectionList.setViewportView(getListConnections());
        p.add(jScrollPaneConnectionList, null);
        return p;
    }

    private JPanel createPanelButtons() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(5, 5, 5, 5);
            p.add(getJButtonLogin(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.insets = new Insets(5, 5, 5, 5);
            cbStorePasswd = new JCheckBox();
            cbStorePasswd.setText(Messages.getString("DBLoginDialog.savepasswort")); //$NON-NLS-1$
            cbStorePasswd.setSelected(true);
            p.add(cbStorePasswd, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.insets = new Insets(5, 5, 5, 5);
            p.add(getJButtonSave(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 3;
            gbc.gridy = 0;
            gbc.insets = new Insets(5, 5, 5, 5);
            p.add(getJButtonDel(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridy = 0;
            gbc.gridx = 4;
            p.add(getJButtonClose(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridy = 0;
            gbc.gridx = 5;
            p.add(getJButtonSort(), gbc);
        }
        return p;
    }
    
    private JButton getJButtonLogin() {
    	if (buttonLogin == null) {
            buttonLogin = new JButton();
            buttonLogin.setText(Messages.getString("DBLoginDialog.login")); //$NON-NLS-1$
            buttonLogin.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					buttonLogin_actionPerformed();
				}
			});
    	}
    	return buttonLogin;
    }
    
    private JButton getJButtonSave() {
    	if (buttonSave == null) {
            buttonSave = new JButton();
            buttonSave.setText(Messages.getString("DBLoginDialog.apply")); //$NON-NLS-1$
            buttonSave.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					buttonSave_actionPerformed();
				}
			});
    	}
    	return buttonSave;
    }
    
    private JButton getJButtonDel() {
    	if (buttonDel == null) {
            buttonDel = new JButton();
            buttonDel.setText(Messages.getString("DBLoginDialog.delete")); //$NON-NLS-1$
            buttonDel.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					buttonDel_actionPerformed();
				}
			});
    	}
    	return buttonDel;
    }
    
    private JButton getJButtonClose() {
    	if (buttonClose == null) {
            buttonClose = new JButton();
            buttonClose.setText(Messages.getString("DBLoginDialog.close")); //$NON-NLS-1$
            buttonClose.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					buttonClose_actionPerformed();
				}
			});
    	}
    	return buttonClose;
    }

    private JButton getJButtonSort() {
    	if (buttonSort == null) {
    		buttonSort = new JButton();
            buttonSort = new JButton();
            buttonSort.setText(Messages.getString("DBLoginDialog.sort")); //$NON-NLS-1$
            buttonSort.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					buttonSort_actionPerformed();
				}
			});
    	}
    	return buttonSort;
    }
    
    /**
     * This method initializes jScrollPaneInitSQL	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getJScrollPaneInitSQL() {
        if (jScrollPaneInitSQL == null) {
            jScrollPaneInitSQL = new JScrollPane();
            jScrollPaneInitSQL.setPreferredSize(new Dimension(10, 50));
            jScrollPaneInitSQL.setViewportView(getJTextAreaInitSQL());
        }
        return jScrollPaneInitSQL;
    }

    /**
     * This method initializes jTextAreaInitSQL	
     * 	
     * @return javax.swing.JTextArea	
     */
    private JTextArea getJTextAreaInitSQL() {
        if (jTextAreaInitSQL == null) {
            jTextAreaInitSQL = new JTextArea();
        }
        return jTextAreaInitSQL;
    }

    /**
     * This method initializes jCheckBoxRunInitSQL	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getJCheckBoxRunInitSQL() {
        if (jCheckBoxRunInitSQL == null) {
            jCheckBoxRunInitSQL = new JCheckBox();
            jCheckBoxRunInitSQL.setText(Messages.getString("DBLoginDialog.execute")); //$NON-NLS-1$
            jCheckBoxRunInitSQL.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
                        checkBoxRunInitSQL_itemStateChanged();
                    }
                }
            });
            jCheckBoxRunInitSQL.setSelected((Main.getUserProperty("RUN_INIT_SQL", "false")).equals("true")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        return jCheckBoxRunInitSQL;
    }
    
    private JCheckBox getCheckBoxProductive() {
    	if (checkBoxProductive == null) {
    		checkBoxProductive = new JCheckBox();
    		checkBoxProductive.setText(Messages.getString("DBLoginDialog.productive"));
    	}
    	return checkBoxProductive;
    }

    /**
     * This method initializes jTextFieldFetchSize
     *
     * @return javax.swing.JTextField
     */
    private JTextField getJTextFieldFetchSize() {
        if (jTextFieldFetchSize == null) {
            jTextFieldFetchSize = new JTextField();
            jTextFieldFetchSize.setBackground(new Color(250, 250, 220));
        }
        return jTextFieldFetchSize;
    }
    
}
