package sqlrunner.export;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import sqlrunner.DBMessageDialog;
import sqlrunner.LongRunningAction;
import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.swinghelper.WindowHelper;
import dbtools.ConnectionDescription;

public class QueryExportFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JPanel jPanelQuery = null;
	private JScrollPane jScrollPaneQuery = null;
	private JTextArea jTextAreaQuery = null;
    private JTextField jTextFieldFetchSize = null;
	private JButton jButtonTakeover = null;
	private ExportFormatPanel exportFormatPanel = null;
	private TargetPanel targetPanel = null;
	private JPanel jPanelButtons = null;
	private JButton jButtonStart = null;
	private JButton jButtonCancel = null;
	private JButton jButtonClose = null;
	private JCheckBox jCheckBoxLineWrap = null;
	private StatusBar status = null;
	private MainFrame mainFrame = null;
	private boolean enabled = true;
	private transient Thread exportThread = null;
	
	/**
	 * This is the default constructor
	 */
	public QueryExportFrame(MainFrame mainFrame) {
		super();
		this.mainFrame = mainFrame;
		initialize();
		pack();
        WindowHelper.checkAndCorrectWindowBounds(this);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setContentPane(getJContentPane());
		this.setTitle(Messages.getString("QueryExportFrame.0")); //$NON-NLS-1$
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
            jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1;
                gbc.weighty = 10;
                jContentPane.add(getJPanelQuery(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                jContentPane.add(getExportFormatPanel(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                jContentPane.add(getTargetPanel(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 3;
                jContentPane.add(getJPanelButtons(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 4;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.SOUTH;
                jContentPane.add(getStatusBar(), gbc);
			}
		}
		return jContentPane;
	}

	/**
	 * This method initializes jPanelQuery	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelQuery() {
		if (jPanelQuery == null) {
            jPanelQuery = new JPanel();
			jPanelQuery.setLayout(new GridBagLayout());
			jPanelQuery.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Query", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.insets = new Insets(2, 20, 2, 2);
                gbc.anchor = GridBagConstraints.EAST;
                jPanelQuery.add(getJButtonTakeover(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 2, 2, 2);
                jPanelQuery.add(getJCheckBoxLineWrap(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.BOTH;
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.weighty = 1.0;
                gbc.gridwidth = 2;
                gbc.weightx = 1.0;
                jPanelQuery.add(getJScrollPaneQuery(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(2, 2, 2, 2);
                JLabel label = new JLabel();
                label.setText(Messages.getString("QueryExportFrame.fetchSize"));
                jPanelQuery.add(label, gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = 2;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 2.0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(2, 2, 2, 2);
                jPanelQuery.add(getJTextFieldFetchSize(), gbc);
			}
		}
		return jPanelQuery;
	}
    
    private JTextField getJTextFieldFetchSize() {
        if (jTextFieldFetchSize == null) {
            jTextFieldFetchSize = new JTextField();
            jTextFieldFetchSize.setText("1000");
        }
        return jTextFieldFetchSize;
    }

	/**
	 * This method initializes jScrollPaneQuery	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPaneQuery() {
		if (jScrollPaneQuery == null) {
			jScrollPaneQuery = new JScrollPane();
			jScrollPaneQuery.setPreferredSize(new Dimension(400, 100));
			jScrollPaneQuery.setViewportView(getJTextAreaQuery());
		}
		return jScrollPaneQuery;
	}

	/**
	 * This method initializes jTextAreaQuery	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJTextAreaQuery() {
		if (jTextAreaQuery == null) {
			jTextAreaQuery = new JTextArea();
		}
		return jTextAreaQuery;
	}

	/**
	 * This method initializes jButtonTakeover	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonTakeover() {
		if (jButtonTakeover == null) {
			jButtonTakeover = new JButton();
			jButtonTakeover.setText(Messages.getString("QueryExportFrame.1")); //$NON-NLS-1$
			jButtonTakeover.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
		            setQueryText(mainFrame.getText());
				}
				
			});
		}
		return jButtonTakeover;
	}

	/**
	 * This method initializes exportFormatPanel	
	 * 	
	 * @return sqlrunner.export.ExportFormatPanel	
	 */
	private ExportFormatPanel getExportFormatPanel() {
		if (exportFormatPanel == null) {
			exportFormatPanel = new ExportFormatPanel();
			exportFormatPanel.setDateSQLExpression(
					Main.getUserProperty(
	                "SQL_STRING_DATE_CONVERSION",  //$NON-NLS-1$
	                "to_date(\'<STRING>\',\'dd.MM.yy HH24:MI\')")); //$NON-NLS-1$
			exportFormatPanel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals(ExportFormatPanel.FLAT_FILE_SELECTED)) {
						getTargetPanel().enableFileTypeCSV(true);
						getTargetPanel().enableFileTypeXLS(true);
						getTargetPanel().enableFileTypeSQL(false);
					} else {
						getTargetPanel().enableFileTypeCSV(false);
						getTargetPanel().enableFileTypeXLS(false);
						getTargetPanel().enableFileTypeSQL(true);
					}
				}
			});
		}
		return exportFormatPanel;
	}

	/**
	 * This method initializes targetPanel	
	 * 	
	 * @return sqlrunner.export.TargetPanel	
	 */
	private TargetPanel getTargetPanel() {
		if (targetPanel == null) {
			targetPanel = new TargetPanel(false);
		}
		return targetPanel;
	}

	/**
	 * This method initializes jPanelButtons	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelButtons() {
		if (jPanelButtons == null) {
            jPanelButtons = new JPanel();
			jPanelButtons.setLayout(new GridBagLayout());
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.anchor = GridBagConstraints.WEST;
                jPanelButtons.add(getJButtonStart(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.insets = new Insets(2, 2, 2, 2);
                jPanelButtons.add(getJButtonCancel(), gbc);
			}
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 2;
                gbc.gridy = 0;
                gbc.insets = new Insets(2, 2, 2, 2);
                gbc.anchor = GridBagConstraints.EAST;
                jPanelButtons.add(getJButtonClose(), gbc);
			}
		}
		return jPanelButtons;
	}

	/**
	 * This method initializes jButtonStart	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonStart() {
		if (jButtonStart == null) {
			jButtonStart = new JButton();
			jButtonStart.setText(Messages.getString("QueryExportFrame.4")); //$NON-NLS-1$
			jButtonStart.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					startExport();
				}
				
			});
		}
		return jButtonStart;
	}
	
	private JFrame getFrame() {
		return this;
	}

	private void startExport() {
        Main.setUserProperty("SQL_STRING_DATE_CONVERSION", exportFormatPanel.getDateSQLExpression()); //$NON-NLS-1$
        if (jTextAreaQuery.getText().trim().length() > 1) {
            if ((mainFrame.getDatabase().getDatabaseSession()).isConnected()) {
                // in File oder in textfeld ?
                int exportType;
                if (exportFormatPanel.isExportInCSVFileSelected()) {
                    exportType = ExporterToTextFile.FILE;
                } else {
                    exportType = ExporterToTextFile.INSERT_FORMAT;
                }
                boolean ok = false;
                if (targetPanel.hasFileName() == false) {
                    JOptionPane.showMessageDialog(
                            this,
                            Messages.getString("ResultTableExportDialog.3"), //$NON-NLS-1$
                            Messages.getString("ResultTableExportDialog.4"), //$NON-NLS-1$
                            JOptionPane.INFORMATION_MESSAGE);
                    ok = false;
                    targetPanel.setFocusToFileName();
                } else {
                    ok = true;
                }
                if (ok) {
                    if (targetPanel.getFile().exists()) {
                        // Nachfragen ob überschrieben werden soll
                        final int answer = JOptionPane.showConfirmDialog(
                            this,
                            Messages.getString("ResultTableExportDialog.5"),
                            Messages.getString("ResultTableExportDialog.6"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                        if (answer == JOptionPane.YES_OPTION) {
                            ok = true;
                        } else {
                            ok = false;
                        }
                    } else {
                        ok = true;
                    }
                } // if (ok)
                if (ok) {
                    // in mehrere Files ?
                    int fetchSize = 0;
                    try {
                        String value = jTextFieldFetchSize.getText();
                        if (value != null) {
                            fetchSize = Integer.parseInt(value.trim());
                        }
                    } catch (NumberFormatException e) {
                        fetchSize = 0;
                    }
                    final ConnectionDescription cdExport = mainFrame.getDatabase().getDatabaseSession().getConnectionDescription().clone();
                    cdExport.setAutoCommit(false); // important for postgresql
                    cdExport.setDefaultFetchSize(fetchSize);
                    long maxRowsPerFile = 0;
                    if (targetPanel.shouldFileSplitted()) {
                    	maxRowsPerFile = targetPanel.getMaxLimitDatasetInSplittedFiles();
                    }
                    File targetFile = targetPanel.getFile();
                    if (targetFile.getName().toLowerCase().endsWith(".xls") || targetFile.getName().toLowerCase().endsWith(".xlsx")) {
                    	
                    } else {
                    	
                    }
                    final ExporterToTextFile exporter = new ExporterToTextFile(
                        cdExport, 
                        jTextAreaQuery.getText().trim(), 
                        targetFile);
                    exporter.setExportType(exportType);
                    exporter.setDelimiter(exportFormatPanel.getDelimiter());
                    exporter.setCharSet(Main.getFileEnoding());
                    exporter.setCreateHeader(exportFormatPanel.isWithHeaderSelected());
                    exporter.useCommaAsDecimalDelimiter(exportFormatPanel.isReplacePointWithCommaSelected());
                    exporter.setMaxDatasetsPerFile(maxRowsPerFile);
                    exporter.setEnclosure(exportFormatPanel.getEnclosure());
                    exporter.setDateFormat(MainFrame.getDateFormatMask());
                    exporter.setLineSeparator(Main.getCurrentLineSeparator());
                    final LongRunningAction lra = new LongRunningAction() {

                        public String getName() {
                            return "Export Query";
                        }

                        public void cancel() {
                            exporter.abort();
                        }

                        public boolean canBeCanceled() {
                            return true;
                        }

                    };
                    exportThread = new Thread() {
                        
                        @Override
                    	public void run() {
                    		SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									setEnabled(false);
									MainFrame.addLongRunningAction(lra);
				                    status.infoAction.setText("CONN"); //$NON-NLS-1$
				                    status.infoAction.setToolTipText(cdExport.toString());
				                    status.message.setText(Messages.getString("QueryExportFrame.18")); //$NON-NLS-1$
				                    status.infoAction.setBackground(Color.red);
								}
                    		});
                    		Timer timer = new Timer(1000, new ActionListener() {

								public void actionPerformed(ActionEvent e) {
									handleStatusInfo(exporter);
								}
                    			
                    		});
                    		timer.start();
                    		try {
                    			if (exporter.connect()) {
                            		exporter.exportData();
                    			}
                    		} catch (IOException ioe) {
                    			JOptionPane.showMessageDialog(
                                        getFrame(),
                                        ioe.getMessage(),
                                        Messages.getString("QueryExportFrame.27"), //$NON-NLS-1$
                                        JOptionPane.ERROR_MESSAGE);
                    		} catch (SQLException sqle) {
                    			new DBMessageDialog(
                                    getFrame(),
                                    sqle.getMessage(),
                                    Messages.getString("QueryExportFrame.26"));
                    		} finally {
                        		timer.stop();
                        		SwingUtilities.invokeLater(new Runnable() {
    								public void run() {
                                		handleStatusInfo(exporter); // ensure showing the latest messages
    				                    status.infoAction.setText("DISC"); //$NON-NLS-1$
    				                    status.infoAction.setToolTipText(Messages.getString("QueryExportFrame.31")); //$NON-NLS-1$
    				                    status.infoAction.setBackground(new Color(204, 204, 204));
    				                    setEnabled(true);
    				                    MainFrame.removeLongRunningAction(lra);
    								}
                        		});
                    		}
                    	}
                        
                    };
                    exportThread.start();
                } // if (ok)
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Messages.getString("QueryExportFrame.8"), //$NON-NLS-1$
                        Messages.getString("QueryExportFrame.9"), //$NON-NLS-1$
                        JOptionPane.ERROR_MESSAGE);
            } // if (mainFrame.database.getDatabaseSession().isConnected()
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    Messages.getString("QueryExportFrame.10"), //$NON-NLS-1$
                    Messages.getString("QueryExportFrame.11"), //$NON-NLS-1$
                    JOptionPane.ERROR_MESSAGE);
        }
	}
	
	private void handleStatusInfo(Exporter exporter) {
		switch (exporter.getStatus()) {
		case Exporter.CONNECTING:
			status.message.setText(Messages.getString("QueryExportFrame.connecting"));
			break;
		case Exporter.PARSING:
			
			break;
		case Exporter.SELECTING:
			status.message.setText(Messages.getString("QueryExportFrame.18"));
			break;
		case Exporter.FETCHING:
			status.message.setText(String.valueOf(exporter.getCurrentRowNum()) 
                + " " 
                + Messages.getString("QueryExportFrame.21"));
			break;
		case Exporter.CLOSING:
			
			break;
		case Exporter.FINISHED:
            status.message.setText(Messages.getString("QueryExportFrame.24") //$NON-NLS-1$
            		+ " "
                    + String.valueOf(exporter.getCurrentRowNum())
                    + Messages.getString("QueryExportFrame.25")); //$NON-NLS-1$			
			break;
		case Exporter.ABORTED:
            status.message.setText(Messages.getString("QueryExportFrame.22") //$NON-NLS-1$
            		+ " "
                    + String.valueOf(exporter.getCurrentRowNum())
                    + Messages.getString("QueryExportFrame.25")); //$NON-NLS-1$			
			break;
		}
	}
	
	/**
	 * This method initializes jButtonCancel	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonCancel() {
		if (jButtonCancel == null) {
			jButtonCancel = new JButton();
			jButtonCancel.setText(Messages.getString("QueryExportFrame.12")); //$NON-NLS-1$
			jButtonCancel.setEnabled(false);
			jButtonCancel.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
			        if ((exportThread != null) && exportThread.isAlive()) {
			        	exportThread.interrupt();
			        }
				}
				
			});
		}
		return jButtonCancel;
	}

	/**
	 * This method initializes jButtonClose	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJButtonClose() {
		if (jButtonClose == null) {
			jButtonClose = new JButton();
			jButtonClose.setText(Messages.getString("QueryExportFrame.13")); //$NON-NLS-1$
			jButtonClose.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					dispose();
				}
				
			});
		}
		return jButtonClose;
	}

	/**
	 * This method initializes jCheckBoxLineWrap	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBoxLineWrap() {
		if (jCheckBoxLineWrap == null) {
			jCheckBoxLineWrap = new JCheckBox();
			jCheckBoxLineWrap.setText(Messages.getString("QueryExportFrame.14")); //$NON-NLS-1$
			jCheckBoxLineWrap.addItemListener(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {
			        if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
			            jTextAreaQuery.setLineWrap(jCheckBoxLineWrap.isSelected());
			        }
				}
				
			});
		}
		return jCheckBoxLineWrap;
	}
	
	private StatusBar getStatusBar() {
		if (status == null) {
			status = new StatusBar();
		}
		return status;
	}

    static class StatusBar extends JPanel {

        private static final long serialVersionUID = 1L;
        public JLabel message            = new FixedLabel();
        public JLabel infoAction         = new FixedLabel();
        final static int INFO_ACTION_BREITE = 60;
        final static int HOEHE              = 25;

        public StatusBar() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            message.setBorder(BorderFactory.createLoweredBevelBorder());
            message.setForeground(Color.black);
            infoAction.setPreferredSize(new Dimension(INFO_ACTION_BREITE, HOEHE));
            infoAction.setOpaque(true);
            infoAction.setText("DISC"); //$NON-NLS-1$
            infoAction.setToolTipText(Messages.getString("QueryExportFrame.16")); //$NON-NLS-1$
            infoAction.setBorder(BorderFactory.createLoweredBevelBorder());
            infoAction.setForeground(Color.black);
            add(message);
            add(infoAction);
            // die zu erwartenden Property-Änderungen sind die setText()
            // Aufrufe, dann soll der ToolTip aktualisiert werden,
            // da dieser den Textinhalt voll anzeigen kann, auch wenn das Label
            // den Platz dafür nicht hat
            message.addPropertyChangeListener(new MeldungPropertyChangeListener());
            // eine bislang sinnvolle Nutzung der Spalte InfoAction
            // ist die Anzeige, ob Bak-Dateien erstellt werden oder nicht
        }

        // wird beim erstmaligen Darstellen , bei jeder FormÄnderung
        // und bei Aufrufen von repaint durchlaufen
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final int meldungBreite=this.getWidth() - INFO_ACTION_BREITE;
            message.setPreferredSize(new Dimension(meldungBreite, HOEHE));
            remove(message); // Label entfernen
            add(message, null, 0); // neu hinzufügen, damit neu eingepasst
            doLayout(); // damit wird die Änderung sofort wirksam !
        }

        // diese Klasse sichert, dass die Label auch in einer festen Groesse
        // dargestellt werden
        static private class FixedLabel extends JLabel {

            private static final long serialVersionUID = 1L;

            public FixedLabel() {
                super();
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

        }

        // Klasse stellt fest wenn Text in Meldung sich geaedert hat
        // und setzt als Tooltip den Inhalt des Textes
        class MeldungPropertyChangeListener implements PropertyChangeListener {

            public void propertyChange(PropertyChangeEvent evt) {
                message.setToolTipText(message.getText());
            }
        }

    }

    //überschreiben, damit das Programm bei Herunterfahren des Systems beendet werden kann
    @Override
    protected void processWindowEvent(WindowEvent winEvent) {
        switch (winEvent.getID()){
            case WindowEvent.WINDOW_CLOSING: {
                if(enabled){
                    dispose();}
                break;
            }
            default:
                super.processWindowEvent(winEvent);}
    }

    @Override
    public void setVisible(boolean visible) {
        if(!isShowing()) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
    	targetPanel.setEnabled(enabled);
    	exportFormatPanel.setEnabled(enabled);
    	jTextAreaQuery.setEditable(enabled);
    	jTextFieldFetchSize.setEditable(enabled);
    	jButtonTakeover.setEnabled(enabled);
    	jButtonStart.setEnabled(enabled);
    	jButtonCancel.setEnabled( ! enabled);
    	jButtonClose.setEnabled(enabled);
    	if (enabled) {
            this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    	} else {
            this.disableEvents(AWTEvent.WINDOW_EVENT_MASK);
    	}
    }

    public void setQueryText(String query) {
        jTextAreaQuery.setText(query);
    }


}  //  @jve:decl-index=0:visual-constraint="10,10"
