package sqlrunner.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.StringTokenizer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;

public class PanelConfigSocksProxy extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private JCheckBox cbActivateSocksProxy = null;
    private JTextField tfSocksHost = null;
    private JTextField tfSocksPort = null;
    private JTextField tfSocksUser = null;
    private JPasswordField tfSocksPassword = null;

    public PanelConfigSocksProxy() {
        try {
            initComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() throws Exception {
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigSocksProxy.bordertitle")));
    	{
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(2, 2, 2, 2);
            setMinimumSize(new Dimension(250, 60));
            setPreferredSize(new Dimension(250, 60));
            setLayout(new GridBagLayout());
            this.add(getCbActivateSocksProxy(), gridBagConstraints);
    	}
    }

    public boolean isChanged() {
        return true;
    }

    public boolean performChanges() {
    	
        return true;
    }

    public void cancel() {}

	private JCheckBox getCbActivateSocksProxy() {
		if (cbActivateSocksProxy == null) {
			cbActivateSocksProxy = new JCheckBox();
			cbActivateSocksProxy.setText(Messages.getString("PanelConfigSocksProxy.host"));
		}
		return cbActivateSocksProxy;
	}

	private JTextField getTfSocksHost() {
		if (tfSocksHost == null) {
			tfSocksHost = new JTextField();
		}
		return tfSocksHost;
	}

	private JTextField getTfSocksPort() {
		if (tfSocksPort == null) {
			tfSocksPort = new JTextField();
		}
		return tfSocksPort;
	}

	private JTextField getTfSocksUser() {
		if (tfSocksUser == null) {
			tfSocksUser = new JTextField();
		}
		return tfSocksUser;
	}

	private JPasswordField getTfSocksPassword() {
		if (tfSocksPassword == null) {
			tfSocksPassword = new JPasswordField();
		}
		return tfSocksPassword;
	}

}
