package sqlrunner.editor;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;

import sqlrunner.Main;

/**
 *  Blockcursor für die Kennzeichnung des überschreibemodus der
 *  Textkomponente
 */
public class XORCaret extends DefaultCaret {

    private static final long serialVersionUID = 1L;
    protected int lastPaintedWidth   = 8;
    protected Font currentFont        = null;
    protected int currentAscent      = 0;
    protected FontMetrics currentFontMetrics = null;
    protected char currentChar;

    public XORCaret() {
        setBlinkRate(Main.CARET_BLINK_RATE);
    }

    // bringt die Breite des zu zeichnenden Caret
    private int currentWidth(Graphics2D g2d) {
        String current = null;
        try {
            current = getComponent().getText(getDot(), 1);
        } catch (BadLocationException ex) {
            // mache nichts
        }
        if ((current != null) && (current.length() > 0)) {
            currentChar = current.charAt(0);
        } else {
            // ersatzweise das Leerzeichen als Basis fuer die Breitenermittlung nehmen
            currentChar = ' ';
        }
        final Font font = getComponent().getFont();
        if (font != currentFont) {
            currentFontMetrics = g2d.getFontMetrics();
            currentAscent = currentFontMetrics.getAscent();
        }
        // ermittle den zu zeichnenden Text unter dem Cursor
        // Methodenrückgabewert setzen
        if (Character.isWhitespace(currentChar)) {
            // wenn nicht sichtbares Zeichen, dann die Breite eines Leerzeichens
            // als Breite zurückgeben
            return currentFontMetrics.charWidth(' ');
        } else {
            // wenn sichtbares Zeichen, dann dessen Breite zurückgeben
            return currentFontMetrics.charWidth(current.charAt(0));
        }
    }

    // ------ DefaultCaret Methoden -----------------------------------------

    // Caret zeichnen an neuer position
    @Override
    public final void paint(Graphics g) {
        if (isVisible()) {
            // ermittle wo der Cursor zu zeichen ist
            final TextUI ui = getComponent().getUI();
            Rectangle r = null;
            final int dot = getDot();
            try {
                r = ui.modelToView(getComponent(), dot);
            } catch (BadLocationException ex) {
            	System.err.println("paint failed: " + ex.getMessage());
                return;
            }
            g.setColor((getComponent()).getCaretColor());
            // zeichne das rechteck,
            // ermittle mit currentWith die erforderliche Breite
            // und merke sie mir für späteres entfernen des Cursors
            lastPaintedWidth = currentWidth((Graphics2D) g);
            g.fillRect(r.x, r.y, lastPaintedWidth, r.height);
            //zeichne den Buchstaben mit der Hintergrundfarbe
            g.setColor((getComponent()).getBackground());
            try {
                if (dot < ((getComponent()).getDocument()).getLength()) {
                    final String s=(getComponent()).getText(dot,1);
                    // keine nicht sichtbaren Zeichen manipulieren
                    if (Character.isISOControl(s.charAt(0)) == false) {
                        // nur zeichen, wenn es sichtbares Zeichen ist
                        g.drawString(s, r.x, r.y + currentAscent);
                    }
                }
            } catch (BadLocationException ex) {
                System.err.println(ex.toString());
            }
        }
    }

    // alte CaretPosition neu zeichnen
    @Override
    public final void damage(Rectangle r) {
        if (r != null) {
            x = r.x;
            y = r.y;
            width = lastPaintedWidth;
            height = r.height;
            repaint();
        }
    }
    
    public int hashCode() {
    	return super.hashCode();
    }
    
    public boolean equals(Object o) {
    	return super.equals(o);
    }
    

}
