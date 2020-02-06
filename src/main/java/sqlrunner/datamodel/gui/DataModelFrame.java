package sqlrunner.datamodel.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import dbtools.ConnectionDescription;
import sqlrunner.Main;
import sqlrunner.MainFrame;
import sqlrunner.XmlFileFilter;
import sqlrunner.datamodel.DatamodelEvent;
import sqlrunner.datamodel.DatamodelListener;
import sqlrunner.datamodel.SQLCatalog;
import sqlrunner.datamodel.SQLConstraint;
import sqlrunner.datamodel.SQLDataModel;
import sqlrunner.datamodel.SQLField;
import sqlrunner.datamodel.SQLIndex;
import sqlrunner.datamodel.SQLObject;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLSequence;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.datamodel.gui.SQLDataTreeTableModel.ProcedureFolder;
import sqlrunner.datamodel.gui.SQLDataTreeTableModel.SequenceFolder;
import sqlrunner.datamodel.gui.SQLDataTreeTableModel.TableFolder;
import sqlrunner.datamodel.gui.SQLDataTreeTableModel.ViewFolder;
import sqlrunner.export.QueryExportFrame;
import sqlrunner.flatfileimport.gui.ImportConfiguratorFrame;
import sqlrunner.generator.SQLCodeGenerator;
import sqlrunner.swinghelper.WindowHelper;
import sqlrunner.talend.SchemaUtil;

public final class DataModelFrame extends JFrame {

	private static final Logger     logger = Logger.getLogger(DataModelFrame.class);
	private static final long       serialVersionUID = 1L;
    StatusBar                       status               = new StatusBar();
    private final JSplitPane        splitPaneModel        = new JSplitPane();
    private final JSplitPane        splitPaneMain        = new JSplitPane();
    private final JTextArea         commentTextArea      = new JTextArea();
    private final JScrollPane       jScrollPaneTree      = new JScrollPane();
    private final JScrollPane       jScrollPaneTable     = new JScrollPane();
    private final JTree             tree                 = new JTree();
    private final JTable            table                = new JTable();
    private final JMenuBar          menuBar              = new JMenuBar();
    private final JMenu             menuModel            = new JMenu();
    private final JMenuItem         miSchemaCompare      = new JMenuItem();
    private final JMenuItem         miClose              = new JMenuItem();
    private final JMenu             menuPreferences      = new JMenu();
    private final JCheckBoxMenuItem miFullQualifiedNames = new JCheckBoxMenuItem();
    private final JCheckBoxMenuItem miOverwrite          = new JCheckBoxMenuItem();
    private final JCheckBoxMenuItem miNewMainFrame       = new JCheckBoxMenuItem();
    private final JCheckBoxMenuItem miStartImmediately   = new JCheckBoxMenuItem();
    private SQLDataTreeTableModel   treeAndTableModel    = new SQLDataTreeTableModel();
    private MainFrame               mainFrame;
    private static DataModelFrame   dataModelFrame       = null;
    private transient SQLDataModel  currentDataModel     = null;
    private Thread                  selectCurrentSchemaThread = null;
    private CountPanel              countPanel           = new CountPanel();
    private JTextField              treeFilterTextField = null;
    
    private DataModelFrame() {
        try {
            initComponents();
            setAlwaysOnTop(true);
            pack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static DataModelFrame getDataModelFrame() {
        if (dataModelFrame == null) {
            dataModelFrame = new DataModelFrame();
        }
        return dataModelFrame;
    }

    @Override
    public void setVisible(boolean visible) {
        if (!isShowing()) {
            try {
                this.setLocationByPlatform(!(WindowHelper.isWindowPositioningEnabled()));
            } catch (NoSuchMethodError e) {}
        }
        super.setVisible(visible);
        if (visible) {
        	Thread t = new Thread(new Runnable() {
        		@Override
				public void run() {
                    treeAndTableModel.buildNodesForDataModels();
        		}
        	});
        	t.start();
        } else {
        	if (selectCurrentSchemaThread != null) {
        		try {
        			selectCurrentSchemaThread.interrupt();
        		} catch (Exception e) {}
        	}
        }
    }

    private void initComponents() throws Exception {
        setTitle(Messages.getString("DataModelFrame.title")); 
        setJMenuBar(menuBar);
        menuModel.setText(Messages.getString("DataModelFrame.1")); 
        menuBar.add(menuModel);
        miSchemaCompare.setText(Messages.getString("DataModelFrame.schemaCompare"));
        miSchemaCompare.addActionListener(compareSQLObjectAction);
        miClose.setText(Messages.getString("DataModelFrame.2"));
        miClose.addActionListener(closeAction);
        menuModel.add(miSchemaCompare);
        menuModel.add(miClose);
        menuPreferences.setText(Messages.getString("DataModelFrame.3")); 
        miFullQualifiedNames.setText(Messages.getString("DataModelFrame.4")); 
        miFullQualifiedNames.setSelected(Main.getUserProperty("FULL_QUALIFIED_NAME", "false").equals("true")); 
        miOverwrite.setSelected((Main.getUserProperty("DM_FRAME_SCRIPT_OVERWRITE", "true")).equals("true"));  
        miOverwrite.setText(Messages.getString("DataModelFrame.11")); 
        miNewMainFrame.setText(Messages.getString("DataModelFrame.12")); 
        miNewMainFrame.setSelected((Main.getUserProperty("DM_FRAME_NEW_FRAME", "false")).equals("true")); 
        miStartImmediately.setText(Messages.getString("DataModelFrame.16")); 
        miStartImmediately.setSelected((Main.getUserProperty("DM_FRAME_START_IMMEDIATELY", "false")).equals("true"));  
        menuBar.add(menuPreferences);
        menuPreferences.add(miFullQualifiedNames);
        menuPreferences.add(miOverwrite);
        menuPreferences.add(miNewMainFrame);
        menuPreferences.add(miStartImmediately);
        splitPaneModel.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPaneModel.setOneTouchExpandable(true);
        splitPaneMain.setPreferredSize(new Dimension(300, 800));
        splitPaneMain.setOneTouchExpandable(true);
        jScrollPaneTree.setPreferredSize(new Dimension(300, 400));
        splitPaneModel.setDividerLocation(400);
        jScrollPaneTable.setPreferredSize(new Dimension(300, 200));
        getContentPane().setLayout(new GridBagLayout());
        {
        	GridBagConstraints gbc = new GridBagConstraints();
        	gbc.gridx = 0;
        	gbc.gridy = 0;
        	gbc.anchor = GridBagConstraints.NORTH;
        	gbc.fill = GridBagConstraints.HORIZONTAL;
        	getContentPane().add(getTreeFilterPanel(), gbc);
        }
        {
        	GridBagConstraints gbc = new GridBagConstraints();
        	gbc.gridx = 0;
        	gbc.gridy = 1;
        	gbc.anchor = GridBagConstraints.NORTH;
        	gbc.weightx = 1;
        	gbc.weighty = 2;
        	gbc.fill = GridBagConstraints.BOTH;
        	getContentPane().add(splitPaneMain, gbc);
        }
        {
        	GridBagConstraints gbc = new GridBagConstraints();
        	gbc.gridx = 0;
        	gbc.gridy = 2;
        	gbc.anchor = GridBagConstraints.SOUTH;
        	gbc.fill = GridBagConstraints.HORIZONTAL;
        	getContentPane().add(status, gbc);
        }
        splitPaneModel.add(jScrollPaneTree, JSplitPane.TOP);
        splitPaneModel.add(jScrollPaneTable, JSplitPane.BOTTOM);
        splitPaneMain.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPaneMain.add(splitPaneModel, JSplitPane.TOP);
        splitPaneMain.add(getInfoTabbedPane(), JSplitPane.BOTTOM);
        jScrollPaneTree.setViewportView(tree);
        jScrollPaneTable.setViewportView(table);
        table.addMouseListener(new TableMouseListener());
        table.setDefaultRenderer(String.class, new FieldTableRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (table.hasFocus()) {
					SQLField field = treeAndTableModel.getCurrentSQLField();
					refreshInfoFor(field);
				}
			}
			
		});
        table.getSelectionModel().addListSelectionListener(treeAndTableModel);
        logger.debug("set table model...");
        table.setModel(treeAndTableModel);
        table.setTransferHandler(new SQLDataModelTransferHandler(treeAndTableModel));
        table.setDragEnabled(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(200); // field name 
        table.getColumnModel().getColumn(1).setPreferredWidth(70); // type 
        table.getColumnModel().getColumn(2).setPreferredWidth(20); // length 
        table.getColumnModel().getColumn(3).setPreferredWidth(20); // precision 
        table.getColumnModel().getColumn(4).setPreferredWidth(25); // is null enabled 
        updateKeyActionMapForTable();
        logger.debug("set tree model...");
        tree.setModel(treeAndTableModel);
        tree.setRootVisible(false);
        tree.addMouseListener(new TreeMouseListener());
        tree.addTreeSelectionListener(treeAndTableModel);
        tree.addTreeWillExpandListener(treeAndTableModel);
        tree.setCellRenderer(new DataModelTreeCellRenderer());
        tree.addTreeSelectionListener(new TreeSelectionListener() {
			
        	@Override
			public void valueChanged(TreeSelectionEvent e) {
				setupTreeContextMenu();
			}
			
		});
        treeAndTableModel.addTableModelListener(new TableModelListener() {

        	@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.INSERT) {
					String message = (e.getLastRow() - e.getFirstRow() + 1) + " columns";
					status.messageLabel.setText(message);
				}
			}
        	
        });
        updateKeyActionMapForTree();
        commentTextArea.setEditable(false);
    }
    
    private void refreshInfoFor(SQLObject object) {
    	if (object instanceof SQLField) {
    		SQLField field = (SQLField) object;
    		commentTextArea.setText(field.getComment());
    	} else if (object instanceof SQLTable) {
    		SQLTable table = (SQLTable) object;
    		commentTextArea.setText(table.getComment());
    		countPanel.setSQLObject(object);
    	} else if (object instanceof SQLSchema) {
    		countPanel.setSQLObject(object);
    	} else if (object instanceof SQLProcedure) {
    		SQLProcedure table = (SQLProcedure) object;
    		commentTextArea.setText(table.getComment());
    	} else {
    		commentTextArea.setText(null);
    	}
    }
    
    private JTabbedPane getInfoTabbedPane() {
    	JTabbedPane infoTabbedPane = new JTabbedPane();
        infoTabbedPane.add("Count", countPanel);
    	JScrollPane spComment = new JScrollPane();
    	spComment.setViewportView(commentTextArea);
        infoTabbedPane.add("comment", spComment);
        infoTabbedPane.setMaximumSize(infoTabbedPane.getPreferredSize());
    	return infoTabbedPane;
    }
    
    private void updateFilterTreeModel() {
		treeAndTableModel.setObjectFilter(treeFilterTextField.getText());
    }
    
    private JPanel getTreeFilterPanel() {
    	if (treeFilterTextField == null) {
    		treeFilterTextField = new JTextField();
/*
    		treeFilterTextField.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					updateFilterTreeModel();
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					updateFilterTreeModel();
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					updateFilterTreeModel();
				}
				
			});
*/ 
    		treeFilterTextField.addKeyListener(new KeyListener() {
				
				@Override
				public void keyTyped(KeyEvent e) {
					if (e.getKeyChar() == KeyEvent.VK_ENTER) {
						treeAndTableModel.setObjectFilter(treeFilterTextField.getText());
				    	tree.clearSelection();
						treeAndTableModel.filterCurrentNodeChildren();
					}
				}
				
				@Override
				public void keyReleased(KeyEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void keyPressed(KeyEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
    	}
    	JPanel panel = new JPanel();
    	panel.setLayout(new GridBagLayout());
    	{
    		GridBagConstraints gbc = new GridBagConstraints();
    		gbc.gridx = 0;
    		gbc.gridy = 0;
    		gbc.gridwidth = 2;
    		JLabel label = new JLabel();
        	label.setText(Messages.getString("DataModelFrame.filter"));
        	panel.add(label, gbc);
    	}
    	{
    		GridBagConstraints gbc = new GridBagConstraints();
    		gbc.gridx = 0;
    		gbc.gridy = 1;
    		gbc.gridwidth = 1;
    		gbc.weightx = 1;
    		gbc.fill = GridBagConstraints.HORIZONTAL;
        	panel.add(treeFilterTextField, gbc);
    	}
    	{
    		GridBagConstraints gbc = new GridBagConstraints();
    		gbc.gridx = 1;
    		gbc.gridy = 1;
    		JButton button = new JButton("X");
    		button.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
			    	tree.clearSelection();
					treeFilterTextField.setText(null);
					treeAndTableModel.filterCurrentNodeChildren();
				}
			});
        	panel.add(button, gbc);
    	}
    	return panel;
    }
    
    private Action f2Action = new AbstractAction("f2") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (mainFrame != null) {
				mainFrame.toFront();
				mainFrame.requestFocus();
			}
		}
    	
    };

    private Action enterInTreeAction = new AbstractAction("enter-tree") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			sendSelectedTreeElementNameToEditor();
		}
    	
    };
    
    private Action enterInTableAction = new AbstractAction("enter-table") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			sendSelectedTableElementNameToEditor();
		}
    	
    };

    private Action sendFieldDeclsAction = new AbstractAction("send-field-decls") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			sendSelectedFieldDeclarationsToEditor();
		}
    	
    };

    private Action sendDropFieldAction = new AbstractAction("send-drop-field") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			sendDropFieldToEditor();
		}
    	
    };

    private void updateKeyActionMapForTree() {
    	InputMap inputMap = tree.getInputMap();
    	ActionMap actionMap = tree.getActionMap();
    	final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
    	inputMap.put(enter, "enter");
    	actionMap.put("enter", enterInTreeAction);
    	final KeyStroke f2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, false);
    	inputMap.put(f2, "F2");
    	actionMap.put("F2", f2Action);
    }

    private void updateKeyActionMapForTable() {
    	InputMap inputMap = table.getInputMap();
    	ActionMap actionMap = table.getActionMap();
    	final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
    	inputMap.put(enter, "enter");
    	actionMap.put("enter", enterInTableAction);
    	final KeyStroke f2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, false);
    	inputMap.put(f2, "F2");
    	actionMap.put("F2", f2Action);
    }

    @Override
    protected void processWindowEvent(WindowEvent winEvent) {
        switch (winEvent.getID()) {
            case WindowEvent.WINDOW_CLOSING: {
                if (mainFrame != null) {
                    mainFrame.closeDMFrame();
                }
                break;
            }
            case WindowEvent.WINDOW_OPENED: {
            	SwingUtilities.invokeLater(new Runnable() {
            		@Override
					public void run() {
            			try {
							Thread.sleep(100);
						} catch (InterruptedException e) {}
                		int divlocMain = splitPaneMain.getSize().height - countPanel.getMaximumSize().height - 10;
                        splitPaneMain.setDividerLocation(divlocMain);
                		int divlocModel = splitPaneModel.getSize().height - jScrollPaneTree.getPreferredSize().height;
                		splitPaneModel.setDividerLocation(divlocModel);
            		}
            	});
            	break;
            }
            case WindowEvent.WINDOW_ACTIVATED: {
            	SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						tree.requestFocusInWindow();
					}
				});
            }
        }
        super.processWindowEvent(winEvent);
    }

    public boolean isScriptOverwriteSelected() {
        return miOverwrite.isSelected();
    }

    public boolean isSelectInNewFrameSelected() {
        return miNewMainFrame.isSelected();
    }

    public boolean isStartImmediatelySelected() {
        return miStartImmediately.isSelected();
    }
    
    public int getModelCount() {
    	return treeAndTableModel.getModelCount();
    }
    
    public boolean useFullName() {
    	if (mainFrame != null) {
        	return miFullQualifiedNames.isSelected() || 
        		treeAndTableModel.isCurrentSchema(
        				currentDataModel.getLoginSchemaName()) == false;
    	} else {
    		return false;
    	}
    }
    
    public boolean isFullQualifiedNameSelected() {
    	return miFullQualifiedNames.isSelected();
    }
    
    final Action selectStarAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                String sql;
                if (useFullName()) {
                    sql = "select * from " + treeAndTableModel.getCurrentSQLTable().getAbsoluteName();
                } else {
                    sql = "select * from " + treeAndTableModel.getCurrentSQLTable().getName();
                }
                if (miNewMainFrame.isSelected()) {
                    MainFrame newMainFrame = Main.createInstance(mainFrame);
                    if (miStartImmediately.isSelected()) {
                        newMainFrame.start(sql);
                    } else {
                        newMainFrame.setScriptText(sql);
                    }
                } else {
                    if (miStartImmediately.isSelected()) {
                        mainFrame.start(sql);
                    } else {
                        if (miOverwrite.isSelected()) {
                            mainFrame.setScriptText(sql);
                        } else {
                            mainFrame.insertOrReplaceText(sql);
                        }
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action selectAllAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                final String sql = SQLCodeGenerator.getInstance().buildSelectStatement(
                        treeAndTableModel.getCurrentSQLTable(),
                        useFullName());
                if (miNewMainFrame.isSelected()) {
                    MainFrame newMainFrame = Main.createInstance(mainFrame);
                    if (miStartImmediately.isSelected()) {
                        newMainFrame.start(sql);
                    } else {
                        newMainFrame.setScriptText(sql);
                    }
                } else {
                    if (miOverwrite.isSelected()) {
                        if (miStartImmediately.isSelected()) {
                            mainFrame.start(sql);
                        } else {
                            mainFrame.setScriptText(sql);
                        }
                    } else {
                        mainFrame.insertOrReplaceText(sql);
                    }
                }
                mainFrame.setState(Frame.NORMAL);
                mainFrame.toFront();
            }
		}
    	
    };

    final Action selectAllCoalesceAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                final String sql = SQLCodeGenerator.getInstance().buildSelectStatement(
                        treeAndTableModel.getCurrentSQLTable(),
                        useFullName(), true);
                if (miNewMainFrame.isSelected()) {
                    MainFrame newMainFrame = Main.createInstance(mainFrame);
                    if (miStartImmediately.isSelected()) {
                        newMainFrame.start(sql);
                    } else {
                        newMainFrame.setScriptText(sql);
                    }
                } else {
                    if (miOverwrite.isSelected()) {
                        if (miStartImmediately.isSelected()) {
                            mainFrame.start(sql);
                        } else {
                            mainFrame.setScriptText(sql);
                        }
                    } else {
                        mainFrame.insertOrReplaceText(sql);
                    }
                }
                mainFrame.setState(Frame.NORMAL);
                mainFrame.toFront();
            }
		}
    	
    };

    final Action insertAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildInsertStatement(
                                treeAndTableModel.getCurrentSQLTable(),
                                useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildInsertStatement(
                                treeAndTableModel.getCurrentSQLTable(),
                                useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action psInsertAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildPSInsertStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildPSInsertStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action psUpdateAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildPSUpdateStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildPSUpdateStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };
    
    final Action psCountAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildPSCountStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildPSCountStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action psDeleteTableAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildPSDeleteStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildPSDeleteStatement(treeAndTableModel.getCurrentSQLTable(), useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action createTableAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
            	new Thread() {
            		
            		@Override
					public void run() {
            			SwingUtilities.invokeLater(new Runnable() {
            				@Override
							public void run() {
            					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            				}
            			});
            			final StringBuilder sql = new StringBuilder();
            			final List<SQLTable> list = SQLCodeGenerator.sortByForeignKeys(treeAndTableModel.getCurrentSelectedSQLTables());
            			for (SQLTable table : list) {
            				sql.append(SQLCodeGenerator.getInstance().buildCreateStatement(table, useFullName()));
            				sql.append("\n\n");
            			}
                    	SwingUtilities.invokeLater(new Runnable() {

                            @Override
							public void run() {
                                if (list.isEmpty() == false) {
                                    if (miOverwrite.isSelected()) {
                                        mainFrame.setScriptText(sql.toString());
                                        mainFrame.setTextSaveEnabled(false);
                                    } else {
                                        mainFrame.insertOrReplaceText(sql.toString());
                                    }
                                    mainFrame.setState(Frame.NORMAL);
                                    mainFrame.toFront();
                                }
            					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                mainFrame.setState(Frame.NORMAL);
                                mainFrame.toFront();
                            }

                        });
            		}
            		
            	}.start();
            }
		}
    	
    };

    final Action createViewAsTableAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildCreateStatement(treeAndTableModel.getCurrentSQLTable(), useFullName(), true));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildCreateStatement(treeAndTableModel.getCurrentSQLTable(), useFullName(), true));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action dropTableAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
    			StringBuilder sql = new StringBuilder();
    			List<SQLTable> list = treeAndTableModel.getCurrentSelectedSQLTables();
    			list = SQLCodeGenerator.sortByForeignKeys(list);
    			for (int i = list.size() - 1; i >= 0; i--) {
    				SQLTable table = list.get(i);
    				sql.append(SQLCodeGenerator.getInstance().buildDropStatement(table, useFullName()));
    				sql.append(";\n");
    			}
                if (list.isEmpty() == false) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(sql.toString());
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(sql.toString());
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action deleteTableAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
    			StringBuilder sql = new StringBuilder();
    			List<SQLTable> list = treeAndTableModel.getCurrentSelectedSQLTables();
    			list = SQLCodeGenerator.sortByForeignKeys(list);
    			for (int i = list.size() - 1; i >= 0; i--) {
    				SQLTable table = list.get(i);
    				sql.append(SQLCodeGenerator.getInstance().buildDeleteStatement(table, useFullName()));
    				sql.append(";\n");
    			}
                if (list.isEmpty() == false) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(sql.toString());
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(sql.toString());
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    	
    };

    final Action createAllTablesAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (mainFrame.isConnectedAndReady()) {
                    if (treeAndTableModel.getCurrentSQLSchema() != null) {
                    	new Thread() {
                    		
                    		@Override
							public void run() {
                    			SwingUtilities.invokeLater(new Runnable() {
                    				@Override
									public void run() {
                    					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    				}
                    			});
                            	final String code = SQLCodeGenerator.getInstance().buildCreateTableStatements(treeAndTableModel.getCurrentSQLSchema(), useFullName());
                            	SwingUtilities.invokeLater(new Runnable() {

                                    @Override
									public void run() {
                                        if (miOverwrite.isSelected()) {
                                        	mainFrame.setScriptText(code);
                                            mainFrame.setTextSaveEnabled(false);
                                        } else {
                                            mainFrame.insertOrReplaceText(code);
                                        }
                    					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                        mainFrame.setState(Frame.NORMAL);
                                        mainFrame.toFront();
                                    }

                                });
                    		}
                    		
                    	}.start();
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            DataModelFrame.this,
                            Messages.getString("DataModelFrame.notready"), 
                            Messages.getString("DataModelFrame.refreshschemas"), 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
		}
    	
    };

    final Action refreshDBAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (mainFrame.isConnectedAndReady()) {
                    new Thread() {

                        @Override
						public void run() {
                			SwingUtilities.invokeLater(new Runnable() {
                				@Override
								public void run() {
                					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                				}
                			});
                            treeAndTableModel.refresh(treeAndTableModel.getCurrentSQLDataModel(), treeAndTableModel.getCurrentNode());
                			SwingUtilities.invokeLater(new Runnable() {
                				@Override
								public void run() {
                					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                				}
                			});
                        }

                    }.start();
                } else {
                    JOptionPane.showMessageDialog(
                            DataModelFrame.this,
                            Messages.getString("DataModelFrame.notready"), 
                            Messages.getString("DataModelFrame.refreshschemas"), 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
		}
    	
    };

    final Action refreshSchemaAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (mainFrame.isConnectedAndReady()) {
                    if (treeAndTableModel.getCurrentSQLSchema() != null) {
                        new Thread() {

                            @Override
							public void run() {
                    			SwingUtilities.invokeLater(new Runnable() {
                    				@Override
									public void run() {
                    					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    				}
                    			});
                                treeAndTableModel.refresh(treeAndTableModel.getCurrentSQLSchema(), treeAndTableModel.getCurrentNode(), true);
                    			SwingUtilities.invokeLater(new Runnable() {
                    				@Override
									public void run() {
                    					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    				}
                    			});
                            }

                        }.start();
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            DataModelFrame.this,
                            Messages.getString("DataModelFrame.notready"), 
                            Messages.getString("DataModelFrame.refreshtables"), 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
		}
    };
			
    final Action refreshCatalogAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (mainFrame.isConnectedAndReady()) {
                    if (treeAndTableModel.getCurrentSQLCatalog() != null) {
                        new Thread() {

                            @Override
							public void run() {
                    			SwingUtilities.invokeLater(new Runnable() {
                    				@Override
									public void run() {
                    					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    				}
                    			});
                                treeAndTableModel.refresh(treeAndTableModel.getCurrentSQLCatalog(), treeAndTableModel.getCurrentNode(), true);
                    			SwingUtilities.invokeLater(new Runnable() {
                    				@Override
									public void run() {
                    					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    				}
                    			});
                            }

                        }.start();
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            DataModelFrame.this,
                            Messages.getString("DataModelFrame.notready"), 
                            Messages.getString("DataModelFrame.refreshtables"), 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
		}
    };

    final Action refreshTableAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
		    if (mainFrame != null) {
		        if (mainFrame.isConnectedAndReady()) {
		            if (treeAndTableModel.getCurrentSQLSchema() != null) {
		                new Thread() {
		
		                    @Override
							public void run() {
		            			SwingUtilities.invokeLater(new Runnable() {
		            				@Override
									public void run() {
		            					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		                    			status.messageLabel.setText("refresh current table");
		            				}
		            			});
		                        treeAndTableModel.refreshCurrentTable();
		            			SwingUtilities.invokeLater(new Runnable() {
		            				@Override
									public void run() {
		            					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		            				}
		            			});
		                    }
		
		                }.start();
		            }
		        } else {
		            JOptionPane.showMessageDialog(
		                    DataModelFrame.this,
		                    Messages.getString("DataModelFrame.notready"), 
		                    Messages.getString("DataModelFrame.refreshfields"), 
		                    JOptionPane.INFORMATION_MESSAGE);
		        }
		    }
		}
    };

    
    final Action dropAllAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLSchema() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildDropTableStatements(
                                treeAndTableModel.getCurrentSQLSchema(),
                                useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildDropTableStatements(
                                treeAndTableModel.getCurrentSQLSchema(),
                                useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    };

    final Action deleteAllAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLSchema() != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildDeleteTableStatements(
                                treeAndTableModel.getCurrentSQLSchema(),
                                useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildDeleteTableStatements(
                                treeAndTableModel.getCurrentSQLSchema(),
                                useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    };

    final Action selectFieldAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                String sql;
                if (useFullName()) {
                    sql = "select " 
                            + treeAndTableModel.getCurrentSQLField().getName()
                            + " from " 
                            + treeAndTableModel.getCurrentSQLTable().getAbsoluteName();
                } else {
                    sql = "select " 
                            + treeAndTableModel.getCurrentSQLField().getName()
                            + " from " 
                            + treeAndTableModel.getCurrentSQLTable().getName();
                }
                if (miNewMainFrame.isSelected()) {
                    mainFrame = Main.createInstance(mainFrame);
                    // nun müsste mainFrame eine neue Instanz darstellen
                    mainFrame.start(sql);
                } else {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(sql);
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(sql);
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    };

    final Action addFieldAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLField() != null) {
                    if (miOverwrite.isSelected()) {
                    	if (useFullName()) {
                            mainFrame.setScriptText("alter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getAbsoluteName()
                                    + " add my_name my_type;"); 
                    	} else {
                            mainFrame.setScriptText("alter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getName()
                                    + " add my_name my_type;"); 
                    	}
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                    	if (useFullName()) {
                            mainFrame.insertOrReplaceText("\nalter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getAbsoluteName()
                                    + " add NeuesFeld FeldType ;"); 
                    	} else {
                            mainFrame.insertOrReplaceText("\nalter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getName()
                                    + " add NeuesFeld FeldType ;"); 
                    	}
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    };

    final Action dropFieldAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (miOverwrite.isSelected()) {
                    	if (useFullName()) {
                            mainFrame.setScriptText("alter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getAbsoluteName()
                                    + " drop column " 
                                    + treeAndTableModel.getCurrentSQLField().getName()
                                    + ";"); 
                    	} else {
                            mainFrame.setScriptText("alter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getName()
                                    + " drop column " 
                                    + treeAndTableModel.getCurrentSQLField().getName()
                                    + ";"); 
                    	}
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                    	if (useFullName()) {
                            mainFrame.insertOrReplaceText("\nalter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getAbsoluteName()
                                    + " drop column " 
                                    + treeAndTableModel.getCurrentSQLField().getName()
                                    + ";"); 
                    	} else {
                            mainFrame.insertOrReplaceText("\nalter table " 
                                    + treeAndTableModel.getCurrentSQLTable().getName()
                                    + " drop column " 
                                    + treeAndTableModel.getCurrentSQLField().getName()
                                    + ";"); 
                    	}
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    };

    final Action queryExportAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    final QueryExportFrame exportDBTableDialog = new QueryExportFrame(mainFrame);
                    WindowHelper.locateWindowAtMiddle(mainFrame, exportDBTableDialog);
                    exportDBTableDialog.setVisible(true);
                    exportDBTableDialog.setQueryText(SQLCodeGenerator.getInstance().buildSelectStatement(
                            treeAndTableModel.getCurrentSQLTable(),
                            useFullName()));
                }
            }
		}
    };

    final Action fileImportAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
                if (treeAndTableModel.getCurrentSQLTable() != null) {
                    if (useFullName()) {
                        ImportConfiguratorFrame frame = new ImportConfiguratorFrame(mainFrame.getDatabase(), treeAndTableModel.getCurrentSQLTable().getAbsoluteName());
                        WindowHelper.locateWindowAtMiddle(mainFrame, frame);
                        frame.setVisible(true);
                    } else {
                        ImportConfiguratorFrame frame = new ImportConfiguratorFrame(mainFrame.getDatabase(), treeAndTableModel.getCurrentSQLTable().getName());
                        WindowHelper.locateWindowAtMiddle(mainFrame, frame);
                        frame.setVisible(true);
                    }
                }
            }
		}
    };

    final Action exportAsTalendSchema = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (mainFrame != null) {
            	SQLTable table = treeAndTableModel.getCurrentSQLTable();
                if (table != null) {
                	// Save file dialog
                    final JFileChooser chooser = new JFileChooser();
                    String lastDir = Main.getUserProperty("talend.schema.xml.dir");
                    if (lastDir != null) {
                    	File f = new File(lastDir, table.getName() + ".xml");
                        chooser.setCurrentDirectory(f.getParentFile());
                        chooser.setSelectedFile(f);
                    } else {
                        final String directory = Main.getUserProperty("SCRIPT_DIR", System.getProperty("user.home"));  
                    	File f = new File(directory, table.getName() + ".xml");
                        chooser.setCurrentDirectory(f.getParentFile());
                        chooser.setSelectedFile(f);
                    }
                    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                    chooser.setMultiSelectionEnabled(false);
                    chooser.setDialogTitle(Messages.getString("MainFrame.122")); 
                    chooser.addChoosableFileFilter(new XmlFileFilter());
                    final int returnVal = chooser.showSaveDialog(DataModelFrame.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File f = chooser.getSelectedFile();
                        if (f.getName().toLowerCase().endsWith(".xml") == false) { 
                            f = new File(f.getAbsolutePath() + ".xml"); 
                        }
                        Main.setUserProperty("talend.schema.xml.dir", f.getParentFile().getAbsolutePath()); 
                        SchemaUtil util = new SchemaUtil();
                        try {
							util.writeSchemaFile(f, table);
						} catch (IOException e1) {
							logger.error("writeSchemaFile f=" + f.getAbsolutePath() + " failed: " + e1.getMessage(), e1);
							JOptionPane.showMessageDialog(DataModelFrame.this, "writeSchemaFile f=" + f.getAbsolutePath() + " failed: " + e1.getMessage());
						}
                    }
                }
            }
		}
    };

    final Action dropConstraintAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            if (treeAndTableModel.getCurrentSQLConstraint() != null) {
                if (mainFrame != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(treeAndTableModel.getCurrentSQLConstraint().getDropStatement(useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(treeAndTableModel.getCurrentSQLConstraint().getDropStatement(useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
		}
    };

    final Action callProcedureAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
        	if (treeAndTableModel.getCurrentSQLProcedure() != null) {
        		if (mainFrame != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildPreparedCallStatement(treeAndTableModel.getCurrentSQLProcedure(), useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildPreparedCallStatement(treeAndTableModel.getCurrentSQLProcedure(), useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
        		}
        	}
		}
    };

    final Action sqlCallProcedureAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
        	if (treeAndTableModel.getCurrentSQLProcedure() != null) {
        		if (mainFrame != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildSQLCallStatement(treeAndTableModel.getCurrentSQLProcedure(), useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildSQLCallStatement(treeAndTableModel.getCurrentSQLProcedure(), useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
        		}
        	}
		}
    };

    final Action dropProcedureAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
    		if (mainFrame != null) {
    			StringBuilder sql = new StringBuilder();
    			List<SQLProcedure> list = treeAndTableModel.getCurrentSelectedSQLProcedures();
    			for (SQLProcedure proc : list) {
    				sql.append(SQLCodeGenerator.getInstance().buildDropStatement(proc, useFullName()));
    				sql.append(";\n");
    			}
    			if (list.isEmpty() == false) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(sql.toString());
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(sql.toString());
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
    			}
    		}
		}
    };

    final Action callSequenceAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
        	if (treeAndTableModel.getCurrentSQLSequence() != null) {
        		if (mainFrame != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(treeAndTableModel.getCurrentSQLSequence().getNextvalCode());
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(treeAndTableModel.getCurrentSQLSequence().getNextvalCode());
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
        		}
        	}
		}
    };

    final Action createSequenceAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
        	if (treeAndTableModel.getCurrentSQLSequence() != null) {
        		if (mainFrame != null) {
        			StringBuilder sql = new StringBuilder();
        			List<SQLSequence> list = treeAndTableModel.getCurrentSelectedSQLSequences();
        			for (SQLSequence seq : list) {
        				sql.append(seq.getCreateCode());
        				sql.append(";\n");
        			}
        			if (list.isEmpty() == false) {
                        if (miOverwrite.isSelected()) {
                            mainFrame.setScriptText(sql.toString());
                            mainFrame.setTextSaveEnabled(false);
                        } else {
                            mainFrame.insertOrReplaceText(sql.toString());
                        }
                        mainFrame.setState(Frame.NORMAL);
                        mainFrame.toFront();
        			}
        		}
        	}
		}
    };

    final Action dropSequenceAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
    		if (mainFrame != null) {
    			StringBuilder sql = new StringBuilder();
    			List<SQLSequence> list = treeAndTableModel.getCurrentSelectedSQLSequences();
    			for (SQLSequence seq : list) {
    				sql.append(SQLCodeGenerator.getInstance().buildDropStatement(seq, useFullName()));
    				sql.append(";\n");
    			}
    			if (list.isEmpty() == false) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(sql.toString());
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(sql.toString());
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
    			}
    		}
		}
    };

    final Action createIndexAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
        	if (treeAndTableModel.getCurrentSQLIndex() != null) {
        		if (mainFrame != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildCreateStatement(treeAndTableModel.getCurrentSQLIndex(), useFullName()));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildCreateStatement(treeAndTableModel.getCurrentSQLIndex(), useFullName()));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
        		}
        	}
		}
    };

    final Action createProcedureAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
    		if (mainFrame != null) {
    			StringBuilder sql = new StringBuilder();
    			List<SQLProcedure> list = treeAndTableModel.getCurrentSelectedSQLProcedures();
    			for (SQLProcedure proc : list) {
    				sql.append(SQLCodeGenerator.getInstance().buildCreateStatement(proc, useFullName(), null));
    				sql.append(";\n");
    			}
    			if (list.isEmpty() == false) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(sql.toString());
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(sql.toString());
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
    			}
    		}
		}
    };

    final Action dropIndexAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
        	if (treeAndTableModel.getCurrentSQLIndex() != null) {
        		if (mainFrame != null) {
                    if (miOverwrite.isSelected()) {
                        mainFrame.setScriptText(SQLCodeGenerator.getInstance().buildDropStatement(treeAndTableModel.getCurrentSQLIndex(), useFullName(), null));
                        mainFrame.setTextSaveEnabled(false);
                    } else {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildDropStatement(treeAndTableModel.getCurrentSQLIndex(), useFullName(), null));
                    }
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
        		}
        	}
		}
    };

    final Action closeAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
            setVisible(false);
		}
    };

    final Action compareSQLObjectAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

    	@Override
		public void actionPerformed(ActionEvent e) {
			Object s1 = treeAndTableModel.getCurrentUserObject();
			Object s2 = treeAndTableModel.getNextSelectedUserObject();
			if ((s1 instanceof SQLSchema && s2 instanceof SQLSchema) || (s1 instanceof SQLTable && s2 instanceof SQLTable)) {
				DataModelCompareFrame cf = new DataModelCompareFrame();
				cf.setObject1((SQLObject) s1);
				cf.setObject2((SQLObject) s2);
				cf.setVisible(true);
		        WindowHelper.locateWindowAtMiddleOfDefaultScreen(cf);
		        cf.pack();
			} else {
				JOptionPane.showMessageDialog(DataModelFrame.this, Messages.getString("DataModelFrame.select2Schemas"), "Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
    };
    
    public void setMainFrame(MainFrame newMainFrame) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("setMainFrame: " + newMainFrame.getTitle());
    	}
    	ConnectionDescription newCd = newMainFrame.getCurrentConnectionDescription();
        ConnectionDescription currCd = this.mainFrame != null ? this.mainFrame.getCurrentConnectionDescription() : null;
    	this.mainFrame = newMainFrame;
    	currentDataModel = newMainFrame.getCurrentDataModel();
        if (newCd != null) {
        	if (newCd.equals(currCd) == false) {
        		selectCurrentSchema();
        	} else {
        		if (currentDataModel != null) {
            		DefaultMutableTreeNode dbNode = treeAndTableModel.findAndCreateNode(currentDataModel);
            		if (dbNode != null) {
                		TreePath path = new TreePath(dbNode.getPath());
                		tree.expandPath(path);
            		}
        		}
        	}
        }
    }
    
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public void addSQLDatamodel(final SQLDataModel model) {
    	if (treeAndTableModel.addUniqueSQLDataModel(model)) {
    		treeFilterTextField.setText(null);
    		model.addDatamodelListener(new DatamodelListener() {
				
            	@Override
				public void eventHappend(final DatamodelEvent event) {
					if (SwingUtilities.isEventDispatchThread()) {
						status.messageLabel.setText(event.getMessage());
					} else {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								status.messageLabel.setText(event.getMessage());
							}
						});
					}
				}
			});
    	}
		final SQLDataTreeTableModel localModel = treeAndTableModel;
		SwingUtilities.invokeLater(new Runnable() {
			
        	@Override
			public void run() {
		    	DefaultMutableTreeNode modelNode = localModel.findTreeNodeByUserObject(model);
				TreePath treePath = new TreePath(modelNode.getPath());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				tree.expandPath(treePath);
			}
		});
    }
    
    public void selectCurrentSchema() {
    	if (currentDataModel != null) {
    		selectCurrentSchemaThread = new Thread() {
    			
    			@Override
    			public void run() {
    				try {
						Thread.sleep(500); // wait until schemas loaded
					} catch (InterruptedException e) {
						logger.error("selectCurrentSchema interrupted");
					}
					SQLSchema currentSchema = currentDataModel.getCurrentSQLSchema();
					if (currentSchema != null) {
				    	if (logger.isDebugEnabled()) {
				    		logger.debug("selectCurrentSchema currentSchema=" + currentSchema + " currentDataModel=" + currentDataModel);
				    	}
			        	final DefaultMutableTreeNode schemaNode = treeAndTableModel.findAndCreateNode(currentSchema);
			        	final TreePath path;
			        	if (schemaNode != null) {
			            	path = new TreePath(schemaNode.getPath());
			        	} else {
			        		DefaultMutableTreeNode dbNode = treeAndTableModel.findAndCreateNode(currentDataModel);
			        		path = new TreePath(dbNode.getPath());
			        	}
			        	if (path != null) {
			        		if (tree.isExpanded(path) == false) {
    			        		SwingUtilities.invokeLater(new Runnable() {
    			        			@Override
    			        			public void run() {
    			                		tree.setSelectionPath(path);
    			                		tree.expandPath(path);
    					        		tree.scrollPathToVisible(path);
    			        			}
    			        		});
			        		}
			        	}
					}
    			}
    		};
    		selectCurrentSchemaThread.start();
    	}
    }
    
    public void removeSQLDataModel(SQLDataModel model) {
    	treeAndTableModel.removeUniqueSQLDataModel(model);
    }

    /**
     * Statusbar
     */
    public static class StatusBar extends JPanel {

        private static final long serialVersionUID = 1L;
        BoxLayout        box        = new BoxLayout(this, BoxLayout.X_AXIS);
        public JLabel    messageLabel    = new FixedLabel();
        public JLabel    infoAction = new FixedLabel();
        final static int STATUS_HEIGHT      = 25;

        public StatusBar() {
            setLayout(box);
            messageLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            messageLabel.setForeground(Color.black);
            add(messageLabel);
            // die zu erwartenden Propertyänderungen sind die setText()
            // Aufrufe, dann soll der ToolTip aktualisiert werden,
            // da dieser den Textinhalt voll anzeigen kann, auch wenn das Label
            // den Platz dafür nicht hat
            messageLabel.addPropertyChangeListener(new MeldungPropertyChangeListener());
            // eine bislang sinnvolle Nutzung der Spalte InfoAction
            // ist die Anzeige, ob Bak-Dateien erstellt werden oder nicht
        }

        // wird beim erstmaligen Darstellen , bei jeder Formänderung
        // und bei Aufrufen von repaint durchlaufen
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final int meldungBreite = this.getWidth();
            messageLabel.setPreferredSize(new Dimension(meldungBreite, STATUS_HEIGHT));
            remove(messageLabel); // Label entfernen
            add(messageLabel, null, 0); // neu hinzufügen, damit neu eingepasst
            doLayout(); // damit wird die Änderung sofort wirksam !
        }

        static class FixedLabel extends JLabel {

            private static final long serialVersionUID = 1L;

            public FixedLabel(String text) {
                super(text);
            }

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

        	@Override
            public void propertyChange(PropertyChangeEvent evt) {
                messageLabel.setToolTipText(messageLabel.getText());
            }
        }

    } // class StatusBar

    private class TableMouseListener extends MouseAdapter {

        protected void fireDoubleClickPerformed(MouseEvent me) {
        	sendSelectedTableElementNameToEditor();
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            final Component comp = (Component) me.getSource();
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent me) {
            final Component comp = (Component) me.getSource();
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger()) {
                createTableContextMenu(me);
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger()) {
                createTableContextMenu(me);
            }
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.isPopupTrigger()) {
                createTableContextMenu(me);
            } else if (me.getClickCount() == 2) {
                fireDoubleClickPerformed(me);
            }
        }

    }
    
    private void createTableContextMenu(MouseEvent me) {
    	int row = table.getSelectedRow();
    	if (row != -1) {
        	Object o = treeAndTableModel.getValueAt(row, 0);
        	if (o instanceof SQLField) {
        		JPopupMenu popup = new JPopupMenu();
                JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.sendToEditor"));
                mi.addActionListener(enterInTableAction); 
                popup.add(mi);
                JMenuItem mi1 = new JMenuItem(Messages.getString("DataModelFrame.sendFieldDeclToEditor"));
                mi1.addActionListener(sendFieldDeclsAction); 
                popup.add(mi1);
                JMenuItem mi2 = new JMenuItem(Messages.getString("DataModelFrame.sendDropFieldToEditor"));
                mi2.addActionListener(sendDropFieldAction); 
                popup.add(mi2);
                int mouseXPos = me.getX();
                int mouseYPos = me.getY();
                popup.show(table, mouseXPos, mouseYPos);
        	}
    	}
    }
    
    private void setupTreeContextMenu() {
    	Object lastSelectedPathComp = tree.getLastSelectedPathComponent();
    	if (lastSelectedPathComp != null) {
	        final Object object = ((DefaultMutableTreeNode) lastSelectedPathComp).getUserObject();
	        int countChildren = ((DefaultMutableTreeNode) lastSelectedPathComp).getChildCount();
	        JPopupMenu popup = null;
	        if (object instanceof SQLObject) {
				refreshInfoFor((SQLObject) object);
	        }
	        if (object instanceof SQLTable) {
	            popup = new JPopupMenu();
	            JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.sendToEditor"));
	            mi.addActionListener(enterInTreeAction); 
	            popup.add(mi);
	            mi = new JMenuItem(Messages.getString("DataModelFrame.refresh"));
	            mi.addActionListener(refreshTableAction); 
	            popup.add(mi);
	            popup.addSeparator();
	            mi = new JMenuItem("select * from"); 
	            mi.addActionListener(selectStarAction);
	            popup.add(mi);
	            mi = new JMenuItem(Messages.getString("SQLDataTreeModel.selectAll"));
	            mi.addActionListener(selectAllAction);
	            popup.add(mi);
	            mi = new JMenuItem(Messages.getString("SQLDataTreeModel.selectAllCoalesce"));
	            mi.addActionListener(selectAllCoalesceAction);
	            popup.add(mi);
	            popup.addSeparator();
	            if (((SQLTable) object).getType().equals(SQLTable.TYPE_TABLE)) {
	                mi = new JMenuItem("insert into <alle Felder>"); 
	                mi.addActionListener(insertAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.0")); 
	                mi.addActionListener(psInsertAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.116")); 
	                mi.addActionListener(psUpdateAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.117")); 
	                mi.addActionListener(psDeleteTableAction);
	                popup.add(mi);
	                mi = new JMenuItem("prepared statement: select count(*)"); 
	                mi.addActionListener(psCountAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.import"));
	                mi.addActionListener(fileImportAction);
	                popup.add(mi);
	            }
	            mi = new JMenuItem(Messages.getString("DataModelFrame.export"));
	            mi.addActionListener(queryExportAction);
                popup.add(mi);
	            popup.addSeparator();
                mi = new JMenuItem(Messages.getString("DataModelFrame.exportTalendSchema"));
                mi.addActionListener(exportAsTalendSchema);
	            popup.add(mi);
	            if (((SQLTable) object).getType().equals(SQLTable.TYPE_TABLE)) {
	                popup.addSeparator();
	                mi = new JMenuItem(Messages.getString("DataModelFrame.createTable")); 
	                mi.addActionListener(createTableAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.98"));
	                mi.addActionListener(dropTableAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.100")); 
	                mi.addActionListener(deleteTableAction);
	                popup.add(mi);
	            } else if (((SQLTable) object).getType().equals(SQLTable.TYPE_MAT_VIEW)) {
	                mi = new JMenuItem(Messages.getString("DataModelFrame.createView")); 
	                mi.addActionListener(createTableAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.102")); 
	                mi.addActionListener(dropTableAction);
	                popup.add(mi);
	            } else if (((SQLTable) object).getType().equals(SQLTable.TYPE_VIEW)) {
	                mi = new JMenuItem(Messages.getString("DataModelFrame.createView")); 
	                mi.addActionListener(createTableAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.102")); 
	                mi.addActionListener(dropTableAction);
	                popup.add(mi);
	                mi = new JMenuItem(Messages.getString("DataModelFrame.createViewAsTable")); 
	                mi.addActionListener(createViewAsTableAction);
	                popup.add(mi);
	            }
	        } else if (object instanceof SQLSchema) {
	            popup = new JPopupMenu();
	            {
		            JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.sendToEditor"));
		            mi.addActionListener(enterInTreeAction); 
		            popup.add(mi);
	            }
	            {
		            final JMenuItem miRefresh = new JMenuItem(Messages.getString("DataModelFrame.refresh")); 
		            miRefresh.addActionListener(refreshSchemaAction);
		            popup.add(miRefresh);
	            }
	            popup.addSeparator();
	            {
		            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.106")); 
		            mi.addActionListener(createAllTablesAction);
		            popup.add(mi);
	            }
	            {
		            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.108")); 
		            mi.addActionListener(dropAllAction);
		            popup.add(mi);
	            }
	            {
	            	final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.110"));
		            mi.addActionListener(deleteAllAction);
		            popup.add(mi);
	            }
	        } else if (object instanceof SQLCatalog) {
	            popup = new JPopupMenu();
	            {
		            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.sendToEditor"));
		            mi.addActionListener(enterInTreeAction); 
		            popup.add(mi);
	            }
	            {
		            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.refresh"));
		            mi.addActionListener(refreshCatalogAction);
		            popup.add(mi);
	            }
	            status.messageLabel.setText(countChildren + " schemas");
	        } else if (object instanceof SQLDataModel) {
	            popup = new JPopupMenu();
	            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.refresh"));
	            mi.addActionListener(refreshDBAction);
	            popup.add(mi);
	            status.messageLabel.setText(countChildren + " databases");
	        } else if (object instanceof SQLConstraint) {
	            popup = new JPopupMenu();
	            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.dropConstraint"));
	            mi.addActionListener(dropConstraintAction);
	            popup.add(mi);
	        } else if (object instanceof SQLIndex) {
	            popup = new JPopupMenu();
	            JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.createIndex"));
	            mi.addActionListener(createIndexAction);
	            popup.add(mi);
	            mi = new JMenuItem(Messages.getString("DataModelFrame.dropIndex"));
	            mi.addActionListener(dropIndexAction);
	            popup.add(mi);
	        } else if (object instanceof SQLProcedure) {
	            popup = new JPopupMenu();
	            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.procedureCallStatement"));
	            mi.addActionListener(callProcedureAction);
	            popup.add(mi);
	            final JMenuItem misql = new JMenuItem(Messages.getString("DataModelFrame.procedureSQLCallStatement"));
	            misql.addActionListener(sqlCallProcedureAction);
	            popup.add(misql);
	            final JMenuItem miCreate = new JMenuItem(Messages.getString("DataModelFrame.createProcedureStatement"));
	            miCreate.addActionListener(createProcedureAction);
	            popup.add(miCreate);
	            final JMenuItem miDrop = new JMenuItem(Messages.getString("DataModelFrame.dropProcedureStatement"));
	            miDrop.addActionListener(dropProcedureAction);
	            popup.add(miDrop);
	        } else if (object instanceof SQLSequence) {
	            popup = new JPopupMenu();
	            final JMenuItem mi = new JMenuItem(Messages.getString("DataModelFrame.sequenceCallStatement"));
	            mi.addActionListener(callSequenceAction);
	            popup.add(mi);
	            final JMenuItem miCreate = new JMenuItem(Messages.getString("DataModelFrame.createSequenceStatement"));
	            miCreate.addActionListener(createSequenceAction);
	            popup.add(miCreate);
	            final JMenuItem miDrop = new JMenuItem(Messages.getString("DataModelFrame.dropSequenceStatement"));
	            miDrop.addActionListener(dropSequenceAction);
	            popup.add(miDrop);
	        } else if (object instanceof TableFolder) {
	            status.messageLabel.setText(countChildren + " tables");
	        } else if (object instanceof ViewFolder) {
	            status.messageLabel.setText(countChildren + " views");
	        } else if (object instanceof ProcedureFolder) {
	            status.messageLabel.setText(countChildren + " procedures");
	        } else if (object instanceof SequenceFolder) {
	            status.messageLabel.setText(countChildren + " sequences");
	        }
        	tree.setComponentPopupMenu(popup);
    	}
    }

    private class TreeMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent me) {
            final Component comp = ((Component) me.getSource());
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent me) {
            final Component comp = ((Component) me.getSource());
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

    }
    
    private void sendSelectedTableElementNameToEditor() {
        int row = table.getSelectedRow();
        if (row != -1) {
            if (mainFrame != null) {
            	Object o = treeAndTableModel.getValueAt(row, 0);
            	if (o instanceof SQLField) {
                    mainFrame.insertOrReplaceText(((SQLField) o).getName(), true);
            	} else if (o instanceof SQLIndex) {
                    mainFrame.insertOrReplaceText(((SQLIndex) o).getName(), true);
            	}
                mainFrame.setState(Frame.NORMAL);
                mainFrame.toFront();
            }
        }
    }

    private void sendSelectedFieldDeclarationsToEditor() {
        int[] rows = table.getSelectedRows();
        if (rows != null) {
            if (mainFrame != null) {
            	for (int row : rows) {
                	Object o = treeAndTableModel.getValueAt(row, 0);
                	if (o instanceof SQLField) {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildFieldDeclaration((SQLField) o), true);
                        mainFrame.insertFormattedBreak();
                	}
            	}
                mainFrame.setState(Frame.NORMAL);
                mainFrame.toFront();
            }
        }
    }

    private void sendDropFieldToEditor() {
        int[] rows = table.getSelectedRows();
        if (rows != null) {
            if (mainFrame != null) {
            	for (int row : rows) {
                	Object o = treeAndTableModel.getValueAt(row, 0);
                	if (o instanceof SQLField) {
                        mainFrame.insertOrReplaceText(SQLCodeGenerator.getInstance().buildDropStatement(((SQLField) o), useFullName()), true);
                        mainFrame.insertFormattedBreak();
                	}
            	}
                mainFrame.setState(Frame.NORMAL);
                mainFrame.toFront();
            }
        }
    }

    private void sendSelectedTreeElementNameToEditor() {
        final Object userObject = treeAndTableModel.getCurrentUserObject();
        if (mainFrame != null) {
            if (useFullName()) {
                if (userObject instanceof SQLTable) {
                    mainFrame.insertOrReplaceText(((SQLTable) userObject).getAbsoluteName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLSchema) {
                    mainFrame.insertOrReplaceText(((SQLSchema) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLCatalog) {
                    mainFrame.insertOrReplaceText(((SQLCatalog) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLConstraint) {
                    mainFrame.insertOrReplaceText(((SQLConstraint) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLIndex) {
                    mainFrame.insertOrReplaceText(((SQLIndex) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLProcedure) {
                    mainFrame.insertOrReplaceText(((SQLProcedure) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            } else {
                if (userObject instanceof SQLTable) {
                    mainFrame.insertOrReplaceText(((SQLTable) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLSchema) {
                    mainFrame.insertOrReplaceText(((SQLSchema) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLCatalog) {
                    mainFrame.insertOrReplaceText(((SQLCatalog) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLConstraint) {
                    mainFrame.insertOrReplaceText(((SQLConstraint) userObject).getName(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                } else if (userObject instanceof SQLProcedure) {
                    mainFrame.insertOrReplaceText(((SQLProcedure) userObject).getProcedureCallCode(), true);
                    mainFrame.setState(Frame.NORMAL);
                    mainFrame.toFront();
                }
            }
        }
    }
    
    /**
     * Renderer für die Tabelle --> Strings
     */
    static private class FieldTableRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        public FieldTableRenderer() {
            super();
            setOpaque(true);
        }

    	@Override
        public Component getTableCellRendererComponent(
                JTable table_loc,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            setFont(table_loc.getFont());
        	SQLField field = null;
            SQLProcedure.Parameter parameter = null;
            if (value instanceof SQLField) {
                field = (SQLField) value;
                switch (column) {
                    case 0: {
                        setText(field.getName());
                        break;
                    }
                    case 1:
                        setText(field.getTypeName());
                        break;
                    case 2:
                        setText(String.valueOf(field.getLength()));
                        break;
                    case 3:
                        setText(String.valueOf(field.getDecimalDigits()));
                        break;
                    case 4:
                        setText(field.isNullValueAllowed() ? Messages.getString("DataModelFrame.114") : Messages.getString("DataModelFrame.115"));  //$NON-NLS-2$
                }
            } else if (value instanceof SQLProcedure.Parameter) {
                parameter = (SQLProcedure.Parameter) value;
                switch (column) {
                    case 0:
                        setText(parameter.toString());
                        break;
                    case 1:
                        setText(parameter.getTypeName());
                        break;
                    case 2:
                        setText(String.valueOf(parameter.getLength()));
                        break;
                    case 3:
                        setText(String.valueOf(parameter.getPrecision()));
                        break;
                    case 4:
                        setText("");
                }
            } else {
            	if (value != null) {
                    setText(value.toString()); 
            	} else {
                    setText(""); 
            	}
            }
            if (field != null && field.isPrimaryKey()) {
                if (isSelected) {
                    if (column > 0) {
                        setForeground(table_loc.getForeground());
                        setBackground(Color.yellow);
                    } else {
                        setForeground(table_loc.getSelectionForeground());
                        setBackground(table_loc.getSelectionBackground());
                    }
                } else {
                    setForeground(table_loc.getForeground());
                    setBackground(Color.yellow);
                }
            } else {
                if (isSelected) {
                    setForeground(table_loc.getSelectionForeground());
                    setBackground(table_loc.getSelectionBackground());
                } else {
                    setForeground(table_loc.getForeground());
                    setBackground(table_loc.getBackground());
                }
            }
            return this;
        }

    }

}