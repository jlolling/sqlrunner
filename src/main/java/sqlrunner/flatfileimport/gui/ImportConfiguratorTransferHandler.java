package sqlrunner.flatfileimport.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;


/**
 * Transfer-Handler für JTextArea-Komponenten, der Dateien und Strings
 * verarbeitet
 */
public class ImportConfiguratorTransferHandler extends TransferHandler {

	private static final Logger logger = LogManager.getLogger(ImportConfiguratorTransferHandler.class);
	private static final long serialVersionUID = 1L;
    private ImportConfiguratorFrame configurator;
    private boolean dragActionIsActive = false;
    
    public ImportConfiguratorTransferHandler(ImportConfiguratorFrame configurator) {
        this.configurator = configurator;
    }
    
	/**
	 * Prüft mit Hilfe der private Methoden hasFileFlavor() und
	 * hasStringFlavor(), ob die angebotenen Daten in einem Data Flavor
	 * angeboten werden, der für die Target-Komponente geeignet ist.
	 */
    @Override
	public boolean canImport(JComponent c, DataFlavor[] flavors) {
    	if (dragActionIsActive) {
    		return false;
    	}
        if (hasFileFlavor(flavors)) {
            return true;
        } else if (hasStringFlavor(flavors)) {
            return true;
        } else {
      		return false;
        }
	}

	private boolean hasFileFlavor(DataFlavor[] flavors) {
		for (int i = 0; i < flavors.length; i++) {
			if (flavors[i].equals(DataFlavor.javaFileListFlavor)) {
				return true;
		    }
		}
		return false;
	}
	
	private boolean hasStringFlavor(DataFlavor[] flavors) {
		for (int i = 0; i < flavors.length; i++) {
			if (flavors[i].equals(DataFlavor.stringFlavor)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * versucht die Daten aus dem Transferable-Objekt t in der Komponente c
	 * abzulegen
	 */
    @SuppressWarnings("unchecked")
	@Override
	public boolean importData(JComponent c,	Transferable t) {
		try {
			if (hasFileFlavor(t.getTransferDataFlavors())) {
				final java.util.List<Object> files = (java.util.List<Object>) t.getTransferData(DataFlavor.javaFileListFlavor);
				if (files.size() > 0) {
					File f = (File) files.get(0);
                    configurator.handleFile(f);
                    return true;
				}
			} else if (hasStringFlavor(t.getTransferDataFlavors())) {
                String propertiesAsString = (String) t.getTransferData(DataFlavor.stringFlavor);
                configurator.handleConfigProperties(propertiesAsString);
                return true;
            }
		} catch (UnsupportedFlavorException e) {
			logger.error("unsuppported dragged data flavor: " + e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * erzeugt ein Transferable-Objekt mit den zu übertragenden Drag-Daten
	 */
    @Override
	protected Transferable createTransferable(JComponent c) {

		// Vorab prüfen, ob sich hinter der Komponente c auch wirklich
		// eine JEditorPane-Komponente verbirgt
		if (!(c instanceof JTable))
			return null;

        String data = null;
        try {
            data = configurator.getConfigurationPropertiesAsString();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        dragActionIsActive = true;
        return new StringSelection(data);
	}

    @Override
	public int getSourceActions(JComponent c) {
		return COPY;
	}

    @Override
	protected void exportDone(JComponent c,
			Transferable data,
			int action) {
        dragActionIsActive = false;
	}

}
