package sqlrunner.editor;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.GapContent;
import javax.swing.text.PlainDocument;

/**
 *  Klasse erweitert das Standardtextdokument um die Funktionalität
 *  zur Syntaxdarstellung erforderlich sind
 *  es werden Kennzeichen für die Kennzeichnung für
 *  mehrzeiligen Kommentaren als Attribute den Textzeilen (Elemente) hinzugefügt
 */
public final class SyntaxDocument extends PlainDocument {

    private static final long serialVersionUID = 1L;
    boolean updateComments = false;
    private transient SyntaxScanner lexer = null;
    private boolean inBlockComment = false;

    public SyntaxDocument(SyntaxScanner lexer) {
        // GapContent ist eine Verwaltung von Text mit verwalteten
        // nicht sichtbaren Lücken um die notwendigen Textverschiebeaktionen
        // zu minimieren
        super(new GapContent(1024));
        this.lexer = lexer;
    }

    /**
     * testet ob die TextÄnderung für die Abgrenzung von Kommentaren relevant
     * sein kann (tritt auch ein wenn Zeilen hinzugefügt oder entfernt werden)
     * @param chng Änderung am Dokument gekapselt in einem Event
     * @return true wenn die TextÄnderung einen Einfluss auf Kommentare haben kann
     */
    boolean testForUpdateIsNecessary(DefaultDocumentEvent chng) {
        // testen ob etwas Kommentarrelevantes dabei ist:
        //     neue Zeilen eingefügt
        //     Klammern enthalten
        //     Stringbegrenzer enthalten
        boolean isNecessary = false;
        String s = "";
        try {
            s = getText(chng.getOffset(), chng.getLength());
        } catch (BadLocationException e) {
            System.err.println("SQLDocument.testForUpdateIsNecessary: error: " + e.getMessage());
        }
        // leider kann an dieser Stelle nur auf einzelne Zeichen geprüft werden,
        // da hier pro Zeichen ein Event ausgelöst wird.
        boolean blockCommentStartFound = (s.indexOf(SyntaxScanner.blockCommentBegin.charAt(0)) != -1);
        if (blockCommentStartFound) {
        	inBlockComment = true;
        }
        boolean blockCommentEndFound = (s.indexOf(SyntaxScanner.blockCommentEnd.charAt(0)) != -1);
        if (blockCommentEndFound) {
        	inBlockComment = false;
        }
        if (blockCommentStartFound || blockCommentEndFound || (s.indexOf('\n') != -1) || (s.indexOf(SyntaxScanner.stringLimiter) != -1)) {
            isNecessary = true;
        } else {
            // testen ob die Elementestruktur erneuert wurde
            // dann muss auch neu gescannt werden
            final Element root = getDefaultRootElement();
            final DocumentEvent.ElementChange ec = chng.getChange(root);
            if (ec != null) {
                isNecessary = true;
            }
        }
        return isNecessary;
    }

    // --- AbstractDocument methods ----------------------------
    // wenn Text hinzugefügt wird
    @Override
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        super.insertUpdate(chng, attr);
        if (testForUpdateIsNecessary(chng) || inBlockComment) {
            if (lexer != null) {
                lexer.updateCommentAttributes(chng.getOffset(), chng.getLength());
            }
        }
    }
    
    public void updateCommentAttributes() {
    	writeLock();
    	lexer.updateCommentAttributes(0, getLength() - 1);
    	writeUnlock();
    }

    // etwas komplizierte Behandlung des löschens erforderlich
    // removeUpdate wird vor dem Entfernen des Textes aus dem Document
    // aufgerufen, somit kann noch an Hand der Offsets im Document
    // nachgesehen werden, was denn nun gelöscht werden soll.
    // Ein Update der Attribute ist aber noch zu früh,
    // am Document hat sich noch nichts geÄndert
    // scanRequest wertet das aus um unötige Scanneraufrufe zu vermeiden
    // postRemoveUpdate liegt zeitlich nach dem löschen, also null-Chance
    // festzustellen, was denn nun gelöscht wurde
    // aber der richtige Zeitpunkt um die Attribute neu zu setzen, da Document nun
    // aktuell!
    // also erst merken was gelöscht werden soll -> Flag setzen und dann nach
    // dem löschen die Attribute neu setzen
    @Override
    protected void removeUpdate(DefaultDocumentEvent chng) {
        super.removeUpdate(chng);
        // feststellen ob notwendig
        updateComments = testForUpdateIsNecessary(chng);
    }

    // wird aufgerufen, nachdem ein remove durchgelaufen ist
    @Override
    protected void postRemoveUpdate(DefaultDocumentEvent chng) {
        super.postRemoveUpdate(chng);
        if (updateComments) {
            // hier nicht scanRequest aufrufen, da diese noch mal die
            // Notwendigkeit des Updates prüft - und auf Grund der
            // bereits gelöschten Daten zu einem falschen Ergebnis kommt !!
            // abgesehen davon ist die Notwendigkeit bereits in updateComment
            if (lexer != null) {
                lexer.updateCommentAttributes(chng.getOffset(), chng.getLength());
            }
            updateComments = false;
        }
    }

}