package sqlrunner.datamodel.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import sqlrunner.datamodel.SQLCatalog;
import sqlrunner.datamodel.SQLDataModel;
import sqlrunner.datamodel.SQLObject;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.datamodel.gui.SQLDataTreeTableModel.Folder;

public class SQLObjectTreeNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	public SQLObjectTreeNode(Object userObject) {
		super(userObject);
		if (userObject instanceof SQLDataModel) {
			setAllowsChildren(true);
		} else if (userObject instanceof SQLCatalog) {
			setAllowsChildren(true);
		} else if (userObject instanceof SQLSchema) {
			setAllowsChildren(true);
		} else if (userObject instanceof SQLTable) {
			setAllowsChildren(true);
		} else if (userObject instanceof Folder) {
			setAllowsChildren(true);
		} else if (userObject instanceof SQLObject) {
			setAllowsChildren(false);
		} else {
			setAllowsChildren(true);
		}
	}
	
	@Override
	public boolean isLeaf() {
		Object userObject = getUserObject();
		if (userObject instanceof SQLDataModel) {
			return false;
		} else if (userObject instanceof SQLCatalog) {
			return false;
		} else if (userObject instanceof SQLSchema) {
			return false;
		} else if (userObject instanceof SQLTable) {
			return false;
		} else if (userObject instanceof Folder) {
			return false;
		} else if (userObject instanceof SQLObject) {
			return true;
		} else {
			return false;
		}
	}
	
}
