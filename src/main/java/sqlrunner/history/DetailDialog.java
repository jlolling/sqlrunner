package sqlrunner.history;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import sqlrunner.Main;
import sqlrunner.swinghelper.WindowHelper;
import dbtools.SQLStatement;

public class DetailDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JSplitPane splitpane=new JSplitPane();
    private final JScrollPane jScrollPaneTop=new JScrollPane();
    private final JScrollPane jScrollPaneButtom=new JScrollPane();
    private final JTextArea textAreaSQL=new JTextArea();
    private final JTextArea textAreaSummary=new JTextArea();
    private JButton buttonClose;
    private SQLStatement statement = null;

    public DetailDialog(JFrame parent) {
        super(parent, false);
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            WindowHelper.checkAndCorrectWindowBounds(this);
            pack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if(!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }

    private void initComponents() throws Exception {
        setTitle("Statement-Details");
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitpane, BorderLayout.CENTER);
        getContentPane().add(getJButtonClose(), BorderLayout.SOUTH);
        jScrollPaneTop.setViewportView(textAreaSummary);
        jScrollPaneTop.setPreferredSize(new Dimension(390, 140));
        jScrollPaneButtom.setViewportView(textAreaSQL);
        jScrollPaneButtom.setPreferredSize(new Dimension(390, 140));
        textAreaSQL.setFont(new java.awt.Font("Monospaced", 0, 12));
        textAreaSummary.setBackground(Main.info);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitpane.setContinuousLayout(true);
        splitpane.setTopComponent(jScrollPaneTop);
        splitpane.setBottomComponent(jScrollPaneButtom);
        splitpane.setDividerSize(5);
        //splitpane.setDividerLocation(100);
    }
    
    private JButton getJButtonClose() {
    	if (buttonClose == null) {
    		buttonClose = new JButton();
    		buttonClose.setText(Messages.getString("HistoryView.7"));
    		buttonClose.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
    			
    		});
    	}
    	return buttonClose;
    }

    public void setStatement(SQLStatement stat) {
        statement = stat;
        refresh();
    }
    
    public void refresh() {
        if (statement != null) {
            textAreaSQL.setText(statement.getSQL());
            textAreaSummary.setText(statement.getSummary());
        } else {
            textAreaSQL.setText(null);
            textAreaSummary.setText(null);
        }
        jScrollPaneTop.invalidate();
        textAreaSQL.setCaretPosition(0);
        jScrollPaneButtom.invalidate();
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
    }

}
