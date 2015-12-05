package sqlrunner.datamodel.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * Transfer-Handler für JTextArea-Komponenten, der Dateien und Strings
 * verarbeitet
 */
public class SQLDataModelTransferHandler extends TransferHandler {

	private static final long serialVersionUID = 1L;

    private SQLDataTreeTableModel treeModel;
    
    public SQLDataModelTransferHandler(SQLDataTreeTableModel treeModel) {
        this.treeModel = treeModel;
    }
    
	/**
	 * Prüft mit Hilfe der private Methoden hasFileFlavor() und
	 * hasStringFlavor(), ob die angebotenen Daten in einem Data Flavor
	 * angeboten werden, der für die Target-Komponente geeignet ist.
	 */
    @Override
	public boolean canImport(JComponent c, DataFlavor[] flavors) {
  		return false;
	}

	/**
	 * versucht die Daten aus dem Transferable-Objekt t in der Komponente c
	 * abzulegen
	 */
	@Override
	public boolean importData(JComponent c,	Transferable t) {
		return false;
	}

	/**
	 * erzeugt ein Transferable-Objekt mit den zu übertragenden Drag-Daten
	 */
    @Override
	protected Transferable createTransferable(JComponent c) {
        String data = DocumentationExport.getHTMLTableFor(treeModel.getCurrentSQLTable());
        return new HTMLDataTransferable(data);
	}

    @Override
	public int getSourceActions(JComponent c) {
		return COPY;
	}

	/**
	 * Am Ende einer String-DROP-Aktion den Text in der Quelle löschen.
	 */
    @Override
	protected void exportDone(JComponent c,
			Transferable data,
			int action) {

	}

}
