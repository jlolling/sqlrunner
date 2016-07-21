package sqlrunner;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.NumberFormatter;
import javax.swing.text.TextAction;
import javax.swing.undo.UndoManager;

import org.apache.log4j.Logger;

import sqlrunner.base64.Base64Viewer;
import sqlrunner.config.PreferencesDialog;
import sqlrunner.datamodel.SQLDataModel;
import sqlrunner.datamodel.SQLObject;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.datamodel.gui.DataModelFrame;
import sqlrunner.datetool.DateConverter;
import sqlrunner.editor.ExtEditorKit;
import sqlrunner.editor.ExtEditorPane;
import sqlrunner.editor.ExtEditorTransferHandler;
import sqlrunner.editor.GotoLineDialog;
import sqlrunner.editor.LineHighlightPainter;
import sqlrunner.editor.SearchReplaceDialog;
import sqlrunner.editor.CodeCompletionAssistent;
import sqlrunner.editor.SyntaxContext;
import sqlrunner.editor.SyntaxDocument;
import sqlrunner.editor.SyntaxScanner;
import sqlrunner.editor.TextViewer;
import sqlrunner.export.QueryExportFrame;
import sqlrunner.export.ResultTableExportDialog;
import sqlrunner.fileconverter.TextFileConverterFrame;
//import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.FileImporter;
import sqlrunner.flatfileimport.gui.ImportConfiguratorFrame;
import sqlrunner.generator.SQLCodeGenerator;
import sqlrunner.history.HistoryFrame;
import sqlrunner.regex.RegexTestFrame;
import sqlrunner.resources.ApplicationIcons;
import sqlrunner.resources.images.ApplicationImages;
import sqlrunner.swinghelper.WindowHelper;
import sqlrunner.talend.SchemaImportFrame;
import sqlrunner.talend.SchemaUtil;
import sqlrunner.xml.ExporterFrame;
import sqlrunner.xml.ImporterFrame;
import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import dbtools.SQLParser;
import dbtools.SQLStatement;

public final class MainFrame extends JFrame implements ActionListener, ListSelectionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(MainFrame.class.getName());
    
    private static boolean useMonospacedFont = false;
    private final JMenuBar menuBarMain = new JMenuBar();
    private final JMenu menuScript = new JMenu();
    private final JMenu menuEdit = new JMenu();
    private final JMenu menuTools = new JMenu();
    private final JMenu menuHelp = new JMenu();
    private final JMenuItem menuScriptNew = new JMenuItem();
    private final JMenuItem menuScriptOpen = new JMenuItem();
    private final JMenuItem menuScriptSave = new JMenuItem();
    private final JMenuItem menuScriptSaveas = new JMenuItem();
    private final JMenu menuScriptReopen = new JMenu();
    private final JMenuItem menuScriptInfo = new JMenuItem();
    private final JMenuItem menuWindowClose = new JMenuItem();
    private final JMenuItem menuScriptShutdown = new JMenuItem();
    private final JMenuItem menuEditUndo = new JMenuItem();
    private final JMenuItem menuEditRedo = new JMenuItem();
    private final JMenuItem menuEditCut = new JMenuItem();
    private final JMenuItem menuEditCopy = new JMenuItem();
    private final JMenuItem menuEditPaste = new JMenuItem();
    private final JMenuItem menuEditPasteSmart = new JMenuItem();
    private final JMenuItem menuEditCopyToJavaString = new JMenuItem();
    private final JMenuItem menuEditCopyToJavaStringBuffer = new JMenuItem();
    private final JMenuItem menuEditReplace = new JMenuItem();
    private final JMenuItem menuEditConvertToSql = new JMenuItem();
    private final JMenuItem menuEditCommentParams = new JMenuItem();
    private final JMenuItem menuEditRemoveParamComments = new JMenuItem();
    private final JMenuItem menuEditRemoveAllComments = new JMenuItem();
    private final JMenuItem menuToolsBase64Viewer = new JMenuItem();
    private final JMenuItem menuToolsRegexTester = new JMenuItem();
    private final JMenuItem menuToolsTextFileConverter = new JMenuItem();
    private final JMenuItem menuToolsTalendSchemaConverter = new JMenuItem();
    private final JMenuItem menuToolsCreateGuid = new JMenuItem();
    private final JMenuItem menuToolsDateTools = new JMenuItem();
    private final JMenuItem menuEditGoto = new JMenuItem();
    private final JMenuItem menuEditTrim = new JMenuItem();
    private final JMenuItem menuEditParse = new JMenuItem();
    private final JMenuItem menuEditDBCfg = new JMenuItem();
    private final JMenuItem menuEditDefCfg = new JMenuItem();
    private final JMenuItem menuEditScanCfg = new JMenuItem();
    private final JMenuItem menuEditAdminCfg = new JMenuItem();
    private final JMenuItem menuEditRestoreCfg = new JMenuItem();
    private final JMenuItem menuEditPreferences = new JMenuItem();
    private final JMenu menuDB = new JMenu();
    private final JMenuItem menuDBOpen = new JMenuItem();
    private final JMenuItem menuDBAbortConnecting = new JMenuItem();
    private final JMenuItem menuDBClose = new JMenuItem();
    private final JMenuItem menuDBRun = new JMenuItem();
    private final JMenuItem menuDBReconnect = new JMenuItem();
    private final JMenuItem menuDBExplain = new JMenuItem();
    private final JMenuItem menuDBStop = new JMenuItem();
    private final JMenuItem menuDBCommit = new JMenuItem();
    private final JMenuItem menuDBRollback = new JMenuItem();
    private final JMenuItem menuDBHistory = new JMenuItem();
    private final JMenuItem menuDBCreateNewRow = new JMenuItem();
    private final JMenuItem menuDBSearchInTable = new JMenuItem();
    private final JMenuItem menuDBExportResultSetAsTalendSchema = new JMenuItem();
    private final JMenu menuDBCSV = new JMenu();
    private final JMenuItem menuDBCsvExportResultTable = new JMenuItem();
    private final JMenuItem menuDBCsvExportDBTable = new JMenuItem();
    private final JMenuItem menuDBCsvImport = new JMenuItem();
    private final JMenu menuDBXML = new JMenu();
    private final JMenuItem menuDBXmlExport = new JMenuItem();
    private final JMenuItem menuDBXmlImport = new JMenuItem();
    private final JCheckBoxMenuItem menuDBDisableParserCheckBox = new JCheckBoxMenuItem();
    private final JCheckBoxMenuItem menuDBAutoCommitCheckBox = new JCheckBoxMenuItem();
    private final JMenuItem menuDBInformation = new JMenuItem();
    private final JMenuItem menuDBDatamodel = new JMenuItem();
    private final JCheckBoxMenuItem menuEditSyntaxhighCheckBox = new JCheckBoxMenuItem();
    private final JMenu menuConfig = new JMenu();
    private final JMenuItem menuDBAdmin = new JMenuItem();
    private final JMenuItem menuHelpAbout = new JMenuItem();
    private final JMenuItem menuHomeBrowser = new JMenuItem();
    private final JMenuItem menuHelpBrowser = new JMenuItem();
    private final JMenu menuWindow = new JMenu();
    private final JMenuItem menuWindowNew = new JMenuItem();
    private final JMenuItem menuWindowSetCustomName = new JMenuItem();
    private final JMenuItem menuWindowClearCustomName = new JMenuItem();
    private final JMenuItem menuWindowCloseAllOther = new JMenuItem();
    private final JMenuItem menuWindowArrangeHorizontal = new JMenuItem();
    private final JMenuItem menuWindowArrangeVertical = new JMenuItem();
    private final JMenuItem menuWindowArrangeOverlapped = new JMenuItem();
    private final JMenuItem menuWindowAllToFront = new JMenuItem();
    // Elemente ToolBar deklarieren
    private final JToolBar mainToolBar = new JToolBar();
    private final JToolBarButton tbButtonNew = new JToolBarButton(ApplicationIcons.NEW_GIF);
    private final JToolBarButton tbButtonOpen = new JToolBarButton(ApplicationIcons.OPEN_GIF);
    private final JToolBarButton tbButtonSave = new JToolBarButton(ApplicationIcons.SAVE_GIF);
    private final JToolBarButton tbButtonDbOpen = new JToolBarButton(ApplicationIcons.DBOPEN_GIF);
    private final JToolBarButton tbButtonDbClose = new JToolBarButton(ApplicationIcons.DBCLOSE_GIF);
    private final JToolBarButton tbButtonDataModel = new JToolBarButton(ApplicationIcons.DATAMODEL_PNG);
    private final JToolBarButton tbButtonHistory = new JToolBarButton(ApplicationIcons.HISTORY_PNG);
    private final JToolBarButton tbButtonCopy = new JToolBarButton(ApplicationIcons.COPY_PNG);
    private final JToolBarButton tbButtonCut = new JToolBarButton(ApplicationIcons.CUT_GIF);
    private final JToolBarButton tbButtonPaste = new JToolBarButton(ApplicationIcons.PASTE_GIF);
    private final JToolBarButton tbButtonUndo = new JToolBarButton(ApplicationIcons.UNDO_GIF);
    private final JToolBarButton tbButtonRedo = new JToolBarButton(ApplicationIcons.REDO_GIF);
    private final JToolBarButton tbButtonEinrueck = new JToolBarButton(ApplicationIcons.EINRUECK_GIF);
    private final JToolBarButton tbButtonAusrueck = new JToolBarButton(ApplicationIcons.AUSRUECK_GIF);
    private final JToolBarButton tbButtonSearch = new JToolBarButton(ApplicationIcons.SEARCH_GIF);
    private final JToolBarButton tbButtonGoto = new JToolBarButton(ApplicationIcons.GOTO_GIF);
    private final JToolBarButton tbButtonRun = new JToolBarButton(ApplicationIcons.START_GIF);
    private final JToolBarButton tbButtonExplain = new JToolBarButton(ApplicationIcons.EXPLAIN_PNG);
    private final JToolBarButton tbButtonStop = new JToolBarButton(ApplicationIcons.STOP_GIF);
    private final JToolBarButton tbButtonCommit = new JToolBarButton(ApplicationIcons.COMMIT_GIF);
    private final JToolBarButton tbButtonRollback = new JToolBarButton(ApplicationIcons.ROLLBACK_GIF);
    private final JToolBarButton tbButtonTableSearch = new JToolBarButton(ApplicationIcons.TABLESEARCH_GIF);
    private final JToolBarButton tbButtonToLowerCase = new JToolBarButton(ApplicationIcons.LOWERCASE_GIF);
    private final JToolBarButton tbButtonToUpperCase = new JToolBarButton(ApplicationIcons.UPPERCASE_GIF);
    private final JToolBarButton tbButtonComments = new JToolBarButton(ApplicationIcons.COMMENTS_GIF);
    private final JToolBarToggleButton tbButtonHighlighter = new JToolBarToggleButton(ApplicationIcons.HIGHLIGHT_PNG);
    private final JToolBarToggleButton tbButtonTableOrientation = new JToolBarToggleButton(ApplicationIcons.TOGGLETABLEORIENTATIONICON_PNG);
    public StatusBar status;                                                                                          // darf erst nach editor und resultTable initiert werden !!
    // Dialoge ...
    private GotoLineDialog dlgGo;
    private SearchReplaceDialog dlgSeRe;
    private AboutDialog dlgAbout;
    private ResultTableExportDialog exportResultTableDialog;
    private TableSearchDialog tableSearchDialog;
    private DatabaseMetaDataView metaDataView;
    private AdminToolChooser atc;
    private PreferencesDialog preferencesDialog;
    private DBLoginDialog dbLogin;
    public static HistoryFrame sqlHistory = null;
    public static DataModelFrame dmFrame = null;
    private File currentFile;
    private long currentFileLoaded = 0;
    private long currentFileLastModified = 0;
    private int currentTextPos = 0;
    private int currentLineNumber = 0;
    private int currentOffsetInCurrLine = 0;
    private int currentLineStartOffset = 0;
    private int currentLineEndOffset = 0;
    private String currentWord = "";
    private int startPosOfCurrentWord = 0;
    static final int READ_BUFFER_SIZE = 16348;
    private BufferedWriter bwFile;
    private int xLoc;
    private int yLoc;
    private int wide;
    private int high;
    public int dlgOpenSave_Aktion; //0=ok, 1=cancel
    private transient TextChangeListener textChangeListener = null;
    private boolean textChanged = false;
    public boolean linewrap = false;
    private final transient UndoManager undoManager = new UndoManager();
    private final transient UndoHandler undoHandler = new UndoHandler();
    private String tabErsatz; 
    private final MenuScriptReopenListener reopenListener = new MenuScriptReopenListener();
    private JSplitPane splitPane = new JSplitPane();
    private ExtEditorPane editor;
    private final JScrollPane editorScrollPane = new JScrollPane();
    private final JScrollPane tableScrollPane = new JScrollPane();
    private JLayeredPane editorLayeredPane;
    private CodeCompletionAssistent syntaxChooser;
    public JTable resultTable = new JTable();
    private transient Database database = null;
    private static String dateFormatMask = "dd.MM.yyyy HH:mm:ss"; 
    private final Vector<ValueEditor> cellEditorList = new Vector<ValueEditor>();
    private boolean databaseIsBusy = false;
    private boolean fileLoaderBusy = false;
    private int selectedRowIndex = -1; // Zeile, die aktuell in der Tabelle ausgewählt wurde
    // wird in valueChanged gefüllt
    private transient SyntaxScanner lexer = null;
    private boolean startRunOnLoad = false;
    private boolean usedForFKNavigation = false;
    public static final int FK_NAVIGATION_THIS_FRAME = 0;
    public static final int FK_NAVIGATION_NEW_FRAME = 1;
    public static final int FK_NAVIGATION_LAST_FRAME = 2;
    private static int fkNavigationFrameMode = FK_NAVIGATION_LAST_FRAME;
    public SQLFileFilter sqlFileFilter = new SQLFileFilter();
    static TextViewer textViewer = null;
    private boolean textIsSelected = false;
    private static ResultTableCellRenderer objectTableRenderer = null;
    private static ArrayList<LongRunningAction> listOfLongRunningActions = new ArrayList<LongRunningAction>();
    private int frameIndex = 0;
    private static int lastFrameIndex = 0;
    private String windowName = null;
    private String customWindowName = null;
    private javax.swing.Timer runTimer;
    private long runTimerStartTime = 0;
    private static final Color productiveBackground = new Color(255, 240, 240);
    
    /**
     * Konstruktur mit Parametern für die Platzierung des Fensters
     * @param xLoc und yLoc position der linken oberen Ecke des Fensters auf dem screen
     * @param wide und high Breite und Höhe des Fensters
     */
    public MainFrame(int xLoc, int yLoc, int wide, int high) {
        this.xLoc = xLoc;
        this.yLoc = yLoc;
        this.wide = wide;
        this.high = high;
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        logger.debug("MainFrame opening");
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                initComponents();
                if (sqlHistory == null) {
                    sqlHistory = new HistoryFrame();
                }
                WindowHelper.checkAndCorrectWindowBounds(this);
            } else {
                final Window self = this;
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        initComponents();
                        if (sqlHistory == null) {
                            sqlHistory = new HistoryFrame();
                        }
                        WindowHelper.checkAndCorrectWindowBounds(self);
                    }
                });
            }
            useMonospacedFont = Boolean.valueOf(Main.getUserProperty("MONOSPACED_TABLECELL_FONT", "false"));
        } catch (Exception e) {
            logger.error("exception: " + e, e); 
            Main.panic("Error in Constructor of MainFrame"); 
        }
        frameIndex = lastFrameIndex++;
        if (frameIndex > 0) {
            updateEditorKeymap();
        }
    }

    public static boolean useMonospacedTableCellFont() {
    	return useMonospacedFont;
    }
    
    public static void setUseMonospacedTableCellFont(boolean useit) {
    	useMonospacedFont = useit;
    	Main.setUserProperty("MONOSPACED_TABLECELL_FONT", String.valueOf(useit));
    }
    
    public Database getDatabase() {
    	return database;
    }
    
    public SyntaxScanner getSyntaxScanner() {
    	return lexer;
    }
    
    public File getCurrentFile() {
        return currentFile;
    }

    public long getCurrentFileLastModified() {
        return currentFileLastModified;
    }

    public long getCurrentFileLoaded() {
        return currentFileLoaded;
    }

    void setDividerLocation(int location) {
        splitPane.setDividerLocation(location);
    }

    public int getDividerLocation() {
        return splitPane.getDividerLocation();
    }

    public boolean isCloseActionDisabled() {
        return listOfLongRunningActions.size() > 0;
    }

    public void setCreateNewRowEnabled(boolean enable) {
        if (menuDBCreateNewRow != null) {
            menuDBCreateNewRow.setEnabled(enable);
        }
    }

    public boolean isCreateNewRowEnabled() {
        if (menuDBCreateNewRow != null) {
            return menuDBCreateNewRow.isEnabled();
        } else {
            return false;
        }
    }

    private void initMenu() {
        setMenuMnemonic(); // abhängig von Property !
        // Menu-shortcut initialisieren
        setMenuShortcut(); // abhängig von Property !
        // setzen der Grund-Parameter der Menuitems
        // Menue Regel
        menuScript.setText(Messages.getString("MainFrame.menufile")); 
        menuScriptNew.setText(Messages.getString("MainFrame.menufilenew")); 
        menuScriptNew.setActionCommand("new"); 
        menuScriptNew.addActionListener(this);
        menuScriptOpen.setText(Messages.getString("MainFrame.menufileopen")); 
        menuScriptOpen.setActionCommand("open"); 
        menuScriptOpen.addActionListener(this);
        menuScriptReopen.setText(Messages.getString("MainFrame.menufilereopen")); 
        // dem MenueItem Reopen wird selbst kein ActionListener zugeordnet
        // die einzelnen Eintraege erhalten den ActionListener in createReopenItems
        menuScriptSave.setText(Messages.getString("MainFrame.menufilesave")); 
        menuScriptSave.setActionCommand("save"); 
        menuScriptSave.addActionListener(this);
        menuScriptSaveas.setText(Messages.getString("MainFrame.menufilesaveas")); 
        menuScriptSaveas.setActionCommand("saveas"); 
        menuScriptSaveas.addActionListener(this);
        menuScriptInfo.setText(Messages.getString("MainFrame.menufileinfo")); 
        menuScriptInfo.setActionCommand("fileinfo"); 
        menuScriptInfo.addActionListener(this);
        menuScriptShutdown.setText(Messages.getString("MainFrame.menuexit")); 
        menuScriptShutdown.setActionCommand("shutdown"); 
        menuScriptShutdown.addActionListener(this);
        menuDB.setText(Messages.getString("MainFrame.menudatabase")); 
        menuConfig.setText(Messages.getString("MainFrame.menuconfigfiles")); 
        menuScript.add(menuScriptNew);
        menuScript.add(menuScriptOpen);
        menuScript.add(menuScriptReopen);
        menuScript.add(menuScriptSave);
        menuScript.add(menuScriptSaveas);
        menuScript.addSeparator();
        menuScript.add(menuScriptInfo);
        menuScript.addSeparator();
        menuScript.add(menuScriptShutdown);
        createReopenItems();
        menuDBOpen.setText(Messages.getString("MainFrame.menudbopen")); 
        menuDBOpen.setActionCommand("opendb"); 
        menuDBOpen.addActionListener(this);
        menuDBReconnect.setText("Reconnect");
        menuDBReconnect.setActionCommand("reconnect");
        menuDBReconnect.addActionListener(this);
        menuDBAbortConnecting.setAction(stopConnectAction);
        menuDBClose.setText(Messages.getString("MainFrame.menudbclose")); 
        menuDBClose.setActionCommand("closedb"); 
        menuDBClose.addActionListener(this);
        menuDBRun.setText("Start"); 
        menuDBRun.setActionCommand("run"); 
        menuDBRun.addActionListener(this);
        menuDBExplain.setText("Explain");
        menuDBExplain.setActionCommand("explain");
        menuDBExplain.addActionListener(this);
        menuDBExplain.setEnabled(false);
        menuDBStop.setText("Stop"); 
        menuDBStop.setActionCommand("stop"); 
        menuDBStop.addActionListener(this);
        menuDBCommit.setText("commit"); 
        menuDBCommit.setActionCommand("commit"); 
        menuDBCommit.addActionListener(this);
        menuDBRollback.setText("rollback"); 
        menuDBRollback.setActionCommand("rollback"); 
        menuDBRollback.addActionListener(this);
        menuDBDisableParserCheckBox.setText(Messages.getString("MainFrame.menudbsendasonestat")); 
        menuDBDisableParserCheckBox.setActionCommand("parseroff"); 
        menuDBDisableParserCheckBox.addActionListener(this);
        menuDBHistory.setText(Messages.getString("MainFrame.menudbhistory")); 
        menuDBHistory.setActionCommand("history"); 
        menuDBHistory.addActionListener(this);
        menuDBCreateNewRow.setText(Messages.getString("MainFrame.menuinsertnewdataset")); 
        menuDBCreateNewRow.setActionCommand("table_insert_row"); 
        menuDBCreateNewRow.addActionListener(this);
        menuDBSearchInTable.setText(Messages.getString("MainFrame.menudbsearchintable")); 
        menuDBSearchInTable.setActionCommand("tablesearch"); 
        menuDBSearchInTable.addActionListener(this);
        menuDBCSV.setText(Messages.getString("MainFrame.menucsvexport")); 
        menuDBCsvExportResultTable.setText(Messages.getString("MainFrame.menudbexportresulttable")); 
        menuDBCsvExportResultTable.addActionListener(this);
        menuDBCsvExportResultTable.setActionCommand("exportresult"); 
        menuDBCsvExportDBTable.setText(Messages.getString("MainFrame.menuexportquery")); 
        menuDBCsvExportDBTable.addActionListener(this);
        menuDBCsvExportDBTable.setActionCommand("exportdb"); 
        menuDBCsvImport.setText(Messages.getString("MainFrame.menucsvimport")); 
        menuDBCsvImport.addActionListener(this);
        menuDBCsvImport.setActionCommand("importdata"); 
        menuDBXML.setText("XML Export/Import"); 
        menuDBXmlExport.setText(Messages.getString("MainFrame.menuxmlexport"));  
        menuDBXmlExport.setActionCommand("xml_export"); 
        menuDBXmlExport.addActionListener(this);
        menuDBExportResultSetAsTalendSchema.setText(Messages.getString("MainFrame.exportTalendSchema"));
        menuDBExportResultSetAsTalendSchema.setActionCommand("export_talend_schema");
        menuDBExportResultSetAsTalendSchema.addActionListener(this);
        menuDBXmlImport.setText(Messages.getString("MainFrame.menuxmlimport")); 
        menuDBXmlImport.setActionCommand("xml_import"); 
        menuDBXmlImport.addActionListener(this);
        menuDBAutoCommitCheckBox.setText(Messages.getString("MainFrame.menuautocommit")); 
        if (Main.getUserProperty("AUTO_COMMIT", "true").equals("true")) {
            if (logger.isDebugEnabled()) {
                logger.debug("auto commit = true"); 
            }
            menuDBAutoCommitCheckBox.setSelected(true);
            menuDBCommit.setEnabled(false);
            menuDBRollback.setEnabled(false);
        } else {
            menuDBAutoCommitCheckBox.setSelected(false);
        }
        menuDBAutoCommitCheckBox.setActionCommand("autocommit"); 
        menuDBAutoCommitCheckBox.addActionListener(this);
        menuDBAdmin.setText(Messages.getString("MainFrame.menuadminsql")); 
        menuDBAdmin.setActionCommand("admin");
        menuDBAdmin.addActionListener(this);
        menuDBInformation.setText(Messages.getString("MainFrame.menumetainfos"));
        menuDBInformation.setActionCommand("metainfo");
        menuDBInformation.addActionListener(this);
        menuDB.add(menuDBOpen);
        menuDB.add(menuDBReconnect);
        menuDB.add(menuDBAbortConnecting);
        menuDB.add(menuDBClose);
        menuDB.addSeparator();
        menuDB.add(menuDBRun);
        menuDB.add(menuDBExplain);
        menuDB.add(menuDBStop);
        menuDB.add(menuDBDisableParserCheckBox);
        menuDB.add(menuDBHistory);
        menuDB.addSeparator();
        menuDB.add(menuDBCommit);
        menuDB.add(menuDBRollback);
        menuDB.add(menuDBAutoCommitCheckBox);
        menuDB.addSeparator();
        menuDB.add(menuDBCreateNewRow);
        menuDB.add(menuDBSearchInTable);
        menuDB.addSeparator();
        menuDB.add(menuDBCSV);
        menuDBCSV.add(menuDBCsvExportResultTable);
        menuDBCSV.add(menuDBCsvExportDBTable);
        menuDBCSV.add(menuDBCsvImport);
        menuDB.add(menuDBXML);
        menuDBXML.add(menuDBXmlExport);
        menuDBXML.add(menuDBExportResultSetAsTalendSchema);
        menuDBXML.add(menuDBXmlImport);
        menuDB.addSeparator();
        menuDB.add(menuDBAdmin);
        menuDB.add(menuDBInformation);
        if (Main.getDefaultProperty("DISABLE_DATAMODEL", "false").equals("false")) {   
            if (logger.isDebugEnabled()) {
                logger.debug("datamodel enabled"); 
            }
            menuDBDatamodel.setText(Messages.getString("MainFrame.menudatamodel")); 
            menuDBDatamodel.setActionCommand("datamodel"); 
            menuDBDatamodel.addActionListener(this);
            menuDB.add(menuDBDatamodel);
        }
        // Menue Bearbeiten
        menuEdit.setText(Messages.getString("MainFrame.menuedit")); 
        menuEditUndo.setText(Messages.getString("MainFrame.menuundo")); 
        menuEditUndo.setEnabled(false);
        menuEditUndo.setActionCommand("undo"); 
        menuEditUndo.addActionListener(this);
        menuEditRedo.setText(Messages.getString("MainFrame.menuredo")); 
        menuEditRedo.setEnabled(false);
        menuEditRedo.setActionCommand("redo"); 
        menuEditRedo.addActionListener(this);
        menuEditCut.setText(Messages.getString("MainFrame.menucut")); 
        menuEditCut.setActionCommand("cut"); 
        menuEditCut.addActionListener(this);
        menuEditCopy.setText(Messages.getString("MainFrame.menucopy")); 
        menuEditCopy.setActionCommand("copy"); 
        menuEditCopy.addActionListener(this);
        menuEditCopyToJavaString.setText(Messages.getString("MainFrame.menuconvtostring")); 
        menuEditCopyToJavaString.setActionCommand("toJavaString"); 
        menuEditCopyToJavaString.addActionListener(this);
        menuEditCopyToJavaStringBuffer.setText(Messages.getString("MainFrame.menuconvtostringbuffer")); 
        menuEditCopyToJavaStringBuffer.setActionCommand("toJavaStringBuffer"); 
        menuEditCopyToJavaStringBuffer.addActionListener(this);
        menuEditConvertToSql.setText(Messages.getString("MainFrame.menuconvtosql")); 
        menuEditConvertToSql.setActionCommand("toSQL"); 
        menuEditConvertToSql.addActionListener(this);
        menuEditCommentParams.setText(Messages.getString("MainFrame.menubuildparamnum")); 
        menuEditCommentParams.setActionCommand("commentParams"); 
        menuEditCommentParams.addActionListener(this);
        menuEditRemoveParamComments.setText(Messages.getString("MainFrame.menuremoveparamnum")); 
        menuEditRemoveParamComments.setActionCommand("removeParamComments"); 
        menuEditRemoveParamComments.addActionListener(this);
        menuEditRemoveAllComments.setText(Messages.getString("MainFrame.menuremoveallcomments")); 
        menuEditRemoveAllComments.setActionCommand("removeAllComments"); 
        menuEditRemoveAllComments.addActionListener(this);
        menuEditPaste.setText(Messages.getString("MainFrame.menupaste")); 
        menuEditPaste.setActionCommand("paste"); 
        menuEditPaste.addActionListener(this);
        menuEditPasteSmart.setText(Messages.getString("MainFrame.menupastesmart")); 
        menuEditPasteSmart.setActionCommand("paste_smart"); 
        menuEditPasteSmart.addActionListener(this);
        menuEditParse.setText(Messages.getString("MainFrame.menuseparate")); 
        menuEditParse.setActionCommand("parse"); 
        menuEditParse.addActionListener(this);
        menuEditReplace.setText(Messages.getString("MainFrame.menusearchandreplace")); 
        menuEditReplace.setActionCommand("search"); 
        menuEditReplace.addActionListener(this);
        menuEditGoto.setText(Messages.getString("MainFrame.menugotoline")); 
        menuEditGoto.setActionCommand("goto"); 
        menuEditGoto.addActionListener(this);
        menuEditTrim.setText(Messages.getString("MainFrame.menutrimsource")); 
        menuEditTrim.setActionCommand("trim"); 
        menuEditTrim.addActionListener(this);
        menuEditSyntaxhighCheckBox.setText(Messages.getString("MainFrame.menusyntaxhighlight")); 
        menuEditSyntaxhighCheckBox.setActionCommand("syntaxhigh"); 
        menuEditSyntaxhighCheckBox.addActionListener(this);
        if (Main.getDefaultProperty("SYNTAX_HIGHLIGHT_ENABLED", "true").equals("false")) {
            if (logger.isDebugEnabled()) {
                logger.debug("syntax highlighting disabled");
            }
            menuEditSyntaxhighCheckBox.setSelected(false);
            menuEditSyntaxhighCheckBox.setEnabled(false);
            lexer = null;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("syntax highlighting enabled");
            }
            lexer = new SyntaxScanner(); // Scanner instanzieren, das muss in jedem Fall erfolgen !!
            if (Main.getUserProperty("SYNTAX_HIGHLIGHT", "true").equals("true")) {
                menuEditSyntaxhighCheckBox.setSelected(true);
            } else {
                menuEditSyntaxhighCheckBox.setSelected(false);
            }
        }
        menuEditDefCfg.setText(Messages.getString("MainFrame.menueditconfeditorandgui")); 
        menuEditDefCfg.setActionCommand("defcfg"); 
        menuEditDefCfg.addActionListener(this);
        menuEditScanCfg.setText(Messages.getString("MainFrame.menueditsyntaxhighlighting")); 
        menuEditScanCfg.setActionCommand("scancfg"); 
        menuEditScanCfg.addActionListener(this);
        menuEditDBCfg.setText(Messages.getString("MainFrame.menueditconfdbtypes")); 
        menuEditDBCfg.setActionCommand("dbcfg"); 
        menuEditDBCfg.addActionListener(this);
        menuEditAdminCfg.setText(Messages.getString("MainFrame.menueditconfadminsql")); 
        menuEditAdminCfg.setActionCommand("admincfg"); 
        menuEditAdminCfg.addActionListener(this);
        menuEditPreferences.setText(Messages.getString("MainFrame.menupreferences")); 
        menuEditPreferences.setActionCommand("preferences"); 
        menuEditPreferences.addActionListener(this);
        menuConfig.add(menuEditDefCfg);
        menuConfig.add(menuEditDBCfg);
        menuConfig.add(menuEditAdminCfg);
        menuEditRestoreCfg.setText(Messages.getString("MainFrame.menucreateconfnew")); 
        menuEditRestoreCfg.setActionCommand("restorecfg"); 
        menuEditRestoreCfg.addActionListener(this);
        menuEdit.add(menuEditUndo);
        menuEdit.add(menuEditRedo);
        menuEdit.addSeparator();
        menuEdit.add(menuEditCut);
        menuEdit.add(menuEditCopy);
        menuEdit.add(menuEditPaste);
        menuEdit.add(menuEditPasteSmart);
        menuEdit.addSeparator();
        menuEdit.add(menuEditCopyToJavaString);
        menuEdit.add(menuEditCopyToJavaStringBuffer);
        menuEdit.add(menuEditConvertToSql);
        menuEdit.addSeparator();
        menuEdit.add(menuEditCommentParams);
        menuEdit.add(menuEditRemoveParamComments);
        menuEdit.add(menuEditRemoveAllComments);
        menuEdit.addSeparator();
        menuEdit.add(menuEditParse);
        menuEdit.addSeparator();
        menuEdit.add(menuEditReplace);
        menuEdit.add(menuEditGoto);
        menuEdit.add(menuEditTrim);
        menuEdit.addSeparator();
        menuEdit.add(menuEditSyntaxhighCheckBox);
        menuEdit.addSeparator();
        menuEdit.add(menuConfig);
        menuEdit.add(menuEditRestoreCfg);
        menuEdit.addSeparator();
        menuEdit.add(menuEditPreferences);
        // menu tools
        menuToolsCreateGuid.setText(Messages.getString("MainFrame.menucreateguid")); 
        menuToolsCreateGuid.setActionCommand("createguid"); 
        menuToolsCreateGuid.addActionListener(this);
        menuToolsBase64Viewer.setText(Messages.getString("MainFrame.menubase64"));
        menuToolsBase64Viewer.setActionCommand("base64"); 
        menuToolsBase64Viewer.addActionListener(this);
        menuToolsRegexTester.setText(Messages.getString("MainFrame.menuRegex")); 
        menuToolsRegexTester.setActionCommand("regex"); 
        menuToolsRegexTester.addActionListener(this);
        menuTools.setText(Messages.getString("MainFrame.menutools")); 
        menuToolsTextFileConverter.setText(Messages.getString("MainFrame.textFileConverter"));
        menuToolsTextFileConverter.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				openTextFileConverterFrame();
			}
        	
        });
        menuToolsTalendSchemaConverter.setText(Messages.getString("MainFrame.talendSchemaConverter"));
        menuToolsTalendSchemaConverter.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				openTalendSchemaConverterFrame();
			}
        	
        });
        menuToolsDateTools.setText(Messages.getString("MainFrame.datetool")); 
        menuToolsDateTools.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				openDateTool();
			}
        	
        });
        menuTools.add(menuToolsCreateGuid);
        menuTools.add(menuToolsBase64Viewer);
        menuTools.add(menuToolsRegexTester);
        menuTools.add(menuToolsTextFileConverter);
        menuTools.add(menuToolsTalendSchemaConverter);
        menuTools.add(menuToolsDateTools);
        // Menue Fenster
        menuWindow.setText(Messages.getString("MainFrame.menuwindow")); 
        menuWindow.setEnabled(true);
        menuWindow.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent e) {
                setupWindowMenu();
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }

        });
        menuWindowNew.setText(Messages.getString("MainFrame.menunewwindow")); 
        menuWindowNew.setActionCommand("window_new"); 
        menuWindowNew.addActionListener(this);
        menuWindowSetCustomName.setText(Messages.getString("MainFrame.customWindowName"));
        menuWindowSetCustomName.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {		        
				String text = JOptionPane.showInputDialog("Titel", windowName);
				if (text != null) {
					logger.info("Set custom window name=" + text);
					customWindowName = text.trim();
				}
				setupWindowTitle();
			}
		});
        menuWindowClearCustomName.setText(Messages.getString("MainFrame.clearCustomWindowName"));
        menuWindowClearCustomName.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				customWindowName = null;
				setupWindowTitle();
			}
		});
        menuWindowCloseAllOther.setText(Messages.getString("MainFrame.menucloseallotherwindows")); 
        menuWindowCloseAllOther.setActionCommand("window_close_all_other"); 
        menuWindowCloseAllOther.addActionListener(this);
        menuWindowArrangeOverlapped.setText(Messages.getString("MainFrame.menuoverlapwindows")); 
        menuWindowArrangeOverlapped.setActionCommand("window_overlapped"); 
        menuWindowArrangeOverlapped.addActionListener(this);
        menuWindowArrangeHorizontal.setText(Messages.getString("MainFrame.menuarrangehorizontal")); 
        menuWindowArrangeHorizontal.setActionCommand("window_horizontal"); 
        menuWindowArrangeHorizontal.addActionListener(this);
        menuWindowArrangeVertical.setText(Messages.getString("MainFrame.menuarrangevertical")); 
        menuWindowArrangeVertical.setActionCommand("window_vertical"); 
        menuWindowArrangeVertical.addActionListener(this);
        menuWindowAllToFront.setText(Messages.getString("MainFrame.bringAllToFront"));
        menuWindowAllToFront.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				MainFrame.bringAllMainFramesToFront(MainFrame.this);
			}
			
		});
        menuWindowClose.setText(Messages.getString("MainFrame.menuclosewindow")); 
        menuWindowClose.setActionCommand("close"); 
        menuWindowClose.addActionListener(this);
        // Menue Hilfe
        menuHelp.setText(Messages.getString("MainFrame.menuhelp")); 
        menuHelpAbout.setText(Messages.getString("MainFrame.menuinfo")); 
        menuHelpAbout.setActionCommand("about"); 
        menuHelpAbout.addActionListener(this);
        menuHelp.add(menuHelpAbout);
    	if (Desktop.isDesktopSupported()) {
            menuHomeBrowser.setText(Messages.getString("MainFrame.homebrowser"));
            menuHomeBrowser.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					openHomePage();
				}
            	
            });
            menuHelp.add(menuHomeBrowser);
            menuHelpBrowser.setText(Messages.getString("MainFrame.helpbrowser"));
            menuHelpBrowser.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					openHelpPage();
				}
            	
            });
            menuHelp.add(menuHelpBrowser);
    	}
        menuBarMain.add(menuScript);
        menuBarMain.add(menuDB);
        menuBarMain.add(menuEdit);
        menuBarMain.add(menuTools);
        menuBarMain.add(menuWindow);
        menuBarMain.add(menuHelp);
        // setzt Menue zum Pane
        setJMenuBar(menuBarMain);
        menuBarMain.setRequestFocusEnabled(false);
        menuBarMain.setFocusable(false);
    }
    
    private void openHelpPage() {
    	Desktop desktop = Desktop.getDesktop();
		String url = "http://jan-lolling.de/sqlrunner/help/SQLRunner.html";
    	URI uri = null;
		try {
			uri = new URI(url);
	    	desktop.browse(uri);
		} catch (URISyntaxException e) {
			logger.warn("openHelpPage:" + e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("openHelpPage:" + e.getMessage(), e);
		}
    }
    
    private void openHomePage() {
    	Desktop desktop = Desktop.getDesktop();
		String url = "http://jan-lolling.de/";
    	URI uri = null;
		try {
			uri = new URI(url);
	    	desktop.browse(uri);
		} catch (URISyntaxException e) {
			logger.warn("openHelpPage:" + e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("openHelpPage:" + e.getMessage(), e);
		}
    }

    private void initToolbar() {
        tbButtonNew.setToolTipText(menuScriptNew.getText());
        tbButtonNew.setActionCommand(menuScriptNew.getActionCommand());
        tbButtonNew.addActionListener(this);
        tbButtonOpen.setToolTipText(menuScriptOpen.getText());
        tbButtonOpen.setActionCommand(menuScriptOpen.getActionCommand());
        tbButtonOpen.addActionListener(this);
        tbButtonSave.setEnabled(false);
        tbButtonSave.setToolTipText(menuScriptSave.getText());
        tbButtonSave.setActionCommand(menuScriptSave.getActionCommand());
        tbButtonSave.addActionListener(this);
        tbButtonDbOpen.setActionCommand(menuDBOpen.getActionCommand());
        tbButtonDbOpen.addActionListener(this);
        tbButtonDbOpen.setToolTipText(menuDBOpen.getText());
        tbButtonDbClose.setActionCommand(menuDBClose.getActionCommand());
        tbButtonDbClose.addActionListener(this);
        tbButtonDbClose.setToolTipText(menuDBClose.getText());
        tbButtonDataModel.setActionCommand(menuDBDatamodel.getActionCommand());
        tbButtonDataModel.addActionListener(this);
        tbButtonDataModel.setToolTipText(menuDBDatamodel.getText());
        tbButtonHistory.setActionCommand(menuDBHistory.getActionCommand());
        tbButtonHistory.addActionListener(this);
        tbButtonHistory.setToolTipText(menuDBHistory.getText());
        tbButtonCut.setToolTipText(menuEditCut.getText());
        tbButtonCut.setActionCommand(menuEditCut.getActionCommand());
        tbButtonCut.addActionListener(this);
        tbButtonCopy.setToolTipText(menuEditCopy.getText());
        tbButtonCopy.setActionCommand(menuEditCopy.getActionCommand());
        tbButtonCopy.addActionListener(this);
        tbButtonPaste.setToolTipText(menuEditPaste.getText());
        tbButtonPaste.setActionCommand(menuEditPaste.getActionCommand());
        tbButtonPaste.addActionListener(this);
        tbButtonUndo.setEnabled(false);
        tbButtonUndo.setToolTipText(menuEditUndo.getText());
        tbButtonUndo.setActionCommand(menuEditUndo.getActionCommand());
        tbButtonUndo.addActionListener(this);
        tbButtonRedo.setEnabled(false);
        tbButtonRedo.setToolTipText(menuEditRedo.getText());
        tbButtonRedo.setActionCommand(menuEditRedo.getActionCommand());
        tbButtonRedo.addActionListener(this);
        tbButtonEinrueck.setToolTipText(Messages.getString("MainFrame.tbindentblockright"));
        tbButtonEinrueck.setActionCommand("einrueck");
        tbButtonEinrueck.addActionListener(this);
        tbButtonAusrueck.setToolTipText(Messages.getString("MainFrame.tbindentblockleft"));
        tbButtonAusrueck.setActionCommand("ausrueck");
        tbButtonAusrueck.addActionListener(this);
        tbButtonSearch.setToolTipText(menuEditReplace.getText());
        tbButtonSearch.setActionCommand(menuEditReplace.getActionCommand());
        tbButtonSearch.addActionListener(this);
        tbButtonGoto.setToolTipText(menuEditGoto.getText());
        tbButtonGoto.setActionCommand(menuEditGoto.getActionCommand());
        tbButtonGoto.addActionListener(this);
        tbButtonRun.setToolTipText(menuDBRun.getText());
        tbButtonRun.setActionCommand(menuDBRun.getActionCommand());
        tbButtonRun.addActionListener(this);
        tbButtonExplain.setToolTipText(menuDBExplain.getText());
        tbButtonExplain.setActionCommand(menuDBExplain.getActionCommand());
        tbButtonExplain.addActionListener(this);
        tbButtonExplain.setEnabled(false);
        tbButtonStop.setToolTipText(menuDBStop.getText());
        tbButtonStop.setActionCommand(menuDBStop.getActionCommand());
        tbButtonStop.addActionListener(this);
        tbButtonCommit.setToolTipText(menuDBCommit.getText());
        tbButtonCommit.setActionCommand(menuDBCommit.getActionCommand());
        tbButtonCommit.addActionListener(this);
        tbButtonRollback.setToolTipText(menuDBRollback.getText());
        tbButtonRollback.setActionCommand(menuDBRollback.getActionCommand());
        tbButtonRollback.addActionListener(this);
        tbButtonTableSearch.setToolTipText(menuDBSearchInTable.getText());
        tbButtonTableSearch.setActionCommand(menuDBSearchInTable.getActionCommand());
        tbButtonTableSearch.addActionListener(this);
        tbButtonToUpperCase.setToolTipText(Messages.getString("MainFrame.tbtouppercase"));
        tbButtonToUpperCase.setActionCommand("to_uppercase");
        tbButtonToUpperCase.addActionListener(this);
        tbButtonToUpperCase.setEnabled(false);
        tbButtonToLowerCase.setToolTipText(Messages.getString("MainFrame.tbtolowercase"));
        tbButtonToLowerCase.setActionCommand("to_lowercase");
        tbButtonToLowerCase.addActionListener(this);
        tbButtonToLowerCase.setEnabled(false);
        tbButtonComments.setEnabled(false);
        tbButtonComments.setActionCommand("comment");
        tbButtonComments.addActionListener(this);
        tbButtonComments.setToolTipText(Messages.getString("MainFrame.tbtogglelinecomment"));
        tbButtonHighlighter.setSelected(Main.getUserProperty("HIGHLIGHT_WORD_UNDER_CARET", "true").equals("true"));
        tbButtonHighlighter.setToolTipText(Messages.getString("MainFrame.tbhighlightidentifier"));
        tbButtonHighlighter.addActionListener(this);
        tbButtonHighlighter.setActionCommand("highlight"); 
        tbButtonTableOrientation.setSelected(Main.getUserProperty("TABLE_ORIENTATION", "h").equals("v")); 
        tbButtonTableOrientation.setToolTipText(Messages.getString("MainFrame.toggleTableOrientation"));
        tbButtonTableOrientation.addActionListener(this);
        tbButtonTableOrientation.setActionCommand("toggle_vertical");
        // Toolbar initialisieren
        mainToolBar.setRollover(true);
        mainToolBar.setFocusable(false);
        mainToolBar.add(tbButtonDbOpen);
        mainToolBar.add(tbButtonDbClose);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonDataModel);
        mainToolBar.add(tbButtonHistory);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonRun);
        mainToolBar.add(tbButtonExplain);
        mainToolBar.add(tbButtonStop);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonCommit);
        mainToolBar.add(tbButtonRollback);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonTableSearch);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonTableOrientation);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonNew);
        mainToolBar.add(tbButtonOpen);
        mainToolBar.add(tbButtonSave);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonCut);
        mainToolBar.add(tbButtonCopy);
        mainToolBar.add(tbButtonPaste);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonUndo);
        mainToolBar.add(tbButtonRedo);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonSearch);
        mainToolBar.add(tbButtonGoto);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonEinrueck);
        mainToolBar.add(tbButtonAusrueck);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonToLowerCase);
        mainToolBar.add(tbButtonToUpperCase);
        mainToolBar.addSeparator();
        mainToolBar.add(tbButtonComments);
        mainToolBar.add(tbButtonHighlighter);
        mainToolBar.setRollover(true);
        mainToolBar.setFloatable(false);
    }
    
    private void initComponents() {
        if (logger.isDebugEnabled()) {
            logger.debug("initComponents start"); 
        }
        setDateFormatMask(Main.getUserProperty("DATE_FORMAT", "dd.MM.yyyy HH:mm:ss"));  
        setIconImage(ApplicationImages.SQLRUNNER_PNG);
        if (WindowHelper.isWindowPositioningEnabled()) {
            setBounds(xLoc, yLoc, wide, high);
        } else {
            setSize(wide, high);
        }
        dmFrame = DataModelFrame.getDataModelFrame();
        resultTable.setBackground(Main.info);
        // TAB-Ersatzstring formen
        tabErsatz = createTabReplacement();
        // Menue initialisieren
        // Mnemonic initialisieren
        initMenu();
        // ToolBar Button einstellen
        // Beschriftung und ActionCommand den zugehörigen MenuItems entnehmen
        initToolbar();
        status = new StatusBar();
        editor = getExtEditor();
        editor.setFont(Main.textFont);
        textChangeListener = new TextChangeListener(editor);
        editor.addCaretListener(textChangeListener);
        setTextSaveEnabled(false);
        setupWindowTitle();
        final Document doc = editor.getDocument();
        doc.addDocumentListener(textChangeListener);
        doc.addUndoableEditListener(undoHandler);
        undoManager.discardAllEdits();
        updateUndoRedoEnabled();
        textIsSelected(false);
        prepareTable();
        tableScrollPane.setViewportView(resultTable);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(getLayeredEditorPane());
        splitPane.setDividerSize(10);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(Integer.parseInt(Main.getUserProperty("DIVIDER_LOCATION", "100")));  
        splitPane.setBottomComponent(tableScrollPane);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainToolBar, BorderLayout.NORTH);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);
        getContentPane().validate();
        setFocusToEditor();
        setGuiToConnected(false);
        setTableSearchEnabled(false);
        if (database != null) {
            database.setVerticalView(Main.getUserProperty("TABLE_ORIENTATION", "h").equals("v"));
        }
    }
    
    private JLayeredPane getLayeredEditorPane() {
    	if (editorLayeredPane == null) {
        	editorLayeredPane = new JLayeredPane();
        	BorderLayout bl = new BorderLayout();
        	editorLayeredPane.setLayout(bl);
            editorScrollPane.setViewportView(editor);
            editorLayeredPane.setLayer(editorScrollPane, JLayeredPane.DEFAULT_LAYER);
			editorLayeredPane.setLayer(getSyntaxChooser(), JLayeredPane.MODAL_LAYER);
            editorLayeredPane.add(editorScrollPane);
			editorLayeredPane.add(getSyntaxChooser(), 1);
			editorLayeredPane.getLayout().removeLayoutComponent(getSyntaxChooser()); // we need null layout
			editorLayeredPane.validate();
        	bl.addLayoutComponent(editorScrollPane, BorderLayout.CENTER);
    	}
    	return editorLayeredPane;
    }
    
    

    public void setProductive(boolean productive) {
    	editor.setBackground(productive ? productiveBackground : Color.WHITE);
    }

    public void setupEditorFont() {
        editor.setFont(Main.textFont);
        if (lexer != null) {
            lexer.setupChangedTextFont();
        }
        SyntaxContext.setupTextFont();
        repaint();
    }

    private void setFocusToEditor() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // nichts zu tun
                }
                editor.requestFocus();
            }
        });
    }

    /**
     * schliesst das Datenmodellfenster
     */
    public void closeDMFrame() {
        if (dmFrame != null) {
            Main.refreshUserProperties(this); // damit die Einstellung erhalten bleiben
            dmFrame.setVisible(false);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (isShowing() == false) {
            this.setLocationByPlatform(WindowHelper.isWindowPositioningEnabled() == false);
        }
        super.setVisible(visible);
    }

    /**
     * aktualisiert das Look&Feel auch für alle von Hauptfenster gestarteten Dialoge
     */
    public void updateUI() {
        SwingUtilities.updateComponentTreeUI(this);
        if (atc != null) {
            SwingUtilities.updateComponentTreeUI(atc);
        }
        if (exportResultTableDialog != null) {
            SwingUtilities.updateComponentTreeUI(exportResultTableDialog);
        }
        if (metaDataView != null) {
            SwingUtilities.updateComponentTreeUI(metaDataView);
        }
        if (dbLogin != null) {
            SwingUtilities.updateComponentTreeUI(dbLogin);
        }
        if (preferencesDialog != null) {
            SwingUtilities.updateComponentTreeUI(preferencesDialog);
        }
        if (dlgSeRe != null) {
            SwingUtilities.updateComponentTreeUI(dlgSeRe);
        }
        if (tableSearchDialog != null) {
            SwingUtilities.updateComponentTreeUI(tableSearchDialog);
        }
        if (dlgGo != null) {
            SwingUtilities.updateComponentTreeUI(dlgGo);
        }
        if (dlgAbout != null) {
            SwingUtilities.updateComponentTreeUI(dlgAbout);
        }
        updateEditorKeymap();
    }

    private boolean tableIsInIntervalSelectionMode = false;

    private void setTableSelectionToIntervalMode(boolean intervalMode) {
        if (intervalMode != tableIsInIntervalSelectionMode) {
            tableIsInIntervalSelectionMode = intervalMode;
            // memorize a existing selection
            int row = resultTable.getSelectedRow();
            int col = resultTable.getSelectedColumn();
            if (intervalMode) {
                resultTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                resultTable.setColumnSelectionAllowed(true);
                if (row != -1 && col != -1) {
                    resultTable.changeSelection(row, col, false, false);
                }
            } else {
                resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                resultTable.setColumnSelectionAllowed(false);
                if (row != -1) {
                    resultTable.setRowSelectionInterval(row, row);
                }
            }
        }
    }

    /**
     * konfiguriert die Ergebnistabelle
     */
    public void prepareTable() {
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getSelectionModel().addListSelectionListener(this);
        resultTable.getTableHeader().addMouseListener(new TableHeaderMouseListener());
        final TableMouseListener ml = new TableMouseListener();
        resultTable.addMouseListener(ml);
        resultTable.addMouseMotionListener(ml);
        // bewirkt dass resultTable.getScrollableTracksViewportWidth()==false ergibt -> vertikaler Scrollbalken
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.getTableHeader().setReorderingAllowed(false); // kein Verschieben der Spalten
        resultTable.setToolTipText(null); // das verhindert, dass allein durch Mausbewegung Zelleninhalte neu gelesen werden
        resultTable.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                if (e.getModifiers() == KeyEvent.SHIFT_MASK) {
                    setTableSelectionToIntervalMode(true);
                } else if (e.getModifiers() == 0) {
                    setTableSelectionToIntervalMode(false);
                }
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }
        });
        resultTable.registerKeyboardAction(
            this,
            "table_edit_cell", 
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED);
        resultTable.registerKeyboardAction(this, "table_insert_row", KeyStroke.getKeyStroke( 
            KeyEvent.VK_I,
            KeyEvent.ALT_MASK), JComponent.WHEN_FOCUSED);
        resultTable.registerKeyboardAction(this, "table_insert_row_cancel", KeyStroke.getKeyStroke( 
            KeyEvent.VK_ESCAPE,
            0), JComponent.WHEN_FOCUSED);
        resultTable.registerKeyboardAction(this, "db_insert_row", KeyStroke.getKeyStroke( 
            KeyEvent.VK_S,
            KeyEvent.ALT_MASK), JComponent.WHEN_FOCUSED);
        resultTable.registerKeyboardAction(this, "toggle_vertical", KeyStroke.getKeyStroke( 
            KeyEvent.VK_V,
            KeyEvent.ALT_MASK), JComponent.WHEN_FOCUSED);
        prepareTableRenderer();
    }

    /**
     * aktualisiert die Tabellen Renderer
     */
    public void prepareTableRenderer() {
        if (logger.isDebugEnabled()) {
            logger.debug("prepareTableRenderer");
        }
        objectTableRenderer = new ResultTableCellRenderer();
        objectTableRenderer.setDateFormatMask(getDateFormatMask());
        resultTable.setDefaultRenderer(java.sql.Date.class, objectTableRenderer);
        resultTable.setDefaultRenderer(Time.class, objectTableRenderer);
        resultTable.setDefaultRenderer(Timestamp.class, objectTableRenderer);
        resultTable.setDefaultRenderer(String.class, objectTableRenderer);
        resultTable.setDefaultRenderer(byte[].class, objectTableRenderer);
        resultTable.setDefaultRenderer(Boolean.class, objectTableRenderer);
        resultTable.setDefaultRenderer(Object.class, objectTableRenderer);
    }

    /**
     * setzt den Fenstertitel nach internen Vorlagen
     */
    public void setupWindowTitle() {
        // wenn verbunden, dann diesen Umstand als erstes darstellen
    	if (customWindowName != null) {
    		windowName = customWindowName;
            final StringBuilder title = new StringBuilder(100);
            title.append(customWindowName);
            title.append(" - SQLRunner"); 
            setTitle(title.toString());
    	} else {
            if (database != null && database.getDatabaseSession().isConnected()) {
                final StringBuilder title = new StringBuilder(100);
                title.append("DB: "); 
                title.append(database.getDatabaseSession().getConnectionDescription().toString());
                if (currentFile != null) {
                    title.append(Messages.getString("MainFrame.fragmenttitlefile")); 
                    title.append(currentFile.getName());
                }
                windowName = title.toString();
                title.append(" - SQLRunner"); 
                setTitle(title.toString());
            } else if (currentFile != null) {
                final StringBuilder title = new StringBuilder(100);
                title.append(Messages.getString("MainFrame.file")); 
                title.append(currentFile.getName());
                windowName = title.toString();
                title.append(" - SQLRunner"); 
                setTitle(title.toString());
            } else {
                windowName = "not used";
                setTitle("SQLRunner");
            }
    	}
    }

    private void moveSplitPaneDividerToButtom() {
        splitPane.setDividerLocation(1000);
    }

    private void moveSplitPaneDividerToMiddle() {
        splitPane.setDividerLocation(splitPane.getMaximumDividerLocation() / 2);
    }

    public void ensureResultTableIsVisible() {
    	if (SwingUtilities.isEventDispatchThread()) {
            int dividerPos = splitPane.getDividerLocation();
            if (dividerPos > splitPane.getMaximumDividerLocation() - 20) {
                moveSplitPaneDividerToMiddle();
            }
    	} else {
    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    		        int dividerPos = splitPane.getDividerLocation();
    		        if (dividerPos > splitPane.getMaximumDividerLocation() - 20) {
    		            moveSplitPaneDividerToMiddle();
    		        }
    			}
    		});
    	}
    }

    public void handleFile(String filePath) {
    	if (filePath != null) {
    		filePath = filePath.trim(); // under Linux some times we get an \n at the end of file name
        	handleFile(new File(filePath));
    	}
    }
    
    /**
     * laden eines Dokuments beim Start der Applikation
     * @param file zu ladende Dateiname
     */
    public void handleFile(File file) {
    	if (file == null) {
    		throw new IllegalArgumentException("file cannot be null.");
    	}
    	String filename = file.getName();
    	logger.info("handleFile: " + file);
        if (filename.toLowerCase().endsWith(".sql")) {
            loadFileInDocument(true, file.getAbsolutePath());
        } else if (filename.toLowerCase().endsWith(ImportConfiguratorFrame.IMPORT_CONFIG_EXTENSION)) {
            openCSVImportDialog(file);
        } else {
        	FileImporter fi = new FileImporter();
        	if (fi.isDataFile(file)) {
                openCSVImportDialog(file);
        	} else {
        		logger.warn("unknown file type in file " + file);
        	}
        }
    }

    public void loadAndRun(String filename) {
        loadFileInDocument(false, filename);
        runScript();
    }

    /**
     * Zusamenfassung der Konfiguration Menue-Mnemonics
     */
    private void setMenuMnemonic() {
        if (Main.getDefaultProperty("USE_MENU_MNEMONIC", "false").equals("true")) {
            if (logger.isDebugEnabled()) {
                logger.debug("MainFrame.useMnemonic: use menu-mnemonics"); 
            }
            menuScript.setMnemonic('D');
            menuDB.setMnemonic('a');
            menuEdit.setMnemonic('B');
            menuWindow.setMnemonic('F');
            menuHelp.setMnemonic('H');
            menuWindowNew.setMnemonic('N');
        }
    }

    /**
     * setzt die Menüshortcuts.
     */
    private void setMenuShortcut() {
        if (Main.getDefaultProperty("USE_MENU_SHORTCUT", "true").equals("true")) {   
            if (logger.isDebugEnabled()) {
                logger.debug("MainFrame.useShortcut: use menu shortcuts"); 
            }
            Toolkit tkLocal = Toolkit.getDefaultToolkit();
            menuScriptNew.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_N,
                tkLocal.getMenuShortcutKeyMask()));
            menuScriptOpen.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O,
                tkLocal.getMenuShortcutKeyMask()));
            menuScriptSave.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S,
                tkLocal.getMenuShortcutKeyMask()));
            menuScriptInfo.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_I,
                tkLocal.getMenuShortcutKeyMask()));
            menuScriptShutdown.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Q,
                tkLocal.getMenuShortcutKeyMask()));
            menuEditUndo.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Z,
                tkLocal.getMenuShortcutKeyMask()));
            menuEditRedo.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Y,
                tkLocal.getMenuShortcutKeyMask()));
            menuEditCut.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_X,
                tkLocal.getMenuShortcutKeyMask()));
            menuEditCopy.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_C,
                tkLocal.getMenuShortcutKeyMask()));
            menuEditPaste.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_V,
                tkLocal.getMenuShortcutKeyMask()));
            menuEditReplace.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_F,
                tkLocal.getMenuShortcutKeyMask()));
            menuEditGoto.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_G,
                tkLocal.getMenuShortcutKeyMask()));
            menuDBRun.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
            menuDBStop.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_F5,
                tkLocal.getMenuShortcutKeyMask()));
            menuDBHistory.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            menuDBOpen.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_D,
                tkLocal.getMenuShortcutKeyMask()));
            menuDBClose.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_E,
                tkLocal.getMenuShortcutKeyMask()));
            menuDBAdmin.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
            menuDBCommit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
            menuDBRollback.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
            menuDBCreateNewRow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
            menuDBSearchInTable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK));
            menuDBDatamodel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
            menuWindowNew.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_N,
                (ActionEvent.ALT_MASK | tkLocal.getMenuShortcutKeyMask())));
            if (System.getProperty("os.name").toLowerCase().trim().startsWith("mac")) {  
                menuWindowClose.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_W,
                    tkLocal.getMenuShortcutKeyMask()));
                menuScriptSaveas.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_S,
                    (ActionEvent.SHIFT_MASK | tkLocal.getMenuShortcutKeyMask())));
                menuEditPreferences.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_COMMA,
                    tkLocal.getMenuShortcutKeyMask()));
            }
        }
    }

    /**
     * gibt editor-Objekt zurück ja nach Property mit oder ohne Zeilenumbruch
     * @return Editor-Komponente
     */
    private ExtEditorPane getExtEditor() {
    	if (editor == null) {
            editor = new ExtEditorPane(this); // siehe unten : innere Klasse
            final ExtEditorKit kit = new ExtEditorKit(this);
            editor.setEditorKitForContentType("text/sql", kit); 
            editor.getCaret().setBlinkRate(Main.CARET_BLINK_RATE);
            editor.setContentType("text/sql");
            // dem Scanner den editor mitteilen
            if (lexer != null) {
                lexer.setEditor(editor);
            }
            // Kontextmenu erstellen durch MouseListener
            EditorMouseListener ml = new EditorMouseListener();
            editor.addMouseListener(ml);
            editor.addMouseMotionListener(ml);
            editor.setDragEnabled(true);
            editor.setTransferHandler(new ExtEditorTransferHandler());
    	}
    	return editor;
    }
    
    private void setupNewDocumentWithListenersAndUndoRedo() {
        final Document doc = editor.getEditorKit().createDefaultDocument();
        doc.addDocumentListener(textChangeListener);
        doc.addUndoableEditListener(undoHandler);
        editor.setDocument(doc);
        undoManager.discardAllEdits();
        updateUndoRedoEnabled();
        setTextSaveEnabled(false);
        status.message.setText(""); 
        currentFile = null;
    }

    public void setTextSaveEnabled(boolean enabled) {
        if (enabled) {
            if (logger.isDebugEnabled()) {
                logger.debug("setTextSaveEnabled: setze changed-Flag auf true"); 
            }
            textChanged = true;
            tbButtonSave.setEnabled(true);
            // das sorgt bei Mac OS X dafür, dass der Schliessen-Button des Fensters
            // mit einem schwarzen Punkt besetzt wird
            getRootPane().putClientProperty("windowModified", Boolean.TRUE); 
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("setTextSaveEnabled: setze changed-Flag auf false"); 
            }
            textChanged = false;
            tbButtonSave.setEnabled(false);
            getRootPane().putClientProperty("windowModified", Boolean.FALSE); 
        }
    }

    private void textIsSelected(boolean selected) {
        tbButtonCopy.setEnabled(selected);
        tbButtonCut.setEnabled(selected);
        tbButtonEinrueck.setEnabled(selected);
        tbButtonAusrueck.setEnabled(selected);
        tbButtonToLowerCase.setEnabled(selected);
        tbButtonToUpperCase.setEnabled(selected);
        tbButtonComments.setEnabled(selected);
        textIsSelected = selected;
    }

    public boolean isTextSelected() {
        return textIsSelected;
    }

    // diese Funktion sollte später in
    // SQLDocument verschoben werden
    // zusammen mit setTextSaveEnabled
    // in dieser Klasse wird die Variable selbst nicht mehr abgefragt
    // die anderen Klassen sollten diese Methode verwenden
    public boolean isTextChanged() {
        return ((editor.getDocument().getLength() == 0) ? false : textChanged);
    }

    @Override
    protected void processWindowEvent(WindowEvent winEvent) {
        switch (winEvent.getID()) {
            case WindowEvent.WINDOW_CLOSING: {
                if (close()) {
                    super.processWindowEvent(winEvent);
                }
                break;
            }
            case WindowEvent.WINDOW_ACTIVATED: {
                if (dmFrame != null && database != null) {
                    dmFrame.setMainFrame(this);
                }
                if (sqlHistory != null) {
                    sqlHistory.setMainFrame(this);
                }
                WindowHelper.checkAndCorrectWindowBounds(this);
            }
            default:
                super.processWindowEvent(winEvent);
        }
    }

    public static void addLongRunningAction(LongRunningAction action) {
        if (listOfLongRunningActions.contains(action) == false) {
            listOfLongRunningActions.add(action);
        }
    }

    public static void removeLongRunningAction(LongRunningAction action) {
        listOfLongRunningActions.remove(action);
    }

    private void setupWindowDefaultMenuItems() {
        menuWindow.removeAll();
        menuWindow.add(menuWindowNew);
        menuWindow.add(menuWindowSetCustomName);
        menuWindow.add(menuWindowClearCustomName);
        menuWindow.addSeparator();
        menuWindow.add(menuWindowArrangeOverlapped);
        menuWindow.add(menuWindowArrangeHorizontal);
        menuWindow.add(menuWindowArrangeVertical);
        menuWindow.add(menuWindowAllToFront);
        menuWindow.addSeparator();
        menuWindow.add(menuWindowClose);
        menuWindow.add(menuWindowCloseAllOther);
    }
    
    private static void bringAllMainFramesToFront(MainFrame current) {
        for (final MainFrame mf : Main.getWindowList()) {
        	if (mf != current) {
    			mf.setVisible(true);
    			mf.setState(Frame.NORMAL);
    			mf.toFront();
        	}
        }
    }

    private void setupWindowMainFrameMenuItems() {
        boolean firstLoop = true;
        for (final MainFrame mf : Main.getWindowList()) {
            if (mf != this) {
                if (firstLoop) {
                    menuWindow.addSeparator();
                    firstLoop = false;
                }
                JMenuItem mi = new JMenuItem(mf.getMenuWindowLabelText());
                String text = getText();
                if (text.length() > 0) {
                    mi.setToolTipText(text);
                }
                mi.addActionListener(new AbstractAction() {

					private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent e) {
						mf.setVisible(true);
						mf.setState(Frame.NORMAL);
						mf.toFront();
                    }

                });
                if (mf.isBusy()) {
                    mi.setIcon(ApplicationIcons.START_GIF);
                }
                menuWindow.add(mi);
            }
        }
        SwingUtilities.updateComponentTreeUI(menuWindow);
    }

    public void setupWindowMenu() {
        setupWindowDefaultMenuItems();
        setupWindowMainFrameMenuItems();
        SwingUtilities.updateComponentTreeUI(menuWindow);
    }

    /**
     * liest die userprop (FILE_ ...) aus und generiert daraus Reopen-Menü-Einträge
     */
    public void createReopenItems() {
        try {
            // alle alten Einträge löschen
            menuScriptReopen.removeAll();
            boolean hasItems = false;
            //wie viele Eintraege darf es geben?
            final int max = Integer.parseInt(Main.getDefaultProperty("MAX_FILES_ENTRIES", "10"));
            for (int i = 0; i < max; i++) {
                final String fileName = Main.getUserProperty("FILE_" + i);
                if (fileName != null && fileName.length() > 0) {
                    final JMenuItem item = new JMenuItem(fileName);
                    item.setActionCommand(item.getText());
                    menuScriptReopen.add(item);
                    item.addActionListener(reopenListener);
                    hasItems = true;
                }
            }
            if (hasItems) {
                menuScriptReopen.addSeparator();
                final JMenuItem item = new JMenuItem(Messages.getString("MainFrame.clearfilelist"));
                item.setActionCommand("clear_file_list"); 
                item.addActionListener(this);
                menuScriptReopen.add(item);
            } else {
                final JMenuItem item = new JMenuItem(Messages.getString("MainFrame.empty"));
                menuScriptReopen.add(item);
            }
            validate();
        } catch (java.security.AccessControlException ae) {
            if (logger.isDebugEnabled()) {
                logger.debug("createReopenItems: " + ae.getMessage());
            }
        }
    }

    /**
     * Methode für das Interface ListSelectionListener
     */
    public void valueChanged(ListSelectionEvent e) {
        final ListSelectionModel lsm = ((ListSelectionModel) e.getSource());
        if (lsm.isSelectionEmpty() == false) {
            if (database.isVerticalView()) {
                selectedRowIndex = database.convertFromVerticalToLogicalRowIndex(lsm.getMinSelectionIndex());
            } else {
                selectedRowIndex = lsm.getMinSelectionIndex();
                int[] selectedRows = resultTable.getSelectedRows();
                int[] selectedColumns = resultTable.getSelectedColumns();
                if (selectedRows.length > 0 && selectedColumns.length > 0) {
                    if (logger.isDebugEnabled()) {
                        if (e.getValueIsAdjusting() == false) {
                            StringBuilder message = new StringBuilder();
                            message.append("rows=" + (selectedRows[0]+1) + ":" + (selectedRows[selectedRows.length - 1]+1));
                            message.append(" cols=" + (selectedColumns[0]+1) + ":" + (selectedColumns[selectedColumns.length - 1]+1));
                            logger.debug(message.toString());
                        }
                    }
                    if (selectedColumns.length == 1 && selectedRows.length > 1) {
                        Statistic stat = database.getRowStatistics(selectedRows, selectedColumns[0]);
                        setStatusMessage(stat.render());
                    } else if (selectedColumns.length > 1 && selectedRows.length == 1) {
                        double sum = database.calculateRowValueSum(selectedRows[0], selectedColumns);
                        setStatusMessage("sum=" + sum + " count columns=" + selectedColumns.length);
                    }
                }
            }
            status.tablePos.setText(String.valueOf(selectedRowIndex + 1));
        }
    }

    /**
     * ActionListener-Methode
     * @param ausgelöster ActionEvent
     * @see ActionListener.actionPerformed()
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("new")) { 
            menuScriptNew_actionPerformed();
        } else if (e.getActionCommand().equals("open")) { 
            menuScriptOpen_actionPerformed();
        } else if (e.getActionCommand().equals("clear_file_list")) { 
            Main.removeAllFileProps();
        } else if (e.getActionCommand().equals("save")) { 
            menuScriptSave_actionPerformed();
        } else if (e.getActionCommand().equals("saveas")) { 
            menuScriptSaveas_actionPerformed();
        } else if (e.getActionCommand().equals("close")) { 
            close();
        } else if (e.getActionCommand().equals("shutdown")) { 
            Main.shutdown();
        } else if (e.getActionCommand().equals("fileinfo")) { 
            menuScriptInfo_actionPerformed();
        } else if (e.getActionCommand().equals("opendb")) { 
            menuDBOpen_actionPerformed();
        } else if (e.getActionCommand().equals("reconnect")) { 
            startReconnect();
        } else if (e.getActionCommand().equals("closedb")) { 
            startDisconnect();
        } else if (e.getActionCommand().equals("undo")) { 
            menuEditUndo_actionPerformed();
        } else if (e.getActionCommand().equals("redo")) { 
            menuEditRedo_actionPerformed();
        } else if (e.getActionCommand().equals("cut")) { 
            menuEditCut_actionPerformed();
        } else if (e.getActionCommand().equals("copy")) { 
            menuEditCopy_actionPerformed();
        } else if (e.getActionCommand().equals("paste")) { 
            menuEditPaste_actionPerformed();
        } else if (e.getActionCommand().equals("paste_smart")) { 
        	menuEditPasteSmart_actionPerformed();
        } else if (e.getActionCommand().equals("to_lowercase")) { 
            new ExtEditorKit.ToLowerCase().actionPerformed(new ActionEvent(editor, 9997, e.getActionCommand()));
        } else if (e.getActionCommand().equals("to_uppercase")) { 
            new ExtEditorKit.ToUpperCase().actionPerformed(new ActionEvent(editor, 9996, e.getActionCommand()));
        } else if (e.getActionCommand().equals("comment")) { 
            new ExtEditorKit.ToggleLineComment().actionPerformed(new ActionEvent(editor, 9995, e.getActionCommand()));
        } else if (e.getActionCommand().equals("preferences")) { 
            menuEditPreferences_actionPerformed();
        } else if (e.getActionCommand().equals("einrueck")) { 
            // im Argument eine neu Action erstellen, da diese zur Weitergabe der Referenz auf den Editor genutzt wird.
            new ExtEditorKit.MoveOut().actionPerformed(new ActionEvent(editor, 9998, e.getActionCommand()));
        } else if (e.getActionCommand().equals("ausrueck")) { 
            new ExtEditorKit.MoveBack().actionPerformed(new ActionEvent(editor, 9999, e.getActionCommand()));
        } else if (e.getActionCommand().equals("trim")) { 
            new ExtEditorKit.Trim().actionPerformed(new ActionEvent(editor, 9999, e.getActionCommand()));
        } else if (e.getActionCommand().equals("search")) { 
            menuEditReplace_actionPerformed();
        } else if (e.getActionCommand().equals("tablesearch")) { 
            menuDBSearchInTable_actionPerformed();
        } else if (e.getActionCommand().equals("goto")) { 
            menuEditGoto_actionPerformed();
        } else if (e.getActionCommand().equals("syntaxhigh")) { 
            menuEditSyntaxhighCheckBox_actionPerformed();
        } else if (e.getActionCommand().equals("run")) { 
            runScript();
        } else if (e.getActionCommand().equals("explain")) { 
            runExplain();
        } else if (e.getActionCommand().equals("parse")) { 
            menuEditParse_actionPerformed();
        } else if (e.getActionCommand().equals("history")) { 
            menuDBHistory_actionPerformed();
        } else if (e.getActionCommand().equals("about")) { 
            menuHelpAbout_actionPerformed();
        } else if (e.getActionCommand().equals("defcfg")) { 
            loadFileInDocument(true, Main.getCfgFileName());
        } else if (e.getActionCommand().equals("dbcfg")) { 
            loadFileInDocument(true, Main.getDbCfgFileName());
        } else if (e.getActionCommand().equals("admincfg")) { 
            loadFileInDocument(true, Main.getAdminCfgFileName());
        } else if (e.getActionCommand().equals("restorecfg")) { 
            menuEditRestoreCfg_actionPerformed();
        } else if (e.getActionCommand().equals("autocommit")) { 
            menuDBAutoCommitCheckBox_actionPerformed();
        } else if (e.getActionCommand().equals("stop")) { 
            cancelStatement();
        } else if (e.getActionCommand().equals("admin")) { 
            menuDBAdmin_actionPerformed();
        } else if (e.getActionCommand().equals("insert")) { 
            menuEditInsertInsertStat_actionPerformed();
        } else if (e.getActionCommand().equals("select")) { 
            menuEditInsertSelectStat_actionPerformed();
        } else if (e.getActionCommand().equals("tablename")) { 
            menuEditInsertTablename_actionPerformed();
        } else if (e.getActionCommand().equals("exportresult")) { 
            menuDBExportResultTable_actionPerformed();
        } else if (e.getActionCommand().equals("exportdb")) { 
            menuDBExportDBTable_actionPerformed();
        } else if (e.getActionCommand().equals("importdata")) { 
            openCSVImportDialog(null);
        // Actions ausgelöst durch Table-Contexmenue
        } else if (e.getActionCommand().equals("xml_export")) { 
            menuDBXmlExport_actionPerformed();
        } else if (e.getActionCommand().equals("xml_import")) { 
            menuDBXmlImport_actionPerformed();
        } else if (e.getActionCommand().equals("table_edit_cell")) { 
            returnKeyPressedOnTable();
        } else if (e.getActionCommand().equals("table_delete_content")) { 
            if (databaseIsBusy == false) {
                if (selectedRowIndex != database.getNewRowIndex()) {
                    database.deleteValue(resultTable.getSelectedRow(), resultTable.getSelectedColumn());
                } else {
                    database.setValueAt(null, resultTable.getSelectedRow(), resultTable.getSelectedColumn());
                }
            }
        } else if (e.getActionCommand().equals("table_insert_row")) { 
            if (database.getNewRowIndex() == -1) {
                if (database.insertRowInTable()) {
                    menuDBCreateNewRow.setEnabled(false);
                    resultTable.setRowSelectionInterval(database.getNewRowIndex(), database.getNewRowIndex());
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            tableScrollPane.getVerticalScrollBar().setValue(tableScrollPane.getVerticalScrollBar().getMaximum());
                        }
                    }); // muss verzögert ausgeführt werden !
                } else {
                    showInfoMessage(
                        Messages.getString("MainFrame.messageinsertonlyinexistingtable"), 
                        Messages.getString("MainFrame.messagetitleinsertline")); 
                }
            }
        } else if (e.getActionCommand().equals("table_insert_row_cancel")) {  
            if (selectedRowIndex == database.getNewRowIndex()) {
                database.removeNewRow();
                menuDBCreateNewRow.setEnabled(true);
            }
        } else if (e.getActionCommand().equals("db_insert_row")) { 
            if (selectedRowIndex == database.getNewRowIndex()) {
                database.insertNewRowInDB();
            }
        } else if (e.getActionCommand().equals("commit")) {  
            commit();
        } else if (e.getActionCommand().equals("rollback")) {  
            rollback();
        } else if (e.getActionCommand().equals("table_col_pk_toggle")) {  
            togglePk();
        } else if (e.getActionCommand().equals("window_new")) { 
            Main.createInstance(this);
        } else if (e.getActionCommand().equals("window_close_all_other")) { 
            Main.closeAllOther(this);
        } else if (e.getActionCommand().equals("window_horizontal")) { 
            Main.arrangeWindowsHorizontal();
        } else if (e.getActionCommand().equals("window_vertical")) { 
            Main.arrangeWindowsVertical();
        } else if (e.getActionCommand().equals("window_overlapped")) { 
            Main.arrangeWindowsOverlapped(this);
        } else if (e.getActionCommand().equals("metainfo")) { 
            showMetaInfoDialog();
        } else if (e.getActionCommand().equals("datamodel")) { 
            menuDBDatamodel_actionPerformed();
        } else if (e.getActionCommand().startsWith("sek@")) { 
            // KontextmenueItems für referenzierende Tabellen
            // sek = select exported keys
            // aufbau ActionCommand: sek@<table>.<column>_row|col
            int p0 = e.getActionCommand().indexOf("."); 
            final int p1 = e.getActionCommand().indexOf("$"); 
            final String tableColumn = e.getActionCommand().substring(4, p1);
            p0 = e.getActionCommand().indexOf("|"); 
            final int row = Integer.parseInt(e.getActionCommand().substring(p1 + 1, p0));
            final int col = Integer.parseInt(e.getActionCommand().substring(p0 + 1, e.getActionCommand().length()));
            if (getFkNavigationFrameMode() == FK_NAVIGATION_NEW_FRAME) {
                final MainFrame newFrame = Main.createInstance(this);
                newFrame.start(database.createSelectForReferencingTable(tableColumn, row, col));
            } else if (getFkNavigationFrameMode() == FK_NAVIGATION_THIS_FRAME) {
                start(database.createSelectForReferencingTable(tableColumn, row, col));
            } else if (getFkNavigationFrameMode() == FK_NAVIGATION_LAST_FRAME) {
                final MainFrame lastFrame = Main.getFrameForFKNavigation(this);
                lastFrame.start(database.createSelectForReferencingTable(tableColumn, row, col));
            }
        } else if (e.getActionCommand().startsWith("sik@")) { 
            // KontextmenuItems für referenzierte Tabellen
            // sik = select imported key
            // Aufbau ActionCommand: sik@row|col
            final int p0 = (e.getActionCommand()).indexOf("|"); 
            final int row = Integer.parseInt(e.getActionCommand().substring(4, p0));
            final int col = Integer.parseInt(e.getActionCommand().substring(p0 + 1, e.getActionCommand().length()));
            if (getFkNavigationFrameMode() == FK_NAVIGATION_NEW_FRAME) {
                final MainFrame newFrame = Main.createInstance(this);
                newFrame.start(database.createSelectForReferencedTable(row, col));
            } else if (getFkNavigationFrameMode() == FK_NAVIGATION_THIS_FRAME) {
                start(database.createSelectForReferencedTable(row, col));
            } else if (getFkNavigationFrameMode() == FK_NAVIGATION_LAST_FRAME) {
                final MainFrame lastFrame = Main.getFrameForFKNavigation(this);
                lastFrame.start(database.createSelectForReferencedTable(row, col));
            }
        } else if ((e.getActionCommand()).startsWith("sai_")) { 
            // Tabelle aufsteigend sortieren
            // das Kommando ist so aufgebaut: "sa_<col>"
            final int col = Integer.parseInt(e.getActionCommand().substring(4, e.getActionCommand().length()));
            final SorterThread sorter = new SorterThread();
            sorter.setColumnIndex(col);
            sorter.setFrame(this);
            sorter.start();
        } else if (e.getActionCommand().startsWith("saea_")) { 
            final int col = Integer.parseInt(e.getActionCommand().substring(5, e.getActionCommand().length()));
            database.sortByNewSelect(col);
        } else if (e.getActionCommand().startsWith("saee_")) { 
            final int col = Integer.parseInt(e.getActionCommand().substring(5, e.getActionCommand().length()));
            setScriptTextAsNewDocument(database.createSQLWithOrderBy(col));
            setTextSaveEnabled(false);
        } else if (e.getActionCommand().equals("toJavaString")) { 
            menuEditConvertSqlToJavaString_actionPerformed();
        } else if (e.getActionCommand().equals("toJavaStringBuffer")) { 
            menuEditConvertSqlToJavaStringBuffer_actionPerformed();
        } else if (e.getActionCommand().equals("toSQL")) { 
            menuEditConvertJavaToSql_actionPerformed();
        } else if (e.getActionCommand().equals("commentParams")) { 
            menuEditCommentParams_actionPerformed();
        } else if (e.getActionCommand().equals("removeParamComments")) { 
            menuEditRemoveParamComments_actionPerformed();
        } else if (e.getActionCommand().equals("removeAllComments")) { 
            menuEditRemoveAllComments_actionPerformed();
        } else if (e.getActionCommand().equals("createguid")) { 
            new TextViewer(this, false, "new GUID", Guid.nextGuid(), true); 
        } else if (e.getActionCommand().equals("base64")) { 
            openBase64Viewer();
        } else if (e.getActionCommand().equals("regex")) { 
            openRegexTester();
        } else if (e.getActionCommand().equals("open_file_in_new_window")) { 
            openCurrentSelectedFileNameInNewWindow();
        } else if (e.getActionCommand().equals("ask_open_file_in_new_window")) { 
            askForOpenCurrentSelectedFileInNewWindows();
        } else if (e.getActionCommand().equals("highlight")) { 
            toggleWordHighlight();
        } else if (e.getActionCommand().equals("toggle_vertical")) { 
            toggleTableViewOrientation();
        } else if (e.getActionCommand().equals("helper_to_date")) { 
            insertToDateCode();
        } else if (e.getActionCommand().equals("helper_to_sum")) { 
        	insertSumCode();
        } else if (e.getActionCommand().equals("helper_to_max")) { 
        	insertMaxCode();
        } else if (e.getActionCommand().equals("export_talend_schema")) {	
        	exportResultTableAsTalendSchema();
        } else {
            logger.warn("unknown action " + e.getActionCommand());
        }
    }

    private void insertToDateCode() {
        insertOrReplaceText("to_date('','dd.MM.yyyy')");
        // move caret to text position for date
        setCaretPos(getCaretPos() - 15);
    }

    private void insertSumCode() {
    	String column = editor.getSelectedText();
    	if (column != null && column.isEmpty() == false) {
            insertOrReplaceText("sum(" + column + ") as " + column);
    	}
    }
    
    private void insertMaxCode() {
    	String column = editor.getSelectedText();
    	if (column != null && column.isEmpty() == false) {
            insertOrReplaceText("max(" + column + ") as " + column);
    	}
    }

    private void returnKeyPressedOnTable() {
        if (resultTable.getSelectedColumn() == 0 && database.isVerticalView()) {
            insertOrReplaceText(database.getColumnNameByLogicalColIndex(database.convertFromVerticalToLogicalColIndex(resultTable.getSelectedRow())));
        } else {
            showEditor(resultTable.getSelectedRow(), resultTable.getSelectedColumn());
        }
    }

    private void openBase64Viewer() {
        Base64Viewer base64Viewer = new Base64Viewer();
        WindowHelper.locateWindowAtMiddle(this, base64Viewer);
    }

    private void openRegexTester() {
        RegexTestFrame testFrame = new RegexTestFrame();
        WindowHelper.locateWindowAtMiddle(this, testFrame);
    }

    private void commit() {
        tbButtonCommit.setEnabled(false);
        tbButtonRollback.setEnabled(false);
        new Thread() {

            @Override
            public void run() {
                final LongRunningAction lra = new LongRunningAction() {

                    public String getName() {
                        return "Commit";
                    }

                    public void cancel() {
                    }

                    public boolean canBeCanceled() {
                        return false;
                    }
                };
                addLongRunningAction(lra);
                try {
                    if ((database.getDatabaseSession()).commit()) {
                        status.message.setText(Messages.getString("MainFrame.114")); 
                    } else {
                        showDBMessageWithoutContinueAction((database.getDatabaseSession()).getLastErrorMessage(), "commit"); 
                    }
                    tbButtonCommit.setEnabled(true);
                    tbButtonRollback.setEnabled(true);
                } finally {
                    removeLongRunningAction(lra);
                }
            }
        }.start();
    }

    private void rollback() {
        tbButtonCommit.setEnabled(false);
        tbButtonRollback.setEnabled(false);
        new Thread() {

            @Override
            public void run() {
                final LongRunningAction lra = new LongRunningAction() {

                    public String getName() {
                        return "Rollback";
                    }

                    public void cancel() {
                    }

                    public boolean canBeCanceled() {
                        return false;
                    }
                };
                addLongRunningAction(lra);
                try {
                    if (database.getDatabaseSession().rollback()) {
                        status.message.setText(Messages.getString("MainFrame.115")); 
                    } else {
                        showDBMessageWithoutContinueAction(database.getDatabaseSession().getLastErrorMessage(), "rollback"); 
                    }
                    tbButtonCommit.setEnabled(true);
                    tbButtonRollback.setEnabled(true);
                } finally {
                    removeLongRunningAction(lra);
                }
            }
        }.start();
    }

    private void showMetaInfoDialog() {
        if (metaDataView == null) {
            metaDataView = new DatabaseMetaDataView(database.getDatabaseMetaData());
            WindowHelper.locateWindowAtMiddle(this, metaDataView);
        } else {
            metaDataView.setDatabaseMetaData(database.getDatabaseMetaData());
        }
        metaDataView.setVisible(true);
        metaDataView.setState(Frame.NORMAL);
        metaDataView.toFront();
    }

    private void toggleWordHighlight() {
        if (tbButtonHighlighter.isSelected()) {
        	findCurrentWord();
            highlightCurrentWord();
        } else {
            lexer.setHighlightedWord(null);
        }
        editor.repaint();
        Main.setUserProperty("HIGHLIGHT_WORD_UNDER_CARET", String.valueOf(tbButtonHighlighter.isSelected())); 
    }

    private void toggleTableViewOrientation() {
        if (database != null) {
            int viewRowIndex = resultTable.getSelectedRow();
            int viewColIndex = resultTable.getSelectedColumn();
            if (logger.isDebugEnabled()) {
                logger.debug("toggleTableViewOrientation() - viewRowIndex=" + viewRowIndex + ", viewColIndex=" + viewColIndex);
            }
            if (database.isVerticalView()) {
                Main.setUserProperty("TABLE_ORIENTATION", "h");
                tbButtonTableOrientation.setSelected(false);
                resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                database.setVerticalView(false);
                // check if a row (in this case a particular cell) is selected and select them in the horizontal view
                if (viewRowIndex != -1 && viewColIndex != -1) {
                    int horizontalViewRowIndex = database.convertFromVerticalToLogicalRowIndex(viewRowIndex);
                    int horizontalViewColIndex = database.convertFromVerticalToLogicalColIndex(viewRowIndex);
                    selectTableCell(horizontalViewRowIndex, horizontalViewColIndex);
                }
            } else {
                Main.setUserProperty("TABLE_ORIENTATION", "v");
                tbButtonTableOrientation.setSelected(true);
                resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
                database.setVerticalView(true);
                if (viewRowIndex != -1 && viewColIndex != -1) {
                    selectTableCell(viewRowIndex, viewColIndex);
                }
            }
        }
    }

    public void selectTableCell(int logicalRow, int logicalCol) {
        if (database != null) {
            if (database.isVerticalView()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("selectTableCell(logicalRow=" + logicalRow + ", logicalCol=" + logicalCol + ") in verticalMode");
                }
                int verticalViewRowIndex = database.convertFromLogicalToVerticalRowSelection(logicalRow) + logicalCol;
                resultTable.changeSelection(verticalViewRowIndex, 1, false, false);
                scrollTableToCell(verticalViewRowIndex);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("selectTableCell(logicalRow=" + logicalRow + ", logicalCol=" + logicalCol + ") in horizontalMode");
                }
                resultTable.changeSelection(logicalRow, logicalCol, false, false);
                scrollTableToCell(logicalRow);
            }
        }
    }

    private void openCurrentSelectedFileNameInNewWindow() {
        final MainFrame newFrame = Main.createInstance(this);
        String selectedText = editor.getSelectedText();
        if (selectedText != null) {
        	selectedText = selectedText.trim();
        	if (selectedText.toLowerCase().endsWith(".sql") == false) {
        		selectedText = selectedText + ".sql";
        	}
        }
        String path = getCurrentDirectory();
        String filePathAndName = selectedText; 
        if (path != null) {
            filePathAndName = path + Main.FILE_SEPARATOR + filePathAndName;
        }
        newFrame.loadFileInDocument(false, filePathAndName);
    }

    public String getCurrentDirectory() {
        if (currentFile != null) {
            return currentFile.getParent();
        } else {
            return Main.getUserProperty("SCRIPT_DIR", System.getProperty("user.home"));  
        }
    }

    private void askForOpenCurrentSelectedFileInNewWindows() {
        final MainFrame newFrame = Main.createInstance(this);
        String selectedText = editor.getSelectedText();
        if (selectedText != null) {
        	selectedText = selectedText.trim();
        	if (selectedText.toLowerCase().endsWith(".sql") == false) {
        		selectedText = selectedText + ".sql";
        	}
        }
        String path = null;
        if (currentFile != null) {
            path = currentFile.getParent();
        }
        final String fileName = selectedText; 
        newFrame.openFileDialog(path, fileName);
    }

    private final class SorterThread extends Thread {

        private int col;
        private JFrame frame;

        public void setColumnIndex(int col_loc) {
            this.col = col_loc;
        }

        public void setFrame(JFrame frame_loc) {
            this.frame = frame_loc;
        }

        @Override
        public void run() {
            setDatabaseBusyFiltered(true, Messages.getString("MainFrame.116") + database.getColumnName(col) + " ...");   
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            database.sortVector(col);
            setDatabaseBusyFiltered(false, Messages.getString("MainFrame.117")); 
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void togglePk() {
        int columnIndex;
        if (database.isVerticalView()) {
            columnIndex = database.convertFromVerticalToLogicalColIndex(resultTable.getSelectedRow());
        } else {
            columnIndex = resultTable.getSelectedColumn();
        }
        togglePk(columnIndex);
    }

    private void togglePk(int logicalColumnIndex) {
        if (database.getColumnTypename(logicalColumnIndex).equals("CLOB") 
            || database.getColumnTypename(logicalColumnIndex).equals("BLOB") 
            || (database.getColumnTypename(logicalColumnIndex).indexOf("UNKNOWN") != -1) 
            || database.getColumnTypename(logicalColumnIndex).equals("LONGVARCHAR") 
            || database.getColumnTypename(logicalColumnIndex).equals("LONGVARBINARY")) { 
            showErrorMessage(Messages.getString("MainFrame.118"), Messages.getString("MainFrame.119"));  
        } else {
            database.togglePrimaryKey(logicalColumnIndex);
            if (database.isVerticalView()) {
                resultTable.repaint();
            } else {
                resultTable.getTableHeader().repaint();
            }
        }
    }

    /**
     * stellt sicher, dass die angegebene Zelle in der Ergebnistabelle sichtbar ist. 
     * @param row Zeile
     * @param col Spalte
     */
    public void scrollTableToCell(int row) {
        final int y = row * resultTable.getRowHeight();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tableScrollPane.getVerticalScrollBar().setValue(y);
            }
        }); // muss verzögert ausgeführt werden !
    }

    public void loadFileInDocument(boolean askForSave, String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("file cannot be null"); 
        }
        File f = new File(filePath);
        MainFrame mf = Main.findMainFrameByFile(f);
        if (mf != null && mf != this) {
            int answer = JOptionPane.showConfirmDialog(this, Messages.getString("MainFrame.fileAlreadyLoaded"), Messages.getString("MainFrame.loadFile"), JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                mf.toFront();
                return;
            }
        }
        int answer;
        if (askForSave && isTextChanged()) {
            answer = JOptionPane.showConfirmDialog(
                this,
                Messages.getString("MainFrame.120"), 
                Messages.getString("MainFrame.121"), 
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            // gebe mir die Antwort selbst wenn sich im Text nichts geändert hat
            answer = JOptionPane.NO_OPTION;
        // so nun wird nicht unnötig nach gespeichert
        }
        switch (answer) {
            case JOptionPane.YES_OPTION: {
                // vorher sichern
                if (currentFile == null) {
                    saveAsScriptFile(); // kein Dateiname bekannt, also Auswahl erforderlich
                } else {
                    saveScriptFile(); // speichern in bekannte Datei
                }
            // hier weiter machen !
            }
            case JOptionPane.NO_OPTION: {
                // neue Datei oeffnen und alten Inhalt verwerfen
                // Document erstellen noch ohne die Undo-Manager und Listeners
                editor.setDocument(editor.getEditorKit().createDefaultDocument());
                currentFile = null;
                if (filePath.startsWith("file:/")) { 
                    try {
                        filePath = filePath.trim();
                        URI uri = URI.create(filePath);
                        currentFile = new File(uri);
                    } catch (Exception e) {
                        // kann ignoriert werden, dann wird
                        // filename direkt als Dateiname interpretiert
                        logger.warn("LoadFileInDocument (create URI=" + filePath + ") failed: " + e.getMessage(), e);  
                        showErrorMessage("create file URI " + filePath + " failed " + e.getMessage(), "load file");   
                        return;
                    }
                } else {
                    currentFile = new File(filePath);
                }
                setupWindowTitle();
                if (logger.isDebugEnabled()) {
                    logger.debug("MainFrame.loadFileToDocument: start loader with " + currentFile.getPath()); 
                }
                Main.setUserProperty("SCRIPT_DIR", f.getParentFile().toString()); 
                currentFileLastModified = currentFile.lastModified();
                currentFileLoaded = System.currentTimeMillis();
                final Thread loader = new FileLoader(currentFile, editor);
                loader.start(); // Aktion asynchron starten
                moveSplitPaneDividerToButtom();
                getRootPane().putClientProperty( "Window.documentFile", currentFile );
                break;
            }
        }
    }

    private boolean saveAsScriptFile() {
        boolean ok = false;
        try {
            final JFileChooser chooser = new JFileChooser();
            if (currentFile != null) {
                if (currentFile.getParentFile() != null) {
                    chooser.setCurrentDirectory(currentFile.getParentFile());
                    chooser.setSelectedFile(currentFile);
                }
            } else {
                final String directory = Main.getUserProperty("SCRIPT_DIR", System.getProperty("user.home"));  
                chooser.setCurrentDirectory(new File(directory));
            }
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            chooser.setDialogTitle(Messages.getString("MainFrame.122")); 
            chooser.addChoosableFileFilter(sqlFileFilter);
            final int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if ((chooser.getFileFilter() == sqlFileFilter) && (f.getName().toLowerCase().endsWith(".sql") == false)) { 
                    f = new File(f.getAbsolutePath() + ".sql"); 
                }
                Main.setUserProperty("SCRIPT_DIR", f.getParentFile().toString()); 
                currentFile = f;
                ok = saveScriptFile();
            }
        } catch (java.security.AccessControlException ae) {
            status.message.setText(Messages.getString("MainFrame.123")); 
            currentFile = null;
        }
        return ok;
    }

    private boolean saveScriptFile() {
        boolean ok = false;
        long lastModifiedOfStoredFile = Main.getLastModified(currentFile);
        if (lastModifiedOfStoredFile > 0 && lastModifiedOfStoredFile > currentFileLoaded && currentFileLoaded > 0) {
            // show hint that file on harddisk is newer than file in editor
            int answer = JOptionPane.showConfirmDialog(this, Messages.getString("MainFrame.fileisnewer"), Messages.getString("MainFrame.122"), JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.NO_OPTION) {
                return false;
            }
        }
        int caretPos = editor.getCaretPosition();
        editor.select(caretPos, caretPos);
        editor.setEditable(false);
        final Document doc = editor.getDocument();
        try {
            undoManager.discardAllEdits();
            updateUndoRedoEnabled();
            setupWindowTitle();
            // testen ob es die datei schon gibt
            File bak = null;
            // BAK-Files nur anlegen wenn erlaubt
            if (currentFile.exists() && Main.getDefaultProperty("CREATE_BACKUP_FILES", "false").equals("true")) {   
                // Bak-Datei anlegen
                // das umbennen funktioniert nur wenn der
                // Zielname noch nicht als Datei existiert !!
                bak = new File(currentFile.getPath() + ".bak"); 
                // testen ob Bak-datei schon existiert, dann löschen
                if (bak.exists()) {
                    if (bak.delete()) {
                        if (currentFile.renameTo(bak)) {
                        	logger.error("couldn't rename to bak");
                        }
                    } else {
                        // Fehlerinfo-Dialog hochbringen
                        if (logger.isDebugEnabled()) {
                            logger.debug("MainFrame.saveScriptFile: delete old backup " 
                                + bak + Messages.getString("MainFrame.126")); 
                        }
                        JOptionPane.showConfirmDialog(
                            this,
                            Messages.getString("MainFrame.127") 
                            + bak + Messages.getString("MainFrame.128") 
                            + Messages.getString("MainFrame.129"), 
                            Messages.getString("MainFrame.130"), 
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                        bak = null;
                    }
                } else {
                    if (!currentFile.renameTo(bak)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("saveScriptFile: error while renaming original file:" 
                                + currentFile.getPath() + "saveScriptFile:" 
                                + bak.getPath());
                        }
                        JOptionPane.showConfirmDialog(
                            this,
                            Messages.getString("MainFrame.133") + bak + Messages.getString("MainFrame.134"),  
                            Messages.getString("MainFrame.135"), 
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                        bak = null;
                    }
                }
            } else {
                bak = currentFile; //Dummy-Zuweisung um unkorrekte Fehlermeldung zu vermeiden
            }
            if (Main.currentCharSet != null) {
                bwFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile), Main.currentCharSet));
            } else {
                bwFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile)));
            }
            // speichert Inhalt von TextComponente in Datei
            if (logger.isDebugEnabled()) {
                logger.debug("save file: " + currentFile.getPath()); 
            }
            // den internen Standard-Zeilenumbruch durch den konfigurierten Zeilenumbruch ersetzen 
            String text = doc.getText(0, doc.getLength());
            // preventiv wipe out windows line breaks
            final StringBuffer tb = new StringBuffer(text.replace("\r", ""));
            text = null;
            int pos = 0;
            while (true) {
                pos = tb.indexOf("\n", pos);
                if (pos != -1) {
                    tb.replace(pos, pos + 1, Main.currentLineSeparator);
                    pos = pos + Main.currentLineSeparator.length();
                } else {
                    break;
                }
            }
            bwFile.write(tb.toString());
            bwFile.close();
            currentFileLastModified = System.currentTimeMillis();
            currentFileLoaded = currentFileLastModified;
            bwFile = null;
            ok = true;
            // wenn bak == null dann ist beim Erstellen der
            // bak-Datei etwas daneben gegangen !!
            // wenn die bak-Datei garnicht erstellt werden musst
            // dann hat bak den Wert von currentFile
            if (bak == null) {
                status.message.setText(Messages.getString("MainFrame.136") 
                    + currentFile.getName() + Messages.getString("MainFrame.137") 
                    + Messages.getString("MainFrame.138")); 
            } else {
                if (Main.getDefaultProperty("CREATE_BACKUP_FILES", "false").equals("true")) {   
                    status.message.setText(Messages.getString("MainFrame.139") 
                        + currentFile.getName() + Messages.getString("MainFrame.140") 
                        + Messages.getString("MainFrame.141")); 
                } else {
                    status.message.setText(Messages.getString("MainFrame.142") 
                        + currentFile.getName() + Messages.getString("MainFrame.143") 
                        + Messages.getString("MainFrame.144")); 
                }
            }
            // nur nicht wenn Konfig-Dateien geladen wurde !!
            if (currentFile.getName().endsWith(".cfg")) { 
                showInfoMessage(Messages.getString("MainFrame.145"), Messages.getString("MainFrame.146"));  
            } else {
                // Properties ergänzen, dort werden auch die Menues aktualisiert.
                Main.addFileProp(currentFile);
            }
            // Button ausschalten, da sich noch nichts geändert hat
            setTextSaveEnabled(false);
        //textChanged=false;
        //tbButtonSave.setEnabled(false);
        } catch (BadLocationException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("saveScriptFile: " + e.toString()); 
            }
            status.message.setText(Messages.getString("MainFrame.147") + currentFile.getPath()); 
        } catch (IOException e) {
            logger.warn("saveScriptFile: " + e.toString()); 
            status.message.setText(Messages.getString("MainFrame.148") + currentFile.getPath()); 
        } catch (java.security.AccessControlException ae) {
            logger.warn("saveScriptFile: " + ae.getMessage()); 
            status.message.setText(Messages.getString("MainFrame.149")); 
        }
        editor.setEditable(true);
        return ok;
    }

    private boolean menuScriptNew_actionPerformed() {
        // altes Document speichern
        // Abfrage speichern ? ja/nein
        // Abfrage nötig ?
        int answer;
        boolean newDocCreated = false;
        if (isTextChanged()) {
            answer = JOptionPane.showConfirmDialog(
                this,
                Messages.getString("MainFrame.150"), 
                Messages.getString("MainFrame.151"), 
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            // gebe mir die Antwort selbst wenn sich im Text nichts geändert hat
            answer = JOptionPane.NO_OPTION;
        // so nun wird nicht unnötig gespeichert
        }
        switch (answer) {
            case JOptionPane.YES_OPTION: {
                // vorher sichern
                if (currentFile == null) {
                    saveAsScriptFile();
                } else {
                    saveScriptFile();
                }
            // keine break !! hier weiter machen !!
            }
            case JOptionPane.NO_OPTION: {
                // gleich alles neu
                setupNewDocumentWithListenersAndUndoRedo();
                setupWindowTitle();
                status.message.setText(Messages.getString("MainFrame.152")); 
                newDocCreated = true;
                currentFileLoaded = 0;
                break;
            }
            default:
                newDocCreated = false;

        }
        return newDocCreated;
    }

    private void menuScriptOpen_actionPerformed() {
        //altes Document speichern
        // Dialog speichern ? ja/nein
        // Abfrage nötig ?
        int answer;
        if (isTextChanged()) {
            answer = JOptionPane.showConfirmDialog(
                this,
                Messages.getString("MainFrame.153"), 
                Messages.getString("MainFrame.154"), 
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            //gebe mir die Antwort selbst wenn sich im Text nichts geändert hat
            answer = JOptionPane.NO_OPTION;
        // so nun wird nicht unnötig nach gespeichert
        }
        if (currentFile != null) {
            switch (answer) {
                case JOptionPane.YES_OPTION: {
                    // vorher sichern
                    saveScriptFile(); // speichern in bekannte Datei
                    break;
                }
                case JOptionPane.NO_OPTION: {
                    setTextSaveEnabled(false);
                    break;
                }
            }
        } // if (currentFile != null)
        if (answer != JOptionPane.CANCEL_OPTION && answer != JOptionPane.CLOSED_OPTION) {
            // neue Datei öffnen
            if ((currentFile != null) && (currentFile.getPath() != null)) {
                openFileDialog(currentFile.getPath(), currentFile.getName());
            } else {
                openFileDialog(null, null);
            }
        } // if (answer != JOptionPane.CANCEL_OPTION)
    }

    public void openFileDialog(String directoryName, String selectFileName) {
        final JFileChooser chooser = new JFileChooser();
        if (directoryName != null) {
            chooser.setCurrentDirectory(new File(directoryName));
        } else {
            final String directory = Main.getUserProperty("SCRIPT_DIR", System.getProperty("user.home"));  
            chooser.setCurrentDirectory(new File(directory));
        }
        if (selectFileName != null) {
            chooser.setSelectedFile(new File(selectFileName));
        }
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(Messages.getString("MainFrame.155")); 
        chooser.addChoosableFileFilter(sqlFileFilter);
        final int returnVal = chooser.showOpenDialog(this);
        // hier weiter wenn der modale FileDialog geschlossen wurde
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File f = chooser.getSelectedFile();
            loadFileInDocument(false, f.getAbsolutePath());
        }
    }

    private void menuScriptSave_actionPerformed() {
        // altes Document speichern
        // Dialog speichern ? ja/nein
        if (currentFile != null) {
            saveScriptFile();
        } else {
            saveAsScriptFile();
        }
    }

    private void menuScriptSaveas_actionPerformed() {
        saveAsScriptFile();
    }

    private void menuScriptInfo_actionPerformed() {
        new FileInfo(this);
    }

    /**
     * gibt an ob die Verbiundung zur Datenbank momentan beutzbar ist
     * @return true wenn connected + frei
     */
    public boolean isConnectedAndReady() {
        boolean isReady = false;
        if ((database != null) && database.getDatabaseSession().isConnected() && (isDatabaseBusy() == false)) {
            isReady = true;
        }
        return isReady;
    }

    /**
     * close this window with question if neccessary
     * @return return true if window can be closed
     */
    public boolean close() {
        boolean exitEnabled = false;
        int answer;
        // ask if close disabled
        if (isCloseActionDisabled()) {
            answer = JOptionPane.showConfirmDialog(
                this,
                Messages.getString("MainFrame.wantAbort"), 
                Messages.getString("MainFrame.close"), 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            answer = JOptionPane.YES_OPTION;
        }
        switch (answer) {
            case JOptionPane.NO_OPTION:
                // don't abort
                break;
            case JOptionPane.YES_OPTION: {
                // abort action is allowed
                // ask if currentFile is changed
                if (isTextChanged()) {
                    // text is changed, ask for saving
                    final Object[] options = {
                        Messages.getString("MainFrame.yes"), 
                        Messages.getString("MainFrame.no"), 
                        Messages.getString("MainFrame.none"), 
                        Messages.getString("MainFrame.cancel")}; 
                    answer = JOptionPane.showOptionDialog(
                        this,
                        Messages.getString("MainFrame.savebefore"), 
                        Messages.getString("MainFrame.close"), 
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[1]);
                } else {
                    // text is not changed
                    answer = JOptionPane.NO_OPTION;
                }
                // answer is array index of options
                switch (answer) {
                    case 0: { // array index of "yes"
                        // want save file
                        if (saveAsScriptFile() == false) {
                            break;
                        }
                    // successfully saved
                    }
                    case 1: // array index of "no"
                    case 2: { // array index of "none"
                        // want not save file
                        exitEnabled = true;
                        closeWithoutQuestion();
                        break;
                    }
                    case 3: // array index of "cancel"
                        break;
                } // switch (answer)
                break;
            }
        } // switch (answer)
        return exitEnabled;
    }

    /**
     * schliesst dieses Fenster ohne jede Rückfrage
     */
    public void closeWithoutQuestion() {
        if ((database != null) && database.getDatabaseSession().isConnected()) {
            if (dmFrame != null) {
                dmFrame.removeSQLDataModel(database.getSQLDataModel());
            }
            final Database oldDatebase = database;
            database = null;
            Thread closeThread = new Thread() {

                @Override
                public void run() {
                    oldDatebase.close();
                }
            };
            closeThread.start();
        }
        //letzte Fensterposition merken für nächsten Start
        // diese Instanz killen
        if (atc != null) {
            atc.dispose();
        }
        if (exportResultTableDialog != null) {
            exportResultTableDialog.dispose();
        }
        if (metaDataView != null) {
            metaDataView.dispose();
        }
        if (dbLogin != null) {
            dbLogin.dispose();
        }
        if (preferencesDialog != null) {
            preferencesDialog.dispose();
        }
        if (dlgSeRe != null) {
            dlgSeRe.dispose();
        }
        if (tableSearchDialog != null) {
            tableSearchDialog.dispose();
        }
        if (dlgGo != null) {
            dlgGo.dispose();
        }
        if (dlgAbout != null) {
            dlgAbout.dispose();
        }
        Main.close(this);
    }

    private void menuDBAdmin_actionPerformed() {
        if (atc == null) {
            atc = new AdminToolChooser(this);
        }
        atc.setVisible(true);
        atc.setState(Frame.NORMAL);
        atc.toFront();
        atc.fillList();
    }

    private void menuDBExportResultTable_actionPerformed() {
        if (database != null && database.size() > 0) {
            if (exportResultTableDialog == null) {
                exportResultTableDialog = new ResultTableExportDialog(this);
            }
            exportResultTableDialog.setVisible(true);
            WindowHelper.locateWindowAtMiddle(this, exportResultTableDialog);
        } else {
            showInfoMessage(Messages.getString("MainFrame.164"), Messages.getString("MainFrame.165"));  
        }
    }

    private void menuDBExportDBTable_actionPerformed() {
        QueryExportFrame frame = new QueryExportFrame(this);
        frame.setVisible(true);
        WindowHelper.locateWindowAtMiddle(this, frame);
    }

    private void openCSVImportDialog(File file) {
        ImportConfiguratorFrame configurator = new ImportConfiguratorFrame(database);
        configurator.setVisible(true);
        WindowHelper.locateWindowAtMiddle(this, configurator);
        if (file != null) {
            configurator.handleFile(file);
        }
    }

    private void menuDBXmlExport_actionPerformed() {
        new ExporterFrame(database.getDatabaseSession().getConnectionDescription(), this);
    }

    private void menuDBXmlImport_actionPerformed() {
        new ImporterFrame(this);
    }

    private void menuDBSearchInTable_actionPerformed() {
        if (tableSearchDialog == null) {
            tableSearchDialog = new TableSearchDialog(this);
        }
        WindowHelper.locateWindowAtMiddle(this, tableSearchDialog);
        tableSearchDialog.setVisible(true);
        tableSearchDialog.setState(Frame.NORMAL);
        tableSearchDialog.toFront();
    }

    private ConnectThread connectThread;
    
    /**
     * verbindet dieses Fenster mit der Datenbank
     * @param cd Parameter-Objekt für das Ertstellen der DB Session
     */
    public void startConnect(final ConnectionDescription cd) {
        status.message.setText(Messages.getString("MainFrame.166")); 
        database = new Database(resultTable, this);
        status.setTablename(null);
        database.setConnectionDescription(cd);
        database.setVerticalView(tbButtonTableOrientation.isSelected());
        setProductive(cd.isProductive());
        setGuiToConnected(false);
        int timeout;
        try {
            timeout = Integer.parseInt(Main.getDefaultProperty("CONNECT_TIMEOUT", "5000"));  
        } catch (NumberFormatException nfe) {
            logger.warn("MainFrame.connect: ERROR by parsing default CONNECT_TIMEOUT-value:" 
                + Main.getDefaultProperty("CONNECT_TIMEOUT")); 
            timeout = 5000;
        }
        DatabaseSession.setTimeout(timeout);
        stopConnecting();
        connectThread = new ConnectThread();
        connectThread.start();
    }

    private Action stopConnectAction = new AbstractAction(Messages.getString("MainFrame.stopConnecting")) {
		
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			stopConnecting();
		}
	};
    
    @SuppressWarnings("deprecation")
	private void stopConnecting() {
        if (connectThread != null && connectThread.isAlive()) {
        	setStatusMessage("Cancel connecting...");
        	connectThread.interrupt();
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
        	if (connectThread.isAlive()) {
        		connectThread.stop();
        	}
        	setGuiToConnected(false);
        	setStatusMessage("Connecting cancelled");
        }
    }
    
    private void menuDBOpen_actionPerformed() {
        if (Main.startUpCd != null) {
            startConnect(Main.startUpCd);
        } else {
            openLoginDialog(null);
        }
    }

    public void openLoginDialog(ConnectionDescription initialCd) {
        if (dbLogin == null) {
            dbLogin = new DBLoginDialog(this, Messages.getString("MainFrame.167")); 
        }
        if (initialCd != null) {
            dbLogin.setInitialConnectionDescription(initialCd);
        }
        dbLogin.setVisible(true);
        editor.requestFocus();
        if (dbLogin.getReturnAction() == DBLoginDialog.LOGIN_ACTION_PERFORMED) {
            // Datenbank erzeugen, der resultTable wird in database das TableModel zugewiesen
            startConnect(dbLogin.getConnectionDescription());
        }
    }

    private void setConnectInProgress() {
    	menuDBAbortConnecting.setEnabled(true);
        menuDBOpen.setEnabled(false);
        menuDBReconnect.setEnabled(false);
        menuDBClose.setEnabled(false);
        tbButtonDbOpen.setEnabled(false);
        tbButtonDbClose.setEnabled(false);
    }
    
    private void updateExplainState(boolean sqlEnabled) {
    	if (sqlEnabled && database != null && database.isConnected()) {
    		boolean canExplain = database.getDatabaseExtension().hasExplainFeature();
    		tbButtonExplain.setEnabled(canExplain);
    		menuDBExplain.setEnabled(canExplain);
    	} else {
    		tbButtonExplain.setEnabled(false);
    		menuDBExplain.setEnabled(false);
    	}
    }

    /**
     * fragt ob mit Datenbank verbunden
     * @return true dann verbunden
     */
    public boolean isConnected() {
        boolean ok = false;
        if (database != null) {
            if (database.getDatabaseSession() != null) {
                if (database.getDatabaseSession().isConnected()) {
                    ok = true;
                }
            }
        }
        return ok;
    }
    
    private void doConnect() {
        disposeAllViewer();
        if (database.loadDriver()) {
            setConnectInProgress();
            if (database.connect()) {
                DataModelFrame.getDataModelFrame().addSQLDatamodel(database.getSQLDataModel());
                if (database.getDatabaseSession().setAutoCommit(menuDBAutoCommitCheckBox.isSelected()) == false) {
                    showInfoMessage(database.getDatabaseSession().getLastErrorMessage(), "auto-commit"); 
                    if (menuDBAutoCommitCheckBox.isSelected()) {
                        menuDBAutoCommitCheckBox.setSelected(false);
                    } else {
                        menuDBAutoCommitCheckBox.setSelected(true);
                    }
                }
                setStatusMessage(Messages.getString("MainFrame.168")); 
                status.infoAction.setText("CONN"); 
                setGuiToConnected(true);
                
                setTableSearchEnabled(true);
                if (atc != null) {
                    atc.fillList();
                }
            } else {
                showDBMessageWithoutContinueAction(
                    database.getDatabaseSession().getLastErrorMessage(),
                    Messages.getString("MainFrame.169")); 
                status.message.setText(Messages.getString("MainFrame.170")); 
                setGuiToConnected(false);
            }
        } else {
            showDBMessageWithoutContinueAction(
                database.getDatabaseSession().getLastErrorMessage(),
                Messages.getString("MainFrame.171")); 
            status.message.setText(Messages.getString("MainFrame.172")); 
        }
        setupWindowTitle();
        if (dmFrame != null && dmFrame.isVisible() && dmFrame.getModelCount() == 1) {
        	dmFrame.setMainFrame(this);
        	dmFrame.selectCurrentSchema();
        }
    }

    private void startRunTimer() {
    	runTimerStartTime = System.currentTimeMillis();
		runTimer = new Timer(500, new ActionListener() {
			
			private NumberFormatter nf = new NumberFormatter();
			
			@Override
			public void actionPerformed(ActionEvent e) {
				long seconds = (System.currentTimeMillis() - runTimerStartTime) / 1000;
				try {
					status.tablePos.setText(nf.valueToString(seconds) + "s");
				} catch (ParseException e1) {
					logger.error(e1);
				}
			}
		});
		runTimer.start();
    }
    
    private void stopRunTimer() {
    	if (runTimer != null) {
    		runTimer.stop();
    	}
    }
    
    private final class ConnectThread extends Thread {

        @Override
        public void run() {
            doConnect();
            // kurze Pause, damit sich die GUI aufbauen kann.
            try {
                sleep(500);
            } catch (InterruptedException ie) {
                // dieser Thread wird nicht unterbrochen
            }
            if (startRunOnLoad) {
                startRunOnLoad = false;
                if (isConnected() && menuDBRun.isEnabled()) {
                    runScript();
                }
            }
        }
    }

    /**
     * startet ein SQLStatement welches als Text übergeben wird
     */
    public void start(String sql) {
        start(sql, false);
    }

    /**
     * startet in diesem Frame ein neues Script
     * @param sql Script
     * @param hideText true=den Text nicht im Editor anzeigen
     */
    public void start(String sql, boolean hideText) {
        if (hideText) {
            if (database != null) {
                database.executeStatement(new SQLStatement(sql));
            }
        } else {
            setScriptTextAsNewDocument(sql);
            setTextSaveEnabled(false);
            if (isConnected() && menuDBRun.isEnabled()) {
                runScript();
            } else {
                startRunOnLoad = true; // diese Flag wird im Thread Connector ausgewertet
            }
        }
    }

    public void cancelStatement() {
        setDatabaseBusyFiltered(true, "Aborting...");
        tbButtonStop.setEnabled(false);
        final Thread cancelThread = new Thread(new Runnable() {

            public void run() {
                if (executerThread != null) {
                	executerThread.interrupt();
                }
                setDatabaseBusyFiltered(false, "Aborted");
            }
        });
        cancelThread.setDaemon(true);
        cancelThread.start();
    }

    /**
     * kennzeichnet diesen Frame als Ziel eine foreign-key-navigation
     * @param useForFKNavigation - true bedeutet, dass in diesem Frame
     *                             Referenzen angezeigt werden.
     */
    public void setUseForFKNavigation(boolean useForFKNavigation) {
        this.usedForFKNavigation = useForFKNavigation;
    }

    /**
     * kennzeichnet diesen Frame als Ziel für FK-Navigation 
     * @return true wenn als Zielframe für FK-Navigation gestartet
     */
    public boolean isUsedForFKNavigation() {
        return usedForFKNavigation;
    }

    /**
     * erlaubt/verbietet das Durchsuchen der Ergebnistabelle
     * @param enabled
     */
    public void setTableSearchEnabled(boolean enabled) {
        this.menuDBSearchInTable.setEnabled(enabled);
        this.tbButtonTableSearch.setEnabled(enabled);
    }

    private void menuEditParse_actionPerformed() {
        final SQLParser parser = new SQLParser(getText(), menuDBDisableParserCheckBox.isSelected());
        for (int i = 0; i < parser.getStatements().size(); i++) {
            sqlHistory.addBatch((SQLStatement) parser.getStatements().get(i), false);
        }
        sqlHistory.setFreezed(true);
    }
    
    private void runExplain() {
    	String text = editor.getSelectedText();
        if ((text == null) || (text.trim().length() < 2)) {
            // kein Text markiert, dann den ganzen text nehmen
            text = getText();
        }
        if (text.trim().length() > 1) {
            runScript(database.getDatabaseExtension().getExplainSQL(text), true, 0, true);
        } else {
        	logger.warn("No text found to parse and explain");
        }
    }
    
    private void runScript() {
    	String text = editor.getSelectedText();
        int textOffset = 0;
        if ((text == null) || (text.trim().length() < 2)) {
            // kein Text markiert, dann den ganzen text nehmen
            text = getText();
        } else {
        	textOffset = editor.getSelectionStart();
        }
    	runScript(text, false, textOffset, false);
    }

    private Thread executerThread;
    
    private void runScript(final String text, final boolean noMetaData, final int textOffset, boolean explain) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("runScript text=" + text);
    	}
        if (text.trim().length() > 1) {
            // kompletten Text dem Parser übergeben
        	executerThread = new Thread() {
        		@Override
        		public void run() {
                    final SQLParser parser = new SQLParser();
                    
                    parser.disableParser(menuDBDisableParserCheckBox.isSelected());
                    
                    setDatabaseBusy(true, "Parse script...");
                    parser.parseScript(text, textOffset);
                    setDatabaseBusy(true, "Configure script...");
                    if (parser.getStatements().size() == 0) {
                    	setDatabaseBusy(false, "Execute script ends not successful: No statements found");
                    	return;
                    }
                    boolean setupSuccessful = false;
                    for (int i = 0; i < parser.getStatements().size(); i++) {
                    	SQLStatement stat = parser.getStatements().get(i);
                        if (stat.isPrepared()) {
                            // Editor starten
                            final SQLPSParamConfigurer configurer = new SQLPSParamConfigurer(MainFrame.this, stat);
                            WindowHelper.checkAndCorrectWindowBounds(configurer);
                            configurer.setVisible(true);
                            // hier geht es erst weiter, wenn der Dialog geschlossen wurde
                            setupSuccessful = configurer.getReturnCode() == SQLPSParamConfigurer.OK;
                            if (setupSuccessful == false) {
                            	break;
                            }
                        } else {
                            setupSuccessful = true;
                        }
                    }
                    if (setupSuccessful) {
                        setDatabaseBusy(true, "Execute script...");
                        startRunTimer();
                        try {
                        	int preferredWidth = tableScrollPane.getWidth() - 10;
                        	database.setPreferredColumnWidthSum(preferredWidth);
                            database.executeScript(parser, noMetaData);
                        } finally {
                        	stopRunTimer();
                        }
                    } else {
                        setDatabaseBusy(false, "Execute script ends not successful");
                    }
        		}
        		
        		@Override
        		public void interrupt() {
        			super.interrupt();
        			database.cancel();
        		}
        		
        	};
        	executerThread.start();
        } else {
        	logger.warn("No text found to parse and run");
        }
    }

    /**
     * gibt den markierten Text aus
     * @return Text
     */
    public String getSelectedText() {
        return editor.getSelectedText();
    }

    private void menuDBHistory_actionPerformed() {
        WindowHelper.locateWindowAtRightSideWithin(this, sqlHistory, 0);
        sqlHistory.setVisible(true);
        sqlHistory.setState(Frame.NORMAL);
        sqlHistory.toFront();
    }

    protected void startDisconnect() {
    	try {
        	if (database != null) {
                // Dummy-Model für die Tabelle setzen, da Objekte in database nach close nicht mehr verfügbar !
                // resultTable.setModel(new DefaultTableModel());
                if (dmFrame != null) {
                	try {
                		dmFrame.removeSQLDataModel(database.getSQLDataModel());
                	} catch (Throwable t) {
                		logger.error("removeSQLDataModel failed: " + t.getMessage(), t);
                	}
                }
                final Database oldDatabase = database;
                Thread closeThread = new Thread() {

                    @Override
                    public void run() {
                        if (oldDatabase.close()) {
                            status.message.setText(Messages.getString("MainFrame.173")); 
                            status.infoAction.setText("DISC"); 
                        } else {
                            showDBMessage(database.getDatabaseSession().getLastErrorMessage(), Messages.getString("MainFrame.174")); 
                            status.message.setText(Messages.getString("MainFrame.175")); 
                            status.infoAction.setText("DISC"); 
                        }
                    }
                };
                closeThread.start();
                if (atc != null) {
                    atc.clearList();
                }
            }
    	} finally {
            setGuiToConnected(false);
            setupWindowTitle();
    	}
    }

    protected void startReconnect() {
        if (database != null) {
            // Dummy-Model für die Tabelle setzen, da Objekte in database nach close nicht mehr verfügbar !
            // resultTable.setModel(new DefaultTableModel());
            setGuiToConnected(false);
            final Database db = database;
            Thread thread = new Thread() {

                @Override
                public void run() {
                    db.close();
                    if (db.connect()) {
                    	SwingUtilities.invokeLater(new Runnable() {
                    		@Override
                    		public void run() {
                                setGuiToConnected(true);
                    		}
                    		
                    	});
                    }
                }
            };
            thread.start();
        }
        setupWindowTitle();
    }

    private void menuDBDatamodel_actionPerformed() {
        dmFrame.setVisible(true);
        dmFrame.setState(Frame.NORMAL);
        dmFrame.toFront();
        Rectangle r = WindowHelper.getCurrentScreenBounds(this);
        WindowHelper.locateAtRightSideOfScreen(r, dmFrame);
        dmFrame.setMainFrame(this);
        dmFrame.selectCurrentSchema();
    }
    
    public ConnectionDescription getCurrentConnectionDescription() {
    	if (database != null && database.getDatabaseSession() != null) {
    		return database.getDatabaseSession().getConnectionDescription();
    	} else {
    		return null;
    	}
    }
    
    public SQLDataModel getCurrentDataModel() {
    	if (database != null) {
    		return database.getDataModel();
    	} else {
    		return null;
    	}
    }

    private void menuEditUndo_actionPerformed() {
        // undo-Funktion des UndoRedo-Managers
        undoManager.undo();
        updateUndoRedoEnabled();
    }

    private void menuEditRedo_actionPerformed() {
        // redo-Funktion des UndoRedo-Managers
        undoManager.redo();
        updateUndoRedoEnabled();
    }

    private void menuEditCut_actionPerformed() {
        // cut-Funktion von TextPane
        editor.cut();
    }

    private void menuEditCopy_actionPerformed() {
        // copy-Funktion von TextPane
        editor.copy();
    }

    private void menuEditConvertSqlToJavaString_actionPerformed() {
        final String javaCode = SQLCodeGenerator.convertSqlToJavaString(getText());
        if (textViewer == null) {
            textViewer = new TextViewer(this, Messages.getString("MainFrame.176"), javaCode, true); 
        } else {
            textViewer.setText(javaCode);
            textViewer.selectAllText();
        }
        textViewer.setToolTipText(Messages.getString("MainFrame.177")); 
        textViewer.setVisible(true);
        textViewer.toFront();
    }

    private void menuEditConvertSqlToJavaStringBuffer_actionPerformed() {
        String stringBufferVarName = Main.getUserProperty("STRINGBUFFER_NAME", "sql");   
        stringBufferVarName = JOptionPane.showInputDialog(this, Messages.getString("MainFrame.179"), stringBufferVarName); 
        if (stringBufferVarName != null) {
            Main.setUserProperty("STRINGBUFFER_NAME", stringBufferVarName); 
            final String javaCode = SQLCodeGenerator.convertSqlToJavaStringBuffer(getText(), stringBufferVarName);
            if (textViewer == null) {
                textViewer = new TextViewer(this, Messages.getString("MainFrame.180"), javaCode, true); 
            } else {
                textViewer.setText(javaCode);
                textViewer.selectAllText();
            }
            textViewer.setToolTipText(Messages.getString("MainFrame.181")); 
            textViewer.setVisible(true);
            textViewer.toFront();
        }
    }

    private void menuEditCommentParams_actionPerformed() {
        //menuEditRemoveComments_actionPerformed();
        editor.setText(SQLParser.commentPSParameter(editor.getText()));
    }

    private void menuEditRemoveParamComments_actionPerformed() {
        editor.setText(SQLParser.removePSComments(editor.getText()));
    }

    private void menuEditRemoveAllComments_actionPerformed() {
        editor.setText(SQLParser.removeAllComments(editor.getText()));
    }

    private void menuEditConvertJavaToSql_actionPerformed() {
        setTextSaveEnabled(false);
        setScriptTextAsNewDocument(SQLCodeGenerator.convertJavaToSqlCode(editor.getText()));
    }

    private void menuEditPaste_actionPerformed() {
        // paste-Funtion von TextPane
    	try {
            editor.paste();
    	} catch (Exception e) {
    		JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(), "Failed to paste content", JOptionPane.ERROR_MESSAGE);
    		logger.error("Failed to paste content: " + e.getMessage(), e);
    	}
    }

    private void menuEditPasteSmart_actionPerformed() {
        // paste-Funtion von TextPane
    	try {
            Clipboard clipboard = getClipboard();
            String text = (String) clipboard.getData(DataFlavor.stringFlavor);
            if (text != null && text.isEmpty() == false) {
                editor.copyTextIntoLinesSmart(text);
            }
    	} catch (Exception e) {
    		JOptionPane.showMessageDialog(MainFrame.this, e.getMessage(), "Failed to paste content", JOptionPane.ERROR_MESSAGE);
    		logger.error("Failed to paste content: " + e.getMessage(), e);
    	}
    }
    
	/**
	 * Returns the clipboard to use for cut/copy/paste.
	 */
    private Clipboard getClipboard() {
    	Clipboard c = null;
    	try {
    		c = getToolkit().getSystemClipboard();
    	} catch (Exception e) {
    		logger.warn("Clipboard is not accessable:" + e.getMessage());
    	}
    	return c;
	}

    private void menuEditInsertInsertStat_actionPerformed() {
        final String s = database.createInsertStatementText();
        if (s != null) {
            insertOrReplaceText(s);
        } else {
            showInfoMessage(
                Messages.getString("MainFrame.182"), 
                Messages.getString("MainFrame.183")); 
        }
    }

    private void menuEditInsertSelectStat_actionPerformed() {
        final String s = database.createSelectStatementText();
        if (s != null) {
            insertOrReplaceText(s);
        } else {
            showInfoMessage(
                Messages.getString("MainFrame.184"), 
                Messages.getString("MainFrame.185")); 
        }
    }

    private void menuEditInsertTablename_actionPerformed() {
        if ((database != null) && (database.getTableName() != null)) {
            insertOrReplaceText(database.getTableName());
        }
    }

    // suchen/erstzen-Dialog auffahren
    private void menuEditReplace_actionPerformed() {
        if (dlgSeRe == null) {
            dlgSeRe = new SearchReplaceDialog(this, false, editor);
        }
        WindowHelper.locateWindowAtMiddle(this, dlgSeRe);
        dlgSeRe.setVisible(true);
    }

    // Zeilenauswahl ermöglichen
    private void menuEditGoto_actionPerformed() {
        if (dlgGo == null) {
            dlgGo = new GotoLineDialog(this, Messages.getString("MainFrame.186"), editor); 
        }
        WindowHelper.locateWindowAtMiddle(this, dlgGo);
        dlgGo.setVisible(true);
    }

    private void menuEditSyntaxhighCheckBox_actionPerformed() {
        // Syntax-Highlighting ein/aus
        if (lexer != null) {
            if (menuEditSyntaxhighCheckBox.isSelected()) {
                // Vorbereiten der hervorzuhebenen Zeichen
                // Initialisieren des HighLighters
                lexer.setEnabled(true);
                Main.setUserProperty("SYNTAX_HIGHLIGHT", "true");  
            } else {
                lexer.setEnabled(false);
                Main.setUserProperty("SYNTAX_HIGHLIGHT", "false");  
            }
        }
        editor.repaint();
    }

    private void menuEditRestoreCfg_actionPerformed() {
        final int answer = JOptionPane.showConfirmDialog(
            this,
            Messages.getString("MainFrame.187"), 
            Messages.getString("MainFrame.188"), 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        switch (answer) {
            case JOptionPane.YES_OPTION: {
                Main.createDefaultAdminCfgFile();
                Main.createDefaultDbCfgFile();
                Main.createDefaultHLCfgFile();
                Main.createDefaultProp();
                showInfoMessage(Messages.getString("MainFrame.189"), Messages.getString("MainFrame.190"));  
            }
        }
    }

    private void menuEditPreferences_actionPerformed() {
        if (preferencesDialog != null) {
            preferencesDialog.dispose();
            preferencesDialog = null;
        }
        preferencesDialog = new PreferencesDialog(this);
        WindowHelper.locateWindowAtMiddle(this, preferencesDialog);
        preferencesDialog.setVisible(true);
    }

    public void openPreferencesDialog() {
        menuEditPreferences_actionPerformed();
    }

    private void menuDBAutoCommitCheckBox_actionPerformed() {
        if (menuDBAutoCommitCheckBox.isSelected()) {
            Main.setUserProperty("AUTO_COMMIT", "true");  
            enableCommitRollback(false);
        } else {
            Main.setUserProperty("AUTO_COMMIT", "false");  
            enableCommitRollback(true);
        }
        if ((database != null) && database.getDatabaseSession().isConnected()) {
            database.getDatabaseSession().setAutoCommit(menuDBAutoCommitCheckBox.isSelected());
        }
    }

    private void menuHelpAbout_actionPerformed() {
        if (dlgAbout != null) {
            dlgAbout.dispose();
            dlgAbout = null;
        }
        dlgAbout = new AboutDialog(this);
        WindowHelper.locateWindowAtMiddle(this, dlgAbout);
        dlgAbout.setModal(false);
        dlgAbout.setVisible(true);
        repaint();
    }

    public void openAboutDialog() {
        menuHelpAbout_actionPerformed();
    }

    /**
     * Statusbar
     */
    public final class StatusBar extends JPanel {

        private static final long serialVersionUID = 1L;
        private final BoxLayout box = new BoxLayout(this, BoxLayout.X_AXIS);
        private JLabel message = new FixedLabel();
        private JLabel tablePos = new FixedLabel();
        private JLabel textPos = new FixedLabel();
        private JLabel infoOverWrite = new FixedLabel("INS"); 
        private JLabel infoAction = new FixedLabel();
        private JLabel tableName = new FixedLabel();
        final static int LINE_NUM_WIDTH = 80;
        final static int TEXTPOS_WIDTH = 100;
        final static int INFO_OVERWRITE_WIDTH = 30;
        final static int INFO_ACTION_WIDTH = 50;
        final static int STATUS_HEIGHT = 25;
        final static int TABLENAME_WIDTH = 200;

        public StatusBar() {
            setLayout(box);
            message.setBorder(BorderFactory.createLoweredBevelBorder());
            message.setForeground(Color.black);
            tablePos.setPreferredSize(new Dimension(LINE_NUM_WIDTH, STATUS_HEIGHT));
            tablePos.setBorder(BorderFactory.createLoweredBevelBorder());
            tablePos.setOpaque(true);
            tablePos.setHorizontalAlignment(JLabel.CENTER);
            tablePos.setToolTipText(Messages.getString("MainFrame.193")); 
            textPos.setPreferredSize(new Dimension(TEXTPOS_WIDTH, STATUS_HEIGHT));
            textPos.setBorder(BorderFactory.createLoweredBevelBorder());
            textPos.setOpaque(true);
            textPos.setHorizontalAlignment(JLabel.CENTER);
            textPos.setToolTipText(Messages.getString("MainFrame.194")); 
            infoOverWrite.setPreferredSize(new Dimension(INFO_OVERWRITE_WIDTH, STATUS_HEIGHT));
            infoOverWrite.setBorder(BorderFactory.createLoweredBevelBorder());
            infoAction.setPreferredSize(new Dimension(INFO_ACTION_WIDTH, STATUS_HEIGHT));
            infoAction.setOpaque(true);
            infoAction.setText("DISC"); 
            infoAction.setBorder(BorderFactory.createLoweredBevelBorder());
            tablePos.setForeground(Color.black);
            textPos.setForeground(Color.black);
            infoOverWrite.setForeground(Color.black);
            infoAction.setForeground(Color.black);
            tableName.setBackground(Main.info);
            tableName.setForeground(Color.black);
            tableName.setOpaque(true);
            tableName.setBorder(BorderFactory.createLoweredBevelBorder());
            tableName.setPreferredSize(new Dimension(TABLENAME_WIDTH, STATUS_HEIGHT));
            tableName.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						if (tableName.getText() != null && tableName.getText().isEmpty() == false) {
							insertOrReplaceText(tableName.getText());
						}
					}
				}
            	
			});
            add(message);
            add(tableName);
            add(tablePos);
            add(textPos);
            add(infoOverWrite);
            add(infoAction);
            // die zu erwartenden Property-änderungen sind die setText()
            // Aufrufe, dann soll der ToolTip aktualisiert werden,
            // da dieser den Textinhalt voll anzeigen kann, auch wenn das Label
            // den Platz dafür nicht hat
            message.addPropertyChangeListener(new MeldungPropertyChangeListener());
        // eine bislang sinnvolle Nutzung der Spalte InfoAction
        // ist die Anzeige, ob Bak-Dateien erstellt werden oder nicht
        }

        private void setMessage(String text) {
            message.setText(text);
        }

        public JLabel getMessageComp() {
            return message;
        }

        public void setTablename(String name) {
            tableName.setText(name);
            tableName.setToolTipText(name);
        }

        public void setTableRowIndex(int index) {
            tablePos.setText(String.valueOf(index));
        }

        public void setInfoAction(String text, Color c) {
            if (text != null) {
                infoAction.setText(text);
            }
            if (c != null) {
                infoAction.setBackground(c);
            }
        }

        public void setTextModeOverWrite(boolean overwrite) {
            infoOverWrite.setText(overwrite ? "OW" : "INS");
        }

        // wird beim erstmaligen Darstellen , bei jeder Formänderung
        // und bei Aufrufen von repaint durchlaufen
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            final int meldungBreite = this.getWidth() - LINE_NUM_WIDTH - TEXTPOS_WIDTH - INFO_OVERWRITE_WIDTH - INFO_ACTION_WIDTH - TABLENAME_WIDTH;
            message.setPreferredSize(new Dimension(meldungBreite, STATUS_HEIGHT));
            remove(message); // Label entfernen
            add(message, null, 0); // neu hinzufügen, damit neu eingepasst
            doLayout(); // damit wird die änderung sofort wirksam !
        }

        // diese Klasse sichert, dass die Label auch in einer festen Groesse
        // dargestellt werden
        final class FixedLabel extends JLabel {

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

        class MeldungPropertyChangeListener implements PropertyChangeListener {

            public void propertyChange(PropertyChangeEvent evt) {
                message.setToolTipText(message.getText());
            }
        }
        
    } // class StatusBar

    private String createTabReplacement() {
        StringBuffer tabErsatz_loc = new StringBuffer(); 
        final int a = Integer.parseInt(Main.getDefaultProperty(Messages.getString("MainFrame.196"), "4"));  
        for (int i = 0; i < a; i++) {
            tabErsatz_loc.append(" "); 
        }
        return tabErsatz_loc.toString();
    }

    public void showInfoMessage(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void showErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void showWarningMessage(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * zeigt Datenbank-Nachrichtendialog an.
     * @param message Nachricht
     * @param title   Titel
     * @return return value des Dialoges (siehe DBMessageDialog)
     */
    public int showDBMessage(String message, String title) {
        final int returnCode;
        final DBMessageDialog md = new DBMessageDialog(this, message, title);
        returnCode = md.getReturnCode();
        md.cancel();
        return returnCode;
    }

    /**
     * zeigt Datenbank-Nachrichtendialog an.
     * erlaubt nicht die Chackbox "weitermachen"
     * @param message Nachricht
     * @param title   Titel
     * @return return value des Dialoges (siehe DBMessageDialog)
     */
    public int showDBMessageWithoutContinueAction(String message, String title) {
        final int returnCode;
        final DBMessageDialog md = new DBMessageDialog(this, message, title, false);
        returnCode = md.getReturnCode();
        md.cancel();
        return returnCode;
    }

    /**
     * zeigt Hinweisdialog an
     * @param message Nachricht
     * @param title Titel
     * @param initValue 
     * @return
     */
    public String showInputDialog(String message, String title, String initValue) {
        return (String) JOptionPane.showInputDialog(
            this,
            message,
            title,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            null,
            initValue);
    }

    /**
     * lädt das Document, während dessen kann editor die
     * ersten Ergebnisse schon anzeigen
     */
    private final class FileLoader extends Thread {

        private Document doc;
        private File f;
        static final char CR = 0x000D;
        static final char LF = 0x000A;
        static final char LSEP = 0x2028;
        static final char PSEP = 0x2029;
        static final char NL = 0x0085;

        FileLoader(File f, JTextComponent textComp) {
            this.f = f;
            this.doc = textComp.getDocument();
        }

        /**
         * enthält die Laderoutine File -> Document
         */
        @Override
        public void run() {
            fileLoaderBusy = true;
            // alten Cursor merken
            SwingUtilities.invokeLater(new Runnable() {
            	public void run() {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    editor.setEditable(false); // hier geht der Caret verloren !!
                    status.message.setText(Messages.getString("MainFrame.200") + f.getName() + " ");  
            	}
            });
            if (textChangeListener != null) {
                textChangeListener.setEnableLineHighlighting(false);
            }
            boolean lexerTemporarilyDisabled = false;
            if ((lexer != null) && lexer.isEnabled()) {
                lexer.setEnabled(false);
                lexerTemporarilyDisabled = true;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("MainFrame.FileLoader: set editor not editable"); 
            }
            //String buffer_s;
            final boolean tabReplace = (Main.getDefaultProperty("TAB_REPLACE_WITH_SPACE", "false")).equals("true");   
            if (logger.isDebugEnabled()) {
                if (tabReplace) {
                    logger.debug("FileLoader: TABs will be replaced " 
                        + Main.getDefaultProperty("TAB_SPACE_MAPPING", "3")  
                        + Messages.getString("MainFrame.208")); 
                } else {
                    logger.debug("MainFrame.FileLoader: TABs will be read transparent"); 
                }
            }
            long t0 = 0;
            if (logger.isDebugEnabled()) {
                t0 = System.currentTimeMillis();
                logger.debug("MainFrame.FileLoader: load ..."); 
            }
            BufferedReader in = null;
            try {
                if (Main.currentCharSet != null) {
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(f), Main.currentCharSet));
                } else {
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                }
                int i;
                int nch;
                char c;
                char c0 = ' ';
                int countTabs = 0;
                final char[] buffer = new char[READ_BUFFER_SIZE];
                StringBuilder sb = new StringBuilder(buffer.length);
                while ((nch = in.read(buffer, 0, buffer.length)) != -1) {
                    for (i = 0; i < nch; i++) {
                        // UNIX-Fileformat herstellen
                        c = buffer[i];
                        // bei der Findung der Zeilenumbrüche auch
                        // UNIX, Windows, und Mac berücksichtigen
                        if ((c == LSEP) || (c == PSEP) || (c == NL)) {
                            buffer[i] = '\n';
                        } else if (c == LF) {
                            if (c0 == CR) {
                                // dann war es ein Windows-Zeilenumbruch
                                // das LF können wir also ignorieren
                                buffer[i] = 0x0000;
                            } else {
                                // wenn kein CR zuvor da war, dann ist es ein Mac
                                buffer[i] = '\n';
                            }
                        } else if (c == CR) {
                            buffer[i] = '\n';
                        }
                        c0 = c;
                    }
                    sb.setLength(0);
                    // sollen TABs ersetzt werden ?
                    if (tabReplace) {
                        int tabIndex;
                        int nextSearchStartPos = 0;
                        boolean fertig = false;
                        // hier TABs ersetzten
                        while (!fertig) {
                            tabIndex = -1;
                            for (int ti = nextSearchStartPos; ti < nch; ti++) {
                                if (buffer[ti] == '\t') {
                                    tabIndex = ti;
                                    break;
                                }
                            }
                            if (tabIndex == -1) {
                                // den Rest kopieren
                                copy(sb, buffer, nextSearchStartPos, nch - nextSearchStartPos);
                                // nichts mehr zu tun
                                fertig = true;
                            } else {
                                copy(sb, buffer, nextSearchStartPos, tabIndex - nextSearchStartPos);
                                sb.append(tabErsatz);
                                nextSearchStartPos = tabIndex + 1;
                                countTabs++;
                            }
                        }
                    } else {
                        copy(sb, buffer, 0, nch);
                    }
                    // Unix_fileFormat herstellen unbedingt erforderlich !!
                    doc.insertString(doc.getLength(), sb.toString(), null);
                }
                sb = null;
                if (logger.isDebugEnabled()) {
                    final long t1 = System.currentTimeMillis() - t0;
                    logger.debug("Load file finished: time: " 
                        + t1 + " ms, doc-size: " 
                        + doc.getLength() + ". " 
                        + countTabs + " TABs were replaced."); 
                }
                status.message.setText(
                    Messages.getString("MainFrame.statusfile_") 
                    + currentFile.getName() + Messages.getString("MainFrame.statusfileloaded")); 
                status.repaint();
                // User-Properties um neue Eintraege ergaenzen
                // an dieser Stelle ist sicher, dass die Datei korrekt geladen
                // dann kann sie auch in das Reopen-menu eingetragen werden
                // nur nicht wenn Konfig-Dateien geladen wurde !!
                if (currentFile.getName().endsWith(Main.CFG_FILE_NAME) || 
                    currentFile.getName().endsWith(Main.ADMIN_CFG_FILE_NAME) ||
                    currentFile.getName().endsWith(Main.DB_CFG_FILE_NAME) ||
                    currentFile.getName().endsWith(Main.KEYWORD_FILE) ||
                    currentFile.getName().endsWith(Main.HIGHLIGHTER_FONT_CFG_FILE)) {
                    // nicht dem Menü reopen hinzufügen
                } else {
                    // Properties ergänzen
                    Main.addFileProp(currentFile);
                // die reopen-Items werden in SQLEditor zentral für alle Instanzen von MainFrame erstellt.
                }
            } catch (IOException e) {
                logger.warn("FileLoader: Datei " 
                    + currentFile.getPath() + " nicht vorhanden, wird beim Speichern neu angelegt"); 
                status.message.setText(Messages.getString("MainFrame.statusfile_") 
                    + currentFile.getPath() + Messages.getString("MainFrame.statusfilenotfound") 
                    + Messages.getString("MainFrame.statusfilewillbenewcreated")); 
            // nun einen ggf vorhandenen Eintrag aus den ReOpen-Menue entfernen.
            } catch (BadLocationException e) {
                logger.warn("FileLoader: Fehler beim Laden des Dokuments"); 
                status.message.setText(
                    Messages.getString("MainFrame.statusfile_") 
                    + currentFile.getPath() + Messages.getString("MainFrame.statusfilenotcorrectloaded")); 
            } catch (java.security.AccessControlException ae) {
                logger.warn("FileLoader: " + ae.getMessage()); 
                status.message.setText(Messages.getString("MainFrame.statusnoaccestofilesystem")); 
                currentFile = null;
                setupWindowTitle();
            } finally {
                // in jedem Fall den Reader schliessen!
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn("Cannot close reader " + e.getMessage(), e); 
                    }
                    in = null;
                }
            }
            // Instanz dem gc ueberlassen
            // Text auf clean setzen
            // den UndoRedoManager nach dem laden anbinden sonst
            // wird das laden falsch als änderung erkannt
            // und in jedem Fall, denn wenn die Datei nicht existiert wird trotzdem
            // leeres Document angelegt.
            SwingUtilities.invokeLater(new Runnable() {
            	public void run() {
                    setTextSaveEnabled(false);
                    undoManager.discardAllEdits();
                    updateUndoRedoEnabled();
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    editor.setCaretPosition(0);
                    setFocusToEditor();
                    editor.setEditable(true);
            	}
            });
            if ((lexer != null) && lexerTemporarilyDisabled) {
                lexer.setEnabled(true);
                ((SyntaxDocument) doc).updateCommentAttributes();
            }
            fileLoaderBusy = false;
            doc.addDocumentListener(textChangeListener);
            doc.addUndoableEditListener(undoHandler);
            if (textChangeListener != null) {
                textChangeListener.setEnableLineHighlighting(true);
            }
            
        } // run()

        private void copy(StringBuilder sb, char[] buffer, int startPos, int length) {
            char c;
            for (int i = startPos; i < (startPos + length); i++) {
                c = buffer[i];
                if (c > 0x0000) {
                    sb.append(c);
                }
            }
        }
    } // FileLoader

    /**
     * Klasse registriert änderungen im Document und
     * veranlasst die änderungen des Status
     */
    private final class TextChangeListener implements DocumentListener, CaretListener {

        private JTextComponent editor;
        private Highlight openBracketHighLightInfo;
        private Highlight closeBracketHighLightInfo;
        private Highlight markBracketHighLightInfo;
        private Highlight lineHighLightInfo;
        private DefaultHighlighter.DefaultHighlightPainter highlightPainterBracketOK = new DefaultHighlighter.DefaultHighlightPainter(
            Color.lightGray);
        private DefaultHighlighter.DefaultHighlightPainter highlightPainterBracketMark = new DefaultHighlighter.DefaultHighlightPainter(
            new Color(255, 255, 230));
        private DefaultHighlighter.DefaultHighlightPainter highlightPainterBracketMissing = new DefaultHighlighter.DefaultHighlightPainter(
            Color.red);
        private Highlighter.HighlightPainter highlightPainterLine = new LineHighlightPainter(null);
        private boolean enableLineHighlighting = true;

        TextChangeListener(JTextComponent editor) {
            this.editor = editor;
        }

        public void setEnableLineHighlighting(boolean enabled) {
            this.enableLineHighlighting = enabled;
            if (enabled == false) {
                removeLineHighlight();
            }
        }

        /**
         * wird bei jeder änderung der Cursorposition aufgerufen
         */
        public final void caretUpdate(CaretEvent e) {
            final Document doc = editor.getDocument();
            // Aktualisieren der Statuszeile
            final Element root = doc.getDefaultRootElement();
            // getElementIndex(..) bringt das Nachfolge-Element, welches
            // am ehesten die angebene TextPosition abdeckt
            // da ein Element mit einer Zeile korrespondiert
            // (nicht so in StyledDocument!!!!)
            // ist die Elementenummer = Zeilennummer
            currentTextPos = e.getDot();
            currentLineNumber = root.getElementIndex(currentTextPos);
            // ElementIndex begionnt ab 0, Zeilen werden aber
            // besser ab 1 gezaehlt
            // Spalte im Text ermitteln
            // Spalte = index im Element
            final Element currElem = root.getElement(currentLineNumber);
            currentLineStartOffset = currElem.getStartOffset();
            currentLineEndOffset = currElem.getEndOffset();
            currentOffsetInCurrLine = currentTextPos - currElem.getStartOffset();
            status.textPos.setText(String.valueOf(currentLineNumber + 1) + ":" 
                + String.valueOf(currentOffsetInCurrLine + 1) + " / " 
                + (currentTextPos + 1));
            // status aktualisieren, sonst wird das hier nicht jetzt dargestellt
            status.revalidate();
            if (getSyntaxChooser().isVisible() || tbButtonHighlighter.isSelected()) {
                findCurrentWord();
            }
            if (syntaxChooser != null) {
            	syntaxChooser.setSearchTerm(currentWord);
            }
            if (tbButtonHighlighter.isSelected()) {
                highlightCurrentWord();
            }
            // nun noch testen ob etwas selektiert wurde
            int sizeSelectedText = Math.abs(editor.getSelectionStart() - editor.getSelectionEnd());
            int startLineNumber = root.getElementIndex(editor.getSelectionStart());
            int selectedLines = Math.abs(currentLineNumber - startLineNumber);
            if (sizeSelectedText > 0 && currentOffsetInCurrLine > 0) {
            	selectedLines++;
            }
            removeLineHighlight();
            if (sizeSelectedText > 0 || e.getDot() != e.getMark()) {
                // es wurde Text selektiert
                textIsSelected(true);                
                status.message.setText(
                    " " 
                    + sizeSelectedText 
                    + Messages.getString("MainFrame.statuscharsselected")
                    + " / "
                    + selectedLines
                    + Messages.getString("MainFrame.statuslinesselected")); 
            } else {
                textIsSelected(false);
                removeBracketHighlighting();
                try {
                    final String text = doc.getText(0, doc.getLength());
                    final int[] bracketPositions = SQLParser.findOppositeParenthese(currentTextPos, text);
                    setBracketHighLighting(bracketPositions);
                    if (Main.isLineHighlightingEnabled() && enableLineHighlighting) {
                        setLineHighlight(currElem.getStartOffset(), currElem.getEndOffset());
                    }
                } catch (BadLocationException e1) {
                    logger.warn("exception: " + e1); 
                }
            }
        }

        private void removeBracketHighlighting() {
            if (openBracketHighLightInfo != null) {
                editor.getHighlighter().removeHighlight(openBracketHighLightInfo);
            }
            if (closeBracketHighLightInfo != null) {
                editor.getHighlighter().removeHighlight(closeBracketHighLightInfo);
            }
            if (markBracketHighLightInfo != null) {
                editor.getHighlighter().removeHighlight(markBracketHighLightInfo);
            }
        }

        private void setBracketHighLighting(int[] positions) throws BadLocationException {
            if (positions[0] != -1) {
                if (positions[1] != -1) {
                    openBracketHighLightInfo = (Highlight) editor.getHighlighter().addHighlight(positions[0], positions[0] + 1, highlightPainterBracketOK);
                    closeBracketHighLightInfo = (Highlight) editor.getHighlighter().addHighlight(positions[1], positions[1] + 1, highlightPainterBracketOK);
                    if (Main.isBracketContentHighlightingEnabled()) {
                        markBracketHighLightInfo = (Highlight) editor.getHighlighter().addHighlight(positions[0] + 1, positions[1] - 1, highlightPainterBracketMark);
                    }
                } else {
                    openBracketHighLightInfo = (Highlight) editor.getHighlighter().addHighlight(positions[0], positions[0] + 1, highlightPainterBracketMissing);
                }
            } else if (positions[1] != -1) {
                closeBracketHighLightInfo = (Highlight) editor.getHighlighter().addHighlight(positions[1], positions[1] + 1, highlightPainterBracketMissing);
            }
        }

        private void removeLineHighlight() {
            if (lineHighLightInfo != null) {
                editor.getHighlighter().removeHighlight(lineHighLightInfo);
                editor.repaint();
            }
        }

        private void setLineHighlight(int start, int end) throws BadLocationException {
            lineHighLightInfo = (Highlight) editor.getHighlighter().addHighlight(start, end, highlightPainterLine);
        }

        /**
         * setzt bei Textänderungen das änderungskennzeichen für das Document
         */
        private void test() {
            if (isTextChanged() == false) {
                setTextSaveEnabled(true);
            }
        }

        public void insertUpdate(DocumentEvent e) {
            test();
        }

        public void removeUpdate(DocumentEvent e) {
            test();
        }

        public void changedUpdate(DocumentEvent e) {
            test();
        }
    }
    
    private String findCurrentWord() {
    	currentWord = "";
    	String line = "";
    	if (currentLineEndOffset > currentLineStartOffset) {
    		try {
    			line = editor.getText(currentLineStartOffset, currentLineEndOffset - currentLineStartOffset - 1);
    		} catch (BadLocationException e) {
    			logger.error("findCurrentWord failed:" + e.getMessage(), e);
    		}
    	}
    	int findStartPos = currentOffsetInCurrLine;
        if (line.length() > 0) { 
            int startIndex = findStartPos;
            if (startIndex > 0) {
                // find the start of word
                while (true) {
                	startIndex--;
                    char c = line.charAt(startIndex);
                    if (Character.isJavaIdentifierPart(c)) {
                        if (startIndex == 0) {
                            break;
                        }
                    } else {
                    	startIndex++;
                        break;
                    }
                }
            }
            int endIndex = findStartPos;
            if (endIndex < line.length()) {
                while (true) {
                    char c = line.charAt(endIndex);
                    if (Character.isJavaIdentifierPart(c)) {
                    	endIndex++;
                        if (endIndex == line.length()) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (startIndex < endIndex) {
            	startPosOfCurrentWord = startIndex + currentLineStartOffset;
            	currentWord = line.substring(startIndex, endIndex);
            	if (logger.isDebugEnabled()) {
            		logger.debug("findCurrentWord word:<" + currentWord + "> findStartPos:" + findStartPos);
            	}
            }
        }
        return currentWord;
    }
    
    private String getText(int lineNumber) {
        final Document doc = editor.getDocument();
    	final Element root = doc.getDefaultRootElement();
    	Element currElem = root.getElement(lineNumber);
    	int startIndex = currElem.getStartOffset();
    	int endIndex = currElem.getEndOffset();
    	if (startIndex < endIndex) {
        	try {
				return editor.getText(startIndex, endIndex - startIndex - 1);
			} catch (BadLocationException e) {
				logger.error("selectCurrentBlock failed: " + e.getMessage(), e);
			}
    	}
    	return "";
    }

    private void selectCurrentBlock() {
    	if (logger.isDebugEnabled()) {
    		logger.debug("selectCurrentBlock start in line number " + currentLineNumber);
    	}
    	int cp = currentLineNumber;
		// we use this as start
		while (true) {
	    	String currentLineText = getText(cp);
	    	if (currentLineText.trim().isEmpty()) {
	    		cp++;
	    		break;
	    	} else {
	    		if (cp > 0) {
	    			cp--;
	    		} else {
	    			break;
	    		}
	    	}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("selectCurrentBlock found start line number: " + cp);
		}
		int startElementIndex = cp;
        final Document doc = editor.getDocument();
    	final Element root = doc.getDefaultRootElement();
    	int elementCount = root.getElementCount();
		cp = currentLineNumber;
		for ( ;cp < elementCount; cp++) {
	    	String currentLineText = getText(cp);
	    	if (currentLineText.trim().isEmpty()) {
	    		cp--;
	    		break;
	    	}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("selectCurrentBlock found end line number: " + cp);
		}
		int endElementIndex = cp;
		Element e = root.getElement(startElementIndex);
		if (e != null) {
			int startSelectPos = e.getStartOffset();
			if (endElementIndex == elementCount) {
				endElementIndex--;
			}
			int endSelectionPos = root.getElement(endElementIndex).getEndOffset();
			if (logger.isDebugEnabled()) {
				logger.debug("selectCurrentBlock select " + startSelectPos + ":" + endSelectionPos);
			}
			editor.select(startSelectPos, endSelectionPos - 1);
		}
    }
    
    private String getWordAtPos(String text, int findStartPos) {
    	String word = "";
        char c = text.charAt(findStartPos);
        if (Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c) || c == '_') {
            int startIndex = -1;
            int endIndex = -1;
            // den Anfang suchen
            int searchIndex = findStartPos;
            while (true) {
                c = text.charAt(searchIndex);
                if (Character.isUpperCase(c) || Character.isLowerCase(c) || Character.isDigit(c) || c == '_') {
                    if (searchIndex > 0) {
                        searchIndex--;
                    } else {
                        break;
                    }
                } else {
                    searchIndex++;
                    break;
                }
            }
            startIndex = searchIndex;
            searchIndex = findStartPos;
            while (true) {
                c = text.charAt(searchIndex);
                if (Character.isUpperCase(c) || Character.isLowerCase(c) || Character.isDigit(c) || c == '_') {
                    if (searchIndex < text.length() - 1) {
                        searchIndex++;
                    } else {
                        break;
                    }
                } else {
                    searchIndex--;
                    break;
                }
            }
            endIndex = searchIndex;
            if (startIndex >= 0 && startIndex <= endIndex) {
            	word = text.substring(startIndex, endIndex + 1);
            	if (logger.isDebugEnabled()) {
            		logger.debug("findCurrentWord word:" + currentWord + " findStartPos:" + findStartPos);
            	}
            }
        }
        return word;
    }

    private void highlightCurrentWord() {
    	if (currentWord.isEmpty() == false) {
            lexer.setHighlightedWord(currentWord);
            editor.repaint();
    	}
    	if (syntaxChooser != null) {
    		syntaxChooser.setSearchTerm(currentWord);
    	}
    }

    // setzte die Freigaben für die Steuerung Undo/Redo
    // entsprechend den möglichkeiten
    private void updateUndoRedoEnabled() {
        if (undoManager.canUndo()) {
            tbButtonUndo.setEnabled(true);
            menuEditUndo.setEnabled(true);
        } else {
            tbButtonUndo.setEnabled(false);
            menuEditUndo.setEnabled(false);
        }
        if (undoManager.canRedo()) {
            tbButtonRedo.setEnabled(true);
            menuEditRedo.setEnabled(true);
        } else {
            tbButtonRedo.setEnabled(false);
            menuEditRedo.setEnabled(false);
        }
    }

    /**
     * Empfänger für die Edit-Events
     * gibt die änderungen an den UndoManager weiter
     */
    private final class UndoHandler implements UndoableEditListener {

        public boolean valid = true;

        public void undoableEditHappened(UndoableEditEvent e) {
            if (valid) {
                undoManager.addEdit(e.getEdit());
                updateUndoRedoEnabled();
            }
        }
    }

    /**
     * Listener für die "Neu öffnen"-MenueItems
     */
    private class MenuScriptReopenListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            loadFileInDocument(true, e.getActionCommand());
        } // actionPerformed
    } // class MenuScriptReopenListener

    // ---------------- Methoden für Database zu Beeinflussung der GUI und Zugriff auf den Text
    /**
     * kennzeichnet, dass SQL-Statements nun absetzbar sind.
     */
    public void setGuiToConnected(boolean enable) {
        tbButtonRun.setEnabled(enable);
        menuDBRun.setEnabled(enable);
        menuDBAdmin.setEnabled(enable);
        menuDBOpen.setEnabled(!enable);
        menuDBReconnect.setEnabled(enable);
        menuDBClose.setEnabled(enable);
        tbButtonDbOpen.setEnabled(!enable);
        tbButtonDbClose.setEnabled(enable);
        menuDBCsvExportDBTable.setEnabled(enable);
        menuDBCreateNewRow.setEnabled(enable);
        menuDBInformation.setEnabled(enable);
        menuDBDatamodel.setEnabled(enable);
        menuDBXML.setEnabled(enable);
        menuDBAbortConnecting.setEnabled(false);
        enableMetadataOps(enable);
        updateExplainState(enable);
        if ((database != null) && (database.getDatabaseSession().isAutoCommit() == false)) {
            enableCommitRollback(true);
        } else {
            enableCommitRollback(false);
        }
        if (enable) {
            menuDBAutoCommitCheckBox.setSelected(database.getDatabaseSession().isAutoCommit());
            status.infoAction.setText("CONN"); 
            status.infoAction.setToolTipText(Messages.getString("MainFrame.infoactiontooltipconnectedwith") 
                + database.getDatabaseSession().getUrl() + Messages.getString("MainFrame.infoactionas") 
                + database.getDatabaseSession().getUser());
        } else {
            enableCommitRollback(false);
            // hier nur ausschalten
            menuDBStop.setEnabled(enable);
            tbButtonStop.setEnabled(enable);
            status.infoAction.setToolTipText(Messages.getString("MainFrame.infoactionnotconnected")); 
        }
    }

    /**
     * erlaubt/verbietet Aktionen zum Metamodell
     * @param enabled
     */
    public void enableMetadataOps(boolean enabled) {
        this.menuDBDatamodel.setEnabled(enabled);
    }

    /**
     * fragt den Zustand der Datenbankaktivität ab
     * @return true ist noch mit Datenbank beschäftigt
     */
    public boolean isDatabaseBusy() {
        return databaseIsBusy;
    }

    public final boolean isFileLoaderBusy() {
        return fileLoaderBusy;
    }

    public final boolean isBusy() {
        return databaseIsBusy || fileLoaderBusy;
    }

    private long lastMessageFrom = 0;

    /**
     * setzt das Fenster auf beschäftigt im Bezug auf Datenbankaktivitäten
     * alle Datenbank-Menüs/Buttons werden deaktiviert
     * @param busy true also beschäftigt
     * @param message Nachricht für die Statuszeile
     */
    public void setDatabaseBusyFiltered(final boolean busy, final String message) {
    	if (busy == false || (System.currentTimeMillis() - lastMessageFrom) > 1000) {
    		lastMessageFrom = System.currentTimeMillis();
    		setDatabaseBusy(busy, message);
    	}
    }
    
    public void setDatabaseBusy(final boolean busy, final String message) {
    	if (SwingUtilities.isEventDispatchThread()) {
    		doSetDatabaseBusy(busy, message);
    	} else {
    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    	    		doSetDatabaseBusy(busy, message);
    			}
    		});
    	}
    }
    
    public void setStatusMessage(final String message) {
    	if (SwingUtilities.isEventDispatchThread()) {
    		status.setMessage(message);
    	} else {
    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    	    		status.setMessage(message);
    			}
    		});
    	}
    }

    private void doSetDatabaseBusy(boolean busy, String message) {
        if (message != null) {
            status.message.setText(message);
        }
    	if (busy == false || databaseIsBusy != busy) {
            if (busy) {
                status.infoAction.setBackground(Color.red);
            } else {
                status.infoAction.setBackground(Color.lightGray);
            }
            setCellEditorUpdateEnabled(!busy);
            tbButtonRun.setEnabled(!busy);
            menuDBRun.setEnabled(!busy);
            if (busy) {
            	menuDBExplain.setEnabled(false);
            	tbButtonExplain.setEnabled(false);
            } else {
            	updateExplainState(true);
            }
            //editor.setEditable(false);
            menuDBStop.setEnabled(busy);
            tbButtonStop.setEnabled(busy);
            menuDBAdmin.setEnabled(!busy);
            menuDBCsvExportResultTable.setEnabled(!busy);
            if (busy == false && (database != null) && (database.getDatabaseSession().isAutoCommit() == false)) {
                enableCommitRollback(true);
            } else {
                enableCommitRollback(false);
            }
            menuDBInformation.setEnabled(!busy);
            menuDBAutoCommitCheckBox.setEnabled(!busy);
            databaseIsBusy = busy;
    	}
    }
    
    private void enableCommitRollback(boolean enable) {
        if (database != null) {
            menuDBCommit.setEnabled(enable);
            menuDBRollback.setEnabled(enable);
            tbButtonCommit.setEnabled(enable);
            tbButtonRollback.setEnabled(enable);
        } else {
            menuDBCommit.setEnabled(false);
            menuDBRollback.setEnabled(false);
            tbButtonCommit.setEnabled(false);
            tbButtonRollback.setEnabled(false);
        }
    }

    /**
     * setzt eine komplett neuen Text für den Editor
     * @param text neuer Text
     */
    public void setScriptTextAsNewDocument(String text) {
        menuScriptNew_actionPerformed();
        try {
            editor.getDocument().insertString(0, text, null);
        } catch (BadLocationException ble) {
            logger.warn("exception: " + ble); 
        }
    }

    /**
     * wenn im Editor Text markiert ist wird dieser ersetzt und wenn nicht wird neuer Text an
     * die Cursor-position eingefügt.
     * @param text einzufügender Text
     */
    public void insertOrReplaceText(String text) {
        if (text != null) {
            try {
                text = text.replace('\r', ' '); // in Statements bereiten diese Zeichen echte Probleme
                if (editor.getSelectedText() != null) {
                    editor.replaceSelection(text);
                } else {
                    editor.getDocument().insertString(editor.getCaretPosition(), text, null);
                }
            } catch (BadLocationException ble) {
                logger.warn("exception: " + ble); 
            }
        }
    }
    
    public void insertFormattedBreak() {
    	ExtEditorKit.insertFormattedBreak(editor);
    }

    /**
     * ersetzt den bestehenden Text komplett
     * @param text neuer Text
     */
    public void setScriptText(String text) {
        if (text != null) {
            if (currentFile != null) {
                // fragen ob das aktuelle Dokument zuvor gespeichert werden soll
                if (menuScriptNew_actionPerformed()) {
                    text = text.replace('\r', ' '); // in Statements bereiten diese Zeichen echte Probleme
                    editor.setText(text);
                    setTextSaveEnabled(false);
                }
            } else {
                text = text.replace('\r', ' '); // in Statements bereiten diese Zeichen echte Probleme
                editor.setText(text);
                setTextSaveEnabled(false);
            }
        }
    }

    /**
     * Inhalt des Editors
     * @return Textinhalt des Editors
     */
    public String getText() {
        try {
            return editor.getDocument().getText(0, editor.getDocument().getLength()).trim();
        } catch (BadLocationException ble) {
            logger.warn("exception: " + ble); 
            return null;
        }
    }

    /**
     * gibt die referenz auf das aktuelle Document
     * @return current document
     */
    public Document getDocument() {
        return editor.getDocument();
    }

    public int getTextLength() {
    	return editor.getDocument().getLength();
    }
    
    /**
     * setzt die Cursor-position vor die position im Text 
     * @param pos absolute position im Text
     */
    public void setCaretPos(int pos) {
        editor.setCaretPosition(pos);
    }

    public int getCaretPos() {
        return editor.getCaretPosition();
    }

    /**
     * löscht alle Feld-Editoren
     */
    public void disposeAllViewer() {
        for (int i = 0; i < cellEditorList.size(); i++) {
            final ValueEditor cv = cellEditorList.elementAt(i);
            if (cv != null) {
                cv.dispose();
            }
        }
        cellEditorList.removeAllElements();
    }

    /**
     * Erstellt einen neuen Feldeditor wenn für diese Zelle noch keiner aktiv oder
     * wenn einer berteits aktiv wird dieser hervorgeholt.
     * @param row Zeile in der Tabelle
     * @param col Spalte in der Tabelle
     */
    void showEditor(int row, int col) {
        if (row != -1 && col != -1) {
            // zuerst mal testen ob dieses Feld schon angezeigt wird !
            boolean isOpen = false;
            for (int i = 0; i < cellEditorList.size(); i++) {
                ValueEditor cellEditor = cellEditorList.elementAt(i);
                if ((cellEditor.getRowIndex() == row) && (cellEditor.getColumnIndex() == col)) {
                    if (cellEditor.isVisible()) { // wenn Fenster disposed ist es eben noch nicht null, gb arbeitet träge!
                        cellEditor.setState(Frame.NORMAL);
                        cellEditor.toFront();
                        isOpen = true;
                        break;
                    }
                }
            }
            if (isOpen == false) {
                ValueEditor cellEditor = new ValueEditor(
                    databaseIsBusy,
                    (row == database.getNewRowIndex()),
                    this);
                if (database.isVerticalView()) {
                    cellEditor.setObject(
                        database.getValueAt(row, col),
                        database.convertFromVerticalToLogicalRowIndex(row),
                        database.convertFromVerticalToLogicalColIndex(row));
                } else {
                    cellEditor.setObject(database.getValueAt(row, col), row, col);
                }
                cellEditor.setTitle(
                    Messages.getString("MainFrame.celleditortitlefield") 
                    + database.getColumnName(col) + Messages.getString("MainFrame.celleditortitleinline") 
                    + String.valueOf(row + 1));
//                boolean updateDisabled = (row == database.getNewRowIndex()) && (database.getColumnBasicType(col) == BasicDataType.BINARY.getId());
//                cellEditor.setUpdateEnabled(updateDisabled == false);
                cellEditorList.addElement(cellEditor);
            }
        }
    }

    private void setCellEditorUpdateEnabled(boolean enable) {
        ValueEditor cellEditor;
        for (int i = 0; i < cellEditorList.size(); i++) {
            cellEditor = (ValueEditor) cellEditorList.elementAt(i);
            if (cellEditor != null) {
                cellEditor.setUpdateEnabled(enable);
            }
        }
    }

    private String getMenuWindowLabelText() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(getFrameIndex());
        sb.append(") ");
        sb.append(windowName);
        String scriptText = getText().replace('\n', ' ');
        if (scriptText.length() > 0) {
            sb.append(" | ");
            int length = scriptText.length();
            if (length > 30) {
                sb.append(scriptText.substring(0, 30));
                sb.append("...");
            } else {
                sb.append(scriptText);
            }
        }
        return sb.toString();
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    private void createEditorContextMenu(MouseEvent me) {
    	createEditorContextMenu(me.getX(), me.getY());
    }
    
    private void createEditorContextMenu() {
		try {
			Rectangle r = editor.modelToView(currentTextPos);
        	createEditorContextMenu(r.x, r.y);
		} catch (BadLocationException e) {
			logger.error("createEditorContextMenu failed: " + e.getMessage(), e);
		}
    }
    
    private void showSyntaxChooser() {
    	try {
    		updateEditorKeyMapForShowingSyntaxChooser();
			Rectangle r = editor.modelToView(currentTextPos);
			int verticalValue = editorScrollPane.getVerticalScrollBar().getValue();
			int horizontalValue = editorScrollPane.getHorizontalScrollBar().getValue();
			int compX = r.x - horizontalValue;
			int compY = r.y - verticalValue;
			int compWidth = 300;
			int compHeight = 300;
			logger.debug("showSyntaxChooser at position " + r.x + ":" + r.y + " [" + compX + ":" + compY + "]");
			int runoutX = (compX + compWidth) - editor.getWidth();
			if (runoutX > 0) {
				compX = compX - runoutX;
			}
			int runoutY = (compY + compHeight) - editor.getHeight();
			if (runoutY > 0) {
				compHeight = compHeight - runoutY;
				if (compHeight < 80) {
					compHeight = 80;
				}
			}
			getSyntaxChooser().setBounds(compX, compY + 20, compWidth, compHeight);
			getSyntaxChooser().reset();
			getSyntaxChooser().setVisible(true);
			if (database != null && database.isConnected()) {
				SQLDataModel dataModel = database.getDataModel();
				if (isDotAtCurrentPos()) {
					String wordBeforeDot = getWordBeforeDot();
					// it could be a schema or a table
					SQLSchema schema = dataModel.getSchema(wordBeforeDot);
					if (schema != null) {
						getSyntaxChooser().addItems(schema.getTables());
						getSyntaxChooser().addItems(schema.getProcedures());
					}
				} else {
					SQLSchema schema = dataModel.getCurrentSQLSchema();
					if (schema != null) {
						getSyntaxChooser().addItems(schema.getTables());
						getSyntaxChooser().addItems(schema.getProcedures());
					}
					getSyntaxChooser().addItems(dataModel.getSchemas());
					getSyntaxChooser().addItems(lexer.getLongKeywords());
				}
			} else {
				getSyntaxChooser().addItems(lexer.getLongKeywords());
			}
			findCurrentWord();
			getSyntaxChooser().setSearchTerm(currentWord);
    	} catch (BadLocationException e) {
			logger.error("showSyntaxChooser failed: " + e.getMessage(), e);
		}
    }
    
    private boolean isDotAtCurrentPos() {
    	try {
    		if (currentTextPos > 0) {
    			String text = editor.getDocument().getText(currentTextPos - 1, 1);
    			if (text.endsWith(".")) {
    				return true;
    			}
    		}
		} catch (BadLocationException e) {
			logger.error(e);
		}
    	return false;
    }
    
    private String getWordBeforeDot() {
    	String word = "";
    	String text = null;
		try {
			text = editor.getDocument().getText(0, editor.getDocument().getLength());
	    	word = getWordAtPos(text, currentTextPos - 2);
	    	if (logger.isDebugEnabled()) {
	    		logger.debug("getWordBeforeDot returns: " + word);
	    	}
		} catch (BadLocationException e) {
			logger.error(e);
		}
    	return word;
    }
    
    private Action defaultEnterAction;
    private Action defaultCaretUpAction;
    private Action defaultCaretDownAction;
    
    private void updateEditorKeyMapForShowingSyntaxChooser() {
    	final Keymap map = editor.getKeymap();
    	// redefine enter to get the selected choise from chooser
    	final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
    	defaultEnterAction = map.getAction(enter); // to be able to restore
    	map.addActionForKeyStroke(enter, new AbstractAction("enter") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				insertSyntaxChooserText();
				closeSyntaxChooser();
			}
    		
    	});
    	final KeyStroke caretUp = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
    	defaultCaretUpAction = map.getAction(caretUp); // to be able to restore
    	map.addActionForKeyStroke(caretUp, new AbstractAction("caretUp") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				getSyntaxChooser().selectUp();
			}
    		
    	});
    	final KeyStroke caretDown = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);
    	defaultCaretDownAction = map.getAction(caretDown);
    	map.addActionForKeyStroke(caretDown, new AbstractAction("caretDown") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				getSyntaxChooser().selectDown();
			}
    		
    	});

    }
    
    public void insertSyntaxChooserText() {
    	Object item = getSyntaxChooser().getSeletedItem();
    	String text = "";
    	if (item instanceof String) {
    		text = (String) item;
    	} else if (item instanceof SQLObject) {
    		text = ((SQLObject) item).getName();
    	}
    	if (currentWord.isEmpty()) {
    		startPosOfCurrentWord = currentTextPos;
    	}
    	if (text.isEmpty() == false) {
        	try {
    			editor.getDocument().remove(startPosOfCurrentWord, currentWord.length());
    			editor.getDocument().insertString(startPosOfCurrentWord, text, null);
    		} catch (BadLocationException e) {
    			logger.error(e);
    		}
    	}
    }
    
    private void updateEditorKeyMapRestoreToDefault() {
    	final Keymap map = editor.getKeymap();
    	// redefine enter to get the selected choise from chooser
    	final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
    	if (defaultEnterAction != null) {
        	map.addActionForKeyStroke(enter, defaultEnterAction);
    	} else {
    		map.removeKeyStrokeBinding(enter);
    	}
    	final KeyStroke caretUp = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
    	if (defaultCaretUpAction != null) {
        	map.addActionForKeyStroke(caretUp, defaultCaretUpAction);
    	} else {
    		map.removeKeyStrokeBinding(caretUp);
    	}
    	final KeyStroke caretDown = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);
    	if (defaultCaretDownAction != null) {
        	map.addActionForKeyStroke(caretDown, defaultCaretDownAction);
    	} else {
    		map.removeKeyStrokeBinding(caretDown);
    	}
    }
    
    /**
     * Keymapping erweitern
     * das ist nur für Funktionen notwendig, die nicht über
     * die Menüs oder Toolbars erreichbar sind
     */
    private void updateEditorKeymap() {
        // bisherigen Keymappings laden
        final Keymap map = JTextComponent.addKeymap("Standard", editor.getKeymap()); 
        // in den nachfolgenden Zeilen werden KeyStrokes definiert und der geladenen
        // Keymap hinzuaddiert.
        // Hinzugefügte KeyStrokes überschreiben bereits vorhandene gleichartige KeyStrokes
        // hier die Neudefinition der Enter-Taste
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
        final TextAction insertFormatedBreak = new ExtEditorKit.InsertFormatedBreak();
        map.addActionForKeyStroke(enter, insertFormatedBreak);
        // ab hier die TAB-Taste
        final KeyStroke tabspace = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false);
        final TextAction insertTab = new ExtEditorKit.InsertTab();
        map.addActionForKeyStroke(tabspace, insertTab);
        // ab hier die TAB-Taste mit SHIFT
        final KeyStroke shiftTab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK, false);
        final TextAction moveBackTab = new ExtEditorKit.MoveBackTab();
        map.addActionForKeyStroke(shiftTab, moveBackTab);
        // springen zum Zeilenanfang
        final KeyStroke home = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0, false);
        final TextAction gotoLineStart = new ExtEditorKit.GotoLineStart();
        map.addActionForKeyStroke(home, gotoLineStart);
        // springen zum Zeilenende
        final KeyStroke end = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0, false);
        final TextAction gotoLineEnd = new ExtEditorKit.GotoLineEnd();
        map.addActionForKeyStroke(end, gotoLineEnd);
        // markieren bis zum Zeilenanfang
        final KeyStroke shiftHome = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.SHIFT_MASK, false);
        final TextAction selectUntilLineStart = new ExtEditorKit.SelectUntilLineStart();
        map.addActionForKeyStroke(shiftHome, selectUntilLineStart);
        // markieren bis zum Zeilenende
        final KeyStroke shiftEnd = KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.SHIFT_MASK, false);
        final TextAction selectUntilLineEnd = new ExtEditorKit.SelectUntilLineEnd();
        map.addActionForKeyStroke(shiftEnd, selectUntilLineEnd);
        // Zeile auskommentieren
        final KeyStroke f12 = KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0, false);
        final TextAction toggleLineComment = new ExtEditorKit.ToggleLineComment();
        map.addActionForKeyStroke(f12, toggleLineComment);
        final KeyStroke ctrl7 = KeyStroke.getKeyStroke(KeyEvent.VK_7, InputEvent.CTRL_MASK, false);
        map.addActionForKeyStroke(ctrl7, toggleLineComment);
        // springen zum Textanfang
        final KeyStroke crtlhome = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_MASK, false);
        final TextAction gotoDocTop = new ExtEditorKit.GotoDocTop();
        map.addActionForKeyStroke(crtlhome, gotoDocTop);
        // springen zum Textende
        final KeyStroke crtlEnd = KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_MASK, false);
        final TextAction gotoDocEnd = new ExtEditorKit.GotoDocEnd();
        map.addActionForKeyStroke(crtlEnd, gotoDocEnd);
        // markieren bis Textanfang
        final KeyStroke shiftCrtlHome = KeyStroke.getKeyStroke(
            KeyEvent.VK_HOME,
            (InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK),
            false);
        final TextAction selectUntilDocTop = new ExtEditorKit.SelectUntilDocTop();
        map.addActionForKeyStroke(shiftCrtlHome, selectUntilDocTop);
        // markieren bis Textende
        final KeyStroke shiftCrtlEnd = KeyStroke.getKeyStroke(
            KeyEvent.VK_END,
            (InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK),
            false);
        final TextAction selectUntilDocEnd = new ExtEditorKit.SelectUntilDocEnd();
        map.addActionForKeyStroke(shiftCrtlEnd, selectUntilDocEnd);
        // select current line
        final KeyStroke shiftCrtlA = KeyStroke.getKeyStroke(
                KeyEvent.VK_A,
                (InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK),
                false);
            map.addActionForKeyStroke(shiftCrtlA, new AbstractAction("selectCurrentBlock") {

    			private static final long serialVersionUID = 1L;

    			public void actionPerformed(ActionEvent e) {
					selectCurrentBlock();
    			}
            	
            });
        // Einfügen ein/aus
        final KeyStroke ins = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0, false);
        final TextAction setStatusOverWriteAction = new ExtEditorKit.SetStatusOverWriteAction();
        map.addActionForKeyStroke(ins, setStatusOverWriteAction);
        // Blockweise einrücken
        final KeyStroke ctrlL = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK, false);
        final TextAction moveOut = new ExtEditorKit.MoveOut();
        map.addActionForKeyStroke(ctrlL, moveOut);
        // Blockweise zurückrücken
        final KeyStroke ctrlJ = KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_MASK, false);
        final TextAction moveBack = new ExtEditorKit.MoveBack();
        map.addActionForKeyStroke(ctrlJ, moveBack);
        // change to upper case letters
        final KeyStroke altU = KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_MASK, false);
        final TextAction toUpperCase = new ExtEditorKit.ToUpperCase();
        map.addActionForKeyStroke(altU, toUpperCase);
        // change to lower case letters
        final KeyStroke altL = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK, false);
        final TextAction toLowerCase = new ExtEditorKit.ToLowerCase();
        map.addActionForKeyStroke(altL, toLowerCase);
        // set new default key action
        map.setDefaultAction(new ExtEditorKit.OverWriteDefaultKeyTyped());
        // show context menu
        final KeyStroke shiftF10 = KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_MASK, false);
        map.addActionForKeyStroke(shiftF10, new AbstractAction("contextMenu") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				createEditorContextMenu();
			}
        	
        });
        // show syntax chooser
        final KeyStroke ctrlSpace = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_MASK, false);
        map.addActionForKeyStroke(ctrlSpace, new AbstractAction("syntaxChooser") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				if (getSyntaxChooser().isVisible() == false) {
					showSyntaxChooser();
				}
			}
        	
        });
        // run current block as statement
        final KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK, false);
        map.addActionForKeyStroke(ctrlEnter, new AbstractAction("runBlock") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				if (database != null && database.isConnected()) {
					int start = editor.getSelectionStart();
					int end = editor.getSelectionEnd();
					if ((end - start) < 3) {
						selectCurrentBlock();
					}
					runScript();
				}
			}
        	
        });
        // close syntax chooser
		final KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        map.addActionForKeyStroke(esc, new AbstractAction("ESC") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				closeSyntaxChooser();
			}
        	
        });
        // set new keymap for editor
        editor.setKeymap(map);
        // show all keybindings in debug mode
        if (logger.isDebugEnabled()) {
        	logoutEditorKeyBindung();
        }
    }
    
    private void logoutEditorKeyBindung() {
    	Keymap map = editor.getKeymap();
        final Action[] actions = map.getBoundActions();
        final KeyStroke[] keys = map.getBoundKeyStrokes();
        logger.debug("MainFrame.updateKeymap: list of additional text actions:"); 
        for (int i = 0; i < actions.length; i++) {
            logger.debug("- name:" 
                + (actions[i]).getValue(Action.NAME) + ", enabled:" 
                + (actions[i]).isEnabled() + ", key:" 
                + (keys[i]).toString());
        }
    }

    public void closeSyntaxChooser() {
    	if (syntaxChooser != null) {
    		syntaxChooser.setVisible(false);
    		editor.requestFocusInWindow();
    	}
		updateEditorKeyMapRestoreToDefault();
    }
    
    private CodeCompletionAssistent getSyntaxChooser() {
    	if (syntaxChooser == null) {
    		syntaxChooser = new CodeCompletionAssistent(this);
    		syntaxChooser.setVisible(false);
    	}
    	return syntaxChooser;
    }
    
    private void createEditorContextMenu(int x, int y) {
        if (editor.hasFocus() == false) {
            editor.requestFocusInWindow();
        }
        final String selectedText = editor.getSelectedText();
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem mi = null;
        if (selectedText != null) {
            // wenn Text selektiert wurde
            // dann ein Kontextmenu öffnen
            // Menü anzeigen
            mi = new JMenuItem(Messages.getString("MainFrame.editorcontextmenuopeninnewwindow")); 
            mi.setActionCommand("open_file_in_new_window"); 
            mi.addActionListener(this);
            popup.add(mi);
            mi = new JMenuItem(Messages.getString("MainFrame.editorcontextmenuopeninnewwindowandask")); 
            mi.setActionCommand("ask_open_file_in_new_window"); 
            mi.addActionListener(this);
            popup.add(mi);
            popup.addSeparator();
            mi = new JMenuItem(Messages.getString("MainFrame.editorcontextmenucut")); 
            mi.setActionCommand("cut"); 
            mi.addActionListener(this);
            popup.add(mi);
            mi = new JMenuItem(Messages.getString("MainFrame.editorcontextmenucopy")); 
            mi.setActionCommand("copy"); 
            mi.addActionListener(this);
            popup.add(mi);
        }
        mi = new JMenuItem(Messages.getString("MainFrame.editcontextmenupaste")); 
        mi.setActionCommand("paste"); 
        mi.addActionListener(this);
        popup.add(mi);
        popup.addSeparator();
        // add useful text helper
        mi = new JMenuItem();
        mi.setText("to_date(...)");
        mi.setActionCommand("helper_to_date");
        mi.addActionListener(this);
        popup.add(mi);
        mi = new JMenuItem();
        mi.setText("sum(column) as column");
        mi.setActionCommand("helper_to_sum");
        mi.addActionListener(this);
        popup.add(mi);
        mi = new JMenuItem();
        mi.setText("max(column) as column");
        mi.setActionCommand("helper_to_max");
        mi.addActionListener(this);
        popup.add(mi);
        // sicherstellen, dass es noch vollständig am Bildschirm zu sehen ist
        final Dimension popupSize = popup.getPreferredSize();
        int yLoc_loc = 0;
        int xLoc_loc = 0;
        // Mausposition ist position bezogen auf Scrollpane!
        // absolute Mausposition errechnen
        final int absMouseYPos = y - editorScrollPane.getVerticalScrollBar().getValue();
        if (absMouseYPos + popupSize.height > editorScrollPane.getHeight()) {
            yLoc_loc = y - popupSize.height;
        } else {
            yLoc_loc = y;
        }
        final int absMouseXPos = x - editorScrollPane.getHorizontalScrollBar().getValue();
        if (absMouseXPos + popupSize.width > this.getSize().width) {
            xLoc_loc = x - popupSize.width;
        } else {
            xLoc_loc = x;
        }
        if (xLoc_loc < 0) {
            xLoc_loc = 0;
        }
        popup.show(editor, xLoc_loc, yLoc_loc);
    }

    private final class EditorMouseListener extends MouseAdapter implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent e) {
            if (textChangeListener != null) {
                textChangeListener.setEnableLineHighlighting(false);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // nop
        }

        @Override
        public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger()) {
                createEditorContextMenu(me);
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger()) {
                createEditorContextMenu(me);
            }
            if (textChangeListener != null) {
                textChangeListener.setEnableLineHighlighting(true);
            }
        }
    }

    private Action actionCopyTableContent = new AbstractAction(Messages.getString("MainFrame.menucopy")) {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			TransferHandler th = resultTable.getTransferHandler();
			if (th != null) {
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				th.exportToClipboard(resultTable, cb, TransferHandler.COPY);
			}
    	}
    };
    
    private Action actionInsertNewTableLineAsCopy = new AbstractAction(Messages.getString("MainFrame.tablecontextmenucreatenewlineascopy")) {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
            if (database.getNewRowIndex() == -1) {
                if (database.insertRowInTableAsCopy()) {
                    menuDBCreateNewRow.setEnabled(false);
                    resultTable.setRowSelectionInterval(database.getNewRowIndex(), database.getNewRowIndex());
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            tableScrollPane.getVerticalScrollBar().setValue(tableScrollPane.getVerticalScrollBar().getMaximum());
                        }
                    }); // muss verzögert ausgeführt werden !
                } else {
                    showInfoMessage(
                        Messages.getString("MainFrame.112"), 
                        Messages.getString("MainFrame.113")); 
                }
            }
    	}
    };

    private Action actionDeleteRowInTable = new AbstractAction(Messages.getString("MainFrame.tablecontextmenudeletedataset")) {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			int[] rows = resultTable.getSelectedRows();
			if (rows != null && rows.length > 0) {
				int answer = JOptionPane.OK_OPTION;
				if (rows.length > 1) {
					answer = JOptionPane.showConfirmDialog(MainFrame.this, Messages.getString("MainFrame.askForMoreRowToDelete1") + " " + rows.length + " " + Messages.getString("MainFrame.askForMoreRowToDelete2"), "Delete", JOptionPane.OK_CANCEL_OPTION);
				}
				if (answer == JOptionPane.OK_OPTION) {
	                database.deleteDataset(resultTable.getSelectedRows());
				}
			}
    	}
    };

    private void createTableContextMenu(MouseEvent e) {
        int mouseXPos = e.getX();
        int mouseYPos = e.getY();
        // die Tabelle hat ggf. nicht sofort den Focus,
        // dann zeigt sich die selektierte Zelle nicht!
        if (resultTable.hasFocus() == false) {
            resultTable.requestFocus();
        }
        // die Zelle unter der Maus selektieren
        final int row = resultTable.rowAtPoint(new Point(mouseXPos, mouseYPos));
        final int col = resultTable.columnAtPoint(new Point(mouseXPos, mouseYPos));
        if (database.isVerticalView() && col == 0) {
            createTableHeaderContextMenu(e);
        } else {
        	int selectedRows = resultTable.getSelectedRowCount();
            if ((row != -1) && (col != -1) && selectedRows < 2) {
                resultTable.changeSelection(row, col, false, false);
            }
            if (resultTable.getSelectedRow() != -1) {
                // nun Kontextmenu anzeigen
                final JPopupMenu popup = new JPopupMenu();
                JMenu menue = null;
                JMenuItem mi = null;
                if (selectedRows == 1) {
                    mi = new JMenuItem(Messages.getString("MainFrame.tablecontextmenuopenfield")); 
                    mi.setActionCommand("table_edit_cell"); 
                    mi.addActionListener(this);
                    mi.setFont(new Font((mi.getFont()).getFamily(), Font.BOLD, (mi.getFont()).getSize()));
                    popup.add(mi);
                    popup.addSeparator();
                }
                if (databaseIsBusy == false && database.isVerticalView() == false) {
                    // alles weitere nur wenn Datenbank nicht momentan beschäftigt
                	if (selectedRows == 1) {
                        mi = new JMenuItem(Messages.getString("MainFrame.tablecontextmenudeletefield")); 
                        mi.setActionCommand("table_delete_content"); 
                        mi.addActionListener(this);
                        popup.add(mi);
                        popup.addSeparator();
                	}
                    if (selectedRowIndex != database.getNewRowIndex()) {
                        mi = new JMenuItem(); 
                        mi.setAction(actionDeleteRowInTable);
                        popup.add(mi);
                        popup.addSeparator();
                        mi = new JMenuItem(); 
                        mi.setAction(actionCopyTableContent);
                        popup.add(mi);
                    }
                    if (database.isReferencingColumn(col) && selectedRows == 1) {
                        popup.addSeparator();
                        mi = new JMenuItem(Messages.getString("MainFrame.showreferenceddataset") 
                            + database.getReferencedColumn(col));
                        mi.setActionCommand("sik@" + String.valueOf(row) + "|" + String.valueOf(col));  
                        mi.addActionListener(this);
                        popup.add(mi);
                    }
                    if (database.isReferencedColumn(col) && selectedRows == 1) {
                        final String[] refColumns = database.getReferencingColumns(col);
                        popup.addSeparator();
                        menue = new JMenu(Messages.getString("MainFrame.showreferencingdatasets")); 
                        popup.add(menue);
                        for (int i = 0; i < refColumns.length; i++) {
                            mi = new JMenuItem(refColumns[i]);
                            mi.setActionCommand("sek@" 
                                + mi.getText() + "$" 
                                + String.valueOf(row) + "|" 
                                + String.valueOf(col));
                            mi.addActionListener(this);
                            menue.add(mi);
                        }
                    }
                    if (selectedRows < 2) {
                        if (selectedRowIndex == database.getNewRowIndex()) {
                            popup.addSeparator();
                            mi = new JMenuItem(Messages.getString("MainFrame.tablecontextappendnewlineindatabsde")); 
                            mi.setActionCommand("db_insert_row"); 
                            mi.addActionListener(this);
                            popup.add(mi);
                            mi = new JMenuItem(Messages.getString("MainFrame.tablecontextmenuremovenewline")); 
                            mi.setActionCommand("table_insert_row_cancel"); 
                            mi.addActionListener(this);
                            popup.add(mi);
                        } else if (database.getNewRowIndex() == -1) {
                            popup.addSeparator();
                            mi = new JMenuItem(Messages.getString("MainFrame.tablecontextmenucreatenewline")); 
                            mi.setActionCommand("table_insert_row"); 
                            mi.addActionListener(this);
                            popup.add(mi);
                            mi = new JMenuItem(); 
                            mi.setAction(actionInsertNewTableLineAsCopy);
                            popup.add(mi);
                        } 
                        mi = new JMenuItem(); 
                        mi.setText(Messages.getString("MainFrame.selectAllRows"));
                        mi.addActionListener(new ActionListener() {
                			
                			@Override
                			public void actionPerformed(ActionEvent e) {
                				selectAllRowsForColumn(col);
                			}

                        });
                        popup.add(mi);
                    }
                } // if (databaseIsBusy == false)
                // Menü anzeigen
                // sicherstellen, dass es noch vollständig am Bildschirm zu sehen ist
                final Dimension popupSize = popup.getPreferredSize();
                int yLoc_loc;
                int xLoc_loc;
                // Mausposition ist position bezogen auf Scrollpane!
                // absolute Mausposition errechnen
                final int absMouseYPos = mouseYPos - tableScrollPane.getVerticalScrollBar().getValue();
                if (absMouseYPos + popupSize.height > tableScrollPane.getHeight()) {
                    yLoc_loc = mouseYPos - popupSize.height;
                } else {
                    yLoc_loc = mouseYPos;
                }
                final int absMouseXPos = mouseXPos - tableScrollPane.getHorizontalScrollBar().getValue();
                if (absMouseXPos + popupSize.width > this.getSize().width) {
                    xLoc_loc = mouseXPos - popupSize.width;
                } else {
                    xLoc_loc = mouseXPos;
                }
                if (xLoc_loc < 0) {
                    xLoc_loc = 0;
                }
                popup.show(resultTable, xLoc_loc, yLoc_loc);
            } // if (resultTable.getSelectedRow() != -1)
        }
    }

    /**
     * eigener MouseAdapter mit der Fähigkeit Doppelklick zu erkennen
     */
    private final class TableMouseListener extends MouseAdapter implements MouseMotionListener {

        private boolean draggStarted = false;

        // hier Reaktion auf Doppelklick festlegen
        protected void fireDoubleClickPerformed(MouseEvent me) {
            // eine Fehlermeldung lesen und in einem separaten Dialog anzeigen
            int row = resultTable.getSelectedRow();
            // z.B. nach dem löschen einer Zeile kann es sein, dass noch keine
            // Zeile selektiert ist und dann bringt resultTable.getSelectedRow() = -1 !
            if (row != -1) {
                if (database.isVerticalView()) {
                    int col = resultTable.getSelectedColumn();
                    if (col > 0) {
                        showEditor(row, col);
                    } else {
                        insertOrReplaceText(database.getColumnNameByLogicalColIndex(database.convertFromVerticalToLogicalColIndex(row)));
                    }
                } else {
                    showEditor(row, resultTable.getSelectedColumn());
                }
            }
        }

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

        @Override
        public void mousePressed(MouseEvent me) {
            if (database.isShowingResultSet() && me.isPopupTrigger()) {
                createTableContextMenu(me);
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            draggStarted = false;
            if (database.isShowingResultSet() && me.isPopupTrigger()) {
                createTableContextMenu(me);
            }
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.getClickCount() == 2 && me.isPopupTrigger() == false) {
                fireDoubleClickPerformed(me);
            } else if (me.getModifiers() == InputEvent.BUTTON1_MASK) {
                setTableSelectionToIntervalMode(false);
            }
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            if (draggStarted == false) {
                resultTable.clearSelection();
                setTableSelectionToIntervalMode(true);
                draggStarted = true;
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {
        }
    }
    
    private void selectAllRowsForColumn(int col) {
    	setTableSelectionToIntervalMode(true);
    	resultTable.changeSelection(0, col, false, false);
    	resultTable.changeSelection(resultTable.getRowCount() - 1, col, false, true);
    }

    private void createTableHeaderContextMenu(MouseEvent e) {
        // nun Kontextmenu anzeigen
        int mouseXPos = e.getX();
        int mouseYPos = e.getY();
        final int col;
        if (database.isVerticalView()) {
            col = database.convertFromVerticalToLogicalColIndex(resultTable.rowAtPoint(new Point(mouseXPos, mouseYPos)));
        } else {
            col = resultTable.columnAtPoint(new Point(mouseXPos, mouseYPos));
        }
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem mi = new JMenuItem(Messages.getString("MainFrame.togglecolumnaspk")); 
        mi.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                togglePk(col);
            }
        });
        popup.add(mi);
        if (database.enabledForSorting(col)) {
            popup.addSeparator();
            mi = new JMenuItem(Messages.getString("MainFrame.sortbycolumnintern")); 
            mi.setActionCommand("sai_" + String.valueOf(col)); 
            mi.addActionListener(this);
            popup.add(mi);
            mi = new JMenuItem(Messages.getString("MainFrame.sortbythiscolumnnewselect")); 
            mi.setActionCommand("saea_" + String.valueOf(col)); 
            mi.addActionListener(this);
            popup.add(mi);
            mi = new JMenuItem(Messages.getString("MainFrame.255")); 
            mi.setActionCommand("saee_" + String.valueOf(col)); 
            mi.addActionListener(this);
            popup.add(mi);
        }
        mi = new JMenuItem(); 
        mi.setText("Select All Rows");
        mi.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectAllRowsForColumn(col);
			}

        });
        popup.add(mi);
        // Menü anzeigen
        // sicherstellen, dass es noch vollständig am Bildschirm zu sehen ist
        final Dimension popupSize = popup.getPreferredSize();
        int xLoc_loc;
        // Mausposition ist position bezogen auf Scrollpane!
        // absolute Mausposition errechnen
        final int absMouseXPos = mouseXPos - (tableScrollPane.getHorizontalScrollBar()).getValue();
        if (absMouseXPos + popupSize.width > this.getSize().width) {
            xLoc_loc = mouseXPos - popupSize.width;
        } else {
            xLoc_loc = mouseXPos;
        }
        if (xLoc_loc < 0) {
            xLoc_loc = 0;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("MainFrame: create table header context menue at " + xLoc_loc + ":" + mouseYPos);  
        }
        popup.show((Component) e.getSource(), xLoc_loc, mouseYPos);
    }

    public static void setDateFormatMask(String dateFormatMask) {
        MainFrame.dateFormatMask = dateFormatMask;
    }

    public static String getDateFormatMask() {
        return dateFormatMask;
    }

    public static void setFkNavigationFrameMode(int fkNavigationFrameMode) {
        MainFrame.fkNavigationFrameMode = fkNavigationFrameMode;
    }

    public static int getFkNavigationFrameMode() {
        return fkNavigationFrameMode;
    }

    private final class TableHeaderMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent me) {
        }

        @Override
        public void mouseExited(MouseEvent me) {
        }

        @Override
        public void mousePressed(MouseEvent me) {
            if (database.isVerticalView() == false) {
                if (me.isPopupTrigger()) {
                    createTableHeaderContextMenu(me);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (database.isVerticalView() == false) {
                if (me.isPopupTrigger()) {
                    createTableHeaderContextMenu(me);
                } else {
                    processTogglePkClick(me);
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent me) {
            if (database.isVerticalView() == false) {
                if ((!me.isControlDown()) && (me.getClickCount() == 2)) {
                    fireDoubleClickPerformed(me);
                }
            }
        }

        private void processTogglePkClick(MouseEvent me) {
            if (me.isControlDown()) {
                final int columnIndex = ((JTableHeader) me.getSource()).columnAtPoint(me.getPoint());
                if ((database.getColumnTypename(columnIndex).equals("CLOB") 
                    || database.getColumnTypename(columnIndex).equals("BLOB") 
                    || database.getColumnTypename(columnIndex).indexOf("UNKNOWN") != -1) 
                    || database.getColumnTypename(columnIndex).equals("LONGVARCHAR") 
                    || database.getColumnTypename(columnIndex).equals("LONGVARBINARY")) { 
                    showErrorMessage(
                        Messages.getString("MainFrame.errorfiledtypenotuserfuleforpk"), 
                        Messages.getString("MainFrame.errormessageinvalidfieldtypeforpktitle")); 
                } else {
                    database.togglePrimaryKey(columnIndex);
                    ((JTableHeader) me.getSource()).repaint();
                }
            }
        }

        protected void fireDoubleClickPerformed(MouseEvent me) {
            // eine Fehlermeldung lesen und in einem separaten Dialog anzeigen
            insertOrReplaceText(database.getColumnName(((JTableHeader) me.getSource()).columnAtPoint(me.getPoint())));
        }
    }

    /**
     * Renderer für die Tabelle --> Strings
     */
    private class ResultTableCellRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;
        private final Color colorOdd = new Color(240, 255, 240);
        private final Color colorEven = new Color(255, 255, 225);
        private final Color greyOdd = new Color(235, 235, 235);
        private final Color greyEven = new Color(240, 240, 240);
        private final Color colorHeader = new Color(220, 220, 220);
        private static final String STAR_SPACE = "* "; 
        private String dateFormatMask = null;
        private Font monospacedFont;
        private Font defaultFont;
        
        public ResultTableCellRenderer() {
            super();
            setOpaque(true); 
            monospacedFont = new Font("Monospaced", Font.PLAIN, getFont().getSize());
        }

        public void setDateFormatMask(String mask) {
            this.dateFormatMask = mask;
        }

        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
            String content = null;
            if (value instanceof java.util.Date) {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormatMask);
                content = sdf.format((java.util.Date) value);
            } else if (value instanceof byte[]) {
            	byte[] byteArray = (byte[]) value;
            	StringBuilder sb = new StringBuilder(byteArray.length * 2);
            	for (byte b : byteArray) {
            		sb.append(String.format("%02X ", b));
            	}
            	content = sb.toString();
            } else {
                if (value != null) {
                    content = String.valueOf(value);
                } else {
                    content = null; 
                }
            }
            if (defaultFont == null) {
            	defaultFont = table.getFont();
            }
            if (useMonospacedFont) {
            	setFont(monospacedFont);
            } else {
            	setFont(defaultFont);
            }
            if (row == database.getNewRowIndex()) {
                setText(STAR_SPACE + (content != null ? content : "")); 
            } else {
                setText(content != null ? content : ""); 
            }
            if (isSelected) {
                if (database.isVerticalView() && column == 0) {
                    // in der verticalDarstellung wird die Spalte gerendert
                    if (database.isPrimaryKey(database.convertFromVerticalToLogicalColIndex(row))) {
                        setBackground(Color.yellow);
                    } else {
                        setBackground(colorHeader);
                    }
                    setForeground(Color.black);
                } else {
                    if (hasFocus) {
                        setForeground(Color.black);
                        setBackground(Color.white);
                    } else {
                        setForeground(table.getSelectionForeground());
                        setBackground(table.getSelectionBackground());
                    }
                }
            } else {
                int x = 0;
                if (database.isVerticalView()) {
                    if (column == 0) {
                        if (database.isPrimaryKey(database.convertFromVerticalToLogicalColIndex(row))) {
                            setBackground(Color.yellow);
                        } else {
                            setBackground(colorHeader);
                        }
                        setForeground(Color.black);
                    } else {
                        int verticalRowIndex = row / database.getLogicalColumnCount();
                        setForeground(table.getForeground());
                        x = (verticalRowIndex >> 1); // muss bei ungeraden Zahlen zu einem Verlust des Restes Fähren
                        // denn keine gebrochenen Zahlen möglich
                        if (content != null) {
                            if ((x << 1) != verticalRowIndex) {
                                setBackground(colorOdd);
                            } else {
                                setBackground(colorEven);
                            }
                        } else {
                            if ((x << 1) != verticalRowIndex) {
                                setBackground(greyOdd);
                            } else {
                                setBackground(greyEven);
                            }
                        }
                    }
                } else {
                    setForeground(table.getForeground());
                    x = (row >> 1); // muss bei ungeraden Zahlen zu einem Verlust des Restes Fähren
                    if (content != null) {
                        if ((x << 1) != row) {
                            setBackground(colorOdd);
                        } else {
                            setBackground(colorEven);
                        }
                    } else {
                        if ((x << 1) != row) {
                            setBackground(greyOdd);
                        } else {
                            setBackground(greyEven);
                        }
                    }
                }
            }
            if (hasFocus) {
                if (database.isVerticalView() && column == 0) {
                    setBorder(BorderFactory.createRaisedBevelBorder());
                } else {
                    setBorder(BorderFactory.createLineBorder(Color.black));
                }
            } else {
                setBorder(null);
            }
            return this;
        }
    }
    
    private void openTextFileConverterFrame() {
		TextFileConverterFrame conv = new TextFileConverterFrame();
		conv.setVisible(true);
		WindowHelper.locateWindowAtMiddle(this,	conv);
    }
    
    private void openDateTool() {
    	System.out.println("Open date tool...");
		DateConverter conv = new DateConverter();
		conv.setVisible(true);
		WindowHelper.locateWindowAtMiddle(this,	conv);
    }
    
    private void openTalendSchemaConverterFrame() {
		SchemaImportFrame conv = new SchemaImportFrame();
		conv.setVisible(true);
		WindowHelper.locateWindowAtMiddle(this,	conv);
    }
    
    private void exportResultTableAsTalendSchema() {
    	SQLTable table = null;
		try {
			table = database.getSQLTableFromCurrentQuery();
		} catch (SQLException e) {
			logger.error("Get metadata from current query failed:" + e.getMessage(), e);
			JOptionPane.showMessageDialog(MainFrame.this, "Get metadata from current query failed:" + e.getMessage());
		}
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
            final int returnVal = chooser.showSaveDialog(MainFrame.this);
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
					JOptionPane.showMessageDialog(MainFrame.this, "writeSchemaFile f=" + f.getAbsolutePath() + " failed: " + e1.getMessage());
				}
            }
        }

    }
    
}