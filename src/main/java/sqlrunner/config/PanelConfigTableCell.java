package sqlrunner.config;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;

import sqlrunner.Main;
import sqlrunner.MainFrame;

public class PanelConfigTableCell extends JPanel implements	ConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private final JLabel labelWidthValue = new JLabel();
	private final JSlider sliderWidth = new JSlider();
	private final JLabel labelHeightValue = new JLabel();
	private final JSlider sliderHeight = new JSlider();
	private final JCheckBox jCheckBoxUseMonoSpacedFontInCells = new JCheckBox();
	private boolean isHeightChanged = false;
	private boolean isWidthChanged = false;
	private boolean fontChanged = false;
	private boolean hasMonospaceFont = false;
	private int oldWidth = 0;
	private int oldHeight = 0;
	private MainFrame mainFrame = null;

	public PanelConfigTableCell() {
		try {
			initComponents();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initComponents() throws Exception {
		setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED,
				Color.white, new Color(142, 142, 142)), Messages
				.getString("PanelConfigTableCell.bordertitle")));
		setMinimumSize(this.getPreferredSize());
		setLayout(new GridBagLayout());
		sliderWidth.setMinimum(10);
		sliderWidth.setMaximum(500);
		mainFrame = Main.getActiveMainFrame();
		if (mainFrame != null && mainFrame.getDatabase() != null) {
			sliderWidth.setValue(mainFrame.getDatabase()
					.getDefaultColumnWidth());
		}
		oldWidth = sliderWidth.getValue();
		sliderWidth.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				sliderWidth_stateChanged(e);
			}
		});
		sliderHeight.setMinimum(10);
		sliderHeight.setMaximum(500);
		if (mainFrame != null) {
			sliderHeight.setValue(mainFrame.resultTable.getRowHeight());
		}
		oldHeight = sliderHeight.getValue();
		sliderHeight.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				sliderHeight_stateChanged(e);
			}
		});
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 5, 2, 5);
			gbc.anchor = GridBagConstraints.EAST;
			gbc.fill = GridBagConstraints.NONE;
			JLabel labelWidthText = new JLabel();
			labelWidthText.setText(Messages.getString("PanelConfigTableCell.columnwidth")); //$NON-NLS-1$
			labelWidthText.setVerticalAlignment(SwingConstants.TOP);
			labelWidthText.setVerticalTextPosition(SwingConstants.TOP);
			add(labelWidthText, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 0, 2, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			labelWidthValue.setText(String.valueOf(sliderWidth.getValue()));
			labelWidthValue.setBorder(BorderFactory.createEtchedBorder());
			labelWidthValue.setHorizontalAlignment(SwingConstants.CENTER);
			add(labelWidthValue, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 2;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 5, 5, 5);
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(sliderWidth, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 5, 2, 5);
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
			JLabel labelHeightText = new JLabel();
			labelHeightText.setText(Messages.getString("PanelConfigTableCell.rowheight")); //$NON-NLS-1$
			labelHeightText.setVerticalAlignment(SwingConstants.TOP);
			labelHeightText.setVerticalTextPosition(SwingConstants.TOP);
			add(labelHeightText, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 2;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 0, 2, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			labelHeightValue.setText(String.valueOf(sliderHeight.getValue()));
			labelHeightValue.setBorder(BorderFactory.createEtchedBorder());
			labelHeightValue.setHorizontalAlignment(SwingConstants.CENTER);
			add(labelHeightValue, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 3;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 2;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 2, 2, 2);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(sliderHeight, gbc);
		}
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 4;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 2;
			gbc.gridheight = 1;
			gbc.insets = new Insets(2, 2, 2, 2);
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
			jCheckBoxUseMonoSpacedFontInCells.setText(Messages.getString("PanelConfigTableCell.monoSpacedFont"));
			jCheckBoxUseMonoSpacedFontInCells.setSelected(MainFrame.useMonospacedTableCellFont());
			hasMonospaceFont = jCheckBoxUseMonoSpacedFontInCells.isSelected();
			jCheckBoxUseMonoSpacedFontInCells.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					fontChanged = (hasMonospaceFont != jCheckBoxUseMonoSpacedFontInCells.isSelected());
				}
				
			});
			add(jCheckBoxUseMonoSpacedFontInCells , gbc);
		}
		
	}

	public boolean isChanged() {
		return isHeightChanged || isWidthChanged || fontChanged;
	}

	public boolean performChanges() {
		oldWidth = sliderWidth.getValue();
		oldHeight = sliderHeight.getValue();
		hasMonospaceFont = jCheckBoxUseMonoSpacedFontInCells.isSelected();
		MainFrame.setUseMonospacedTableCellFont(jCheckBoxUseMonoSpacedFontInCells.isSelected());
		return true;
	}

	public void cancel() {
		if (mainFrame.getDatabase() != null) {
			if (isWidthChanged) {
				mainFrame.getDatabase().setColumnWidthForAllColumns(oldWidth);
			}
			if (isHeightChanged) {
				mainFrame.resultTable.setRowHeight(oldHeight);
			}
		}
	}

	private void sliderWidth_stateChanged(ChangeEvent e) {
		if (mainFrame.getDatabase() != null) {
			mainFrame.getDatabase().setColumnWidthForAllColumns(
					sliderWidth.getValue());
			isWidthChanged = true;
			labelWidthValue.setText(String.valueOf(sliderWidth.getValue()));
		}
	}

	private void sliderHeight_stateChanged(ChangeEvent e) {
		mainFrame.resultTable.setRowHeight(
				sliderHeight.getValue());
		isHeightChanged = true;
		labelHeightValue.setText(String.valueOf(
				sliderHeight.getValue()));
	}

}
