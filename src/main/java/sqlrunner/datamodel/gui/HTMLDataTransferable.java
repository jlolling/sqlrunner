package sqlrunner.datamodel.gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

public class HTMLDataTransferable implements Transferable, ClipboardOwner {
	
	private static final DataFlavor HTML_FLAVOR = new DataFlavor("text/html", "HTML");
	
	@SuppressWarnings("deprecation")
	private DataFlavor[] df = new DataFlavor[] {
		new DataFlavor("text/html", "HTML"), DataFlavor.stringFlavor, DataFlavor.plainTextFlavor
	};
	
	private String htmlText = null;

	public HTMLDataTransferable(String htmlText) {
		this.htmlText = htmlText;
	}

	@SuppressWarnings("deprecation")
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (HTML_FLAVOR.equals(flavor)) {
			return new ByteArrayInputStream((htmlText == null ? "" : htmlText).getBytes());
	    } else if (DataFlavor.stringFlavor.equals(flavor)) {
			return htmlText == null ? "" : htmlText;
		} else if (DataFlavor.plainTextFlavor.equals(flavor)) {
			return new StringReader(htmlText == null ? "" : htmlText);
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

	public DataFlavor[] getTransferDataFlavors() {
		return df;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor f : df) {
			if (f.equals(flavor)) {
				return true;
			}
		}
		return false;
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

}
