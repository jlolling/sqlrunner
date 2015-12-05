/*
 * created on 29.07.2005
 * created by lolling.jan
 */
package dbtools;

import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class DatabaseSessionPoolInfoFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private DatabaseSessionPoolInfoPanel infoPanel = new DatabaseSessionPoolInfoPanel();


    /**
     * This is the default constructor
     */
    public DatabaseSessionPoolInfoFrame() {
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setSize(300, 200);
        this.setContentPane(infoPanel);
        this.setTitle("Database Pool Info");
    }


    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()) {
            case WindowEvent.WINDOW_OPENED:
                infoPanel.startRefreshing();
                break;
            case WindowEvent.WINDOW_CLOSING:
                infoPanel.stopRefreshing();
        }
        super.processWindowEvent(e);
    }
    
}
