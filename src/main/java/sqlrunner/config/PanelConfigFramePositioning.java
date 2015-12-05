package sqlrunner.config;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;
import sqlrunner.swinghelper.WindowHelper;

public class PanelConfigFramePositioning extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private final JCheckBox cbEnableFramePositioning=new JCheckBox();

    public PanelConfigFramePositioning() {
        try {
            initComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() throws Exception {
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigFramePositioning.bordertitle"))); //$NON-NLS-1$
        setLayout(new BorderLayout());
        this.add(cbEnableFramePositioning, BorderLayout.WEST);
        if (Main.getUserProperty("WM_FRAME_POSITIONING","false").equals("true")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            cbEnableFramePositioning.setSelected(true);
        } else {
            cbEnableFramePositioning.setSelected(false);
        }
        cbEnableFramePositioning.setText(Messages.getString("PanelConfigFramePositioning.bywm")); //$NON-NLS-1$
    }

    public boolean isChanged() {
        return true;
    }

    public boolean performChanges() {
        if (cbEnableFramePositioning.isSelected()) {
            Main.setUserProperty("WM_FRAME_POSITIONING", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            WindowHelper.enableWindowPositioning(false);
        } else {
            Main.setUserProperty("WM_FRAME_POSITIONING", "false"); //$NON-NLS-1$ //$NON-NLS-2$
            WindowHelper.enableWindowPositioning(true);
        }
        return true;
    }

    public void cancel() {}

}  //  @jve:decl-index=0:visual-constraint="10,10"
