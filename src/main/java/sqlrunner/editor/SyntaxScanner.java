package sqlrunner.editor;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;

import org.apache.log4j.Logger;

import sqlrunner.Main;

/**
 * Klasse enthält Methoden zu Erfassung von Schlüsselwörtern sowie
 * Stringkonstaten und mehrzeiligen Kommentaren die dazugehörigen Fonts und
 * Farben werden als Ergebnis geliefert für die Kommentarbehandlung werden
 * Textattribute gesetzt
 */
public final class SyntaxScanner {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(SyntaxScanner.class.getName());

	private Document doc;

	private int startPos;

	private int endPos;

	private int startOffset = 0;

	private int endOffset = 0;

	static String blockCommentBegin = "/*"; // wird durch Lesen der Konfigfiles überschrieben

	static String blockCommentEnd = "*/"; // wird durch Lesen der Konfigfiles überschrieben

	static String lineComment = "--"; // wird durch Lesen der Konfigfiles überschrieben

	static char stringLimiter = '\''; // wird durch Lesen der Konfigfiles überschrieben

	private boolean inStringConstants;

	private boolean inLineComment;

	private boolean inBlockComment;

	private boolean inVariable;

	private Hashtable<String, DisplayAttribute> defs;

	private Hashtable<String, DisplayAttribute> defsForAdds;

	private Properties sections;

	private String word;

	// Darstellungsattribute
	private DisplayAttribute da;

	private DisplayAttribute commentDA;

	private DisplayAttribute stringConstantsDA;

	private DisplayAttribute findWordDA = new DisplayAttribute(Color.BLACK, Color.YELLOW, Main.textFont);;
	private String fontType = null;

	private int fontSize = 0;

	private Element root;

	private final Object commentAttribute = new AttributeFlag();

	private JTextComponent editor;

	private boolean valid = true;

	public boolean docLoaded = false;

	private String languageName;

	private String highlightedWord = null;

	public SyntaxScanner() {
		// laden der Voreinstellungen
		defs = new Hashtable<String, DisplayAttribute>();
		sections = new Properties();
		defineFontMetrics();
		languageName = Main.getDefaultProperty("WORDFILE_LANGUAGE_NAME", "SQL");
		if (loadSectionCfg()) {
			loadWordFile();
		}
	}
	
	public void setupChangedTextFont() {
		defineFontMetrics();
		for (DisplayAttribute da : defs.values()) {
			da.font = changeFontSize(da.font, fontSize);
		}
		commentDA.font = changeFontSize(commentDA.font, fontSize);
		findWordDA.font = Main.textFont;
	}
	
	private Font changeFontSize(Font font, int newSize) {
		return new Font(font.getName(), font.getStyle(), newSize);
	}

	private void defineFontMetrics() {
		fontType = Main.textFont.getName();
		fontSize = Main.textFont.getSize();
	}
	
	// ----- Methoden zur Initialisierung des Scanners
	// --------------------------------

	private String getLanguageProperty(String line, String propName) {
		int p0 = line.indexOf(propName);
		final StringBuffer value = new StringBuffer();
		if (p0 > 0) {
			// das = finden
			p0 = line.indexOf('=', p0);
			if (p0 > 0) {
				value.append("");
				boolean reading = false;
				for (int i = p0 + 1; i < line.length(); i++) {
					if (line.charAt(i) != ' ') {
						reading = true;
						value.append(line.charAt(i));
					} else if (reading) {
						// fertig, da nun nachfolgendes Leerzeichen kommt
						break;
					}
				}
			}
		}
		if (value.length() > 0) {
			return value.toString();
		} else {
			return null;
		}
	}

	private boolean loadSectionCfg() {
		boolean ok;
		try {
			try {
				final FileInputStream fis = new FileInputStream(Main.getHighlighterFontCfgFileName());
				sections.load(fis);
				fis.close();
				ok = true;
			} catch (java.security.AccessControlException ae) {
				// wenn nicht vom File erlaubt, dann aus den Archiv direckt
				// laden
				final InputStream is = (getClass()).getResourceAsStream("/"
						+ Main.HIGHLIGHTER_FONT_CFG_FILE);
				sections.load(is);
				is.close();
				ok = true;
			}
		} catch (IOException ioe) {
			logger.warn("Scanner.loadSectionCfg: error reading description for highlighter - : exception: " + ioe);
			disableScanner();
			ok = false;
		}
		return ok;
	}

	private void loadWordFile() {
		boolean inLanguage = false;
		if (logger.isDebugEnabled()) {
			logger.debug("Scanner: load configuration from " + Main.KEYWORD_FILE + " ...");
		}
		// Konfiguration aus Datei lesen
		final BufferedReader br = Main.getTextResource(Main.KEYWORD_FILE);
		if (br == null) {
			disableScanner();
		} else {
			try {
				// Zeilen lesen und parsen, dabei Kommentare auslassen
				String line;
				da = null;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					// Achtung Fallen:
					// 1. JDK1.1.8 bringt hier das Zeilenende mit in den String!
					// 2. Leerzeichen bringen beim parsen Probleme !!
					// Lösung durch trim ! entfernt alle whitespace (auch cr!)
					// am Anfang und Ende des Strings, aber nicht innerhalb des
					// Strings
					// Kommentare ausfiltern
					if (line.length() > 0) { // Leerzeilen auslassen
						if (inLanguage) {
							if (line.startsWith("/L")) {
								inLanguage = false;
								break; // while-Schleife abbrechen
							} else if (line.startsWith("/C")) {
								da = parseAttributeLine(sections
										.getProperty(languageName + "_"
												+ line.substring(1, 3)));
							} else if ((!line.startsWith("/")) && (da != null)) { // keine andereren Konfigurationszeilen erlaubt
								// nun diese Zeilen mit einem StringTokenizer
								// zerlegen und einsetzen
								final StringTokenizer st = new StringTokenizer(
										line, " ");
								while (st.hasMoreTokens()) {
									defs.put(st.nextToken(), da);
								}
							}
						} else if (line.startsWith("/L") && (line.toUpperCase().indexOf(languageName.toUpperCase()) != -1)) { // Konfigurationsdaten
							// richtige Sektion gefunden, dass merken
							// nun Zusatzinformationen einsammeln
							lineComment = getLanguageProperty(line,
									"Line Comment");
							blockCommentBegin = getLanguageProperty(line,
									"Block Comment On");
							blockCommentEnd = getLanguageProperty(line,
									"Block Comment Off");
							stringLimiter = (getLanguageProperty(line,
									"String Chars")).charAt(0);
							inLanguage = true;
							commentDA = parseAttributeLine(sections.getProperty(languageName + "_Comments"));
							stringConstantsDA = parseAttributeLine(sections.getProperty(languageName + "_String_Constants"));
						}
					} // if (line.length() > 0)
				} // while ((line = br.readLine()) != null)
				br.close();
			} catch (NullPointerException npe) {
				disableScanner();
				da = null;
				logger.warn("Scanner not initialized "
								+ npe.toString() + "exception: " + npe);
			} catch (IOException ioe) {
				disableScanner();
				da = null;
				logger.warn("Scanner not initialized "
								+ ioe.toString() + "exception: " + ioe);
			}
			if (logger.isDebugEnabled()) {
				// auslesen der defs
				final Enumeration<String> defsEnum = defs.keys();
				logger.debug("Scanner.loadWordFile:");
				while (defsEnum.hasMoreElements()) {
					final String alias = defsEnum.nextElement();
					final DisplayAttribute test = defs.get(alias);
					logger.debug("    " + alias + ":    TextAttr="
							+ test.font.getStyle() + "    Color="
							+ test.foregroundColor);
				}
			}
		}
	}

	/**
	 * keywords muss eine Liste der Keywords enthalten die Kommagetrennt
	 * aufeinanderfolgen Funktion arbeitet nur, wenn das Property <language>_ADD
	 * gesetzt ist.
	 * 
	 * @param keywords
	 *            Liste der Wörter
	 */
	public void addAdditionalKeywords(String keywords) {
		da = parseAttributeLine(sections.getProperty(
				languageName
				+ "_ADDED_KEYWORDS"));
		if (da != null) {
			if (defsForAdds == null) {
				defsForAdds = new Hashtable<String, DisplayAttribute>();
			}
			if (keywords != null) {
				final StringTokenizer tk = new StringTokenizer(keywords.toLowerCase(), ",");
				while (tk.hasMoreTokens()) {
					defsForAdds.put(tk.nextToken().trim(), da);
				}
			} else {
				defsForAdds = null;
			}
		}
	}
	
	public void deregisterAdditionalKeywords() {
		defsForAdds = null;
	}
	
	/**
	 * keywords muss eine Liste der Keywords enthalten die Kommagetrennt
	 * aufeinanderfolgen Funktion arbeitet nur, wenn das Property <language>_ADD
	 * gesetzt ist.
	 * 
	 * @param keywords
	 *            Liste der Wörter
	 */
	public void addAdditionalKeywords(List<String> keywords) {
		da = parseAttributeLine(sections.getProperty(languageName
				+ "_ADDED_KEYWORDS"));
		if (da != null) {
			if (defsForAdds == null) {
				defsForAdds = new Hashtable<String, DisplayAttribute>();
			}
			for (String word : keywords) {
				defsForAdds.put(word, da);
			}
		}
	}

	public void addAdditionalSQLDataTypes(List<String> dataTypes) {
		da = parseAttributeLine(sections.getProperty(languageName
				+ "_C2"));
		if (da != null) {
			if (defsForAdds == null) {
				defsForAdds = new Hashtable<String, DisplayAttribute>();
			}
			for (String word : dataTypes) {
				defsForAdds.put(word, da);
			}
		}
	}

	public void addAdditionalPLSQLKeywords(List<String> dataTypes) {
		da = parseAttributeLine(sections.getProperty(languageName
				+ "_C3"));
		if (da != null) {
			if (defsForAdds == null) {
				defsForAdds = new Hashtable<String, DisplayAttribute>();
			}
			for (String word : dataTypes) {
				defsForAdds.put(word, da);
			}
		}
	}

	public List<String> getLongKeywords() {
		List<String> list = new ArrayList<String>();
		if (defs != null) {
			for (String keyword : defs.keySet()) {
				if (list.contains(keyword) == false && keyword.length() > 2) {
					list.add(keyword.toLowerCase());
				}
			}
		}
		if (defsForAdds != null) {
			for (String keyword : defsForAdds.keySet()) {
				if (list.contains(keyword) == false && keyword.length() > 2) {
					list.add(keyword.toLowerCase());
				}
			}
		}
		Collections.sort(list);
		return list;
	}

	/**
	 * Scanner ein/aus-schalten
	 * 
	 * @param enabled
	 *            true=Scanner ein, false=Scanner aus
	 */
	public void setEnabled(boolean enabled) {
		SyntaxContext.setSyntaxHighlighting(enabled); // farbige Darstellung ein/aus
		this.valid = enabled;
	}

	public boolean isEnabled() {
		return SyntaxContext.isSyntaxHighlightingEnabled() && valid;
	}

	private void disableScanner() {
		if (logger.isDebugEnabled()) {
			logger.debug("SyntaxHighlighting disabled !");
		}
		Main.setDefaultProperty("SYNTAX_HIGHLIGHT_ENABLED", "false");
		Main.setUserProperty("SYNTAX_HIGHLIGHT", "false");
		setEnabled(false);
	}

	/**
	 * Dokument zuweisen, da zum Zeitpunkt des Konstruktors noch nicht bekannt
	 * 
	 * @param doc
	 *            aktuelles Textdokument
	 */
	public void setDocument(Document doc_loc) {
		this.doc = doc_loc;
		root = this.doc.getDefaultRootElement();
	}

    public Document getDocument() {
        return doc;
    }

	/**
	 * gibt dem Scanner die aktuelle Textkomponente bekannt
	 * 
	 * @param editor
	 *            Textkomponente
	 */
	public void setEditor(JTextComponent editor_loc) {
		this.editor = editor_loc;
	}

	/**
	 * zerlegt die im Konfigurationsfile enthaltene Syntax für
	 * Darstellungsattribute
	 * 
	 * @param Zeile
	 *            des Konfig-Files
	 * @return DisplayAttribute (innere Klasse), welches die Darstellung
	 *         beschreibt
	 */
	private DisplayAttribute parseAttributeLine(String aLine) {
		// parst die Zeile mit den DisplayAttributen
		final int i1;
		final int i2;
		final int i3;
		final int rot;
		final int gruen;
		final int blau;
		DisplayAttribute da_loc = null;
		if (aLine != null) {
			da_loc = new DisplayAttribute();
			try {
				// erstes Komma suchen und den String bis dahin als x-top
				// speichern
				i1 = aLine.indexOf(','); // 1. Komma finden
				da_loc.font = new Font(fontType, Integer.parseInt(aLine
						.substring(0, i1)), fontSize);
				i2 = aLine.indexOf(',', i1 + 1); // 2. Komma finden
				rot = Integer.parseInt(aLine.substring(i1 + 1, i2)); // rot separieren
				i3 = aLine.indexOf(',', i2 + 1); // 3. Komma finden
				gruen = Integer.parseInt(aLine.substring(i2 + 1, i3)); // gruen separieren
				blau = Integer.parseInt(aLine.substring(i3 + 1, aLine.length())); // blau separieren
				da_loc.foregroundColor = new Color(rot, gruen, blau);
			} catch (NumberFormatException e) {
				logger.warn("Scanner.parseAttributes: invalid value in config file line:"
								+ aLine
								+ " scanner deactivated "
								+ e);
				Main.setDefaultProperty("SYNTAX_HIGHLIGHT_ENABLED", "false");
				da_loc = null;
			} catch (StringIndexOutOfBoundsException e) {
				logger.warn("Scanner.parseAttributes: not enough values in config file line:"
								+ aLine
								+ " scanner deactivated "
								+ e);
				Main.setDefaultProperty("SYNTAX_HIGHLIGHT_ENABLED", "false");
				da_loc = null;
			}
		}
		return da_loc;
	}

	// ---- Methoden für die Darstellung des Textes aufgerufen aus SQLContext
	// --------------------

	/**
	 * Zusammenfassung der Attribute für die Textdarstellung in einem Objekt
	 */
	static class DisplayAttribute {
		
		public Font font; // = new Font(Schriftart,0,Schriftgroesse);

		public Color foregroundColor = Color.black;

		public Color backgroundColor = null;

		public DisplayAttribute() {
		}

		public DisplayAttribute(Color foreground, Color background, Font font) {
            this.foregroundColor = foreground;
            this.backgroundColor = background;
            this.font = font;
		}

        @Override
		public String toString() {
			return "DisplayAttribut: " + font.toString() + " "
					+ foregroundColor.toString();
		}
	}

	/**
	 * Scannbereich festlegen (erfolgt aus SQLContext heraus)
	 */
	public void setRange(int start, int ende) {
		// scanner anhalten
		startPos = start;
		endPos = ende;
	}

	/**
	 * gibt den Staroffeset der ersten lexikalischen Einheit innerhalb des
	 * Scannbereiches zurück
	 * 
	 * @return position des ersten Zeichen der ersten lexikalischen Einheit
	 *         relativ zum Startoffeset
	 * @see setRange()
	 */
	public int getStartOffset() {
		// hier den Startpunkt der aktuellen lexikalischen Einheit angeben
		return startOffset;
	}

	/**
	 * gibt den Offset für das Ende der ersten lexikalischen Einheit innerhalb
	 * des Scanbvereiches zurück
	 * 
	 * @return Offest des Ende relativ zum Startoffset
	 * @see setRange()
	 */
	public int getEndOffset() {
		// hier das Ende der aktuellen lexikalischen Einheit angeben
		// wenn nichts gefunden wird hier der Endebereich des zu scannend
		// Bereiches angegeben
		return ((endOffset <= endPos) ? endOffset : endPos);
	}

	/**
	 * gibt den für die aktuell gefundene lexikalische Einheit zu verwendenden
	 * Font zurück
	 * 
	 * @return in SQLContext zu nutzender Font für die Textauszeichnung
	 */
	public Font getFont() {
		return da.font;
	}

	/**
	 * gibt den für die aktuell gefundene lexikalische Einheit zu verwendende
	 * Farbe zurück
	 * 
	 * @return in SQLContext zu nutzende Farbe für die Textauszeichnung
	 */
	public Color getColor() {
		return da.foregroundColor;
	}

	public Color getBackgroundColor() {
		if (da != null) {
			return da.backgroundColor;
		} else {
			return null;
		}
	}

	public boolean testIfCommentBegin(String s, int pos) {
		final int iline = s.indexOf(lineComment);
		if ((iline != -1) && (iline == pos - 1)) {
			inLineComment = true;
			inBlockComment = false;
			return true;
		} else if ((s.indexOf(blockCommentBegin) != -1)
				&& (s.indexOf(blockCommentBegin) == pos - 1)) {
			inLineComment = false;
			inBlockComment = true;
			return true;
		} else {
			inLineComment = false;
			inBlockComment = false;
			return false;
		}
	}

	public boolean testIfBlockCommentEnd(String s, int pos) {
		// beide Vergleiche sind erforderlich, da bei pos=0 sonst ein true
		// rauskommen würde!
		if ((s.indexOf(blockCommentEnd) != -1)
				&& (s.indexOf(blockCommentEnd) == pos - 1)) {
			inLineComment = false;
			inBlockComment = true;
			return true;
		} else {
			inLineComment = false;
			inBlockComment = false;
			return false;
		}
	}

	/**
	 * scannt den mit setRange() begrenzten Textbereich von doc.
	 * 
	 * @return true wenn hervorzuhebender Text gefunden
	 * @see setRange(), setDocument()
	 */
	public boolean scanLine() {
		try {
			// hier der eigentliche Scannvorgang
			// wenn Zeile innerhalb eines Kommentares,
			// dann nach dem Ende des Kommentares suchen
			// und bis dahin die Kommentar-Darstellungsattribute einstellen
			final Element elem = root.getElement(root.getElementIndex(startPos));
			final MutableAttributeSet mas = (MutableAttributeSet) elem.getAttributes();
			// das Attribute comment ist nur für den scannvorgang am Begin der
			// Zeile
			// gültig, mitten in der Zeile kann der Kommentar aufhören.
			// der erste scan in der zeile geht bei gesetztem Attribute bis zum
			// Ende der zeile,
			// daher muss der nächste scan von normalen Text ausgehen
			if ((elem.getStartOffset() == startPos) && (mas != null) && mas.isDefined(commentAttribute)) {
				startOffset = startPos; // Anfang ist gleich lexikalischer Beginn
				// jetzt nach dem Ende suchen
				final int e = doc.getText(startPos, endPos - startPos).indexOf("*/");
				if (e != -1) {
					// Ende gefunden
					da = commentDA;
					startOffset = startPos;
					endOffset = (startPos + e) + 2;
				} else {
					// komplette Zeile im Kommentar
					da = commentDA;
					startOffset = startPos;
					endOffset = endPos;
				}
			} else {
				// hier weiter wenn Zeile zu scannen
				int a;
				int e;
				final String s = doc.getText(startPos, endPos - startPos).toLowerCase();
				// unterscheiden was kommt
				// Anfang finden
				char c0 = ' ';
				char c;
				inStringConstants = false;
				inLineComment = false;
				inBlockComment = false;
				inVariable = false;
				for (a = 0; a < s.length(); a++) {
					c = s.charAt(a);
					// Test ob String-Konstante
					if (a > 0) {
						c0 = s.charAt(a - 1);
					}
					if (c == stringLimiter && c0 != '\\') {
						inStringConstants = true;
						break; // for-Schleife abbrechen
					} else {
						// Test ob Kommentar
						if (testIfCommentBegin(s, a)) {
							// Flags werden in der Methode oben gesetzt
							break; // for-Schleife abbrechen
						} else {
							if (c == '\"' || c == '`') {
								inVariable = true;
								break;
							} else {
								// Test ob Wort-Anfang
								if (((c >= 'A') && (c <= 'Z')) 
										|| ((c >= 'a') && (c <= 'z')) 
										|| (c == '_')
										|| (c == '\n')) {
									break; // for-Schleife abbrechen sobald
											// Buchstabe in c
								} // if ((((c >= 'A')...
							} // if (c == '\"')
						} // if (testOfCommentBegin(s, a))
					} // if (c == stringLimiter)
				} // for (a = 0; a < s.length(); a++)
				// Ende finden
				// aber von was ?
				// das Ende eines Bezeichners
				if (inVariable) {
					// Suche nach dem Ende des Kommentares
					for (e = a + 1; e < s.length(); e++) {
						c = s.charAt(e);
						if ((c == '\"')  || c == '`' || (c == '\n')) {
							break;
						}
					}
					startOffset = a + startPos;
					endOffset = (e + 1) + startPos;
					da = null;
					word = s.substring(a + 1, e);
					// Wort in defs suchen
					// und das zugeordnete Displayattribute laden
					if (highlightedWord != null) {
						// das word testen ob es mit dem aktuellen
						// Suchwort übereinstimmt
						if (word.equalsIgnoreCase(highlightedWord)) {
							da = findWordDA;
						}
					}
				} else {
					// Ende vom Zeilen-Kommentar
					if (inLineComment) {
						// Suche nach dem Ende der Zeile da
						// Inline-Kommentare ermöglichen nachfolgend keinen
						// Inhalt in der gleichen Zeile
						for (e = a + 1; e < s.length(); e++) {
							c = s.charAt(e);
							if (c == '\n') {
								break;
							}
						}
						da = commentDA;
						startOffset = (a + startPos) - 1;
						endOffset = (e + 1) + startPos;
					} else if (inBlockComment) {
						// bei Block-Comments ist danach in der Zeile weiterer
						// Inhalt möglich !!
						for (e = a + 1; e < s.length(); e++) {
							if (testIfBlockCommentEnd(s, e)) {
								// die Flags werden in der Methode entsprechend
								// gesetzt
								break;
							}
						}
						da = commentDA;
						startOffset = (a + startPos) - 1;
						endOffset = (e + 1) + startPos;
					} else {
						// Ende einer String-Konstante behandeln
						if (inStringConstants) {
							// Suche nach dem Ende der String-Konstanten
							for (e = a + 1; e < s.length(); e++) {
								c = s.charAt(e);
								if (e > 0) {
									c0 = s.charAt(e - 1);
								}
								if ((c == stringLimiter && c0 != '\\') || (c == '\n')) {
									break;
								}
							}
							da = stringConstantsDA;
							startOffset = a + startPos;
							endOffset = (e + 1) + startPos;
						} else {
							// Suche nach dem Ende eines Wortes
							for (e = a; e < s.length(); e++) {
								c = s.charAt(e);
								if (((((c >= 'A') && (c <= 'Z')) 
										|| ((c >= 'a') && (c <= 'z')) 
										|| (c == '_') 
										|| ((c >= '0') && (c <= '9'))) == false)
										|| (c == '\n')) {
									break;
								}
							}
							if (e > a) {
								word = s.substring(a, e);
								// Wort in defs suchen
								// und das zugeordnete Displayattribute laden
								da = defs.get(word);
								if ((da == null) && (defsForAdds != null)) {
									da = defsForAdds.get(word);
								}
								if (da == null && highlightedWord != null) {
									// das word testen ob es mit dem aktuellen
									// Suchwort übereinstimmt
									if (word.equalsIgnoreCase(highlightedWord)) {
										da = findWordDA;
									}
								}
								// wenn das Wort kein Keyword, dann bleibt da ==
								// null
								// wonach sich auch der Rückgabewert der Methode
								// richtet
                            } else {
								da = null;
								e++; // wenn nichts gefunden wurde muss der
										// Zeiger trotzdem eins weiter
							}
							startOffset = a + startPos;
							endOffset = e + startPos;
						} // if (inStringConstants)
					} // if (inLineComment) ... else if ... else ...
				} // if (inVariable) ... else ...
			} // if ((elem.getStartOffset() == startPos) && (mas != null)
		} catch (BadLocationException ble) {
			logger.warn("Scanner.scan(): error in startPos: "
							+ startPos
							+ " endPos: "
							+ endPos
							+ " Scanner: deaktivated: " + ble);
			Main.setUserProperty("SYNTAX_HIGHLIGHT", "false");
			SyntaxContext.setSyntaxHighlighting(false);
			da = null;
		}
		// wenn gefunden dann ok setzen
		if (da != null) {
			return true; // was gefunden
		} else {
			return false; // nichts gefunden
		}
	}

	// --- Methoden und Klassen für Kennzeichnung der Kommentare aufgerufen aus
	// SQLDocument ----

	/**
	 * Objekttyp für die Nutzung als Attribute zur Kennzeichnung von Kommentaren
	 * wird in scanLine() nur auf null geprüft, daher keine eigenen Werte
	 * 
	 * @see scanLine()
	 */
	static class AttributeFlag {

        @Override
		public String toString() {
			return "comment";
		}
	}

	/**
	 * ermittelt den Anfang des Kommentares in s der in nachfolgenden Zeilen
	 * fortgesetzt wird beachtet dabei die StringLiterale
	 * 
	 * @param s
	 *            Textfragment
	 */
	private final int indexOfMultiLineCommentBegin(String s) {
		inStringConstants = false;
		inVariable = false;
		int i = 0;
		int pos = -1;
		for (; i < s.length(); i++) {
			if (s.charAt(i) == stringLimiter) {
				// testen ob StringConstante zu ende
				if (!inStringConstants) {
					inStringConstants = true;
				} else {
					inStringConstants = false;
				}
			}
			if (s.charAt(i) == '\"') {
				// testen ob StringConstante zu ende
				if (!inVariable) {
					inVariable = true;
				} else {
					inVariable = false;
				}
			}
			if ((!inStringConstants) && (!inVariable)) {
				if ((s.charAt(i) == blockCommentBegin.charAt(0))
						&& (s.charAt(++i) == blockCommentBegin.charAt(1))) {
					// Kommentar gefunden
					pos = i;
				} else {
					// testen ob noch ein Kommentarende vorhanden in Zeile
					// dann Entwarnung
					if ((s.charAt(i) == blockCommentEnd.charAt(0))
							&& (s.charAt(++i) == blockCommentEnd.charAt(1))) {
						pos = -1;
					}
				} // if ((s.charAt(i) == blockCommentBegin.charAt(0)) &&
					// (s.charAt(++i) == blockCommentBegin.charAt(1)))
			} // if (!inStringConstants &&!inVariable)
		} // for (; i < s.length(); i++)
		return pos;
	}

	private final int setCommentAttributes(int textStartPos) {
		// startPos ist die Doc-position ab der Kommentar anfängt
		// verfolgt ab der gegebenen TextPos. alle
		// nachfolgenden Elemente und findet Ende des Kommentares
		// versieht diese Elemente mit den comment-Attribute
		// wo ist denn nun das nächst gelegene Ende des Kommentares ?
		int endPos_loc = 0;
		try {
			endPos_loc = (doc.getText(textStartPos,	(doc.getLength() - textStartPos))).indexOf(blockCommentEnd);
			if (endPos_loc != -1) {
				endPos_loc += textStartPos;
			} else {
				endPos_loc = doc.getLength();
			}
		} catch (BadLocationException ble) {
			logger.warn("error in handling comments " + ble);
		}
		final Element root_loc = doc.getDefaultRootElement();
		final int elemStartIndex = root_loc.getElementIndex(textStartPos);
		final int elemEndIndex = root_loc.getElementIndex(endPos_loc);
		Element elem;
		// erste Zeile wird nicht mit Attributen versehen
		// Attribute bedeutet, dass vorhergehende
		// Zeile mit Kommentar endet !
		for (int i = elemStartIndex + 1; i <= elemEndIndex; i++) {
			elem = root_loc.getElement(i);
			final MutableAttributeSet mattr = (MutableAttributeSet) elem.getAttributes();
			mattr.addAttribute(commentAttribute, commentAttribute);
		}
		return endPos_loc;
	}

	private final String readWholeLine(int pos) {
		final Element root_loc = doc.getDefaultRootElement();
		final Element elem = root_loc.getElement(root_loc.getElementIndex(pos));
		final int p0 = elem.getStartOffset();
		final int p1 = elem.getEndOffset();
		String s = "";
		try {
			s = doc.getText(p0, p1 - p0);
		} catch (BadLocationException e) {
			logger.warn(e.getMessage(), e);
		}
		return s;
	}

	/*
	 * löscht alle Attribute in einem Bereich definiert vom Begin der
	 * Änderungen bis zum Ende des nächstliegenden Kommentares
	 */
	private final int removeAttributes(int startPos_loc) {
		// finde den Kommentaranfang
		final Element root_loc = doc.getDefaultRootElement();
		final int elemStartIndex = root_loc.getElementIndex(startPos_loc);
		// das Finden der EndPosition funktioniert nicht sicher, er findet
		// trotzdem das eigentlich schon gelöschte Zeichen
		int endPos_loc = 0;
		try {
			endPos_loc = (doc.getText(startPos_loc, doc.getLength()
					- startPos_loc)).indexOf(blockCommentBegin);
			if (endPos_loc == -1) {
				endPos_loc = doc.getLength();
			} else {
				endPos_loc += startPos_loc; // Nullpunktkorrektur
			}
		} catch (BadLocationException e) {
			logger.warn(e.getMessage(), e);
		}
		final int elemEndIndex = root_loc.getElementIndex(endPos_loc);
		Element elem = root_loc; // Dummyzuweisung
		// erste Zeile wird nicht mit Attributen versehen
		// Attribute bedeutet, dass der Kommentar in der vorhergehenden
		// Zeile nicht endet und demnach in der darauffolgenden Zeile weitergeht
		// für scanLine an Hand des Zeileninhalts nicht zu ermitteln,
		// daher die Attribute
		for (int i = elemStartIndex; i <= elemEndIndex; i++) {
			elem = root_loc.getElement(i);
			final MutableAttributeSet set = (MutableAttributeSet) elem.getAttributes();
			set.removeAttribute(commentAttribute);
		}
		// Achtung, das Ende des letzten Elements liegt merkwuerdiger
		// Weise ab und an ausserhalb der Laenge :-(
		return Math.min(elem.getEndOffset(), doc.getLength());
	}

	/**
	 * ermittelt das Ende des letzten Kommentares
	 * 
	 * @param startPos
	 *            position bis zu der der Kommentar gefunden werden soll
	 * @return position
	 */
	private final int findLastCommentBegin(int startPos_loc) {
		int lastPos = 0;
		int suchPos = startPos_loc;
		boolean gefunden = false;
		String s = "";
		try {
			s = doc.getText(0, startPos_loc);
		} catch (BadLocationException e) {
			logger.warn(e.getMessage(), e);
		}
		while (!gefunden) {
			lastPos = s.lastIndexOf(blockCommentBegin, suchPos);
			if (lastPos != -1) {
				if (indexOfMultiLineCommentBegin(readWholeLine(lastPos)) != -1) {
					// wenn das tatsächlich der letzte Kommentarbeginn ist dann ist
					// dessen position der passende Beginn für das Setzen der
					// Attribute
					gefunden = true;
				} else {
					suchPos = lastPos - 1;
				}
			} else {
				lastPos = startPos_loc;
				gefunden = true;
			}
		}
		if (lastPos > 0) {
			lastPos--;
		}
		return lastPos;
	}

	/**
	 * setzt für einen Textbereich die Kommentarattribute
	 * 
	 * @param startPos
	 *            Startoffset ab Textbeginn im Textdokument
	 * @param length
	 *            Länge des zu untersuchenden Textbereiches
	 */
	public void updateCommentAttributes(int startPos_loc, int length) {
		if (valid) {
			if (logger.isDebugEnabled()) {
				logger.debug("start at:" + startPos_loc + " until:"
						+ ((startPos_loc + length) - 1) + "...");
			}
			// Attribute ab der aktuelle position bis zum Anfang des nächsten
			// Komentares löschen
			final int ende = Math.max(removeAttributes(startPos_loc), startPos_loc + length);
			// ersten möglichen Anfang eines Kommentares finden
			boolean fertig = false;
			int commentBegin = -1;
			// int commentEnd=0;
			// hier sollte ein passenderer Begin für die Neusetzung der
			// Attribute
			// gefunden werden: z.B. der letzte Anfang eines Kommentares vor
			// der EinfügePosition
			int commentEnd = findLastCommentBegin(startPos_loc);
			while (!fertig) {
				try {
					commentBegin = (doc.getText(commentEnd, doc.getLength()
							- commentEnd)).indexOf(blockCommentBegin);
				} catch (BadLocationException e) {
					logger.warn(e.getMessage(), e);
				}
				if (commentBegin != -1) {
					// Wert korrigieren (NullOffsetAnpassung)
					commentBegin += commentEnd;
					// testen ob auch echter Kommentarbeginn ?
					if (indexOfMultiLineCommentBegin(readWholeLine(commentBegin)) != -1) {
						// Kommentaranfang gefunden der auch nachfolgende Zeilen
						// betrifft
						// nun alle nachfolgenden Zeilen mit Attributen versehen
						commentEnd = setCommentAttributes(commentBegin);
						if (commentEnd >= ende) {
							fertig = true;
						}
					} else {
						// neuen Start für weitere Suche festlegen
						// Start muss nach dem letzten vermuteten Begin
						// des Kommentares liegen
						commentEnd = commentEnd + commentBegin;
						if (commentEnd == 0) {
							commentEnd += blockCommentBegin.length();
						}
						// ist der nächste mögliche Such-Anfang das Dateiende
						// dann fertig !
						// if (commentEnd == doc.getLength()) {
						if (commentEnd >= ende) {
							fertig = true;
						}
					}
				} else {
					// kein weiterer Kommentar gefunden also fertig
					fertig = true;
				}
			}
			editor.repaint(); // notwendig, sonst wird zeitweise das Bild nicht aktualisiert
		}
	}

	public final String getHighlightedWord() {
		return highlightedWord;
	}

	public final void setHighlightedWord(String highlightedWord) {
		this.highlightedWord = highlightedWord;
	}

}
