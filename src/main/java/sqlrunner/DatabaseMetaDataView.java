package sqlrunner;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import sqlrunner.swinghelper.WindowHelper;

public class DatabaseMetaDataView extends JFrame implements TableModel, ActionListener {

	private static Logger logger = Logger.getLogger(DatabaseMetaData.class);
    private static final long          serialVersionUID = 1L;
    private final JPanel               jPanel1          = new JPanel();
    private final JPanel               jPanel2          = new JPanel();
    private final JButton              buttonClose      = new JButton();
    private final JScrollPane          jScrollPane1     = new JScrollPane();
    private final JTable               table            = new JTable();
    private transient DatabaseMetaData dbmd;
    private final Vector<Property>     model            = new Vector<Property>();
    private DatabaseMetaDataView       self;

    public DatabaseMetaDataView(DatabaseMetaData dbmd) {
        this.self = this;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            setDatabaseMetaData(dbmd);
        } catch (Exception e) {
        	logger.error("init gui failed:" + e.getMessage(), e);
        }
    }

    public void setDatabaseMetaData(DatabaseMetaData dbmd_loc) {
        this.dbmd = dbmd_loc;
        model.removeAllElements();
    	Thread t = new Thread() {
    		@Override
    		public void run() {
    	        try {
                    viewMetaData();
                    SwingUtilities.invokeLater(new Runnable() {
                    	@Override
                    	public void run() {
                            fireTableRowsUpdated(0, model.size());
                    	}
                    });
                } catch (Throwable e) {
                	logger.error("viewMetaData failed:" + e.getMessage(), e);
                }
    		}
    	};
    	t.start();
    }

    @Override
    public void setVisible(boolean visible) {
        if (!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    private void initComponents() throws Exception {
        getContentPane().setLayout(new BorderLayout());
        buttonClose.setText(Messages.getString("DatabaseMetaDataView.close")); //$NON-NLS-1$
        buttonClose.addActionListener(this);
        buttonClose.setActionCommand("close"); //$NON-NLS-1$
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        jPanel1.setLayout(new BorderLayout());
        jPanel1.setPreferredSize(new Dimension(400, 400));
        table.setBackground(Main.info);
        table.setToolTipText(null);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new TableMouseListener());
        table.registerKeyboardAction(
                this,
                "show", //$NON-NLS-1$
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_FOCUSED);
        getContentPane().add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        jScrollPane1.getViewport().add(table, null);
        getContentPane().add(jPanel2, BorderLayout.SOUTH);
        jPanel2.add(buttonClose, null);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("close")) { //$NON-NLS-1$
            dispose();
        } else if (e.getActionCommand().equals("show")) { //$NON-NLS-1$
            new TextViewer(self, getValueAt(
            		table.getSelectedRow(), 0).toString(), 
            		getValueAt(table.getSelectedRow(), 1).toString());
        }
    }

    private void viewMetaData() {
        String name;
        String value;
        final int iValue;
        name = "URL"; //$NON-NLS-1$
        try {
            try {
                value = dbmd.getURL();
                setTitle(Messages.getString("DatabaseMetaDataView.metainfofor") + dbmd.getURL()); //$NON-NLS-1$
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "user-name"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getUserName());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "database-product-name"; //$NON-NLS-1$
            try {
                value = dbmd.getDatabaseProductName();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "database-product-version"; //$NON-NLS-1$
            try {
                value = dbmd.getDatabaseProductVersion();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "database major version"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getDatabaseMajorVersion());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "database minor version"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getDatabaseMinorVersion());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "driver major version"; //$NON-NLS-1$
            value = String.valueOf(dbmd.getDriverMajorVersion());
            model.addElement(new Property(name, value));
            name = "driver minor version"; //$NON-NLS-1$
            value = String.valueOf(dbmd.getDriverMinorVersion());
            model.addElement(new Property(name, value));
            name = "driver name"; //$NON-NLS-1$
            try {
                value = dbmd.getDriverName();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "driver version"; //$NON-NLS-1$
            try {
                value = dbmd.getDriverVersion();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "all procedures are callable"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.allProceduresAreCallable());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "all tables are selectable"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.allTablesAreSelectable());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports transactions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsTransactions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports multiple transactions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsMultipleTransactions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports transaction isolation level=TRANSACTION_NONE"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsTransactionIsolationLevel(Connection.TRANSACTION_NONE));
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports transaction isolation level=TRANSACTION_READ_COMMITTED"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED));
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports transaction isolation level=TRANSACTION_READ_UNCOMMITTED"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED));
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports transaction isolation level=TRANSACTION_REPEATABLE_READ"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ));
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports transaction isolation level=TRANSACTION_SERIALIZABLE"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsTransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE));
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "default transaction isolation"; //$NON-NLS-1$
            try {
                iValue = dbmd.getDefaultTransactionIsolation();
                switch (iValue) {
                    case Connection.TRANSACTION_NONE:
                        value = "TRANSACTION_NONE"; //$NON-NLS-1$
                        break;
                    case Connection.TRANSACTION_READ_COMMITTED:
                        value = "TRANSACTION_READ_COMMITTED"; //$NON-NLS-1$
                        break;
                    case Connection.TRANSACTION_READ_UNCOMMITTED:
                        value = "TRANSACTION_READ_UNCOMMITTED"; //$NON-NLS-1$
                        break;
                    case Connection.TRANSACTION_REPEATABLE_READ:
                        value = "TRANSACTION_REPEATABLE_READ"; //$NON-NLS-1$
                        break;
                    case Connection.TRANSACTION_SERIALIZABLE:
                        value = "TRANSACTION_SERIALIZABLE"; //$NON-NLS-1$
                        break;
                }
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "data definition causes transaction commit"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.dataDefinitionCausesTransactionCommit());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "data definition ignored in transactions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.dataDefinitionIgnoredInTransactions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "does max row size include blobs"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.doesMaxRowSizeIncludeBlobs());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "catalog-separator"; //$NON-NLS-1$
            try {
                value = dbmd.getCatalogSeparator();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "catalog term"; //$NON-NLS-1$
            try {
                value = dbmd.getCatalogTerm();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            name = "schema term"; //$NON-NLS-1$
            try {
                value = dbmd.getSchemaTerm();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "extra name characters"; //$NON-NLS-1$
            try {
                value = dbmd.getExtraNameCharacters();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            name = "search string escape"; //$NON-NLS-1$
            try {
                value = dbmd.getSearchStringEscape();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "identifier quote string"; //$NON-NLS-1$
            try {
                value = dbmd.getIdentifierQuoteString();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max binary literal length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxBinaryLiteralLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max catalog name length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxCatalogNameLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max char literal length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxCharLiteralLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max column name length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxColumnNameLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max columns in 'group by'"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxColumnsInGroupBy());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max columns in index"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxColumnsInIndex());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max columns in 'order by'"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxColumnsInOrderBy());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max columns in 'select'"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxColumnsInSelect());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max columns in table"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxColumnsInTable());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max connections"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxConnections());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max cursor name length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxCursorNameLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max index length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxIndexLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max procedure name length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxProcedureNameLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max row size"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxRowSize());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max schema name length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxSchemaNameLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max statement length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxStatementLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max statements"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxStatements());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max table name length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxTableNameLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max tables in select"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxTablesInSelect());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "max user name length"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getMaxUserNameLength());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "numeric functions"; //$NON-NLS-1$
            try {
                value = dbmd.getNumericFunctions();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "procedure term"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getProcedureTerm());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "search string escape"; //$NON-NLS-1$
            try {
                value = dbmd.getSearchStringEscape();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "additional SQL keywords"; //$NON-NLS-1$
            try {
                value = dbmd.getSQLKeywords();
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "string functions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getStringFunctions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "system functions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getSystemFunctions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "time date functions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.getTimeDateFunctions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "is catalog at start"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.isCatalogAtStart());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "is read only"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.isReadOnly());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "null plus non null is null"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.nullPlusNonNullIsNull());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "nulls are sorted at end"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.nullsAreSortedAtEnd());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "nulls are sorted at start"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.nullsAreSortedAtStart());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "nulls are sorted high"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.nullsAreSortedHigh());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "nulls are sorted low"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.nullsAreSortedLow());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "stores lower case identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.storesLowerCaseIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "stores lower case quoted identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.storesLowerCaseQuotedIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "stores mixed case identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.storesMixedCaseIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "stores mixed case quoted identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.storesMixedCaseQuotedIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "stores mixed case quoted identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.storesMixedCaseQuotedIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "stores upper case identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.storesUpperCaseIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "stores upper case quoted identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.storesUpperCaseQuotedIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports alter table with add column"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsAlterTableWithAddColumn());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports alter table with drop column"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsAlterTableWithDropColumn());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports ANSI92 entry level SQL"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsANSI92EntryLevelSQL());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports ANSI92 full SQL"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsANSI92FullSQL());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports ANSI92 intermediate SQL"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsANSI92IntermediateSQL());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports batch updates"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsBatchUpdates());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports catalogs in data-manipulation"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsCatalogsInDataManipulation());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports catalogs in index-definitions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsCatalogsInIndexDefinitions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports catalogs in privilege-definitions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsCatalogsInPrivilegeDefinitions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports catalogs in procedure-calls"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsCatalogsInProcedureCalls());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports catalogs in table-definitions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsCatalogsInTableDefinitions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports schemas in data-manipulation"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSchemasInDataManipulation());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports schemas in privilege-definitions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSchemasInPrivilegeDefinitions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports schemas in index-definitions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSchemasInIndexDefinitions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports schemas in procedure-calls"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSchemasInProcedureCalls());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports schemas in table-definitions?\t"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSchemasInTableDefinitions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports Column aliasing"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsColumnAliasing());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports convert"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsConvert());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports core SQL grammar"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsCoreSQLGrammar());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports correlated subqueries"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsCorrelatedSubqueries());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports data-definition- and data-manipulation-transactions"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsDataDefinitionAndDataManipulationTransactions());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports data-manipulation-transactions only"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsDataManipulationTransactionsOnly());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports different table correlation-names"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsDifferentTableCorrelationNames());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports expressions in 'order by'"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsExpressionsInOrderBy());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports extended SQL grammar"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsExtendedSQLGrammar());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports full outer-joins"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsFullOuterJoins());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports 'group by'"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsGroupBy());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports 'group by' beyond select"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsGroupByBeyondSelect());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports 'group by' unrelated"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsGroupByUnrelated());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports integrity enhancement facility"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsIntegrityEnhancementFacility());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports like escape clause"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsLikeEscapeClause());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports limited outer-joins"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsLimitedOuterJoins());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports minimum SQL grammar"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsMinimumSQLGrammar());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports mixed case identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsMixedCaseIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports mixed case quoted identifiers"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsMixedCaseQuotedIdentifiers());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports multiple resultSets"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsMultipleResultSets());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports non nullable columns"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsNonNullableColumns());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports open cursors across commit"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsOpenCursorsAcrossCommit());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports open cursors across rollback"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsOpenCursorsAcrossRollback());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports open statements across commit"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsOpenStatementsAcrossCommit());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports open statements across rollback"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsOpenStatementsAcrossRollback());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports 'order by' unrelated"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsOrderByUnrelated());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports outer joins"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsOuterJoins());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports positioned delete"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsPositionedDelete());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports positioned update"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsPositionedUpdate());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports select for update"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSelectForUpdate());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports stored procedures"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsStoredProcedures());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports subqueries in comparisons"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSubqueriesInComparisons());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports subqueries in exists"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSubqueriesInExists());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports subqueries in ins"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSubqueriesInIns());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports subqueries in quantifieds?\t"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsSubqueriesInQuantifieds());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports table correlation-names"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsTableCorrelationNames());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports union"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsUnion());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "supports union all"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.supportsUnionAll());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "uses local file per table"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.usesLocalFilePerTable());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
            name = "uses local files"; //$NON-NLS-1$
            try {
                value = String.valueOf(dbmd.usesLocalFiles());
            } catch (Exception e) {
                value = e.getMessage();
                if ((value == null) || (value.length() == 0)) {
                    value = e.toString();
                }
            } catch (java.lang.AbstractMethodError ame) {
                value = "information not implemented"; //$NON-NLS-1$
            }
            model.addElement(new Property(name, value));
        } catch (NullPointerException npe) {
            JOptionPane.showMessageDialog(this, Messages.getString("DatabaseMetaDataView.errornull"), Messages.getString("DatabaseMetaDataView.readdata"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
        }
        table.setModel(this);
    }

    public int getRowCount() {
        return model.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return Messages.getString("DatabaseMetaDataView.name"); //$NON-NLS-1$
        } else {
            return Messages.getString("DatabaseMetaDataView.value"); //$NON-NLS-1$
        }
    }

    public Class<String> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else {
            return String.class;
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return model.elementAt(rowIndex).name;
        } else {
            return model.elementAt(rowIndex).value;
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    // bleibt unbenutzt
    }

    /** List of listeners */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Add a listener to the list that's notified each time a change
     * to the data model occurs.
     *
     * @param   l               the TableModelListener
     */
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * change to the data model occurs.
     *
     * @param   l               the TableModelListener
     */
    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    /**
     * Forward the given notification event to all TableModelListeners that registered
     * themselves as listeners for this table model.
     * Notify that TableModelEvent concerning only changes of rows !
     * If the table-structure has been changed you must fire TableColumnModelEvents !
     * @see #addTableModelListener
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableChanged(TableModelEvent e) {
        final Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener) listeners[i + 1]).tableChanged(e);
            }
        }
    }

    /**
     * Notify all listeners that rows in the (inclusive) range
     * [<I>firstRow</I>, <I>lastRow</I>] have been inserted.
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableRowsInserted(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(
                this,
                firstRow,
                lastRow,
                TableModelEvent.ALL_COLUMNS,
                TableModelEvent.INSERT));
    }

    public void fireTableRowsUpdated(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(
                this,
                firstRow,
                lastRow,
                TableModelEvent.ALL_COLUMNS,
                TableModelEvent.UPDATE));
    }

    class TableMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent me) {
            final Component comp = ((Component) me.getSource());
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent me) {
            final Component comp = ((Component) me.getSource());
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.getClickCount() == 2) {
                new TextViewer(self, (getValueAt(table.getSelectedRow(), 0)).toString(), (getValueAt(
                        table.getSelectedRow(),
                        1)).toString());
            }
        }

    }

    static private class Property {

        String name;
        String value;

        Property(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}
