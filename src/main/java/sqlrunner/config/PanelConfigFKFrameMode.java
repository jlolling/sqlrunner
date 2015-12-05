package sqlrunner.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;
import sqlrunner.MainFrame;

public class PanelConfigFKFrameMode extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private final JRadioButton rbThisFrame=new JRadioButton();
    private final JRadioButton rbNewFrame=new JRadioButton();
    private final JRadioButton rbLastFrame=new JRadioButton();
    private final ButtonGroup rbGroup=new ButtonGroup();

    public PanelConfigFKFrameMode() {
        try {
            initComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() throws Exception {
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints2.gridy = 2;
        gridBagConstraints2.anchor = GridBagConstraints.WEST;
        gridBagConstraints2.gridx = 0;
        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints1.gridy = 1;
        gridBagConstraints1.anchor = GridBagConstraints.WEST;
        gridBagConstraints1.gridx = 0;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.gridx = 0;
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigFKFrameMode.bordertitle"))); //$NON-NLS-1$
        setMinimumSize(new Dimension(250, 85));
        setLayout(new GridBagLayout());
        rbThisFrame.setBackground(SystemColor.control);
        rbThisFrame.setFont(new java.awt.Font("Dialog", 0, 12)); //$NON-NLS-1$
        rbThisFrame.setOpaque(false);
        rbThisFrame.setText(Messages.getString("PanelConfigFKFrameMode.nonewwindow")); //$NON-NLS-1$
        rbNewFrame.setText(Messages.getString("PanelConfigFKFrameMode.3")); //$NON-NLS-1$
        rbNewFrame.setBackground(SystemColor.control);
        rbNewFrame.setFont(new java.awt.Font("Dialog", 0, 12)); //$NON-NLS-1$
        rbNewFrame.setOpaque(false);
        rbLastFrame.setText(Messages.getString("PanelConfigFKFrameMode.5")); //$NON-NLS-1$
        rbLastFrame.setBackground(SystemColor.control);
        rbLastFrame.setFont(new java.awt.Font("Dialog", 0, 12)); //$NON-NLS-1$
        rbLastFrame.setOpaque(false);
        this.add(rbThisFrame, gridBagConstraints);
        this.add(rbNewFrame, gridBagConstraints1);
        this.add(rbLastFrame, gridBagConstraints2);
        rbGroup.add(rbThisFrame);
        rbGroup.add(rbNewFrame);
        rbGroup.add(rbLastFrame);
        if (MainFrame.getFkNavigationFrameMode() == MainFrame.FK_NAVIGATION_LAST_FRAME) {
            rbLastFrame.setSelected(true);
        } else if (MainFrame.getFkNavigationFrameMode() == MainFrame.FK_NAVIGATION_NEW_FRAME) {
            rbNewFrame.setSelected(true);
        } else if (MainFrame.getFkNavigationFrameMode() == MainFrame.FK_NAVIGATION_THIS_FRAME) {
            rbThisFrame.setSelected(true);
        }
    }

    public boolean isChanged() {
        return true;
    }

    public boolean performChanges() {
        if (rbThisFrame.isSelected()) {
            Main.setUserProperty("FK_NAVIGATION_MODE", String.valueOf(MainFrame.FK_NAVIGATION_THIS_FRAME)); //$NON-NLS-1$
            MainFrame.setFkNavigationFrameMode(MainFrame.FK_NAVIGATION_THIS_FRAME);
        } else if (rbNewFrame.isSelected()) {
            Main.setUserProperty("FK_NAVIGATION_MODE", String.valueOf(MainFrame.FK_NAVIGATION_NEW_FRAME)); //$NON-NLS-1$
            MainFrame.setFkNavigationFrameMode(MainFrame.FK_NAVIGATION_NEW_FRAME);
        } else if (rbLastFrame.isSelected()) {
            Main.setUserProperty("FK_NAVIGATION_MODE", String.valueOf(MainFrame.FK_NAVIGATION_LAST_FRAME)); //$NON-NLS-1$
            MainFrame.setFkNavigationFrameMode(MainFrame.FK_NAVIGATION_LAST_FRAME);
        }
        return true;
    }

    public void cancel() {}

}  //  @jve:decl-index=0:visual-constraint="10,10"
