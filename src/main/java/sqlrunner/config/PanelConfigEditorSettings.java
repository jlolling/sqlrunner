package sqlrunner.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.Main;
import sqlrunner.config.fontchooser.JFontChooser;
import sqlrunner.editor.SyntaxContext;

public class PanelConfigEditorSettings extends JPanel implements ConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private JCheckBox jCheckBoxUseAntialising = new JCheckBox();
    private JCheckBox jCheckBoxLineHighlighting = new JCheckBox();
    private JCheckBox jCheckBoxBracketContentHighlighting = new JCheckBox();
    private JButton jButtonChooseFont;
	private boolean changed = false;
	private Font selectedFont;
    
    public PanelConfigEditorSettings() {
        try {
            initComponents();
        } catch (Exception e) {
            e.printStackTrace();
        }
        selectedFont = Main.textFont;
    }

    private void initComponents() throws Exception {
        setMinimumSize(new Dimension(250, 85));
        setPreferredSize(new Dimension(250, 85));
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)),
                Messages.getString("PanelConfigEditorSettings.bordertitle"))); //$NON-NLS-1$
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridwidth = 2;
            jCheckBoxUseAntialising.setText(Messages.getString("PanelConfigEditorSettings.antialiasing")); //$NON-NLS-1$
            jCheckBoxUseAntialising.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    changed = true;
                }
            });
            jCheckBoxUseAntialising.setSelected(SyntaxContext.isUseAntiAliasing());
            add(jCheckBoxUseAntialising, gbc);
        }
        {
            JLabel jLabel = new JLabel();
            jLabel.setText(Messages.getString("PanelConfigEditorSettings.font"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = new Insets(2, 2, 2, 2);
            add(jLabel, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            this.add(getButtonChooseFont(), gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            jCheckBoxLineHighlighting.setText(Messages.getString("PanelConfigEditorSettings.linehighlighting")); //$NON-NLS-1$
            jCheckBoxLineHighlighting.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    changed = true;
                }
            });
            jCheckBoxLineHighlighting.setSelected(Main.isLineHighlightingEnabled());
            add(jCheckBoxLineHighlighting, gbc);
        }
        {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            jCheckBoxBracketContentHighlighting.setText(Messages.getString("PanelConfigEditorSettings.bracketcontenthighlighting")); //$NON-NLS-1$
            jCheckBoxBracketContentHighlighting.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    changed = true;
                }
            });
            jCheckBoxBracketContentHighlighting.setSelected(Main.isBracketContentHighlightingEnabled());
            add(jCheckBoxBracketContentHighlighting, gbc);
        }
    }

    public boolean isChanged() {
        return changed;
    }

    public boolean performChanges() {
        SyntaxContext.setUseAntiAliasing(jCheckBoxUseAntialising.isSelected());
        Main.setUserProperty("USE_FONT_ANTIALIASING", String.valueOf(jCheckBoxUseAntialising.isSelected()));
        Main.setUserProperty("EDITOR_FONT_SIZE", String.valueOf(selectedFont.getSize()));
        Main.setUserProperty("EDITOR_FONT_FAMILY", selectedFont.getFamily());
        Main.redefineEditorFont();
        Main.enableLineHighlighting(jCheckBoxLineHighlighting.isSelected());
        Main.enableBracketContentHighlighting(jCheckBoxBracketContentHighlighting.isSelected());
        return true;
    }

    public void cancel() {}

    private JButton getButtonChooseFont() {
    	if (jButtonChooseFont == null) {
    		jButtonChooseFont = new JButton();
    		jButtonChooseFont.setText(Messages.getString("PanelConfigEditorSettings.chooseFont"));
    		jButtonChooseFont.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					chooseFont();
				}
			});
    	}
    	return jButtonChooseFont;
    }
    
    private void chooseFont() {
    	JFontChooser fc = new JFontChooser();
    	fc.setSelectedFontFamily(selectedFont.getFamily());
    	fc.setSelectedFontSize(selectedFont.getSize());
    	int rc = fc.showDialog(this);
    	if (rc == JFontChooser.OK_OPTION) {
    		selectedFont = new Font(fc.getSelectedFontFamily(), Font.PLAIN, fc.getSelectedFontSize());
    	}
    }
    
}
