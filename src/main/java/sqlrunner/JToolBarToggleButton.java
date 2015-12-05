package sqlrunner;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

/**
 * @author Lolling.Jan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class JToolBarToggleButton extends JToggleButton {

    private static final long serialVersionUID = 1L;
    private Dimension dim = new Dimension();

    public JToolBarToggleButton() {
        super();
    }
    
    public JToolBarToggleButton(ImageIcon icon) {
        super(icon);
        dim.height = icon.getIconHeight()+5;
        dim.width = icon.getIconWidth()+5;
        setFocusable(false);
    }

    @Override
    public void setIcon(Icon icon) {
        super.setIcon(icon);
        if (icon != null) {
            dim = new Dimension();
            dim.height = icon.getIconHeight()+5;
            dim.width = icon.getIconWidth()+5;
        }
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
                if (isEnabled()) setBorderPainted(true);
                break;
            case MouseEvent.MOUSE_EXITED:
                setBorderPainted(false);
                break;
        }
        super.processMouseEvent(me);
    }

}

