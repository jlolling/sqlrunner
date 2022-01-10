package sqlrunner.editor;

import java.awt.event.ActionEvent;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.text.ViewFactory;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import sqlrunner.Main;
import sqlrunner.MainFrame;

public class ExtEditorKit extends DefaultEditorKit {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(ExtEditorKit.class);
	private SyntaxContext preferences;
	private MainFrame mainFrame;
	private static int tabSize = 0;
	private static String tabErsatz = null;

	public ExtEditorKit(MainFrame mainFrame) {
		super();
		this.mainFrame = mainFrame;
		setTabSize(Integer.parseInt(Main.getUserProperty("TAB_SPACE_MAPPING", "4")));
		if (tabErsatz == null) {
			tabErsatz = "";
			for (int i = 0; i < getTabSize(); i++) {
				tabErsatz += " ";
			}
		}
	}

	// additional text actions
	// ----------------------------------------------------

	public static class ToUpperCase extends TextAction {

		private static final long serialVersionUID = 1L;

		public ToUpperCase() {
			super("toUpperCase");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = (JTextComponent) e.getSource();
			final Document doc = editor.getDocument();
			final int pos0 = editor.getSelectionStart();
			final int pos1 = editor.getSelectionEnd();
			if (pos0 != pos1) {
				try {
					final String selectedText = doc.getText(pos0, pos1 - pos0);
					editor.replaceSelection(selectedText.toUpperCase());
				} catch (BadLocationException e1) {
					logger.error("ToUpperCase failed: " + e1.getMessage(), e1);
				}
			}
		}

	}

	public static class ToLowerCase extends TextAction {

		private static final long serialVersionUID = 1L;

		public ToLowerCase() {
			super("toLowerCase");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = (JTextComponent) e.getSource();
			final Document doc = editor.getDocument();
			final int pos0 = editor.getSelectionStart();
			final int pos1 = editor.getSelectionEnd();
			if (pos0 != pos1) {
				try {
					final String selectedText = doc.getText(pos0, pos1 - pos0);
					editor.replaceSelection(selectedText.toLowerCase());
				} catch (BadLocationException e1) {
					logger.error("ToLowerCase failed: " + e1.getMessage(), e1);
				}
			}
		}

	}
	
	public static void insertFormattedBreak(JTextComponent editor) {
		final Document doc = editor.getDocument();
		final int pos = editor.getCaretPosition();
		try {
			// Anfang von Zeile ermitteln
			String s = doc.getText(0, pos);
			int i;
			for (i = pos - 1; i >= 0; i--) {
				if (s.charAt(i) == '\n') {
					i++; // auf erstes Zeichen schauen
					break;
				}
			}
			// kein \n vorhanden also in der ersten Zeile
			// dann position = 0 setzen
			if (i < 0) {
				i = 0;
			}
			// nun ermitteln, was in der Zeile anfangs vor dem Text steht
			// damit werden Leerzeichen und Tabs erfasst !!
			int a;
			for (a = i; a < pos; a++) {
				if ((s.charAt(a) > ' ') || (s.charAt(a) == '\n')) {
					break;
				}
			}
			// und genau das extrahieren und später nach \n einsetzen
			s = s.substring(i, a);
			editor.replaceSelection("\n" + s);
			// Cursor neu setzen
			editor.setCaretPosition((pos + 1) + s.length());
		} catch (BadLocationException ble) {
			logger.error("InsertFormatedBreak: at pos: " + pos, ble);
		}
	}

	/**
	 * Einfüge-Action alle ENTER landen hier !! Es wird zusätzlich zum
	 * Zeilenumbruch auch der in der vorhergehenden Zeile enthaltene Freiraum
	 * vor dem Text mit eingefügt.
	 */
	public static class InsertFormatedBreak extends TextAction {

		private static final long serialVersionUID = 1L;

		public InsertFormatedBreak() {
			super("insert-break");
		}

		public void actionPerformed(ActionEvent e) {
			insertFormattedBreak(getTextComponent(e));
		}
	}

	public static class ToggleLineComment extends TextAction {

		private static final long serialVersionUID = 1L;

		public ToggleLineComment() {
			super("toggle-line-comment");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent comp = getTextComponent(e);
			final Document doc = comp.getDocument();
			final Element root = doc.getDefaultRootElement();
			final int pos0 = comp.getSelectionStart();
			final int pos1 = comp.getSelectionEnd();
			final int line0Index = root.getElementIndex(pos0);
			int line1Index = root.getElementIndex(pos1);
			if (root.getElement(line1Index).getStartOffset() >= pos1) { 
				// letzte Zeile ist nicht mit markiert
				// dann auslassen
				line1Index--;
				if (line1Index < line0Index) {
					line1Index = line0Index;
				} // damit eine einzelne Zeile auch behandelt werden kann
			}
			int lineStart;
			try {
				for (int i = line0Index; i <= line1Index; i++) {
					// Zeilenanfang ermitteln und dann Leerzeichen einfügen
					lineStart = root.getElement(i).getStartOffset();
					// ermitteln ob das Leerzeichen an die zweite Zeilen-
					// position eingesetzt werden kann, dann kann die
					// Neustrukturierung der Elemente wegfallen (Performance!)
					if (doc.getText(lineStart, 2).equals("--")) {
						// dann Space als zweites Zeichen einsetzen (ist
						// schneller)
						doc.remove(lineStart, 2);
					} else {
						// geht nicht, also als erstes Zeichen ensetzen
						doc.insertString(lineStart, "--", null);
					}
				}
			} catch (BadLocationException ble) {
				logger.error("ToggleLineComment:" + ble.getMessage(), ble);
			}
		}
	}

	public static class SelectCurrentLine extends TextAction {

		private static final long serialVersionUID = 1L;

		public SelectCurrentLine() {
			super("select-current-line");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent comp = getTextComponent(e);
			final Document doc = comp.getDocument();
			final Element root = doc.getDefaultRootElement();
			final int lineIndex = root.getElementIndex(comp.getCaret().getDot());
			final Element line = root.getElement(lineIndex);
			int start = line.getStartOffset();
			int end = line.getEndOffset();
			if (start < end) {
				comp.select(start, end);
			}
		}
	}

	public static class MoveBackTab extends TextAction {

		private static final long serialVersionUID = 1L;

		public MoveBackTab() {
			super("block-move-left-tab");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent comp = (JTextComponent) e.getSource();
			final Document doc = comp.getDocument();
			final int pos0 = comp.getSelectionStart();
			final int pos1 = comp.getSelectionEnd();
			if (pos0 != pos1) {
				// nur wenn etwas selektiert wurde ausrücken
				// das dient der Transparenz der Action, die
				// eigentlich auch ohne Selektion arbeitet
				final Element root = comp.getDocument().getDefaultRootElement();
				final int line0Index = root.getElementIndex(pos0);
				int line1Index = root.getElementIndex(pos1);
				if (root.getElement(line1Index).getStartOffset() >= pos1) { 
					// letzte Zeile ist nicht mit markiert
					// dann auslassen
					line1Index--;
				}
				int lineStart;
				try {
					for (int i = line0Index; i <= line1Index; i++) {
						for (int x = getTabSize(); x > 0;) {
							lineStart = root.getElement(i).getStartOffset();
							final char c = doc.getText(lineStart, 1).charAt(0);
							if ((c == ' ') || (c == '\t')) {
								// nur löschen wenn Leerraum
								doc.remove(lineStart, 1);
								if (c == ' ') {
									x--;
								} else {
									x = x - getTabSize();
								}
							} else {
								break;
							}
						}
					}
				} catch (BadLocationException ble) {
					logger.error("MoveBack: " + ble.getMessage(), ble);
				}
			}
		}
	}

	/**
	 * Einfüge-Action für TAB TAB wird in vordefinierte Anzahl von Space
	 * gewandelt
	 */
	public static class InsertTab extends TextAction {

		private static final long serialVersionUID = 1L;

		public InsertTab() {
			super("insert-tab-space");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent comp = (JTextComponent) e.getSource();
			final Document doc = comp.getDocument();
			final int pos0 = comp.getSelectionStart();
			final int pos1 = comp.getSelectionEnd();
			if (pos0 != pos1) {
				// nur wenn etwas selektiert wurde einrücken
				// das dient der Transparenz der Action, die
				// eigentlich auch ohne Selektion arbeitet
				final Element root = comp.getDocument().getDefaultRootElement();
				final int line0Index = root.getElementIndex(pos0);
				int line1Index = root.getElementIndex(pos1);
				if (root.getElement(line1Index).getStartOffset() >= pos1) { 
					// letzte Zeile ist nicht mit markiert
					// dann auslassen
					line1Index--;
				}
				int lineStart;
				try {
					for (int i = line0Index; i <= line1Index; i++) {
						// Zeilenanfang ermitteln und dann Leerzeichen einfügen
						lineStart = root.getElement(i).getStartOffset();
						// ermitteln ob das Leerzeichen an die zweite Zeilen-
						// position eingesetzt werden kann, dann kann die
						// Neustrukturierung der Elemente wegfallen
						// (Performance!)
						if (doc.getText(lineStart, 1).equals(" ")) {
							// dann Space als zweites Zeichen einsetzen (ist
							// schneller)
							doc.insertString(lineStart + 1, tabErsatz, null);
						} else {
							// geht nicht, also als erstes Zeichen ensetzen
							doc.insertString(lineStart, tabErsatz, null);
						}
					}
				} catch (BadLocationException ble) {
					logger.error("MoveOut: " + ble.getMessage(), ble);
				}
			} else {
				comp.replaceSelection(tabErsatz);
			}
		}
	}

	public static class Trim extends TextAction {

		private static final long serialVersionUID = 1L;

		public Trim() {
			super("trim");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			final Document doc = editor.getDocument();
			final Element rootElement = doc.getDefaultRootElement();
			final int elemCount = rootElement.getElementCount();
			Element elem = null;
			String lineText = null;
			char c;
			int i;
			int startOffset = 0; // bezogen auf das Document ! Beginn des zu
									// löschenden Bereiches
			int endOffset = 0; // bezogen auf das Document ! Ende des zu
								// löschenden Bereiches
			for (int ec = 0; ec < elemCount; ec++) {
				elem = rootElement.getElement(ec);
				try {
					lineText = doc.getText(elem.getStartOffset(),
							elem.getEndOffset() - elem.getStartOffset());
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
				// am Ende fast jeder Zeile ist ein \n
				c = lineText.charAt(lineText.length() - 1);
				if (c == '\n') {
					endOffset = elem.getEndOffset() - 1;
					i = lineText.length() - 2;
				} else {
					endOffset = elem.getEndOffset();
					i = lineText.length() - 1;
				}
				startOffset = endOffset;
				for (; i >= 0; i--) {
					c = lineText.charAt(i);
					if ((c == ' ') || (c == '\t') || (c == 0)) {
						startOffset--;
					} else {
						break;
					}
				}
				if (startOffset < endOffset) {
					try {
						doc.remove(startOffset, endOffset - startOffset);
					} catch (BadLocationException e2) {
						logger.error("Trim failed: " + e2.getMessage(), e2);
					}
				}
			}
		}
	}

	/**
	 * Textaction die den Textcursor an den Anfang der Zeile setzt.
	 */
	public static class GotoLineStart extends TextAction {

		private static final long serialVersionUID = 1L;

		public GotoLineStart() {
			super("goto-line-start");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			final Document doc = editor.getDocument();
			final Element elem = doc.getDefaultRootElement();
			// getElementIndex(..) bringt das Nachfolge-Element, welches
			// am ehesten die angebene TextPosition abdeckt
			// da ein Element mit einer Zeile korrespondiert
			// (nicht so in StyledDocument!!!!)
			// ist die Elementenummer = Zeilennummer
			final int lineNum = elem.getElementIndex(editor.getCaretPosition());
			final Element line = elem.getElement(lineNum);
			editor.setCaretPosition(line.getStartOffset());
		}
	}

	/**
	 * Textaction die den Text vom Zeilenbegin bis zur aktuellen position
	 * selektiert
	 */
	public static class SelectUntilLineStart extends TextAction {

		private static final long serialVersionUID = 1L;

		public SelectUntilLineStart() {
			super("select-until-line-start");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			final Document doc = editor.getDocument();
			final Element elem = doc.getDefaultRootElement();
			final int lineNum = elem.getElementIndex(editor.getCaretPosition());
			final Element line = elem.getElement(lineNum);
			// den Cursor verschieben hiesst ab der momentanen position
			// bis zum Ziel markieren !
			editor.moveCaretPosition(line.getStartOffset());
		}
	}

	/**
	 * Textaction die den Cursor an das Ende der Zeile setzt.
	 */
	public static class GotoLineEnd extends TextAction {

		private static final long serialVersionUID = 1L;

		public GotoLineEnd() {
			super("goto-line-end");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			final Document doc = editor.getDocument();
			final Element elem = doc.getDefaultRootElement();
			final int lineNum = elem.getElementIndex(editor.getCaretPosition());
			final Element line = elem.getElement(lineNum);
			if (editor.getCaretPosition() < line.getEndOffset()) {
				editor.setCaretPosition(line.getEndOffset() - 1);
			}
		}
	}

	/**
	 * Textaction die den Text bis zum Zeilenende selektiert.
	 */
	public static class SelectUntilLineEnd extends TextAction {

		private static final long serialVersionUID = 1L;

		public SelectUntilLineEnd() {
			super("select-until-line-end");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			final Document doc = editor.getDocument();
			final Element elem = doc.getDefaultRootElement();
			final int lineNum = elem.getElementIndex(editor.getCaretPosition());
			final Element line = elem.getElement(lineNum);
			if (editor.getCaretPosition() < line.getEndOffset()) {
				editor.moveCaretPosition(line.getEndOffset() - 1);
			}
		}
	}

	/**
	 * Textaktion die den Cursor an den Dokumentanfang setzt.
	 */
	public static class GotoDocTop extends TextAction {

		private static final long serialVersionUID = 1L;

		public GotoDocTop() {
			super("goto-doc-top");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			editor.setCaretPosition(0);
		}
	}

	/**
	 * Textaction die den Text von der aktuellen position bis zum Textanfang
	 * selektiert.
	 */
	public static class SelectUntilDocTop extends TextAction {

		private static final long serialVersionUID = 1L;

		public SelectUntilDocTop() {
			super("select-until-doc-top");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			editor.moveCaretPosition(0);
		}
	}

	/**
	 * Textaction die den Cursor an das Ende des Textes setzt.
	 */
	public static class GotoDocEnd extends TextAction {

		private static final long serialVersionUID = 1L;

		public GotoDocEnd() {
			super("goto-doc-end");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			final Document doc = editor.getDocument();
			editor.setCaretPosition(doc.getLength());
		}
	}

	/**
	 * Textaction die den Text von der aktuellen position bis zum Textende
	 * selektiert.
	 */
	public static class SelectUntilDocEnd extends TextAction {

		private static final long serialVersionUID = 1L;

		public SelectUntilDocEnd() {
			super("select-until-doc-end");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent editor = getTextComponent(e);
			final Document doc = editor.getDocument();
			editor.moveCaretPosition(doc.getLength());
		}
	}

	/**
	 * Standard-Textaction mit OverWrite-Unterstützung. abhängig vom Zustand des
	 * Flag overWrite wird der vor dem bestehenden Text eingefügt und ggf.
	 * danach gelöscht.
	 */
	public static class OverWriteDefaultKeyTyped extends TextAction {

		private static final long serialVersionUID = 1L;

		public OverWriteDefaultKeyTyped() {
			super("default-key-overwrite");
		}

		public void actionPerformed(ActionEvent e) {
			final ExtEditorPane target = (ExtEditorPane) getTextComponent(e);
			if ((target != null) && (e != null)) {
				final String content = e.getActionCommand();
				int mod = e.getModifiers();
				if ((content != null) 
						&& (content.length() > 0)
						&& ((mod & (ActionEvent.META_MASK | ActionEvent.CTRL_MASK)) == 0)) {
					final char c = content.charAt(0);
					if ((c >= 0x20) && (c != 0x7F)) {
						target.replaceSelection(content);
						final Document doc = target.getDocument();
						final int pos = target.getCaretPosition();
						// Zeichen nur löschen wenn etwas zu löschen da ist,
						// wenn überschreiben eingeschaltet
						// und wenn nicht Zeilenumbruch
						try {
							if ((pos < doc.getLength()) 
									&& target.isOverWrite()
									&& (doc.getText(pos, 1).charAt(0) != '\n')) {
								doc.remove(pos, 1);
							}
						} catch (BadLocationException ble) {
							logger.error("OverWriteDefaultKeyTyped: error at pos:"
									+ pos, ble);
						}
					}
				}
			}
		}
	}

	/**
	 * setzt den Status überschreiben/Einfügen und tauscht Cursorlayout
	 */
	public static class SetStatusOverWriteAction extends TextAction {

		private static final long serialVersionUID = 1L;

		public SetStatusOverWriteAction() {
			super("set-overwrite-status");
		}

		public void actionPerformed(ActionEvent e) {
			final ExtEditorPane comp = (ExtEditorPane) e.getSource();
			// Cursorposition merken, da neuer Caret an doc-Anfang gesetzt wird
			final int pos = comp.getCaretPosition();
			// nur den Zustand wechseln wenn kein text selectiert ist
			// do sonst der Hintergrund dauerhaft falsch dargestellt wird
			if (comp.getSelectionStart() == comp.getSelectionEnd()) {
				if (comp.isOverWrite()) {
					comp.setOverWrite(false);
					comp.getMainFrame().status.setTextModeOverWrite(false);
				} else {
					comp.setOverWrite(true);
					comp.getMainFrame().status.setTextModeOverWrite(true);
				}
				comp.setCaretPosition(pos); // Caretposition restaurieren
				comp.repaint(); // neuzeichnen
			}
		}
	}

	/**
	 * Blockverschiebeaktion nach rechts Vor allen Zeilen, die sich im
	 * selektierten bereich befinden (unabhängig davon, ob die zeile vollstÄndig
	 * markiert ist) wird ein Leerzeichen gesetzt, soweit Möglich an die zweite
	 * position in der Zeile um eine Elemente-Reorganisation zu vermeiden.
	 */
	public static class MoveOut extends TextAction {

		private static final long serialVersionUID = 1L;

		public MoveOut() {
			super("block-move-right");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent comp = (JTextComponent) e.getSource();
			final Document doc = comp.getDocument();
			final int pos0 = comp.getSelectionStart();
			final int pos1 = comp.getSelectionEnd();
			if (pos0 != pos1) {
				// nur wenn etwas selektiert wurde einrücken
				// das dient der Transparenz der Action, die
				// eigentlich auch ohne Selektion arbeitet
				final Element root = comp.getDocument().getDefaultRootElement();
				final int line0Index = root.getElementIndex(pos0);
				int line1Index = root.getElementIndex(pos1);
				if (root.getElement(line1Index).getStartOffset() >= pos1) { 
					// letzte Zeile ist nicht mit markiert
					// dann auslassen
					line1Index--;
				}
				int lineStart;
				try {
					for (int i = line0Index; i <= line1Index; i++) {
						// Zeilenanfang ermitteln und dann Leerzeichen einfügen
						lineStart = root.getElement(i).getStartOffset();
						// ermitteln ob das Leerzeichen an die zweite Zeilen-
						// position eingesetzt werden kann, dann kann die
						// Neustrukturierung der Elemente wegfallen
						// (Performance!)
						if (doc.getText(lineStart, 1).equals(" ")) {
							// dann Space als zweites Zeichen einsetzen (ist
							// schneller)
							doc.insertString(lineStart + 1, " ", null);
						} else {
							// geht nicht, also als erstes Zeichen einsetzen
							doc.insertString(lineStart, " ", null);
						}
					}
				} catch (BadLocationException ble) {
					logger.error("ExtEditorKit.MoveOut: error in doc", ble);
				}
			}
		}
	}

	/**
	 * Blockverschiebeaktion nach links, solange wie es whitespace-Zeichen vor
	 * der Zeile noch gibt
	 */
	public static class MoveBack extends TextAction {

		private static final long serialVersionUID = 1L;

		public MoveBack() {
			super("block-move-left");
		}

		public void actionPerformed(ActionEvent e) {
			final JTextComponent comp = (JTextComponent) e.getSource();
			final Document doc = comp.getDocument();
			final int pos0 = comp.getSelectionStart();
			final int pos1 = comp.getSelectionEnd();
			if (pos0 != pos1) {
				// nur wenn etwas selektiert wurde ausrücken
				// das dient der Transparenz der Action, die
				// eigentlich auch ohne Selektion arbeitet
				final Element root = comp.getDocument().getDefaultRootElement();
				final int line0Index = root.getElementIndex(pos0);
				int line1Index = root.getElementIndex(pos1);
				if (root.getElement(line1Index).getStartOffset() >= pos1) { 
					// letzte Zeile ist nicht mit markiert
					// dann auslassen
					line1Index--;
				}
				int lineStart;
				try {
					for (int i = line0Index; i <= line1Index; i++) {
						// Zeilenanfang ermitteln und dann Leerzeichen einfügen
						lineStart = root.getElement(i).getStartOffset();
						final char c = doc.getText(lineStart, 1).charAt(0);
						if ((c == ' ') || (c == '\t')) {
							// nur löschen wenn Leerraum
							doc.remove(lineStart, 1);
						}
					}
				} catch (BadLocationException ble) {
					logger.error("ExtEditorKit.MoveBack: error in doc", ble);
				}
			}
		}
	}
	
	// --- ExtEditorKit methods
	// ------------------------------------------------------

    @Override
	public String getContentType() {
		return "text/SQL";
	}

    @Override
	public Document createDefaultDocument() {
		return new SyntaxDocument(mainFrame.getSyntaxScanner());
	}

    @Override
	public final ViewFactory getViewFactory() {
		if (preferences == null) {
			preferences = new SyntaxContext(mainFrame.getSyntaxScanner());
		}
		return preferences;
	}

	private static void setTabSize(int tabSize) {
		ExtEditorKit.tabSize = tabSize;
	}

	private static int getTabSize() {
		return tabSize;
	}

}
