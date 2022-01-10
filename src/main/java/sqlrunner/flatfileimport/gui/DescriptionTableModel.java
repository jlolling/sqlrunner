package sqlrunner.flatfileimport.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.FieldDescription;
import sqlrunner.flatfileimport.FieldTokenizer;

public class DescriptionTableModel implements TableModel {

    private static Logger logger = LogManager.getLogger(DescriptionTableModel.class);
    private FieldTokenizer tokenizer;
    private ArrayList<FieldDescription> listDescriptions = new ArrayList<FieldDescription>();
    
    public void setTokenizer(FieldTokenizer tokenizer_loc) {
        this.tokenizer = tokenizer_loc; // das parsen wurde im Dialog angestossden
        this.fireTableRowsUpdated(0, listDescriptions.size() - 1);
    }
    
    private final static String[] columnNames={
    	"enabled",
    	"unique",
    	"not null",
    	"table column",
    	"position",
    	"test dataset"};

    public void setValueAt(Object value, int row, int col) {
    	// not used
    }

    public Object getValueAt(int row, int col) {
    	if (listDescriptions.size() <= row || row < 0) {
    		throw new IllegalArgumentException("row="+row+" equals/greater than rowCount="+listDescriptions.size()+" !");
    	}
        Object cellContent = null;
        final FieldDescription fd = listDescriptions.get(row);
        if (fd == null) {
        	throw new IllegalArgumentException("row=" + row + "out of list bounds=" + listDescriptions.size() + " !");
        }
        switch (col) {
            case 0:
                cellContent = Boolean.valueOf(fd.isEnabled());
                break;
            case 1:
                cellContent = Boolean.valueOf(fd.isPartOfPrimaryKey());
                break;
            case 2:
                cellContent = Boolean.valueOf(fd.isNullEnabled() == false);
                break;
            case 3:
                cellContent = fd.getName();
                break;
            case 4:
                cellContent = fd.getExtractionDescription();
                break;
            case 5:
                if (tokenizer == null) {
                    cellContent = "";
                } else {
                    cellContent = tokenizer.getData(row);
                }
        }
        return cellContent;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Class<?> getColumnClass(int col) {
        switch (col) {
            case 0:
                return Boolean.class;
            case 1:
                return Boolean.class;
            case 2:
                return Boolean.class;
            case 3:
                return String.class;
            case 4:
                return String.class;
            case 5:
                return Object.class;
            default:
                return Object.class;
        }
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public int getRowCount() {
        return listDescriptions.size();
    }

    public void sort() {
        if (logger.isDebugEnabled()) {
            logger.debug("sort()");
        }
        final Object[] temp = listDescriptions.toArray();
        this.removeAllFieldDescriptions();
        Arrays.sort(temp);
        for (int i = 0; i < temp.length; i++) {
        	listDescriptions.add((FieldDescription) temp[i]);
        }
        this.fireTableRowsUpdated(0, listDescriptions.size() - 1);
    }

    public ArrayList<FieldDescription> getDescriptions() {
    	return listDescriptions;
    }
    
    // ------------------- Event-handling ----------------------------
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

    /**
     * Notify all listeners that rows in the (inclusive) range
     * [<I>firstRow</I>, <I>lastRow</I>] have been updated.
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

    public synchronized void removeAllFieldDescriptions() {
        final int oldSize = listDescriptions.size();
        if (oldSize > 0) {
        	listDescriptions.clear();
            fireTableRowsDeleted(0, oldSize - 1);
        }
    }

    // ----------------------- Methods to handle Descriptions -----------------

    public FieldDescription getFieldDescription(int row) {
        return listDescriptions.get(row);
    }

    public boolean addFieldDescription(FieldDescription fd) {
        if (existsFieldDescription(fd) == false) {
            if (logger.isDebugEnabled()) {
                logger.debug("addFieldDescription fd=" + fd);
            }
            listDescriptions.add(fd);
            fireTableRowsInserted(listDescriptions.size() - 1, listDescriptions.size() - 1);
            return true;
        } else {
            return false;
        }
    }
    
    private boolean existsFieldDescription(FieldDescription newField) {
        FieldDescription temp = null;
        for (int i = 0; i < listDescriptions.size(); i++) {
            temp = listDescriptions.get(i);
            if (temp.getDelimPos() == newField.getDelimPos()) {
                if (newField.getName().startsWith("#")) {
                    return true;
                } else {
                    if (temp.getName().startsWith("#")) {
                        listDescriptions.remove(i);
                    }
                    return false;
                }
            }
        }
        return false;
    }
    
    public void sortBy(Comparator<FieldDescription> comparator) {
    	Collections.sort(listDescriptions, comparator);
    	fireTableRowsUpdated(0, listDescriptions.size() - 1);
    }
    
    public void createTestFieldDescriptions(int count) {
        if (logger.isDebugEnabled()) {
            logger.debug("createTestFieldDescriptions count=" + count);
        }
        int lastCountFields = listDescriptions.size();
        for (int i = 0; i < count; i++) {
            FieldDescription fd = new FieldDescription();
            fd.setEnabled(false);
            fd.setPositionType(FieldDescription.DELIMITER_POSITION);
            fd.setDelimPos(i);
            fd.setBasicTypeId(BasicDataType.CHARACTER.getId());
            if (existsFieldDescription(fd) == false) {
                listDescriptions.add(fd);
            }
        }
        fireTableRowsInserted(lastCountFields - 1, listDescriptions.size() - 1);
    }

    public void removeTestFieldDescriptions() {
        if (logger.isDebugEnabled()) {
            logger.debug("removeTestFieldDescriptions");
        }
        for (int i = 0; i < listDescriptions.size(); i++) {
            if (listDescriptions.get(i).isDummy()) {
                removeFieldDescriptionAt(i);
                i--;
            }
        }
    }
    
    public void insertFieldDescriptionAt(FieldDescription object, int row) {
    	listDescriptions.add(row, object);
        fireTableRowsInserted(row, row);
    }

    public void removeFieldDescriptionAt(int index) {
    	listDescriptions.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public void setFieldDescriptions(List<FieldDescription> source) {
        removeAllFieldDescriptions();
        for (int i = 0; i < source.size(); i++) {
            addFieldDescription(source.get(i));
        }
    }
    
    public int getRowIndex(FieldDescription fd) {
    	return listDescriptions.indexOf(fd);
    }
    
    public void setupFieldDescriptionIndex() {
    	int i = 0;
    	for (FieldDescription fd : listDescriptions) {
    		if (fd.isDummy() == false) {
    			fd.setIndex(i++);
    		}
    	}
    }

}
