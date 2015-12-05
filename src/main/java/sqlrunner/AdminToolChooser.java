package sqlrunner;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sqlrunner.resources.ApplicationIcons;
import sqlrunner.swinghelper.WindowHelper;
import dbtools.SQLParser;

public class AdminToolChooser extends JFrame implements ActionListener, ListSelectionListener {

    private static final long serialVersionUID = 1L;
    private final JPanel panelList=new JPanel();
    private final JPanel panelControl=new JPanel();
    private final JScrollPane jScrollPane1=new JScrollPane();
    private final DefaultListModel<AdminStatement> model = new DefaultListModel<AdminStatement>();
    private final JList<AdminStatement> list = new JList<AdminStatement>(model);
    private final JButton buttonRun=new JButton();
    private final JButton buttonEdit=new JButton();
    private transient AdminStatement as;
    private transient AdminSQLConfigDialog ascd;
    private MainFrame mainFrame;

    public AdminToolChooser(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            jbInit();
            pack();
            WindowHelper.locateWindowAtMiddle(mainFrame, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if(!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    @SuppressWarnings("unchecked")
	private void jbInit() throws Exception {
        setTitle(Messages.getString("AdminToolChooser.title")); //$NON-NLS-1$
        (getRootPane()).setDefaultButton(buttonRun);
        panelList.setPreferredSize(new Dimension(400, 350));
        panelList.setLayout(new BorderLayout());
        buttonRun.setMnemonic('A');
        buttonRun.setText(Messages.getString("AdminToolChooser.buttonRun")); //$NON-NLS-1$
        buttonRun.addActionListener(this);
        buttonEdit.setText(Messages.getString("AdminToolChooser.copytoscriptwindow")); //$NON-NLS-1$
        buttonEdit.addActionListener(this);
        (this.getContentPane()).add(panelList, BorderLayout.CENTER);
        (this.getContentPane()).add(panelControl, BorderLayout.SOUTH);
        panelList.add(jScrollPane1, BorderLayout.CENTER);
        (jScrollPane1.getViewport()).add(list, null);
        panelControl.add(buttonRun, null);
        panelControl.add(buttonEdit, null);
        list.setFont(new Font("Dialog", Font.PLAIN, 12)); //$NON-NLS-1$
        list.addListSelectionListener(this);
        list.addMouseListener(new ListMouseListener());
        list.setEnabled(true);
        list.setBackground(Main.info);
        list.setCellRenderer(new ATListCellRenderer());
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            setVisible(false);
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent winEvent) {
        switch (winEvent.getID()){
            case WindowEvent.WINDOW_CLOSING: {
                setVisible(false);
                break;
            }
            default:
                super.processWindowEvent(winEvent);}
    }

    public void fillList() {
        AdminStatement astat;
        clearList();
        final int dbtype=(((mainFrame.getDatabase().getDatabaseSession()).getConnectionDescription()).getDatabaseType()).getAdminOptionID();
        for (int i = 0; i < AdminStatement.adminSQLs.size(); i++) {
            astat = (AdminStatement) AdminStatement.adminSQLs.elementAt(i);
            if ((astat.getDbType() == dbtype) || (astat.getDbType() == 0)) {
                model.addElement(astat);
            }
        }
    }

    public void clearList() {
        model.removeAllElements();
        as = null;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonRun) {
            buttonRun_actionPerformed();
        } else if (e.getSource() == buttonEdit) {
            if (as != null) {
                mainFrame.toFront();
                mainFrame.setScriptText(as.getSQL());
            }
        }
    }

    private void buttonRun_actionPerformed() {
        if (as != null) {
            if (as.getCommand() == 'A') {
                // kompletten Text dem Parser übergeben
                final SQLParser parser=new SQLParser(as.getSQL(),true);
                mainFrame.toFront();
                mainFrame.getDatabase().executeScript(parser, false);
            } else if (as.getCommand() == 'E') {
                ascd = new AdminSQLConfigDialog(this, as.getComment(), as.getSQL());
                if (ascd.getReturnCode() == AdminSQLConfigDialog.OK) {
                    final SQLParser parser=new SQLParser(ascd.getSQL(),true);
                    mainFrame.toFront();
                    mainFrame.getDatabase().executeScript(parser, false);
                }
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        as = list.getSelectedValue();
    }

    @SuppressWarnings("rawtypes")
	private static class ATListCellRenderer extends JLabel implements ListCellRenderer {

        private static final long serialVersionUID = 1L;

        ATListCellRenderer() {
            setOpaque(true);
        }

		@Override
		public Component getListCellRendererComponent(
				JList list,
				Object value, 
				int index, 
				boolean isSelected,
				boolean cellHasFocus) {
            final AdminStatement as_loc = (AdminStatement) value;
            setText(as_loc.getComment());
            if (as_loc.getCommand() == 'A') {
                setIcon(ApplicationIcons.ADMINRUN_PNG);
            } else if (as_loc.getCommand() == 'E') {
                setIcon(ApplicationIcons.ADMINEDIT_PNG);
            } else {
                setIcon(null);
            }
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setComponentOrientation(list.getComponentOrientation());
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            return this;
        }


    }

    private class ListMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent me) {
            if((!me.isControlDown()) && (me.getClickCount() == 2)) {
                fireDoubleClickPerformed(me);
            }
        }

        protected void fireDoubleClickPerformed(MouseEvent me) {
            buttonRun_actionPerformed();
        }

    }

}
