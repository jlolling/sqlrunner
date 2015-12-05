package sqlrunner.config;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;

public class PanelConfigDebugMode extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 4226474096338614693L;
    private JCheckBox jCheckBox = null;
	
    public PanelConfigDebugMode() {
        try {
            initComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() throws Exception {
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigDebugMode.bordertitle"))); //$NON-NLS-1$
        setLayout(new BorderLayout());
        this.add(getJCheckBox(), BorderLayout.WEST);
        jCheckBox.setSelected(Main.isDebug());
    }

    public boolean isChanged() {
        return true;
    }

    public boolean performChanges() {
        Main.setDebug(jCheckBox.isSelected());
        return true;
    }

    public void cancel() {}

	/**
	 * This method initializes jCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */    
	private JCheckBox getJCheckBox() {
		if (jCheckBox == null) {
			jCheckBox = new JCheckBox();
			jCheckBox.setText(Messages.getString("PanelConfigDebugMode.checkbox")); //$NON-NLS-1$
		}
		return jCheckBox;
	}
    
 }  //  @jve:decl-index=0:visual-constraint="10,10"
