/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlrunner.editor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

public class LineHighlightPainter extends LayeredHighlighter.LayerPainter {

    private Color color;
    private boolean enabled = true;
    static final Color DEFAULT_COLOR = new Color(235, 235, 255);

    /**
     * Constructs a new highlight painter. If <code>c</code> is null,
     * the JTextComponent will be queried for its selection color.
     *
     * @param c the color for the highlight
     */
    public LineHighlightPainter(Color c) {
        if (c != null) {
            color = c;
        } else {
            color = DEFAULT_COLOR;
        }
    }

    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    /**
     * Returns the color of the highlight.
     *
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    // --- HighlightPainter methods ---------------------------------------
    /**
     * Paints a highlight.
     *
     * @param g the graphics context
     * @param offs0 the starting model offset >= 0
     * @param offs1 the ending model offset >= offs1
     * @param bounds the bounding box for the highlight
     * @param c the editor
     */
    public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
        if (enabled) {
            try {
                // --- determine locations ---
                TextUI mapper = c.getUI();
                final Rectangle p0 = mapper.modelToView(c, offs0);
                final Rectangle p1 = mapper.modelToView(c, offs1);

                // --- render ---
                if (getColor() == null) {
                    g.setColor(c.getSelectionColor());
                } else {
                    g.setColor(getColor());
                }
                final Rectangle r = p0.union(p1);
                g.fillRect(r.x, r.y, r.width, r.height);
            } catch (BadLocationException e) {
                // can't render
                }
        }
    }

    // --- LayerPainter methods ----------------------------
    /**
     * Paints a portion of a highlight.
     *
     * @param g the graphics context
     * @param offs0 the starting model offset >= 0
     * @param offs1 the ending model offset >= offs1
     * @param bounds the bounding box of the view, which is not
     *        necessarily the region to paint.
     * @param c the editor
     * @param view View painting for
     * @return region drawing occured in
     */
    public Shape paintLayer(
        Graphics g,
        int offs0,
        int offs1,
        Shape bounds,
        JTextComponent c,
        View view) {
        if (enabled) {
            if (getColor() == null) {
                g.setColor(c.getSelectionColor());
            } else {
                g.setColor(getColor());
            }
            // Should only render part of View.
            try {
                // --- determine locations ---
                Shape shape = view.modelToView(
                    offs0,
                    Position.Bias.Forward,
                    offs1,
                    Position.Bias.Backward,
                    bounds);
                Rectangle r = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
                g.fillRect(r.x, r.y, c.getWidth(), r.height);
                return r;
            } catch (BadLocationException e) {
                // can't render
                }
        }
        return null;
    }
}