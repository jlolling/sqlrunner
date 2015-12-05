package sqlrunner.editor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

import org.apache.log4j.Logger;

import sqlrunner.MainFrame;
import sqlrunner.text.StringReplacer;

/**
 * Transfer-Handler für JTextArea-Komponenten, der Dateien und Strings
 * verarbeitet
 */
public class ExtEditorTransferHandler extends TransferHandler {

	private static final Logger logger = Logger.getLogger(ExtEditorTransferHandler.class);
	private static final long serialVersionUID = 1L;
	private transient Position p0 = null;
	private transient Position p1 = null; // Anfang und Ende zu exportierender
	// Drag-Daten

	private ExtEditorPane source; // Drag-Quelle, wird bei String-Drag
	// (Methode createTransferable())
	// gespeichert, um bei Drop feststellen
	// zu können, ob Quelle und Ziel
	// identisch sind. Wenn dann auch die
	// Drop-position im Bereich der
	// Drag-Auswahl liegt, muss nichts
	// verändert werden.

	private boolean shouldRemove; // Wird auf false gesetzt, wenn Drop-
	// position im Drag-Bereich liegt (s.o.),
	// um zu verhindern, dass exportDone()
	// die Drag-Daten löscht

	/**
	 * Prüft mit Hilfe der private Methoden hasFileFlavor() und
	 * hasStringFlavor(), ob die angebotenen Daten in einem Data Flavor
	 * angeboten werden, der für die Target-Komponente geeignet ist.
	 */
    @Override
	public boolean canImport(JComponent c, DataFlavor[] flavors) {
        if (logger.isDebugEnabled()) {
        	for (DataFlavor f : flavors) {
            	logger.debug("canImport: checks flavor: " + f);
        	}
        }
        if (flavors.length == 0) {
        	logger.warn("canImport: No favors given");
        }
		if (c instanceof ExtEditorPane) {
            if (hasFileFlavor(flavors)) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("import flavors as file flavor");
        		}
                return true;
            }
            if (hasStringFlavor(flavors)) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("import flavors as string flavor");
        		}
                return true;
            }
        } else {
        	logger.warn("target component is not ExtEditorPane! current target: " + c);
        }
		if (logger.isDebugEnabled()) {
			logger.debug("canImport: cannot import flavors");
		}
  		return false;
	}

	private boolean hasFileFlavor(DataFlavor[] flavors) {
		for (int i = 0; i < flavors.length; i++) {
			if (logger.isDebugEnabled()) {
				logger.debug("hasFileFlavor: check flavor: " + flavors[i]);
			}
			if (flavors[i].equals(DataFlavor.javaFileListFlavor)) {
				return true;
		    }
		}
		return false;
	}
	
	private boolean hasStringFlavor(DataFlavor[] flavors) {
		for (int i = 0; i < flavors.length; i++) {
			if (logger.isDebugEnabled()) {
				logger.debug("hasStringFlavor: check flavor: " + flavors[i]);
			}
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

		// Vorab prüfen, ob sich hinter der Target-Komponente c auch wirklich
		// eine JTextArea-Komponente verbirgt und ob die angebotenen Daten in
		// einem Data Flavor angeboten werden, der für die Target-Komponente
		// geeignet ist.
		if (!(c instanceof ExtEditorPane) || !canImport(c, t.getTransferDataFlavors())) {
			return false;
		}

		// Referenz c in den Typ ExtEditorPane umwandeln
		final ExtEditorPane target = (ExtEditorPane) c;

		try {
			if (hasFileFlavor(t.getTransferDataFlavors())) {
				// Transferable-Objekt enthält eine (oder mehrere) Dateien.
				// Lese die erste Datei und füge ihren Inhalt in die
				// Target-Komponente ein
				if (logger.isDebugEnabled()) {
					logger.debug("handle transferable as files");
				}
				
				final java.util.List<Object> files = (java.util.List<Object>) t.getTransferData(DataFlavor.javaFileListFlavor);

				if (files.size() > 0) {

					// nur erste Datei lesen
					File f = (File) files.get(0);
					MainFrame mainFrame = target.getMainFrame();
					mainFrame.handleFile(f);
					return true;
				}

			} else if (hasStringFlavor(t.getTransferDataFlavors())) {

				String str = (String) t.getTransferData(DataFlavor.stringFlavor);
				// testen ob sich um eine locale URI handelt ?
				if (str.startsWith("file:/")) {
					if (logger.isDebugEnabled()) {
						logger.debug("handle transferable as file uri");
					}
					// doch als File behandeln
					// bei Linux Gnome wird leider keine File eingepackt
					// sondern eine URI in Stringform übergeben
					URI uri = null;
					try {
						uri = URI.create(str.trim()); // Gnome adds a line feed at the end of uri
						File f = new File(uri);
						MainFrame mainFrame = target.getMainFrame();
						mainFrame.handleFile(f);
					} catch (Exception e) {
						logger.error("handle transferable as file uri failed: str=" + str + " uri=" + uri, e);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("handle transferable as text content");
					}
					// Transferable-Objekt enthält eine (oder mehrere) Dateien
					// Wenn Quelle und Ziel identisch sind, verschiebe nur, wenn
					// die Einfügeposition ausserhalb des zu verschiebenden
					// Textes liegt

					if ((target == source)
							&& (target.getCaretPosition() >= p0.getOffset())
							&& (target.getCaretPosition() <= p1.getOffset())) {

						shouldRemove = false;
						return true;
					}
					if (str != null) {
						str = str.replace("\00", "");
					}
					// take care we don not have a strange \r-only line break.
					target.replaceSelection(StringReplacer.fixLineBreaks(str));
					str = null;
				}
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
		if (!(c instanceof ExtEditorPane))
			return null;

		source = (ExtEditorPane) c;

		int start = source.getSelectionStart();
		int end = source.getSelectionEnd();
		if (start == end) {
			return null;

		} else {
			try {
				// Anfangs- und Endposition des markierten Textes
				// im Dokument merken
				Document doc = source.getDocument();
				p0 = doc.createPosition(start);
				p1 = doc.createPosition(end);
			} catch (BadLocationException e) {
				logger.error("text cannot moved: " + e.getMessage());
			}

			shouldRemove = true;
			String data = source.getSelectedText();
			return new StringSelection(data);
		}
	}

    @Override
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	/**
	 * Am Ende einer String-DROP-Aktion den Text in der Quelle löschen.
	 */
    @Override
	protected void exportDone(JComponent c,
			Transferable data,
			int action) {

		if (shouldRemove && (action == MOVE)) {
			if ((p0 != null)
					&& (p1 != null)
					&& (p0.getOffset() != p1.getOffset())) {
				try {
					ExtEditorPane sourceLoc = (ExtEditorPane) c;
					sourceLoc.getDocument().remove(p0.getOffset(),
							p1.getOffset() - p0.getOffset());
				} catch (BadLocationException e) {
					logger.error("text cannot be deleted: " + e.getMessage());
				}
			}
		}
		source = null;
	}

}
