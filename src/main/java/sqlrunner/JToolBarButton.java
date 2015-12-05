package sqlrunner;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public final class JToolBarButton extends JButton {

    private static final long serialVersionUID = 1L;
    private final Dimension dim = new Dimension();

    public JToolBarButton(ImageIcon icon) {
        super(icon);
        dim.height = 25; //icon.getIconHeight() + 5;
        dim.width = 25; //icon.getIconWidth() + 5;
        setBorderPainted(false);
        setFocusPainted(false);
        setRequestFocusEnabled(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return dim;
    }

    @Override
    public Dimension getMaximumSize() {
        return dim;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setBorderPainted(false);
    }

    @Override
    public void processMouseEvent(MouseEvent me) {
        switch (me.getID()) {
            case MouseEvent.MOUSE_ENTERED:
                if (isEnabled()) {
                    setBorderPainted(true);
                }
                break;
            case MouseEvent.MOUSE_EXITED:
                setBorderPainted(false);
                break;
        }
        super.processMouseEvent(me);
    }
}