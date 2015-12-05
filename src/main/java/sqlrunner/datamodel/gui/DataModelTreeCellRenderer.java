package sqlrunner.datamodel.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import sqlrunner.datamodel.SQLCatalog;
import sqlrunner.datamodel.SQLDataModel;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.datamodel.gui.SQLDataTreeTableModel.Folder;
import sqlrunner.resources.ApplicationIcons;

public final class DataModelTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    /**
     * Configures the renderer based on the passed in components.
     * The foreground color is set based on the selection and the icon
     * is set based on on leaf and expanded.
     */
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus_loc) {
    	super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus_loc);
        //setText(value.toString());
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node.getUserObject() instanceof SQLDataModel) {
            setDisabledIcon(ApplicationIcons.DB_GIF);
            setIcon(ApplicationIcons.DB_GIF);
        } else if (node.getUserObject() instanceof SQLCatalog) {
            setDisabledIcon(ApplicationIcons.CATALOG_GIF);
            setIcon(ApplicationIcons.CATALOG_GIF);
        } else if (node.getUserObject() instanceof SQLSchema) {
            setDisabledIcon(ApplicationIcons.SCHEMA_PNG);
            setIcon(ApplicationIcons.SCHEMA_PNG);
        } else if (node.getUserObject() instanceof Folder) {
            setDisabledIcon(ApplicationIcons.FOLDER_CLOSED_PNG);
            setIcon(ApplicationIcons.FOLDER_CLOSED_PNG);
        } else if (node.getUserObject() instanceof SQLProcedure) {
        	if (((SQLProcedure) node.getUserObject()).isFunction()) {
                setDisabledIcon(ApplicationIcons.FUNCTION_PNG);
                setIcon(ApplicationIcons.FUNCTION_PNG);
        	} else {
                setDisabledIcon(ApplicationIcons.PROCEDURE_PNG);
                setIcon(ApplicationIcons.PROCEDURE_PNG);
        	}
        } else if (node.getUserObject() instanceof SQLTable) {
            if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_TABLE)) {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
            } else if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_VIEW)) {
                setDisabledIcon(ApplicationIcons.VIEW_PNG);
                setIcon(ApplicationIcons.VIEW_PNG);
            } else if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_ALIAS)) {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
            } else if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_SYNONYM)) {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
            } else if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_GLOBAL_TEMPORARY)) {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
            } else if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_LOCAL_TEMPORARY)) {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
            } else if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_SYSTEM_TABLE)) {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
            } else if (((SQLTable) node.getUserObject()).getType().equals(SQLTable.TYPE_SYSTEM)) {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
            } else {
                setDisabledIcon(ApplicationIcons.TABLE_PNG);
                setIcon(ApplicationIcons.TABLE_PNG);
                setText(getText()+" ["+((SQLTable) node.getUserObject()).getType()+"]");
            }
        } else {
            setDisabledIcon(null);
            setIcon(null);
        }
        setOpaque(true);
        // workaround for the buggy Nimbus Look&Feel
        if (sel) {
        	setBackground(Color.lightGray);
        } else {
        	setBackground(Color.white);
        }
        return this;
    }

}
