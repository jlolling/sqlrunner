package sqlrunner.config;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;
import sqlrunner.MainFrame;

public class PanelConfigDateFormat extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private JComboBox<String> cbDateFormat;
    static String tooltipTextDate = "<HTML><BODY>"
      + Messages.getString("PanelConfigDateFormat.1") 
      + Messages.getString("PanelConfigDateFormat.2") 
      + Messages.getString("PanelConfigDateFormat.3")
      + Messages.getString("PanelConfigDateFormat.4")
      + Messages.getString("PanelConfigDateFormat.5")
      + Messages.getString("PanelConfigDateFormat.6")
      + Messages.getString("PanelConfigDateFormat.7")
      + "</BODY></HTML>";

    public PanelConfigDateFormat() {
        try {
            initComponents();
            prepareCbDateFormat();
            if (MainFrame.getDateFormatMask() != null) {
                setSelectedCbDateFormatItem(MainFrame.getDateFormatMask());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() throws Exception {
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigDateFormat.bordertitle")));
        setLayout(new BorderLayout());
        cbDateFormat = new JComboBox<String>();
        cbDateFormat.setEditable(true);
        cbDateFormat.setToolTipText(tooltipTextDate);
        add(cbDateFormat, BorderLayout.CENTER);
    }

    private void prepareCbDateFormat() {
        int i = 0;
        String formatStr;
        while (true) {
            formatStr = Main.getDefaultProperty("DATE_FORMAT_" + String.valueOf(i)); //$NON-NLS-1$
            if (formatStr != null) {
                cbDateFormat.addItem(formatStr);
            } else {
                break; // Schleife abbrechen, wenn keine weiteren Einträge vorhanden
            }
            i++;
        }
    }

    private void setSelectedCbDateFormatItem(String dateFormat) {
        for (int i = 0; i < cbDateFormat.getItemCount(); i++) {
            if (cbDateFormat.getItemAt(i).equals(dateFormat)) {
                cbDateFormat.setSelectedIndex(i);
                break;
            }
        }
    }

    public boolean isChanged() {
        return MainFrame.getDateFormatMask().equals((String) cbDateFormat.getSelectedItem()) == false;
    }

    public boolean performChanges() {
        MainFrame.setDateFormatMask((String) cbDateFormat.getSelectedItem());
        Main.setUserProperty("DATE_FORMAT", MainFrame.getDateFormatMask());
        MainFrame mainFrame = Main.getActiveMainFrame();
        if (mainFrame != null) {
            // die Renderer für die Tabelle neu initialisieren
            mainFrame.prepareTableRenderer();
            // den Date-Parser neu initialisieren
            if (mainFrame.getDatabase() != null) {
                mainFrame.getDatabase().createSDF();
            }
        }
        return true;
    }

    public void cancel() {}

}
