package sqlrunner.editor;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.StyleContext;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import sqlrunner.Main;

/**
 * Klasse ist für die Darstellung des Textes in der Editorkomponente zuständig.
 * Diese Klasse realisiert in dem MVC-Konzept die View-Komponente.
 * Die zu benutzenden Darstellungsattribute werden über Methoden der Klasse Scanner ermittelt.
 */
public class SyntaxContext extends StyleContext implements ViewFactory {

    private static final long serialVersionUID = 1L;
    // der Wert von syntaxHighlighting wird in MainFrame auch gesetzt
    // bei der Änderungsabfrage der Syntax-Checkbox
    private static boolean syntaxHighlighting = false;
    private transient SyntaxScanner lexer = null;
    private static boolean useAntiAliasing;
    private static FontMetrics fontMetrics;
    private Document doc;

    public SyntaxContext(SyntaxScanner lexer) {
        super();
        this.lexer = lexer;
        // Syntaxhighlighting gewollt ?
        if ((Main.getUserProperty("SYNTAX_HIGHLIGHT","true")).equals("true") && (lexer != null)) {
            setSyntaxHighlighting(true);
        } else {
            setSyntaxHighlighting(false);
        }
        setUseAntiAliasing(Boolean.valueOf(Main.getUserProperty("USE_FONT_ANTIALIASING", "false")).booleanValue());
        if (isUseAntiAliasing()) {
            System.out.println("use font antialiasing");
        }
    }

    // --- ViewFactory methods -------------------------------------

    public View create(Element elem) {
        doc = elem.getDocument();
        if (lexer != null) {
            if (lexer.getDocument() == null || lexer.getDocument().equals(doc) == false) {
                lexer.setDocument(doc);
            }
        }
        return new SQLPlainView(elem);
    }

    // Zeilenumbruch ist in Source-Code-Editor unerwuenscht !!
    // für Zeilenumbruch muss WrappedPlainView erweitert werden !

    /**
     * Sicht auf das PlainDocument die den unselektierten Text
     * zeilenweise nach Keyworten untersucht und diese farbig mit angepassten
     * Textstyle darstellt. Die Zerlegung der Textzeilen in Wort, die Vergleich mit
     * Keyworten und die Ermittlung des Darstellungsstiles erfolgt in Scanner
     */
    class SQLPlainView extends PlainView {

        SQLPlainView(Element elem) {
            super(elem);
        }
        
        // der selektierte Text wird ohne jede weitere Beeinflussung dargestellt!
        // deshalb hier kein überschreiben der Methode drawSelectedText() !

        /**
         * eigentliche Druckroutine
         * alle Textattribute müssen als Eigenschaft des canvas
         * vor der jeweiligen Ausgabe eingestellt werden
         * in der Regel umreissen p0 und p1 eine Textzeile (Element)
         * @param g Grafik-Kontext auf den der text gezeichnet werden soll
         * @param x und y Pixelposition ab der die grafische Textausgabe erfolgen soll
         * @param p1 und p1 Anfang und Endeposition des auszugebenden Textes im Textdokument
         * @return x-position ab der die nächste Ausgabe auf dem Grafikkontext erfolgen kann
         * @exception wenn fehlerhafter Zugriff auf das Document.
         */
        @Override
        protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
        	//System.out.println("drawUnselectedText p0=" + p0 + " p1=" + p1);
            Segment textSegment;
            // nur Scanner bemühen wenn auch erwünscht
            if (useAntiAliasing) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            } else {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }
            if (fontMetrics == null) {
                fontMetrics = g.getFontMetrics(Main.textFont);
            }
            if (isSyntaxHighlightingEnabled() && (p1 > p0)) {
                int mark1 = p0;
                Color color = null;
                int highlighterWidth = 0;
                int highlighterHeigh = 0;
                // Schleife arbeitet solange, bis zu druckende Endposition (p1) erreicht ist
                while (p1 > mark1) {
                    // aktuellen Scannbereich setzen
                    lexer.setRange(mark1, p1);
                    // Darstellungsattribute für die einzelnen TextPositionen
                    // holen und dem Grafik-Context zuweisen
                    if (lexer.scanLine()) {
                        // wenn erfolgreicher scan
                        // d.h im Range ist eine lexikalische Einheit gefunden worden
                        // nachsehen, ob der Beginn der lexikalischen
                        // Einheit auch Beginn des aktuellen Scannbereiches ist
                        // wenn nicht den Anfang vorab normal drucken
                        int mark2 = lexer.getStartOffset();
                        if (mark2 > mark1) {
                            // bis zum Beginn der lexikalischen Einheit Standardtextattribute nutzen
                            g.setColor(Color.black);
                            g.setFont(Main.textFont);
                            textSegment = new Segment();
                            doc.getText(mark1, mark2 - mark1, textSegment);
                            // und Text ausgeben
                            x = Utilities.drawTabbedText(textSegment, x, y, g, this, p0);
                            mark1 = mark2;
                        }
                        // nun die gefunden Textattribute für die aktuelle lexikalische
                        // Einheit setzen
                        color = lexer.getColor();
                        g.setFont(lexer.getFont());
                    } else {
                        color = Color.black;
                        g.setFont(Main.textFont);
                    }
                    p0 = mark1;
                    mark1 = lexer.getEndOffset();
                    textSegment = new Segment();
                    // das Segment füllen
                    doc.getText(p0, mark1 - p0, textSegment);
                    if (lexer.getBackgroundColor() != null) {
                        // ganze Worte hervorheben 
                        g.setColor(lexer.getBackgroundColor());
                        highlighterWidth = fontMetrics.charsWidth(textSegment.array, textSegment.offset, textSegment.count);
                        highlighterHeigh = fontMetrics.getHeight();
                        g.fillRect(x, y - highlighterHeigh + 3, highlighterWidth, highlighterHeigh);
                    }
                    g.setColor(color);
                    // der eigentliche Zeichner des Textes (Segment)
                    x = Utilities.drawTabbedText(textSegment, x, y, g, this, mark1);
                }
            } else {
                // hier wenn kein SyntaxHighlighting gewuenscht
                g.setFont(Main.textFont);
                g.setColor(Color.black);
                textSegment = new Segment();
                doc.getText(p0, p1 - p0, textSegment);
                x = Utilities.drawTabbedText(textSegment, x, y, g, this, p0);
            }
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            return x; // x-Pixelposition auf dem canvas ab der die neue Ausgabe erfolgen kann
        }

    } // class SQLPlainView

    public static final boolean isUseAntiAliasing() {
        return useAntiAliasing;
    }

    public static final void setUseAntiAliasing(boolean useAntiAliasing) {
        System.out.println("use font antialiasing: "+ useAntiAliasing);
        SyntaxContext.useAntiAliasing = useAntiAliasing;
    }

    static void setSyntaxHighlighting(boolean syntaxHighlighting) {
        SyntaxContext.syntaxHighlighting = syntaxHighlighting;
    }

    static boolean isSyntaxHighlightingEnabled() {
        return syntaxHighlighting;
    }
    
    public static void setupTextFont() {
    	fontMetrics = null;
    	// jetzt wird in der Methode drawUnselectedText dieser Umstand erkannt
    	// und eine neue Instanz erstellt
    }

}
