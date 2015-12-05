package sqlrunner.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.StringTokenizer;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;

public class PanelConfigCharSets extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private final JComboBox<String> cbCharSets = new JComboBox<String>();

    public PanelConfigCharSets() {
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
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigCharSets.bordertitle"))); //$NON-NLS-1$
        setMinimumSize(new Dimension(250, 60));
        setPreferredSize(new Dimension(250, 60));
        setLayout(new GridBagLayout());
        this.add(cbCharSets, gridBagConstraints);
        cbCharSets.getAccessibleContext().setAccessibleName(Messages.getString("PanelConfigCharSets.bordertitle"));
        final String charSets=Main.getDefaultProperty("CHAR_SETS","ISO-8859-1|ISO-8859-15|UTF-8|UTF-16|Cp1252|MacRoman"); //$NON-NLS-1$ //$NON-NLS-2$
        final String currentCharSet=Main.getUserProperty("CURR_CHAR_SET"); //$NON-NLS-1$
        final String systemCharSet=System.getProperty("file.encoding"); //$NON-NLS-1$
        final StringTokenizer st=new StringTokenizer(charSets,"|"); //$NON-NLS-1$
        String charSet = null;
        while (st.hasMoreTokens()) {
            charSet = st.nextToken();
            cbCharSets.addItem(charSet);
            if (currentCharSet != null) {
                if (charSet.equals(currentCharSet)) {
                    cbCharSets.setSelectedItem(charSet);
                }
            } else if (systemCharSet != null) {
                if (charSet.equals(systemCharSet)) {
                    cbCharSets.setSelectedItem(charSet);
                }
            }
        }
    }

    public boolean isChanged() {
        return true;
    }

    public boolean performChanges() {
        Main.setFileEncoding((cbCharSets.getSelectedItem()).toString());
        return true;
    }

    public void cancel() {}

}
