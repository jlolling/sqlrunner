package sqlrunner.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;

public class PanelConfigLineSeparator extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private final JComboBox<String> cbLineSeparator=new JComboBox<String>();

    public PanelConfigLineSeparator() {
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
                Messages.getString("PanelConfigLineSeparator.bordertitle"))); //$NON-NLS-1$
        setMinimumSize(new Dimension(250, 60));
        setPreferredSize(new Dimension(250, 60));
        setLayout(new GridBagLayout());
        this.add(cbLineSeparator, gridBagConstraints);
        final String currentLineSeparator=Main.getUserProperty("CURR_LINE_SEPARATOR", Main.currentLineSeparatorType); //$NON-NLS-1$
        cbLineSeparator.addItem("Windows"); //$NON-NLS-1$
        cbLineSeparator.addItem("UNIX"); //$NON-NLS-1$
        cbLineSeparator.addItem("Mac OS (classic)"); //$NON-NLS-1$
        if (currentLineSeparator.equals("Windows")) { //$NON-NLS-1$
            cbLineSeparator.setSelectedIndex(0);
        } else if (currentLineSeparator.equals("Mac OS (classic)")) { //$NON-NLS-1$
            cbLineSeparator.setSelectedIndex(2);
        } else {
            cbLineSeparator.setSelectedIndex(1);
        }
    }

    public boolean isChanged() {
        return true;
    }

    public boolean performChanges() {
        Main.setLineSeparatorType((cbLineSeparator.getSelectedItem()).toString());
        return true;
    }

    public void cancel() {}

}
