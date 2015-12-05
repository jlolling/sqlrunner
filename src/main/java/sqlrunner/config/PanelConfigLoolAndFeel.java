package sqlrunner.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;

public class PanelConfigLoolAndFeel extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    
    private final JComboBox<String> cbLF = new JComboBox<String>();

    public PanelConfigLoolAndFeel() {
        try {
            initComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() throws Exception {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigLoolAndFeel.bordertitle"))); //$NON-NLS-1$
        setMinimumSize(new Dimension(250, 60));
        setPreferredSize(new Dimension(250, 60));
        setLayout(new GridBagLayout());
        this.add(cbLF, gridBagConstraints);
        final Enumeration<Object> enumeration = Main.lookAndFeels.keys();
        cbLF.addItem("System"); //$NON-NLS-1$
        String alias;
        while (enumeration.hasMoreElements()) {
            alias = (enumeration.nextElement()).toString();
            cbLF.addItem(alias);
            if (alias.equals(Main.getCurrentLookAndFeelAlias())) {
                cbLF.setSelectedItem(alias);
            }
        }
    }

    public boolean isChanged() {
        return true;
    }

    public boolean performChanges() {
        Main.setLookAndFeel((cbLF.getSelectedItem()).toString());
        return true;
    }

    public void cancel() {}

}
