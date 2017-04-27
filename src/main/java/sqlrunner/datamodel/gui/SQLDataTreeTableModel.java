package sqlrunner.datamodel.gui;

import java.awt.Cursor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import sqlrunner.datamodel.SQLCatalog;
import sqlrunner.datamodel.SQLConstraint;
import sqlrunner.datamodel.SQLDataModel;
import sqlrunner.datamodel.SQLField;
import sqlrunner.datamodel.SQLIndex;
import sqlrunner.datamodel.SQLObject;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLSequence;
import sqlrunner.datamodel.SQLTable;

public final class SQLDataTreeTableModel extends DefaultTreeModel
        implements
        TableModel,
        TreeSelectionListener,
        TreeWillExpandListener,
        ListSelectionListener { 

	private static final Logger     logger = Logger.getLogger(SQLDataTreeTableModel.class);
    private static final long       serialVersionUID    = 1L;
    private String                  errorMessage;
    private boolean                 hasSuccessfulLoaded = false;
    private List<SQLDataModel>      listModels = new ArrayList<SQLDataModel>();
    private transient SQLDataModel  currentSQLDataModel;
    private transient SQLSchema     currentSQLSchema = null;
    private transient SQLCatalog    currentSQLCatalog = null;
    private transient SQLSchema     nextSelectedSchema = null;
    private transient SQLTable      currentSQLTable = null;
    private transient SQLField      currentSQLField = null;
    private transient SQLProcedure  currentSQLProcedure = null;
    private transient SQLConstraint currentSQLConstraint = null;
    private transient SQLIndex      currentSQLIndex = null;
    private transient SQLSequence   currentSQLSequence = null;
    private Object                  currentUserObject;
    private DefaultMutableTreeNode  currentNode;
    private DefaultMutableTreeNode  currentFilterStartNode;
    private HashMap<SQLDataModel, Integer> counterMap = new HashMap<SQLDataModel, Integer>();
    private boolean buildTreeNodesRecursive = false;
    private boolean stopBeforeColumns = false;
    private String objectFilter = null;
    private List<SQLObject> currentSelectedSQLObjects = new ArrayList<SQLObject>();

    public SQLDataTreeTableModel() {
        super(new SQLObjectTreeNode(Messages.getString("SQLDataTreeModel.rootnode")));
    }
    
    private SQLObjectTreeNode getRootNode() {
    	return (SQLObjectTreeNode) getRoot();
    }

    public boolean addUniqueSQLDataModel(SQLDataModel sqlDataModel) {
        Integer counter = counterMap.get(sqlDataModel);
        if (counter == null || counter.intValue() == 0) {
            counter = Integer.valueOf(0);
            if (logger.isDebugEnabled()) {
                logger.debug("addSQLdataModel " + sqlDataModel);
            }
            listModels.add(sqlDataModel);
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(sqlDataModel);
            insertNodeInto(newNode, getRootNode(), getRootNode().getChildCount());
        }
        counter = Integer.valueOf(counter.intValue() + 1);
        counterMap.put(sqlDataModel, counter);
        return counter.intValue() == 1;
    }
    
    public void removeUniqueSQLDataModel(SQLDataModel sqlDataModel) {
        if (sqlDataModel != null) {
            Integer counter = counterMap.get(sqlDataModel);
            if (counter == null || counter.intValue() < 2) {
                if (logger.isDebugEnabled()) {
                    logger.debug("removeSQLdataModel " + sqlDataModel);
                }
                int index = listModels.indexOf(sqlDataModel);
                if (index != -1) {
                    listModels.remove(index);
                }
                MutableTreeNode node = findTreeNodeByUserObject(sqlDataModel);
                if (node != null) {
                    removeNodeFromParent(node);
                }
                if (currentSQLDataModel != null && currentSQLDataModel.equals(sqlDataModel)) {
                    currentSQLDataModel = null;
                }
                if (currentSQLTable != null && sqlDataModel.equals(currentSQLTable.getModel())) {
                    currentSQLTable = null;
                    currentSQLField = null;
                    fireTableChanged();
                }
            }
            if (counter != null) {
                counter = Integer.valueOf(counter.intValue() - 1);
                counterMap.put(sqlDataModel, counter);
            }
        }
    }
    
    public int getModelCount() {
    	return listModels.size();
    }

    public boolean isReady() {
        if (hasSuccessfulLoaded) {
            return true;
        } else {
            errorMessage = "NO metadata loaded";
            return false;
        }
    }
    
    public void filterCurrentNodeChildren() {
    	if (currentFilterStartNode != null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("filterCurrentNodeChildren currentFilterStartNode=" + currentFilterStartNode);
        	}
//            buildTreeNodesRecursive = true;
        	try {
        		buildNodes(currentFilterStartNode);
        	} finally {
//                buildTreeNodesRecursive = false;
        	}
    	}
    }

    public void refresh(SQLDataModel sqlDataModel, DefaultMutableTreeNode dataModelNode) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("refresh sqlDataModel=" + sqlDataModel);
    	}
    	sqlDataModel.loadCatalogs();
        buildTreeNodesRecursive = true;
        try {
        	buildNodes(dataModelNode);
        } finally {
            buildTreeNodesRecursive = false;
        }
    }

    public void refresh(SQLSchema schema, DefaultMutableTreeNode schemaNode, boolean stopBeforeColumns) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("refresh schema=" + schema);
    	}
        schema.loadTables();
        schema.loadProcedures();
        schema.loadSequences();
        // refresh the child nodes
        buildTreeNodesRecursive = true;
        this.stopBeforeColumns = stopBeforeColumns;
        try {
            buildNodes(schemaNode);
        } finally {
            buildTreeNodesRecursive = false;
            this.stopBeforeColumns = false;
        }
    }

    public void refresh(SQLCatalog catalog, DefaultMutableTreeNode node, boolean stopBeforeColumns) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("refresh catalog=" + catalog);
    	}
        catalog.loadSchemas();
        // refresh the child nodes
        buildTreeNodesRecursive = true;
        this.stopBeforeColumns = stopBeforeColumns;
        try {
            buildNodes(node);
        } finally {
            buildTreeNodesRecursive = false;
            this.stopBeforeColumns = false;
        }
    }

    public void refreshCurrentTable() {
    	if (currentSQLTable != null) {
    		buildTreeNodesRecursive = true;
    		try {
        		buildNodes(currentNode);
    		} finally {
        		buildTreeNodesRecursive = false;
    		}
            if (currentSQLTable.getFieldCount() > 0) {
                fireTableRowsDeleted(0, currentSQLTable.getFieldCount() - 1);
            }
            currentSQLTable.loadColumns();
            if (currentSQLTable.getFieldCount() > 0) {
                fireTableRowsInserted(0, currentSQLTable.getFieldCount() - 1);
            }
    	}
    }
    
    private DefaultMutableTreeNode findNode(DefaultMutableTreeNode parentNode, Object userObject) {
        DefaultMutableTreeNode child = null;
        if (userObject != null) {
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                child = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                if (userObject.equals(child.getUserObject())) {
                    break;
                } else {
                    child = null;
                }
            }
        }
        return child;
    }
    
    protected void buildNodesForDataModels() {
        setAsksAllowsChildren(true);
    	for (int i = 0; i < listModels.size(); i++) {
            SQLDataModel model = listModels.get(i);
    		DefaultMutableTreeNode node = findNode(getRootNode(), model);
            if (node == null) {
                node = new DefaultMutableTreeNode(model);
        		insertNodeInto(node, getRootNode(), getRootNode().getChildCount());
            }
    	}
    }
    
    public DefaultMutableTreeNode findTreeNodeByUserObject(Object userObject) {
    	return findTreeNodeByUserObject((DefaultMutableTreeNode) root, userObject);
    }

    private DefaultMutableTreeNode findTreeNodeByUserObject(DefaultMutableTreeNode node, Object userObject) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("findTreeNodeByUserObject: node=" + node + " userObject=" + userObject);
    	}
        if (node.getUserObject() != null && userObject.equals(node.getUserObject())) {
        	return node;
        } else {
        	for (int i = 0; i < node.getChildCount(); i++) {
        		DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
        		child = findTreeNodeByUserObject(child, userObject);
        		if (child != null) {
        			return child;
        		}
            }
        }
        return null;
    }
    
    public DefaultMutableTreeNode findAndCreateNode(Object userObject) {
    	ArrayList<Object> path = collectObjectPath(userObject);
    	DefaultMutableTreeNode node = findAndCreateNodeByPath(path);
    	return node;
    }
    
    /**
     * find the node (or create if necessary) for the userObjectPath
     * 
     */
    public DefaultMutableTreeNode findAndCreateNodeByPath(ArrayList<Object> userObjectPath) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("findAndCreateNodeByPath userObjectPath=" + userObjectPath);
    	}
    	DefaultMutableTreeNode node = null;
    	for (Object userObject : userObjectPath) {
    		node = findTreeNodeByUserObject(userObject);
    		if (node != null && node.getChildCount() == 0) {
    			buildNodes(node);
    		}
    	}
    	return node;
    }
    
    public String getLastErrorMessage() {
        return errorMessage;
    }
    
    public static abstract class Folder {
        
        private SQLSchema schema;
        private SQLTable table;
        
        public Folder(SQLSchema schema) {
            this.schema = schema;
        }
        
        public Folder(SQLTable table) {
            this.table = table;
        }
        
        public SQLSchema getSchema() {
            return schema;
        } 
        
        public SQLTable getTable() {
            return table;
        }
        
        @Override
        public boolean equals(Object o) {
        	if (getClass().isInstance(o)) {
        		Folder of = (Folder) o;
        		if (getTable() != null && getTable().equals(of.getTable())) {
        			return true;
        		} else if (getSchema() != null && getSchema().equals(of.getSchema())) {
        			return true;
        		} else {
        			return false;
        		}
        	} else {
        		return false;
        	}
        }

    }
    
    public static final class TableFolder extends Folder {
        
        public TableFolder(SQLSchema schema) {
            super(schema);
        }
        
        @Override
        public String toString() {
            return Messages.getString("SQLDataTreeModel.tableFolderName");
        }
                
    }
    
    public static final class ViewFolder extends Folder {
        
        public ViewFolder(SQLSchema schema) {
            super(schema);
        }
        
        @Override
        public String toString() {
            return Messages.getString("SQLDataTreeModel.viewFolderName");
        }
        
    }

    public static final class ProcedureFolder extends Folder {
        
        public ProcedureFolder(SQLSchema schema) {
            super(schema);
        }
        
        @Override
        public String toString() {
            return Messages.getString("SQLDataTreeModel.procedureFolderName");
        }
                
    }

    public static final class SequenceFolder extends Folder {
        
        public SequenceFolder(SQLSchema schema) {
            super(schema);
        }
        
        @Override
        public String toString() {
            return Messages.getString("SQLDataTreeModel.sequenceFolderName");
        }
                
    }

    public static final class TableConstraintsFolder extends Folder {
        
        public TableConstraintsFolder(SQLTable table) {
            super(table);
        }
        
        @Override
        public String toString() {
            return Messages.getString("SQLDataTreeModel.tableConstraintsFolderName");
        }
        
    }

    public static final class TableIndexesFolder extends Folder {
        
        public TableIndexesFolder(SQLTable table) {
            super(table);
        }
        
        @Override
        public String toString() {
            return Messages.getString("SQLDataTreeModel.tableIndexesFolderName");
        }
        
    }

    // -------------- Methods for interface TableModel -----------------------------------------

    @Override
	public int getRowCount() {
    	if (currentUserObject != null) {
            if (currentUserObject == currentSQLTable) {
                return currentSQLTable.getFieldCount();
            } else if (currentUserObject == currentSQLProcedure) {
                return currentSQLProcedure.getParameterCount();
            } else if (currentUserObject == currentSQLSequence) {
                return 4; // start, stop, increment, current
            } else if (currentUserObject == currentSQLConstraint) {
            	return currentSQLConstraint.getColumnCount();
            } else if (currentUserObject == currentSQLIndex) {
            	return currentSQLIndex.getCountFields();
            } else {
                return 0;
            }
    	} else {
    		return 0;
    	}
    }

    @Override
	public int getColumnCount() {
        return 5;
    }

    @Override
	public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Messages.getString("SQLDataTreeModel.146"); //$NON-NLS-1$
            case 1:
                return Messages.getString("SQLDataTreeModel.147"); //$NON-NLS-1$
            case 2:
                return Messages.getString("SQLDataTreeModel.148"); //$NON-NLS-1$
            case 3:
                return Messages.getString("SQLDataTreeModel.149"); //$NON-NLS-1$
            case 4:
                return Messages.getString("SQLDataTreeModel.150"); //$NON-NLS-1$
            default:
                return ""; //$NON-NLS-1$
        }
    }

    @Override
	public Class<String> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            default:
                return String.class;
        }
    }

    @Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     *
     * @param rowIndex -
     * @param columnIndex
     * @return
     */
    @Override
	public Object getValueAt(int rowIndex, int columnIndex) {
    	if (currentUserObject != null) {
            if (currentUserObject == currentSQLTable) {
                return currentSQLTable.getFieldAt(rowIndex);
            } else if (currentUserObject == currentSQLProcedure) {
                return currentSQLProcedure.getParameterAt(rowIndex);
            } else if (currentUserObject == currentSQLConstraint) {
            	if (columnIndex == 0) {
                	return currentSQLConstraint.getColumnAt(rowIndex);
            	} else {
            		return null;
            	}
            } else if (currentUserObject == currentSQLIndex) {
            	if (columnIndex == 0) {
                	return currentSQLIndex.getFieldAt(rowIndex);
            	} else {
            		return null;
            	}
            } else if (currentUserObject == currentSQLSequence) {
            	switch (rowIndex) {
            	case 0: {
            		if (columnIndex == 0) {
            			return "Start";
            		} else if (columnIndex == 1) {
            			return currentSQLSequence.getStartsWith();
                	} else {
                		return null;
                	}
            	}
            	case 1: {
            		if (columnIndex == 0) {
            			return "End";
            		} else if (columnIndex == 1) {
            			return currentSQLSequence.getEndsWith();
                	} else {
                		return null;
                	}
            	}
            	case 2: {
            		if (columnIndex == 0) {
            			return "Step";
            		} else if (columnIndex == 1) {
            			return currentSQLSequence.getStepWith();
                	} else {
                		return null;
                	}
            	}
            	case 3: {
            		if (columnIndex == 0) {
            			return "Current";
            		} else if (columnIndex == 1) {
            			if (currentSQLSequence.getCurrentValue() > currentSQLSequence.getStartsWith()) {
                			return currentSQLSequence.getCurrentValue();
            			} else {
                    		return null;
            			}
                	} else {
                		return null;
                	}
            	}
            	default: return null;
            	}
            } else {
                return null;
            }
    	} else {
    		return null;
    	}
    }

    @Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    // nicht zu tun
    }

    // --------------- Methoden und Variablen für das EventHandling im TableModel ----------------

    /**
     * Add a listener to the list that's notified each time a change
     * to the data model occurs.
     *
     * @param   l               the TableModelListener
     */
    @Override
	public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * change to the data model occurs.
     *
     * @param   l               the TableModelListener
     */
    @Override
	public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    /**
     * Forward the given notification event to all TableModelListeners that registered
     * themselves as listeners for this table model.
     * Notify that TableModelEvent concerning only changes of rows !
     * If the table-structure has been changed you must fire TableColumnModelEvents !
     * @param e - received TableModelEvent
     * @see #addTableModelListener
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableChanged(final TableModelEvent e) {
    	if (SwingUtilities.isEventDispatchThread()) {
    		doFireTableChanged(e);
    	} else {
    		Thread t = new Thread(new Runnable() {
    			@Override
				public void run() {
    				doFireTableChanged(e);
    			}
    		});
    		t.start();
    	}
    }

    private void doFireTableChanged(final TableModelEvent e) {
        final Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableModelListener.class) {
            	try {
            		((TableModelListener) listeners[i + 1]).tableChanged(e);
            	} catch (Exception ex) {
            		// ignore
            	}
            }
        }
    }
    
    /**
     * Notify all listeners that rows in the (inclusive) range
     * [<I>firstRow</I>, <I>lastRow</I>] have been inserted.
     * @param firstRow index der ersten der neu eingefügten Zeilen
     * @param lastRow index der letzten der neu eingefügten Zeilen
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

    /**
     * Notify all listeners that rows in the (inclusive) range
     * [<I>firstRow</I>, <I>lastRow</I>] have been updated.
     * @param firstRow index der ersten der geänderten Zeilen
     * @param lastRow index der letzten der geänderten Zeilen
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableRowsUpdated(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(
                this,
                firstRow,
                lastRow,
                TableModelEvent.ALL_COLUMNS,
                TableModelEvent.UPDATE));
    }

    /**
     * Notify all listeners that rows in the (inclusive) range
     * [<I>firstRow</I>, <I>lastRow</I>] have been deleted.
     * @param firstRow index der ersten der gelöschten Zeilen
     * @param lastRow index der letzten der gelöschten Zeilen
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableRowsDeleted(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(
                this,
                firstRow,
                lastRow,
                TableModelEvent.ALL_COLUMNS,
                TableModelEvent.DELETE));
    }
    
    public void fireTableChanged() {
        fireTableChanged(new TableModelEvent(this));
    }

    //  ---------------- Methods für Interface ListSelectionListener ------------------------------------

    /**
     * Methode für das Interface ListSelectionListener
     * @param lse - Event
     */
    @Override
	public void valueChanged(ListSelectionEvent lse) {
        final ListSelectionModel lsm = ((ListSelectionModel) lse.getSource());
        if (lsm.isSelectionEmpty()) {
            currentSQLField = null;
        } else {
            if (currentSQLTable != null) {
                currentSQLField = currentSQLTable.getFieldAt(lsm.getMinSelectionIndex());
            } else if (currentSQLProcedure != null) {
                currentSQLField = null;
            }
        }
    }

    // ---------------- Methods für Interface TreeSelectionListener ------------------------------------

    /**
     * Methode für das Interface TreeSelectionListener
     * @param tse - Event
     */
    @Override
	public void valueChanged(final TreeSelectionEvent tse) {
		final JTree tree = (JTree) tse.getSource();
    	TreePath[] selectedPaths = tree.getSelectionPaths();
    	currentSelectedSQLObjects = new ArrayList<SQLObject>();
        if (selectedPaths != null && selectedPaths.length > 0) {
        	boolean firstloop = true;
        	for (TreePath tp : selectedPaths) {
    			Object o = tp.getLastPathComponent();
        		if (firstloop) {
                	if (o instanceof SQLObjectTreeNode) {
                        currentNode = (SQLObjectTreeNode) o;
                	}
                	firstloop = false;
        		}
        		if (o instanceof SQLObjectTreeNode) {
        			SQLObjectTreeNode node = (SQLObjectTreeNode) o;
        			Object userObject = node.getUserObject();
        			if (userObject instanceof SQLObject) {
            			currentSelectedSQLObjects.add((SQLObject) userObject);
        			}
        		}
        		if (logger.isDebugEnabled()) {
        			logger.debug(currentSelectedSQLObjects.size() + " SQLObjects selected");
        		}
        	}
        	if (currentNode == null) {
        		return;
        	}
        	DefaultMutableTreeNode nextSelectedNode = null;
            Object nextSelectedUserObject = null;
            if (selectedPaths.length > 1) {
            	nextSelectedNode = (DefaultMutableTreeNode) selectedPaths[1].getLastPathComponent();
            }
        	nextSelectedSchema = null;
            if (nextSelectedNode != null) {
                nextSelectedUserObject = nextSelectedNode.getUserObject();
            }
            currentUserObject = currentNode.getUserObject();
            if (currentUserObject instanceof SQLTable) {
                if (currentSQLTable != null && currentSQLTable.isFieldsLoaded()) {
                	SwingUtilities.invokeLater(new Runnable() {
                		@Override
						public void run() {
                            fireTableRowsDeleted(0, currentSQLTable.getFieldCount() - 1);
                		}
                	});
                }
                currentSQLConstraint = null;
                currentSQLProcedure = null;
                currentSQLSequence = null;
                currentSQLTable = (SQLTable) currentUserObject;
                currentSQLSchema = currentSQLTable.getSchema();
                currentSQLCatalog = currentSQLSchema.getCatalog();
                currentSQLDataModel = currentSQLTable.getModel();
    			final SQLTable table = currentSQLTable;
    			if (table.isFieldsLoaded() == false) {
    				if (table.isLoadingColumns() == false) {
    					
                    	new Thread() {
                    		@Override
                    		public void run() {
                    			if (table != null) {
                                	SwingUtilities.invokeLater(new Runnable() {
                                		@Override
                                		public void run() {
                                			tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                		}
                                	});
                    				table.loadColumns();
                                    if (table.getFieldCount() > 0) {
                                    	SwingUtilities.invokeLater(new Runnable() {
                                    		@Override
                                    		public void run() {
                                            	fireTableRowsInserted(0, table.getFieldCount() - 1);
                                    		}
                                    	});
                                    }
                                	SwingUtilities.invokeLater(new Runnable() {
                                		@Override
                                		public void run() {
                        	    			tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                		}
                                	});
                    			}
                    		}
                    	}.start();
    				}
    			} else {
                    if (currentSQLTable.getFieldCount() > 0) {
                    	
                    	SwingUtilities.invokeLater(new Runnable() {
                    		@Override
                    		public void run() {
                            	fireTableRowsInserted(0, currentSQLTable.getFieldCount() - 1);
                    		}
                    	});
                    }
    			}
            } else if (currentUserObject instanceof SQLConstraint) {
                currentSQLConstraint = (SQLConstraint) currentUserObject;
                currentSQLTable = currentSQLConstraint.getTable();
                currentSQLSchema = currentSQLTable.getSchema();
                currentSQLCatalog = currentSQLSchema.getCatalog();
                currentSQLDataModel = currentSQLConstraint.getModel();
                nextSelectedSchema = null;
                currentSQLProcedure = null;
                currentSQLSequence = null;
                fireTableChanged();
            } else if (currentUserObject instanceof SQLIndex) {
                currentSQLIndex = (SQLIndex) currentUserObject;
                currentSQLTable = currentSQLIndex.getTable();
                currentSQLSchema = currentSQLTable.getSchema();
                currentSQLCatalog = currentSQLSchema.getCatalog();
                currentSQLDataModel = currentSQLIndex.getModel();
                nextSelectedSchema = null;
                currentSQLProcedure = null;
                currentSQLSequence = null;
                fireTableChanged();
            } else if (currentUserObject instanceof SQLSchema) {
                currentSQLConstraint = null;
                currentSQLTable = null;
                currentSQLProcedure = null;
                currentSQLSequence = null;
                currentSQLSchema = (SQLSchema) currentUserObject;
                currentSQLCatalog = currentSQLSchema.getCatalog();
                currentSQLDataModel = currentSQLSchema.getModel();
                fireTableChanged();
                if (nextSelectedUserObject instanceof SQLSchema) {
                	nextSelectedSchema = (SQLSchema) nextSelectedUserObject;
                } else {
                	nextSelectedSchema = null;
                }
            } else if (currentUserObject instanceof SQLCatalog) {
                currentSQLConstraint = null;
                currentSQLTable = null;
                currentSQLProcedure = null;
                currentSQLSequence = null;
                currentSQLSchema = null;
                currentSQLCatalog = (SQLCatalog) currentUserObject;
                currentSQLDataModel = currentSQLCatalog.getModel();
                nextSelectedSchema = null;
                fireTableChanged();
            } else if (currentUserObject instanceof SQLProcedure) {
                currentSQLConstraint = null;
                currentSQLTable = null;
                currentSQLSequence = null;
                currentSQLProcedure = (SQLProcedure) currentUserObject;
                currentSQLSchema = currentSQLProcedure.getSchema();
                currentSQLDataModel = currentSQLSchema.getModel();
                currentSQLCatalog = currentSQLSchema.getCatalog();
                nextSelectedSchema = null;
                fireTableChanged();
            } else if (currentUserObject instanceof SQLSequence) {
                currentSQLConstraint = null;
                currentSQLTable = null;
                currentSQLSequence = (SQLSequence) currentUserObject;
                currentSQLProcedure = null;
                currentSQLSchema = currentSQLSequence.getSchema();
                currentSQLDataModel = currentSQLSchema.getModel();
                currentSQLCatalog = currentSQLSchema.getCatalog();
                nextSelectedSchema = null;
                fireTableChanged();
            } else if (currentUserObject instanceof SQLDataModel) {
                currentSQLConstraint = null;
                currentSQLTable = null;
                currentSQLSchema = null;
                currentSQLCatalog = null;
                nextSelectedSchema = null;
                currentSQLDataModel = (SQLDataModel) currentUserObject;
                fireTableChanged();
            } else {
                currentSQLConstraint = null;
                currentSQLProcedure = null;
                currentSQLTable = null;
                currentSQLSchema = null;
                currentSQLCatalog = null;
                currentSQLDataModel = null;
                nextSelectedSchema = null;
                fireTableChanged();
            }
	        setCurrentFilterNode(currentNode);
	        if (currentSQLDataModel != null) {
	        	currentSQLDataModel.setCurrentSQLSchema(currentSQLSchema);
	        }
        }
    }

    private void setCurrentFilterNode(DefaultMutableTreeNode node) {
    	Object userObject = node.getUserObject();
        if (userObject instanceof SQLTable) {
			currentFilterStartNode = (DefaultMutableTreeNode) node.getParent(); // use folder node to filter
        } else if (userObject instanceof SQLConstraint) {
			currentFilterStartNode = (DefaultMutableTreeNode) node.getParent();
        } else if (userObject instanceof SQLIndex) {
			currentFilterStartNode = (DefaultMutableTreeNode) node.getParent();
        } else if (userObject instanceof SQLSchema) {
			currentFilterStartNode = (DefaultMutableTreeNode) node.getParent();
        } else if (userObject instanceof SQLCatalog) {
			currentFilterStartNode = node;
        } else if (userObject instanceof SQLProcedure) {
			currentFilterStartNode = (DefaultMutableTreeNode) node.getParent();
        } else if (userObject instanceof SQLSequence) {
			currentFilterStartNode = (DefaultMutableTreeNode) node.getParent();
        } else if (userObject instanceof SQLDataModel) {
			currentFilterStartNode = node;
        } else if (userObject instanceof Folder) {
			currentFilterStartNode = node;
        } else {
			currentFilterStartNode = null;
        }
    }
    
    // -------------- Methoden für TreeWillExpandListener -----------------------------------------------

    /* (non-Javadoc)
     * @see javax.swing.event.TreeWillExpandListener#treeWillCollapse(javax.swing.event.TreeExpansionEvent)
     */
    @Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        // nothing to do
    }

    /* (non-Javadoc)
     * @see javax.swing.event.TreeWillExpandListener#treeWillExpand(javax.swing.event.TreeExpansionEvent)
     */
    @Override
	public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
    	new Thread() {
    		@Override
			public void run() {
    			final JTree tree = (JTree) event.getSource();
    			SwingUtilities.invokeLater(new Runnable() {
    				@Override
					public void run() {
    	    			tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    				}
    			});
    	        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
    	        currentNode = node;
    	        buildNodes(node);
    	        setCurrentFilterNode(currentNode);
    			SwingUtilities.invokeLater(new Runnable() {
    				@Override
					public void run() {
    	    			tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    				}
    			});
    		}
    	}.start();
    }
    
    private ArrayList<Object> collectObjectPath(Object userObject) {
    	ArrayList<Object> path = new ArrayList<Object>();
    	if (userObject instanceof SQLDataModel) {
    		path.add(userObject);
    	} else if (userObject instanceof SQLCatalog) {
    		SQLCatalog catalog = (SQLCatalog) userObject;
    		path.add(catalog.getModel());
    		path.add(catalog);
    	} else if (userObject instanceof SQLSchema) {
    		SQLSchema schema = (SQLSchema) userObject;
    		path.add(schema.getModel());
    		path.add(schema.getCatalog());
    		path.add(schema);
    	} else if (userObject instanceof SQLTable) {
    		SQLTable table = (SQLTable) userObject;
    		path.add(table.getModel());
    		path.add(table.getSchema().getCatalog());
    		path.add(table.getSchema());
    		if (table.isView()) {
    			path.add(new ViewFolder(table.getSchema()));
    		} else {
    			path.add(new TableFolder(table.getSchema()));
    		}
    		path.add(table);
    	}
    	return path;
    }
    
    private void buildNodes(DefaultMutableTreeNode parentNode) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("buildNodes parentNode=" + parentNode + " class=" + parentNode.getUserObject().getClass());
    	}
        final Object userObject = parentNode.getUserObject();
        if (userObject instanceof SQLDataModel) {
        	buildNodesForCatalogs((SQLDataModel) userObject, parentNode);
        } else if (userObject instanceof SQLCatalog) {
        	buildNodesForSchemas((SQLCatalog) userObject, parentNode);
        } else if (userObject instanceof SQLSchema) {
            buildNodesForSchemaFolders((SQLSchema) userObject, parentNode);
        } else if (userObject instanceof SQLTable) {    
            final SQLTable table = (SQLTable) userObject;
            buildNodesForTableSubFolders(table, parentNode);
            if (stopBeforeColumns) {
            	return;
            }
        } else if (userObject instanceof TableConstraintsFolder) {    
            final SQLTable table = ((TableConstraintsFolder) userObject).getTable();
            buildNodesForTableConstraints(table, parentNode);
        } else if (userObject instanceof TableIndexesFolder) {    
            final SQLTable table = ((TableIndexesFolder) userObject).getTable();
            buildNodesForTableIndexes(table, parentNode);
        } else if (userObject instanceof TableFolder) {    
            final SQLSchema schema = ((Folder) userObject).getSchema();
            buildNodesForTables(schema, parentNode);
        } else if (userObject instanceof ViewFolder) {    
            final SQLSchema schema = ((Folder) userObject).getSchema();
            buildNodesForViews(schema, parentNode);
        } else if (userObject instanceof ProcedureFolder) {
            final SQLSchema schema = ((Folder) userObject).getSchema();
            buildNodesForFunctions(schema, parentNode);
        } else if (userObject instanceof SequenceFolder) {
            final SQLSchema schema = ((Folder) userObject).getSchema();
            buildNodesForSequences(schema, parentNode);
        } else {
        	return;
        }
        if (buildTreeNodesRecursive) {
        	for (int i = 0; i < parentNode.getChildCount(); i++) {
        		buildNodes((DefaultMutableTreeNode) parentNode.getChildAt(i));
        	}
        }
    }
    
    private void buildNodes(final DefaultMutableTreeNode parentNode, final List<? extends Object> listUserObjects) {
       	if (SwingUtilities.isEventDispatchThread()) {
    		doBuildNodes(parentNode, listUserObjects);
    	} else {
    		if (buildTreeNodesRecursive) {
        		try {
					SwingUtilities.invokeAndWait(new Runnable() {
						
	        			@Override
						public void run() {
							doBuildNodes(parentNode, listUserObjects);
						}
						
					});
				} catch (InterruptedException e) {
					logger.error("buildNodes interrupted:" + e.getMessage(), e);
				} catch (InvocationTargetException e) {
					logger.error("buildNodes failed:" + e.getMessage(), e);
				}
    		} else {
        		SwingUtilities.invokeLater(new Runnable() {
        			
        			@Override
        			public void run() {
        				doBuildNodes(parentNode, listUserObjects);
        			}
        			
        		});
    		}
    	}
    }
    
    private synchronized void doBuildNodes(DefaultMutableTreeNode parentNode, List<? extends Object> newListOfChildUserObjects) {
    	List<Object> currentNewChildren = new ArrayList<Object>();
    	if (objectFilter != null) {
    		currentNewChildren = new ArrayList<Object>();
    		for (Object o : newListOfChildUserObjects) {
    			if (o instanceof SQLTable || o instanceof SQLProcedure) {
        			if (o.toString().toLowerCase().contains(objectFilter)) {
            			currentNewChildren.add(o);
        			}
    			} else {
        			currentNewChildren.add(o);
    			}
    		}
    	} else {
    		for (Object o : newListOfChildUserObjects) {
    			currentNewChildren.add(o);
    		}
    	}
    	if (logger.isDebugEnabled()) {
    		logger.debug("doBuildNodes parentNode=" + parentNode + " newListOfChildUserObjects=" + currentNewChildren);
    	}
    	int parentNodeChildCount = parentNode.getChildCount();
    	for (int i = 0; i < currentNewChildren.size(); i++) {
    		Object newUserObject = currentNewChildren.get(i);
    		if (i < parentNodeChildCount) {
            	DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            	if (newUserObject.equals(childNode.getUserObject()) == false) {
            		childNode.setUserObject(newUserObject);
            		nodeChanged(childNode);
            	}
    		} else {
    			DefaultMutableTreeNode newNode = new SQLObjectTreeNode(newUserObject);
    			insertNodeInto(newNode, parentNode, i);
    		}
    	}
    	if (parentNodeChildCount > currentNewChildren.size()) {
        	for (int i = parentNodeChildCount - 1; i >= currentNewChildren.size(); i--) {
        		removeNodeFromParent((DefaultMutableTreeNode) parentNode.getChildAt(i));
        	}
    	}
    }
    
    void buildNodesForCatalogs(SQLDataModel sqlDataModel, DefaultMutableTreeNode parentNode) {
    	if (sqlDataModel.isCatalogsLoaded() == false) {
    		sqlDataModel.loadCatalogs();
    	}
    	buildNodes(parentNode, sqlDataModel.getCatalogs());
    }

    void buildNodesForSchemas(SQLCatalog catalog, DefaultMutableTreeNode parentNode) {
    	buildNodes(parentNode, catalog.getSchemas());
    }

    private void buildNodesForTables(SQLSchema schema, DefaultMutableTreeNode tableFolderNode) {
        SQLTable table = null;
        List<SQLTable> list = new ArrayList<SQLTable>();
        for (int i = 0; i < schema.getTableCount(); i++) {
            table = schema.getTableAt(i);
            if (table.isView() == false) {
            	list.add(table);
            }
        }
        buildNodes(tableFolderNode, list);
    }
    
    private void buildNodesForTableSubFolders(SQLTable table, DefaultMutableTreeNode tableNode) {
    	List<Folder> listFolders = new ArrayList<Folder>();
    	listFolders.add(new TableConstraintsFolder(table));
    	listFolders.add(new TableIndexesFolder(table));
        buildNodes(tableNode, listFolders);
    }
    
    private void buildNodesForTableConstraints(SQLTable table, DefaultMutableTreeNode tableIndexFolderNode) {
        if (table.isFieldsLoaded() == false) {
            table.loadColumns(); // constraints will be loaded within loading columns
        }
        List<SQLConstraint> listConstraints = table.getConstraints();
        List<SQLConstraint> list = new ArrayList<SQLConstraint>();
        for (int i = 0; i < listConstraints.size(); i++) {
            SQLConstraint constraint = listConstraints.get(i);
            if (constraint.getType() == SQLConstraint.PRIMARY_KEY) {
            	list.add(constraint);
            }
        }
        for (int i = 0; i < listConstraints.size(); i++) {
            SQLConstraint constraint = listConstraints.get(i);
            if (constraint.getType() == SQLConstraint.UNIQUE_KEY) {
            	list.add(constraint);
            }
        }
        for (int i = 0; i < listConstraints.size(); i++) {
            SQLConstraint constraint = listConstraints.get(i);
            if (constraint.getType() == SQLConstraint.FOREIGN_KEY) {
            	list.add(constraint);
            }
        }
        buildNodes(tableIndexFolderNode, list);
    }
    
    private void buildNodesForTableIndexes(SQLTable table, DefaultMutableTreeNode tableIndexFolderNode) {
        if (table.isFieldsLoaded() == false) {
            table.loadColumns(); // constraints will be loaded within loading columns
        }
        List<SQLIndex> list = table.getIndexes();
        buildNodes(tableIndexFolderNode, list);
    }

    private void buildNodesForViews(SQLSchema schema, DefaultMutableTreeNode viewFolderNode) {
        SQLTable table = null;
        ArrayList<SQLTable> list = new ArrayList<SQLTable>();
        for (int i = 0; i < schema.getTableCount(); i++) {
            table = schema.getTableAt(i);
            if (table.isView()) {
            	list.add(table);
            }
        }
        buildNodes(viewFolderNode, list);
    }
    
    private void buildNodesForFunctions(SQLSchema schema, DefaultMutableTreeNode functionFolderNode) {
        SQLProcedure proc = null;
        ArrayList<SQLProcedure> list = new ArrayList<SQLProcedure>();
        for (int i = 0; i < schema.getProcedureCount(); i++) {
            proc = schema.getProcedureAt(i);
            list.add(proc);
        }
        buildNodes(functionFolderNode, list);
    }

    private void buildNodesForSequences(SQLSchema schema, DefaultMutableTreeNode sequenceFolderNode) {
        SQLSequence seq = null;
        ArrayList<SQLSequence> list = new ArrayList<SQLSequence>();
        for (int i = 0; i < schema.getSequenceCount(); i++) {
            seq = schema.getSequenceAt(i);
            list.add(seq);
        }
        buildNodes(sequenceFolderNode, list);
    }

    private void buildNodesForSchemaFolders(SQLSchema schema, DefaultMutableTreeNode schemaNode) {
    	ArrayList<Object> list = new ArrayList<Object>();
    	TableFolder tf = new TableFolder(schema);
    	list.add(tf);
    	ViewFolder vf = new ViewFolder(schema);
        list.add(vf);
        ProcedureFolder pf = new ProcedureFolder(schema);
        list.add(pf);
        SequenceFolder sf = new SequenceFolder(schema);
        list.add(sf);
        buildNodes(schemaNode, list);
    }
    
	public SQLDataModel getCurrentSQLDataModel() {
		return currentSQLDataModel;
	}

	public SQLSchema getCurrentSQLSchema() {
		return currentSQLSchema;
	}
	
	public SQLCatalog getCurrentSQLCatalog() {
		return currentSQLCatalog;
	}
	
	public SQLSchema getNextSelectedSQLSchema() {
		return nextSelectedSchema;
	}
	
	public boolean isCurrentSchema(String schemaName) {
		if (currentSQLSchema != null) {
			return currentSQLSchema.getName().equalsIgnoreCase(schemaName);
		} else {
			return false;
		}
	}

	public SQLTable getCurrentSQLTable() {
		return currentSQLTable;
	}

	public SQLField getCurrentSQLField() {
		return currentSQLField;
	}
    
    public SQLProcedure getCurrentSQLProcedure() {
        return currentSQLProcedure;
    }
    
    public SQLSequence getCurrentSQLSequence() {
        return currentSQLSequence;
    }
    
    public List<SQLSequence> getCurrentSelectedSQLSequences() {
    	List<SQLSequence> list = new ArrayList<SQLSequence>();
    	for (SQLObject so : currentSelectedSQLObjects) {
    		if (so instanceof SQLSequence) {
    			list.add((SQLSequence) so);
    		}
    	}
    	return list;
    }
    
    public List<SQLTable> getCurrentSelectedSQLTables() {
    	List<SQLTable> list = new ArrayList<SQLTable>();
    	for (SQLObject so : currentSelectedSQLObjects) {
    		if (so instanceof SQLTable) {
    			list.add((SQLTable) so);
    		}
    	}
    	return list;
    }

    public List<SQLProcedure> getCurrentSelectedSQLProcedures() {
    	List<SQLProcedure> list = new ArrayList<SQLProcedure>();
    	for (SQLObject so : currentSelectedSQLObjects) {
    		if (so instanceof SQLProcedure) {
    			list.add((SQLProcedure) so);
    		}
    	}
    	return list;
    }

    public SQLConstraint getCurrentSQLConstraint() {
        return currentSQLConstraint;
    }

    public SQLIndex getCurrentSQLIndex() {
        return currentSQLIndex;
    }

	public DefaultMutableTreeNode getCurrentNode() {
		return currentNode;
	}
	
    public Object getCurrentUserObject() {
        return currentUserObject;
    }

	public String getObjectFilter() {
		return objectFilter;
	}

	public void setObjectFilter(String objectFilter) {
		if (objectFilter == null || objectFilter.trim().isEmpty()) {
			this.objectFilter = null;
		} else {
			this.objectFilter = objectFilter.trim().toLowerCase();
		}
	}

	public List<SQLObject> getCurrentSelectedSQLObjects() {
		return currentSelectedSQLObjects;
	}

}
