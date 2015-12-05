/*
 * created on 29.07.2005
 * created by lolling.jan
 */
package dbtools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class DatabaseSessionPoolInfoPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private JScrollPane jScrollPane = null;
    private JTable jTable = null;
    private SessionTableModel sessionTableModel = new SessionTableModel();
    private Timer refreshTimer;
    private JPanel poolThreadInfoPanel;
    /**
     * This is the default constructor
     */
    public DatabaseSessionPoolInfoPanel() {
        super();
        initialize();
    }

    public void startRefreshing() {
        if (refreshTimer == null) {
            refreshTimer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (sessionTableModel != null) {
                        sessionTableModel.refresh();
                        repaint();
                    }
                }
            });
        }
        if (refreshTimer.isRunning() == false) {
            refreshTimer.start();
        }
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        setLayout(new BorderLayout());
        add(getJScrollPane(), java.awt.BorderLayout.CENTER);
        add(getPoolThreadInfoPanel(), java.awt.BorderLayout.SOUTH);
    }
    
    private JPanel getPoolThreadInfoPanel() {
    	if (poolThreadInfoPanel == null) {
    		poolThreadInfoPanel = new JPanel();
    	}
    	return poolThreadInfoPanel;
    }

    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getJTable());
        }
        return jScrollPane;
    }

    /**
     * This method initializes jTable	
     * 	
     * @return javax.swing.JTable	
     */
    private JTable getJTable() {
        if (jTable == null) {
            jTable = new JTable();
            jTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            jTable.setEnabled(false);
            jTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
            jTable.setModel(sessionTableModel);
            jTable.setDefaultRenderer(String.class, new SessionTableRenderer());
        }
        return jTable;
    }
    
    final static class SessionTableRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            try {
                DatabaseSession session = (DatabaseSession) DatabaseSessionPool.getPool().get(row);
                if (session != null) {
                    comp.setForeground(Color.BLACK);
                    if (session.isReady() == false) {
                        if (session.isSuccessful()) {
                            comp.setBackground(Color.YELLOW);
                        } else {
                            comp.setBackground(Color.RED);
                        }
                    } else {
                        comp.setForeground(Color.BLACK);
                        comp.setBackground(Color.WHITE);
                    }
                } else {
                    comp.setBackground(Color.LIGHT_GRAY);
                }
            } catch (Exception e) {
            	// nothing
            }
            return comp;
        }
        
    }

    final static class SessionTableModel extends DefaultTableModel {

        private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            if (DatabaseSessionPool.getPool() != null) {
                return DatabaseSessionPool.getPool().size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 13;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0: return "ID";
                case 1: return "class";
                case 2: return "connected";
                case 3: return "last used";
                case 4: return "often used";
                case 5: return "idle time";
                case 6: return "ready";
                case 7: return "last error message";
                case 8: return "occured at";
                case 9: return "URL";
                case 10: return "schema";
                case 11: return "last action";
                case 12: return "alias";
                default: return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DatabaseSession session = (DatabaseSession) DatabaseSessionPool.getPool().get(rowIndex);
            switch (columnIndex) {
                case 0: return String.valueOf(session.getSessionID());
                case 1: return session.getClass().getName();
                case 2: return sdf.format(new Date(session.getConnectedTimestamp()));
                case 3: if (session.getLastOccupiedTimestamp() > 0) {
                            return sdf.format(new Date(session.getLastOccupiedTimestamp()));
                        } else {
                            return "";
                        }
                case 4: return String.valueOf(session.getCountUsage());
                case 5: return String.valueOf(session.getIdleTime());
                case 6: return String.valueOf(session.isReady());
                case 7: return session.getLastErrorMessage();
                case 8: return sdf.format(new Date(session.getLastErrorTimestamp()));
                case 9: return session.getUrl();
                case 10: return session.getUser();
                case 11: return session.getLastSQL();
                case 12: return session.getAliasName();
                default: return null;
            }
        }
        
        public void refresh() {
            fireTableDataChanged();
        }
        
    }

    public void stopRefreshing() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }

    
}
