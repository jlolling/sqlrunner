package sqlrunner;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import dbtools.ConnectionDescription;
import dbtools.DatabaseSessionPool;
import dbtools.DatabaseType;
import sqlrunner.datamodel.SQLField;
import sqlrunner.log4jpanel.LogPanel;
import sqlrunner.swinghelper.WindowHelper;

/**
 * Startklasse der Applikation
 * liest alle Voreinstellungen ein und verwaltet applikationsweite Einstellungen
 * @author Jan Lolling
 */
public final class Main {
    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(Main.class);

    static String                       script                     = null;
    static String                       programDirectory           = null;
    static String                       cfgFileName                = null;
    static String                       userCfgFileName            = null;
    static String						contextCfgFileName 		   = null;
    private static String               highlighterFontCfgFileName = null;
    static String                       dbCfgFileName              = null;
    static String                       adminCfgFileName           = null;
    private static String               sqlHistoryFileName         = null;
    static String                       connectionListFileName     = null;
    static int                          x                          = 9999;
    static int                          xt                         = 9999;
    static int                          y                          = 0;
    static int                          b                          = 0;
    static int                          h                          = 0;
    static int                          yt                         = 0;
    static int                          bt                         = 0;
    static int                          ht                         = 0;
    static boolean                      editorWindowParameterGiven = false;
    static boolean                      textWindowParameterGiven   = false;
    static final int                    MIN_WIDTH                  = 800;
    static final int                    MIN_HEIGHT                 = 200;
    static final int                    DEF_WIDTH                  = 800;
    static final int                    DEF_HEIGHT                 = 600;
    static final int                    DEF_XLOC                   = 0;
    static final int                    DEF_YLOC                   = 0;
    public static String                VERSION                    = null;
    public static final String          CFG_FILE_NAME              = "default.cfg";
    public static final String          USER_CFG_FILE_NAME         = "user.cfg";
    public static final String          HIGHLIGHTER_FONT_CFG_FILE  = "highlighter.cfg";
    public static final String          KEYWORD_FILE               = "wordfile.txt";
    public static final String          DB_CFG_FILE_NAME           = "dbtypes.cfg";
    public static final String          DB_CFG_FILE_NAME_USER      = "user_dbtypes.cfg";
    public static final String          ADMIN_CFG_FILE_NAME        = "dbadmin.cfg";
    static final String                 SQL_HISTORY_FILE           = "sqlhistory.ini";
    static final String                 CONNECTION_LIST_FILE       = "connection.lst";
    static final String                 LOOK_AND_FEEL_CFG_FILE     = "lookandfeel.properties";
    static final String                 CONTEXT_CFG_FILE           = "talend_context.properties";
    static final String                 WORKING_DIR                = ".sqlrunner";
    static final int                    MAX_FILES_ENTRIES          = 10;
    static int                          maxFilenameLength          = 0;
    static private final Properties     defaultProp                = new Properties();
    static private final Properties     userprop                   = new Properties();
    static public final Properties      lookAndFeels               = new Properties();
    static public final String          FILE_SEPARATOR             = System.getProperty("file.separator");
    static public final String          LINE_FEED                  = System.getProperty("line.separator");
    public static Font                  textFont                   = null;
    static public final int             CARET_BLINK_RATE           = 500;
    static GregorianCalendar            cal                        = new GregorianCalendar();
    static int                          heliosStatusHeight         = 0;
    static String                       currentLookAndFeelAlias    = null;
    private static final List<MainFrame>         listMainFrames    = new ArrayList<MainFrame>();
    static String                       codeBase                   = null;
    static String                       helpUrl                    = null;
    static public final Color           info                       = new Color(250, 250, 220);
    static private String               loginParamsAsURLElements   = null;
    static public ConnectionDescription startUpCd                  = null;
    static public String                currentCharSet             = null;
    static public String                currentLineSeparatorType   = null;
    static public String                currentLineSeparator       = null;
    private static Properties           log4jProperties            = null;
    private static String[]             commandLineArguments       = null;
    private static String fileToLoad;
    private static boolean initializationFinished = false;
    public static final String OS_WINDOWS = "Windows";
    public static final String OS_MACOSX = "Mac OS X";
    public static final String OS_UNIX = "Unix";
    private static String osType = null;
    private static String customSqlTypeKeyMap = "customSqlTypeMap";
    private static String customSqlTypeMappingEnabled = "customSqlTypeEnabled";
    public static boolean useNativeFileDialog = true;
    
    private static void setupCustomSqlTypeMapping() {
    	 boolean enabled = Boolean.parseBoolean(getUserProperty(customSqlTypeMappingEnabled, "false"));
    	 if (enabled) {
    		 String map = getUserProperty(customSqlTypeKeyMap, "");
    		 try {
    			 logger.info("setCustomSqlTypeMapping");
				 SQLField.setCustomSqlTypeMapping(map);
				 Set<Map.Entry<Integer, Integer>> set = SQLField.getCustomTypeMap();
				 for (Map.Entry<Integer, Integer> entry : set) {
					 logger.info("sql type " + entry.getKey() + "->" + SQLField.getBasicTypeName(entry.getValue()));
				 }
			} catch (Exception e) {
				logger.error("setupCustomSqlTypeMapping failed:" + e.getMessage());
			}
    	}
    }
    
    public static boolean startupFinished() {
    	return initializationFinished;
    }
    
    public static void setStartupFile(String file) {
    	logger.info("setStartupFile: " + file);
    	fileToLoad = file;
    }
    
    public static String getStartupFile() {
    	return fileToLoad;
    }
    
    public static String getCodebase() {
    	return codeBase;
    }

    public static String getWorkDirectory() {
        return programDirectory;
    }

    public static String getFileNameForConnectionList() {
        return connectionListFileName;
    }

    public static String getCfgFileName() {
        return cfgFileName;
    }

    public static String getDbCfgFileName() {
        return dbCfgFileName;
    }

    public static String getAdminCfgFileName() {
        return adminCfgFileName;
    }
    
    static void setupProgramDirectories() {
        try {
            createWorkingDir();
            programDirectory = System.getProperty("user.home") + FILE_SEPARATOR + WORKING_DIR + FILE_SEPARATOR;
        } catch (java.security.AccessControlException ace) {
            programDirectory = ".";
        }
        adminCfgFileName = programDirectory + ADMIN_CFG_FILE_NAME;
        cfgFileName = programDirectory + CFG_FILE_NAME;
        contextCfgFileName = programDirectory + CONTEXT_CFG_FILE;
        connectionListFileName = programDirectory + CONNECTION_LIST_FILE;
        dbCfgFileName = programDirectory + DB_CFG_FILE_NAME_USER;
        setSqlHistoryFileName(programDirectory + SQL_HISTORY_FILE);
        setHighlighterFontCfgFileName(programDirectory + HIGHLIGHTER_FONT_CFG_FILE);
        userCfgFileName = programDirectory + USER_CFG_FILE_NAME;
    }

    static void createWorkingDir() throws java.security.AccessControlException {
        final File dir = new File(System.getProperty("user.home") + FILE_SEPARATOR + WORKING_DIR);
        if (!(dir.exists() && dir.isDirectory())) {
            if (dir.mkdir() == false) {
            	logger.error("couldn't create working directory " + dir.getAbsolutePath());
            }
        }
    }

    static public void setFileEncoding(String codeName) {
        currentCharSet = codeName;
        userprop.setProperty("CURR_CHAR_SET", codeName);
    }
    
    public static final String getFileEnoding() {
        return currentCharSet;
    }

    public static boolean isLineHighlightingEnabled() {
        return Boolean.parseBoolean(Main.getUserProperty("LINE_HIGHLIGHTING", "true"));
    }

    public static void enableLineHighlighting(boolean enable) {
        Main.setUserProperty("LINE_HIGHLIGHTING", String.valueOf(enable));
    }
    
    public static boolean isBracketContentHighlightingEnabled() {
        return Boolean.parseBoolean(Main.getUserProperty("BRACKET_CONTENT_HIGHLIGHTING", "false"));
    }

    public static void enableBracketContentHighlighting(boolean enable) {
        Main.setUserProperty("BRACKET_CONTENT_HIGHLIGHTING", String.valueOf(enable));
    }

    public static final void setUserProperty(String key, String value) {
    	if (value != null) {
            userprop.put(key, value);
    	} else {
            userprop.remove(key);
    	}
    }
    
    public static final String getUserProperty(String key, String defaultValue) {
        return userprop.getProperty(key, defaultValue);
    } 

    public static final String getUserProperty(String key) {
        return userprop.getProperty(key);
    } 
    
    public static final String getDefaultProperty(String key, String defaultValue) {
        return userprop.getProperty(key, defaultValue);
    } 

    public static final void setDefaultProperty(String key, String value) {
    	if (value != null) {
            defaultProp.setProperty(key, value);
    	} else {
    		defaultProp.remove(key);
    	}
    } 

    public static final String getDefaultProperty(String key) {
        return defaultProp.getProperty(key);
    } 

    public static String getCurrentLineSeparator() {
        return currentLineSeparator;
    }

    /**
     * setzt den Typ des Zeilenumbruchs
     * @param osName anhand des Names des Betriebssystemes
     */
    static public void setLineSeparatorType(String osName) {
        osName = (osName.toLowerCase()).trim();
        if (osName.indexOf("windows") != -1) {
            currentLineSeparatorType = "Windows";
            currentLineSeparator = "\r\n";
        } else if (osName.indexOf("mac") != -1) {
            currentLineSeparatorType = "Mac OS (classic)";
            currentLineSeparator = "\r";
        } else {
            currentLineSeparatorType = "UNIX";
            currentLineSeparator = "\n";
        }
        userprop.setProperty("CURR_LINE_SEPARATOR", currentLineSeparatorType);
    }

    /**
     * initialisiert das Hauptfenster
     * zuvor wird noch geprüft, ob der Editor geladen werden soll durch das Flag showEditor=true
     * ist das Property SYNTAX_HIGHLIGHT_ENABLED=true wird vor den Editor noch der Scanner initialisiert
     * Diese Reihenfolge ist erforderlich um ggf. eine beim Start zu ladenende Datei gleich
     * mit Syntaxhervorhebung darstellen zu können.
     * Am Ende der Methode wird das Startfenster entfernt.
     */
    static void initialize() {
        logger.info("start initialisation...");
        logger.info("register pool at platform JMX server...");
        try {
            DatabaseSessionPool.registerAtPlatformJMXServer();
        } catch (Exception e) {
            logger.warn("register failed: " + e.getMessage(), e);
        }
        try {
            WindowHelper.init();
            loadVersion();
            // Standardwerte laden
            loadDefaultProperties();
            // User-Werte laden
            loadUserProperties();
            // jetzt stehen die Properties für die Font-Einstellungen zur Verfügung
            setupEditorFont();
            // look and feels laden
            loadLookAndFeels();
            setupCustomSqlTypeMapping();
            loadContextProperties();
            // Zeilenumbruch konfigurieren
            String osName = userprop.getProperty("CURR_LINE_SEPARATOR");
            if (osName == null) {
                osName = System.getProperty("os.name");
            }
            setLineSeparatorType(osName);
            // File encoding initial setzen
            final String currentCharSet_loc = Main.userprop.getProperty("CURR_CHAR_SET");
            final String systemCharSet = System.getProperty("file.encoding");
            if (currentCharSet_loc != null) {
                setFileEncoding(currentCharSet_loc);
            } else if (systemCharSet != null) {
                setFileEncoding(systemCharSet);
            }
            // Fensterpositionierung 
            WindowHelper.enableWindowPositioning((userprop.getProperty("WM_FRAME_POSITIONING", "false")).equals("false"));
            // teste ob Scannerkonfiguration vorhanden
            // nur dann wenn auch erwünscht
            // testen ob WindowParameter mitgegeben worden
            if (editorWindowParameterGiven == false) {
                // nachsehen in den user-properties ob dort letzte Werte vorhanden
                x = Integer.parseInt(userprop.getProperty("WINDOW_X_POS", "-1"));
                y = Integer.parseInt(userprop.getProperty("WINDOW_Y_POS", "-1"));
                h = Integer.parseInt(userprop.getProperty("WINDOW_HEIGHT", "-1"));
                b = Integer.parseInt(userprop.getProperty("WINDOW_WIDTH", "-1"));
                if ((x == -1) || (y == -1) || (h == -1) || (b == -1)) {
                    // wenn nicht, dann Defaultwerte setzen
                    final Rectangle r = createDefaultWindowParameters();
                    if (WindowHelper.isWindowPositioningEnabled()) {
                        x = r.x;
                        y = r.y;
                    } else {
                        x = 0;
                        y = 0;
                    }
                    b = r.width;
                    h = r.height;
                }
            }
            // Helios-Statusleiste berücksichtigen
            try {
                heliosStatusHeight = Integer.parseInt(Main.defaultProp.getProperty("HELIOS_STATUS_HEIGHT", "30"));
            } catch (NumberFormatException nfe) {
                if (logger.isDebugEnabled()) {
                    logger.debug("invalid value in key HELIOS_STATUS_HEIGHT");
                }
                heliosStatusHeight = 30;
            }
            // look & feel setzen
            // fk-frame-mode setzen
            MainFrame.setFkNavigationFrameMode(Integer.parseInt(userprop.getProperty(
                    "FK_NAVIGATION_MODE",
                    String.valueOf(MainFrame.FK_NAVIGATION_LAST_FRAME))));
            // nur wenn erforderlich den Scanner instanzieren und initialisieren
            // die Scanner-relevanten Funktionen testen ob (lexer != null)
            // oder richten sich ebenfalls nach dem folgenden Property
            if (defaultProp.getProperty("SYNTAX_HIGHLIGHT_ENABLED", "false").equals("true")) {
                final File f = new File(getHighlighterFontCfgFileName());
                if (!f.exists()) {
                    createDefaultHLCfgFile();
                }
                // sollte bei der Inatzierung ein Fehler auftreten, so
                // wird in der Klasse Scanner das Property
            } else {
                userprop.setProperty("SYNTAX_HIGHLIGHT", "false");
            }
            // MainFrame instanzieren und Konstruktor aufrufen
            logger.info("Start GUI...");
            final MainFrame mainFrame = createInstance(null);
            currentLookAndFeelAlias = userprop.getProperty("LOOK&FEEL", "System");
            setLookAndFeel(currentLookAndFeelAlias);
            // da mainFrame nicht modal geht es hier weiter !!
            // Datenbankkonfiguration prüfen auf Vorhandensein
            // ggf aus Archiv extrahieren
            // Admin-SQLs einlesen
            final File fdbc = new File(dbCfgFileName);
            if (!fdbc.exists()) {
                createDefaultDbCfgFile();
            }
            // Datenbanktypen einlesen
            // die Typen aus dem Archiv lesen
            setDefaultDatabaseTypes();
            // die selbstdefinierten User-Typen lesen
            setUserDatabaseTypes(fdbc);
            // Admin-SQLs prüfen und ggf aus Archiv neu erstellen
            final File fac = new File(adminCfgFileName);
            if (!fac.exists()) {
                createDefaultAdminCfgFile();
            }
            AdminStatement.readAdminSQLs(fac);
            try {
                final BasicService bs = ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService"));
                codeBase = bs.getCodeBase().toExternalForm();
                helpUrl = codeBase;
                if (logger.isDebugEnabled()) {
                    logger.debug("running as web-start application: use " + codeBase + " for codebase");
                }
            } catch (UnavailableServiceException use) {
                codeBase = System.getProperty("user.dir");
                helpUrl = "file://" + codeBase;
                if (logger.isDebugEnabled()) {
                    logger.debug("running as standalone application: use " + codeBase + " for codebase");
                }
            }
            if (loginParamsAsURLElements != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("login at start...");
                }
                mainFrame.openLoginDialog(new ConnectionDescription(loginParamsAsURLElements));
            } else if (startUpCd != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("login at start...");
                }
                mainFrame.startConnect(startUpCd);
            }
            if (fileToLoad != null) {
            	mainFrame.handleFile(fileToLoad);
            	fileToLoad = null;
            }
        } catch (java.security.AccessControlException ae) {
            if (logger.isDebugEnabled()) {
                logger.debug("running in sandbox: " + ae.getMessage());
            }
        }
        logger.info("initializing complete.");
        initializationFinished = true;
    }
    
    public static long getLastModified(File file) {
        return getLastModified(file.getAbsolutePath());
    }

    public static long getLastModified(String file) {
        File f = new File(file);
        if (f.exists()) {
            return f.lastModified();
        } else {
            return 0;
        }
    }

    static public void setLookAndFeel(String alias) {
    	 // continuous layout on frame resize
        Toolkit.getDefaultToolkit().setDynamicLayout(true);
        // no flickering on resize
        System.setProperty("sun.awt.noerasebackground", "true");
        // verhindert unter Mac OS X dass Fenster im nicht sichtbaren Bereich geöffnet werden können
        System.setProperty("apple.awt.window.position.forceSaveCreation", "true");
        // verhindert, das Fenster programatich aus dem sichtbaren Bereich herausgeschoben werden können
        System.setProperty("apple.awt.window.position.forceSaveProgrammaticPositioning", "true");
        // verhindert, das Fenster vom User aus dem sichtbaren Bereich herausgeschoben werden können
        System.setProperty("apple.awt.window.position.forceSaveUserPositioning", "true");
    	final String className = lookAndFeels.getProperty(alias, UIManager.getSystemLookAndFeelClassName());
        try {
            UIManager.put("JFileChooser.packageIsTraversable", "nerver");
            UIManager.setLookAndFeel(className);
            currentLookAndFeelAlias = alias;
            Main.userprop.setProperty("LOOK&FEEL", alias);
            // Sprachanpassung
            // setzen der Texte für Standard-Dialoge/Button
            MainFrame frame;
            for (int i = 0; i < listMainFrames.size(); i++) {
                frame = listMainFrames.get(i);
                frame.updateUI();
            }
            frame = null;
            if (MainFrame.sqlHistory != null) {
                SwingUtilities.updateComponentTreeUI(MainFrame.sqlHistory);
            }
            if (MainFrame.dmFrame != null) {
                SwingUtilities.updateComponentTreeUI(MainFrame.dmFrame);
            }
            SwingUtilities.updateComponentTreeUI(LogPanel.getInstance());
        } catch (Throwable e) {
            logger.warn("setLookAndFeel: error in initialization UIManager:" + e.toString());
        }
    }

    static public String getCurrentLookAndFeelAlias() {
        return currentLookAndFeelAlias;
    }

    static private void setDefaultDatabaseTypes() {
        final BufferedReader br = getTextResource(DB_CFG_FILE_NAME);
        String paramStr;
        try {
            while ((paramStr = br.readLine()) != null) {
                paramStr = paramStr.trim();
                if ((paramStr.startsWith("#") == false) && (paramStr.length() > 8)) {
                    ConnectionDescription.getDBTypes().add(new DatabaseType(paramStr));
                }
            }
            br.close();
        } catch (IOException ioe) {
            logger.warn("setDefaultDatabaseTypes:" + ioe.toString() + " exception: " + ioe);
        }
    }

    /**
     * read DatabaseTypes from File
     * wird aus Main.initialize() aufgerufen
     * @param File contains the descriptions
     */
    static private synchronized void setUserDatabaseTypes(File f) {
        String paramStr;
        DatabaseType dbType;
        try {
            final BufferedReader br = new BufferedReader(new FileReader(f));
            while ((paramStr = br.readLine()) != null) {
                paramStr = paramStr.trim();
                if ((paramStr.startsWith("#") == false) && (paramStr.length() > 8)) {
                    dbType = new DatabaseType(paramStr);
                    if (!alreadyExists(dbType)) {
                        ConnectionDescription.getDBTypes().add(dbType);
                    } else {
                        logger.warn("user-db-type ignored: id="
                                + String.valueOf(dbType.getId())
                                + " is already used by default-type! -  : exception: "
                                + null);
                    }
                }
            }
            br.close();
        } catch (IOException ioe) {
            logger.warn("setUserDatabaseTypes:" + ioe.toString() + " exception: " + ioe);
        }
    }

    static private boolean alreadyExists(DatabaseType testType) {
        boolean exists = false;
        for (int i = 0; i < ConnectionDescription.getDBTypes().size(); i++) {
            if (testType.getId() == ConnectionDescription.getDBTypes().get(i).getId()) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    /**
     * lookup a window which has loaded the same file.
     * @param f
     * @return
     */
    public static MainFrame findMainFrameByFile(File f) {
        MainFrame mfWithSameFile = null;
        for (MainFrame mf : listMainFrames) {
            if (mf.getCurrentFile() != null && mf.getCurrentFile().equals(f)) {
                mfWithSameFile = mf;
                break;
            }
        }
        return mfWithSameFile;
    }
    
    /**
     * erzeugt eine neue Instanz von MainFrame und initialisiert diese
     * auf Basis der aktuellen Instanz von MainFrame
     */
    static public synchronized MainFrame createInstance(MainFrame currentFrame) {
        if (currentFrame != null) {
            x = currentFrame.getLocation().x;
            y = currentFrame.getLocation().y + 20;
            b = currentFrame.getWidth();
            h = currentFrame.getHeight();
            refreshUserProperties(currentFrame);
        }
        final MainFrame mainFrame = new MainFrame(x, y, b, h);
        // die als Parameter mitgegebene Datei laden
        if (currentFrame == null) {
            if ((script != null) && (script.trim().length() > 0)) {
                mainFrame.handleFile(script);
            }
        } else {
            if ((currentFrame.getDatabase() != null) && currentFrame.isConnected()) {
                mainFrame.startConnect(
                		currentFrame.getDatabase().getDatabaseSession().getConnectionDescription());
            }
        }
        // Frames validieren, die eine voreingestellte Grösse besitzen
        if (SwingUtilities.isEventDispatchThread()) {
            mainFrame.validate();
            mainFrame.setVisible(true); // Frame sichtbar machen
            mainFrame.toFront();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
				public void run() {
                    mainFrame.validate();
                    mainFrame.setVisible(true); // Frame sichtbar machen
                    mainFrame.toFront();
                }
            });
        }
        listMainFrames.add(mainFrame);
        return mainFrame;
    }

    public static MainFrame getFrameForFKNavigation(MainFrame lastFrame) {
        MainFrame frame = null;
        boolean foundUsefulFrame = false;
        for (int i = 0; i < listMainFrames.size(); i++) {
            frame = listMainFrames.get(i);
            if (frame.isUsedForFKNavigation()) {
                foundUsefulFrame = true;
                break;
            }
        }
        if (!foundUsefulFrame) {
            frame = createInstance(lastFrame);
            frame.setUseForFKNavigation(true);
        }
        return frame;
    }

    /**
     * löscht ein Fenster aus der Fensterliste
     * @param mainFrame - zu löschendes Referenz
     */
    public static void removeInstance(MainFrame mainFrame) {
        listMainFrames.remove(mainFrame);
    }

    /**
     * hier werden die Properties aktualisiert, die nicht bereits durch Bedienhandlung
     * selbst aktuell gehalten werden.
     */
    public static void refreshUserProperties(MainFrame mainFrame) {
        userprop.setProperty("WINDOW_X_POS", String.valueOf(mainFrame.getLocation().x));
        userprop.setProperty("WINDOW_Y_POS", String.valueOf(mainFrame.getLocation().y));
        userprop.setProperty("WINDOW_HEIGHT", String.valueOf(mainFrame.getSize().height));
        userprop.setProperty("WINDOW_WIDTH", String.valueOf(mainFrame.getSize().width));
        userprop.setProperty("DIVIDER_LOCATION", String.valueOf(mainFrame.getDividerLocation()));
        userprop.setProperty("DATE_FORMAT", MainFrame.getDateFormatMask());
        if (MainFrame.dmFrame != null) {
            userprop.setProperty(
                    "DM_FRAME_SCRIPT_OVERWRITE",
                    String.valueOf(MainFrame.dmFrame.isScriptOverwriteSelected()));
            userprop.setProperty("DM_FRAME_NEW_FRAME", String.valueOf(MainFrame.dmFrame.isSelectInNewFrameSelected()));
            userprop.setProperty("FULL_QUALIFIED_NAME", String.valueOf(MainFrame.dmFrame.isFullQualifiedNameSelected()));
            userprop.setProperty("DM_FRAME_START_IMMEDIATELY", String.valueOf(MainFrame.dmFrame.isStartImmediatelySelected()));
        }
        if (MainFrame.sqlHistory != null) {
            userprop.setProperty("HISTORY_NO_DOUBLE", String.valueOf(MainFrame.sqlHistory.isNoDoublesAllowed()));
            userprop.setProperty("HISTORY_FREESE", String.valueOf(MainFrame.sqlHistory.isFreesed()));
            userprop.setProperty(
                    "HISTORY_SCRIPT_WITH_COMMENTS",
                    String.valueOf(MainFrame.sqlHistory.isCommentsInHistoryScriptEnabled()));
            userprop.setProperty("HISTORY_RUN_IMMIDATELY", String.valueOf(MainFrame.sqlHistory.isRunImmidatlyEnabled()));
        }
    }

    /**
     * schliesst ein einzelnes Fenster
     * @param mainFrame - zu schliessendes Fenster
     */
    public static void close(MainFrame mainFrame) {
        listMainFrames.remove(mainFrame);
        if (listMainFrames.isEmpty()) {
            refreshUserProperties(mainFrame);
            Main.saveUserProp();
            if (MainFrame.dmFrame != null) {
                MainFrame.dmFrame.dispose();
            }
            if (MainFrame.sqlHistory != null) {
                MainFrame.sqlHistory.saveList();
                MainFrame.sqlHistory.dispose();
            }
            Main.saveContextProp();
            mainFrame.dispose();
            mainFrame = null;
            logger.info("program finished.");
            System.exit(0);
        } else {
            mainFrame.dispose();
            // den letzten MainFrame hervorholen
            mainFrame = listMainFrames.get(listMainFrames.size() - 1);
            mainFrame.toFront();
        }
    }

    static public List<MainFrame> getWindowList() {
        return listMainFrames;
    }

    /**
     * schliesst alle Fenster aus dem mit der Referenz in mainFrame
     * @param mainFrame - nicht zu schliessendes Fenster
     */
    public static void closeAllOther(MainFrame mainFrame) {
        MainFrame frame;
        int answer = 0;
        final Object[] frames = listMainFrames.toArray();
        for (int i = 0; i < frames.length; i++) {
            frame = (MainFrame) frames[i];
            if (frame != mainFrame) {
                if (answer == 2) { // Keine-Option
                    frame.closeWithoutQuestion();
                } else {
                    if (frame.close()) {
                        frame.dispose();
                    } else {
                        break;
                    }
                } // if (answer==2)
            } // if (frame != mainFrame)
        } // for (int i=0; i <  frames.length; i++)
    }

    /**
     * beendet die komplette Anwendung, durch das Schliessen der einzelnen Fenster+Sessions
     * das beenden des Java-Programmes erfolgt in close(..) wenn der letzte
     * verbliebene Frame geschlossen wird.
     */
    public static void shutdown() {
        MainFrame frame;
        int answer = 0;
        final Object[] frames = listMainFrames.toArray();
        for (int i = 0; i < frames.length; i++) {
            frame = (MainFrame) frames[i];
            if (answer == 2) { // Keine-Option
                frame.closeWithoutQuestion();
            } else {
                if (frame.close()) {
                    frame.dispose();
                } else {
                    break;
                }
            } // if (answer==2)
        } // for (int i=0; i <  frames.length; i++)
        DatabaseSessionPool.close();
    }
    
    public static boolean canShutdown() {
    	for (MainFrame mf : listMainFrames) {
    		if (mf.isCloseActionDisabled()) {
    			return false;
    		}
    	}
    	return true;
    }

    public static void arrangeWindowsHorizontal() {
        WindowHelper.arrangeWindowsHorizontal(listMainFrames);
    }

    public static void arrangeWindowsVertical() {
        WindowHelper.arrangeWindowsVertical(listMainFrames);
    }

    public static void arrangeWindowsOverlapped(MainFrame frame) {
        WindowHelper.arrangeWindowsOverlapped(listMainFrames, frame, DEF_WIDTH, DEF_HEIGHT);
    }

    /**
     * Ausstieg aus dem Programm wenn nichts mehr geht.
     * Das Programm wird mit OS-Fehlercode=1 beendet.
     * Vor dem Ende wird noch ein Meldungsfenster aufgefahren.
     * @param message auszugebender Meldungstext
     */
    public static void panic(String message) {
        logger.fatal("panic: " + message);
        JOptionPane.showConfirmDialog(
                null,
                message + "\nprogram will be terminated!",
                "Program error ...",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    /**
     * gibt auf der Standardausgabe die Syntax für den Programmstart aus
     */
    static void printUsage() {
        System.out.println("call: java -jar sql.jar [<SQL-Script>] [options] \n\noptions:\n  -h shows this help.\n     program will be finished.\n  -d switch on debug modus\n     -w<left-top-x>,<left-top-y>,<width>,<height>\n  -l<encrypted login-parameter> connect immediately \n  -i create new default configuration");
    }

    /**
     * lädt die Vorgabe-Properties aus cfg-Datei
     */
    static void loadDefaultProperties() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("load defaults...");
            }
            try {
                final InputStream inifileIn = new FileInputStream(cfgFileName);
                defaultProp.load(inifileIn);
                inifileIn.close();
                if (logger.isDebugEnabled()) {
                    logger.debug("...loaded from user.home");
                }
            } catch (java.security.AccessControlException ae) {
                final Main dummy = new Main();
                // wenn nicht vom File erlaubt, dann aus den Archiv direckt laden
                final InputStream is = dummy.getClass().getResourceAsStream("/" + Main.CFG_FILE_NAME);
                defaultProp.load(is);
                is.close();
                if (logger.isDebugEnabled()) {
                    logger.debug("...loaded from archive");
                }
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("loadProperties: failed\ndefault configuration "
                        + cfgFileName
                        + " will be created");
            }
            createDefaultProp();
        }
    }
    
    static public void loadLookAndFeels() {
        final Main dummy = new Main();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("load look and feels from archive");
            }
            final InputStream is = dummy.getClass().getResourceAsStream("/" + Main.LOOK_AND_FEEL_CFG_FILE);
            lookAndFeels.load(is);
            is.close();
            lookAndFeels.setProperty("metal", UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("exception: " + e);
        }
    }

    private static void loadVersion() {
        try {
        	Main dummy = new Main();
            InputStreamReader reader = new InputStreamReader(
            		dummy.getClass()
            		.getResourceAsStream("/VERSION"));
            char[] buffer = new char[64];
            int len = reader.read(buffer);
            VERSION = new String(buffer, 0, len);
            reader.close();
        } catch (IOException e) {
        	logger.error("loadVersion failed: " + e.getMessage(), e);
        }
    }

    /**
     * lädt die User-Properties aus ini-Datei
     */
    static void loadUserProperties() {
        try {
            final FileInputStream inifileIn = new FileInputStream(userCfgFileName);
            userprop.load(inifileIn);
            inifileIn.close();
            boolean debug = Boolean.valueOf(userprop.getProperty("DEBUG", "false")).booleanValue();
            useNativeFileDialog = Boolean.valueOf(userprop.getProperty("USE_NATIVE_FILE_DIALOG", "true")).booleanValue();
            setDebug(debug);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("loadUserProperties: failed\ndefault configuration "
                        + userCfgFileName
                        + " will be created");
            }
            createDefaultUserProp();
        }
    }

    /**
     * lädt die User-Properties aus ini-Datei
     */
    static void loadContextProperties() {
        try {
            final FileInputStream inifileIn = new FileInputStream(contextCfgFileName);
            MainFrame.getContextVarResolver().getContextVars().load(inifileIn);
            inifileIn.close();
        } catch (IOException e) {
        	logger.error("loadContextProperties failed: " + e.getMessage(), e);
        }
    }

    /**
     * sichert die User-Properties in Datei
     */
    static void saveUserProp() {
        if (logger.isDebugEnabled()) {
            logger.debug("save user properties in file " + userCfgFileName);
        }
        try {
            final FileOutputStream inifileOut = new FileOutputStream(userCfgFileName);
            userprop.store(inifileOut, "SQLRunner user-config");
            inifileOut.close();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("saveUserProp: write in file failed:" + userCfgFileName);
            }
        }
    }

    /**
     * sichert die User-Properties in Datei
     */
    static void saveContextProp() {
        if (logger.isDebugEnabled()) {
            logger.debug("save context properties in file " + contextCfgFileName);
        }
        try {
            final FileOutputStream inifileOut = new FileOutputStream(contextCfgFileName);
            MainFrame.getContextVarResolver().getContextVars().store(inifileOut, "SQLRunner context properties");
            inifileOut.close();
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("saveContextProp: write in file failed:" + contextCfgFileName);
            }
        }
    }

    /**
     * extrahiert eine Datei aus einem Archiv und schreibt sie als separate Datei in das Filesystem
     * @param archiName Archiv aus dem die Datei extrahiert werden soll
     * @param zippedFileName Name der zu extrahierenden Datei
     * @param outFileName Dateiname in dem der Inhalt der Inhalt der extrahierten Datei gespeichert werden soll
     */
    static boolean extractFileFromArchive(String zippedFileName, String outFileName) {
        final Main dummy = new Main();
        boolean ok = true;
        try {
            final InputStream in = dummy.getClass().getResourceAsStream("/" + zippedFileName);
            final FileOutputStream out = new FileOutputStream(outFileName);
            final byte[] b_loc = new byte[4096];
            int c;
            while ((c = in.read(b_loc)) != -1) {
                out.write(b_loc, 0, c);
            }
            in.close();
            out.close();
        } catch (NullPointerException npe) {
            ok = false;
            logger.warn("extractFileFromArchive: extraction failed\n   entry:      "
                    + zippedFileName
                    + "\n   File: "
                    + outFileName);
        } catch (IOException ioe) {
            ok = false;
            logger.warn("extractFileFromArchive: extraction failed\n   entry:      "
                    + zippedFileName
                    + "\n   File: "
                    + outFileName);
        }
        return ok;
    }

    static public BufferedReader getTextResource(String fileName) {
        final Main dummy = new Main();
        try {
            return new BufferedReader(new InputStreamReader(dummy.getClass().getResourceAsStream("/" + fileName)));
        } catch (Exception e) {
            logger.warn("getTextResource(" + fileName + ") failed: " + e.toString(), e);
            return null;
        }
    }

    /** extrahiert aus dem Archiv die Datei default.cfg */
    public static void createDefaultProp() {
        if (logger.isDebugEnabled()) {
            logger.debug("erzeuge neue Standardkonfiguration: " + cfgFileName);
        }
        if (!extractFileFromArchive(CFG_FILE_NAME, cfgFileName)) {
            panic("Fehler beim Zugriff auf die Standardkonfiguration");
        }
        try {
            // Properties neu laden
            final FileInputStream inifileIn = new FileInputStream(cfgFileName);
            defaultProp.load(inifileIn);
            inifileIn.close();
        } catch (Exception e) {
            logger.warn("createDefaultProp: read default configuration failed");
            panic("error reading default configuration");
        }
    }

    public static void createDefaultDbCfgFile() {
        if (logger.isDebugEnabled()) {
            logger.debug("create default database types: " + dbCfgFileName);
        }
        extractFileFromArchive(DB_CFG_FILE_NAME_USER, dbCfgFileName);
    }

    public static void createDefaultAdminCfgFile() {
        if (logger.isDebugEnabled()) {
            logger.debug("create default admin configuration: " + adminCfgFileName);
        }
        extractFileFromArchive(ADMIN_CFG_FILE_NAME, adminCfgFileName);
    }

    public static void createDefaultHLCfgFile() {
        if (logger.isDebugEnabled()) {
            logger.debug("create default higlighter configuration: " + getHighlighterFontCfgFileName());
        }
        extractFileFromArchive(HIGHLIGHTER_FONT_CFG_FILE, getHighlighterFontCfgFileName());
    }

    /**
     * erstellt Standardwerte für die User-Properties
     * diese Methode füllt die static definerten userprop
     */
    public static void createDefaultUserProp() {
        if (logger.isDebugEnabled()) {
            logger.debug("erzeuge neue User-Standardkonfiguration: " + userCfgFileName);
        }
        // User-Einstellungen
        //userprop.setProperty("LINE_WRAP","true");
        // hierzu siehe MainFrame.getEditor() !!
        userprop.setProperty("SYNTAX_HIGHLIGHT", "true");
        saveUserProp();
    }

    /**
     * ermittelt den Namen der Datei aus der kompletten Pfadangabe
     * und nutzt \ als auch / als Trenner!
     * @param f kompletter Pfadname
     * @return Filename
     */
    static public String getName(File f) {
        String name = f.getPath();
        int beginOfName = name.lastIndexOf("/");
        if (beginOfName == -1) {
            // nun dann eben mal sehen ob es ein Backslash ist
            beginOfName = name.lastIndexOf("\\");
            // immer noch nichts ?
            if (beginOfName == -1) {
                beginOfName = 0; // keine Pfadangabe enthalten
            } else {
                beginOfName++; // auf der erste Zeichen des Namens setzen
            }
        } else {
            beginOfName++; // auf der erste Zeichen des Namens setzen
        } // if (beginOfName == -1)
        name = (f.getPath()).substring(beginOfName, (f.getPath()).length());
        return name;
    }

    public static void setHighlighterFontCfgFileName(String highlighterFontCfgFileName) {
		Main.highlighterFontCfgFileName = highlighterFontCfgFileName;
	}

	public static String getHighlighterFontCfgFileName() {
		return highlighterFontCfgFileName;
	}

	public static void setSqlHistoryFileName(String sqlHistoryFileName) {
		Main.sqlHistoryFileName = sqlHistoryFileName;
	}

	public static String getSqlHistoryFileName() {
		return sqlHistoryFileName;
	}

	public static void removeAllFileProps() {
        final int max = Integer.parseInt(defaultProp.getProperty("MAX_FILES_ENTRIES", "4"));
        for (int i = 0; i < max; i++) {
            userprop.remove("FILE_" + i);
        }
        refreshReopenItems();
    }

    static void refreshReopenItems() {
        for (MainFrame mainFrame : listMainFrames) {
            mainFrame.createReopenItems();
            mainFrame.validate();
        }
    }

    /**
     * fügt eine FILE_n-Eintrag in Properties hinzu
     * wobei gestestet wird, ob der Eintrag schon existiert,
     * dann wird er nur an die Spitze verschoben
     * die Anzahl der Einträge wird auf den in den Properties definierten
     * Wert gehalten
     * @param file Filename der als Reopen-Eintrag erstellt werden soll
     */
    public static void addFileProp(File file) {
        final String filepath = file.getAbsolutePath();
        //max Anzahl der Möglichen FILE-Einträge ermitteln
        final int max = Integer.parseInt(defaultProp.getProperty("MAX_FILES_ENTRIES", "4"));
        final String[][] filePropArray = new String[max][2];
        for (int i = 0; i < max; i++) {
            filePropArray[i][0] = "FILE_" + i;
            filePropArray[i][1] = userprop.getProperty("FILE_" + i);
        }
        // nach dem Einlesen alle FILE_n-Properties entfernen
        for (int i = 0; i < max; i++) {
            userprop.remove("FILE_" + i);
        }
        // Leerstellen entfernen
        // Doppelte Positionen werden eleminiert
        // zählen wie viele dann frei
        String fileName;
        for (int i = 0; i < max; i++) {
            fileName = filePropArray[i][1];
            if (fileName != null && fileName.length() > 0) {
                // vergleichen mit dem neu hinzuzufügenden Eintrag
                // wenn schon vorhanden diesen löschen
                if (fileName.equals(filepath)) {
                    filePropArray[i][0] = "";
                    filePropArray[i][1] = "";
                }
                // nach weiteren Doppelgängern suchen und löschen
                for (int y_loc = i + 1; y_loc < max; y_loc++) {
                    // Doppelgänger löschen
                    if (fileName.equals(filePropArray[y_loc][1])) {
                        filePropArray[y_loc][0] = "";
                        filePropArray[y_loc][1] = "";
                    }
                } // for (int y = i + 1; y < max; y++)
            } else {
                // wenn "leer", dann auch löschen
                filePropArray[i][0] = "";
                filePropArray[i][1] = "";
            }
        } // for (int i = 0; i < max; i++)
        // array packen / Leerstellen gehen an das Ende der Liste
        for (int i = 0; i < max; i++) {
            // Leerstelle suchen und merken
            if (filePropArray[i][1].length() == 0) {
                // nächste besetzte Stelle suchen und
                // an frei Stelle verschieben
                for (int n = i + 1; n < max; n++) {
                    if (filePropArray[n][1].length() > 0) {
                        // an Pos LeerPos copieren
                        filePropArray[i][0] = filePropArray[n][0];
                        filePropArray[i][1] = filePropArray[n][1];
                        // an Pos n löschen
                        filePropArray[n][0] = "";
                        filePropArray[n][1] = "";
                        // wenn gefunden for-Schleife abbrechen !!
                        break;
                    } // if (!filePropArray[n][1].equals(""))
                } // for (int n = i + 1; n < max; n++)
            } // if (filePropArray[i][1].equals(""))
        } // for (int i = 0; i < max; i++)
        // um eine Stelle tiefer setzen
        // die letzte Stelle wenn erforderlich entfernen
        // anfangen bei Pos 1, da Pos 0 neu besetzt werden soll
        // und gleich in defaultProp ablegen
        for (int i = max - 1; i > 0; i--) {
            if ((filePropArray[i - 1][1].length() > 0)) {
                // keine leeren Einträge erzeugen
                userprop.setProperty("FILE_" + i, filePropArray[i - 1][1]);
            }
        }
        // Eintrag an die Spitze hinzufügen
        userprop.setProperty("FILE_0", filepath);
        // Properties speichern in Datei
        refreshReopenItems();
    }

    static public final void setDebug(boolean debug) {
        if (debug) {
            logger.info("set debug on");
            userprop.setProperty("DEBUG", "true");
            final Logger rootLogger = Logger.getRootLogger();
            rootLogger.setLevel(Level.DEBUG);
        } else {
            logger.info("set debug off");
            userprop.setProperty("DEBUG", "false");
            final Logger rootLogger = Logger.getRootLogger();
            rootLogger.setLevel(Level.INFO);
        }
    }

    /**
     * lädt die Vorgabe-Properties aus cfg-Datei
     */
    static void setupLog4j() {
        System.out.println("setup log4j...");
        final InputStream inifileIn = Main.class.getResourceAsStream("/log4j.properties");
        if (inifileIn != null) {
            try {
                log4jProperties = new Properties();
                log4jProperties.load(inifileIn);
                inifileIn.close();
                PropertyConfigurator.configure(log4jProperties);
                logger.info("log4j successful initiated");
            } catch (IOException ioe) {
                BasicConfigurator.configure();
                logger.error("setupLog4j load from resource failed:" + ioe.getMessage(), ioe);
            }
        } else {
            BasicConfigurator.configure();
            logger.info("setupLog4j: no log4j.properties found, use basic configuration");
        }
        logger.info(" ready.");
        Logger.getRootLogger().addAppender(LogPanel.getInstance().getAppender());
    }

    static public final boolean isDebug() {
        Logger rootLogger = Logger.getRootLogger();
        return rootLogger.isDebugEnabled();
    }

    /**
     * instanziert alle in dieser Klasse static deklarierte Fonts
     * alle Text beinhaltenden Komponenten arbeiten mit diesen Fonts
     * Die Werte für die Initialisierung werden aus den Programm-Properties geladen.
     */
    static void setupEditorFont() {
        try {
        	int fontSize = Integer.parseInt(userprop.getProperty("EDITOR_FONT_SIZE", "12"));
        	if (fontSize == 0) {
            	fontSize = Integer.parseInt(defaultProp.getProperty("EDITOR_FONT_SIZE", "12"));
        	}
        	String fontFamily = userprop.getProperty("EDITOR_FONT_FAMILY", "Monospaced");
            textFont = new Font(fontFamily, Font.PLAIN, fontSize);
        } catch (NumberFormatException e) {
            logger.warn("defineFonts: invalid values", e);
        }
    }
    
    static public void redefineEditorFont() {
    	setupEditorFont();
        for (MainFrame mainFrame : listMainFrames) {
            mainFrame.setupEditorFont();
        }
    }

    static public void disconnect() {
        for (MainFrame mainFrame : listMainFrames) {
            mainFrame.startDisconnect();
        }
    }

    /**
     * erstellt Standardwerte für die Platzierung / Abmasse des Hauptfensters auf dem Bildschirm
     * @return Rectangle welches die Platzierung / Abmasse beschreibt
     */
    static Rectangle createDefaultWindowParameters() {
        final Rectangle r = new Rectangle();
        try {
            r.x = Integer.parseInt(defaultProp.getProperty("DEF_XLOC"));
            r.y = Integer.parseInt(defaultProp.getProperty("DEF_YLOC"));
            r.width = Integer.parseInt(defaultProp.getProperty("DEF_WIDTH"));
            r.height = Integer.parseInt(defaultProp.getProperty("DEF_HEIGHT"));
        } catch (NumberFormatException e) {
            r.x = DEF_XLOC;
            r.y = DEF_YLOC;
            r.width = DEF_WIDTH;
            r.height = DEF_HEIGHT;
                logger.warn("createDefaultWindowParameters: invalid or missing values: "
                        + e.getMessage()
                        + "\n   for keys DEF_XLOC ... DEF_HEIGHT in "
                        + cfgFileName
                        + "\n   use internal default values.");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("setDefaultWindowParameters: set default values: x="
                    + r.x
                    + " ,y="
                    + r.y
                    + " ,b="
                    + r.width
                    + " ,h="
                    + r.height);
        }
        return r;
    }

    /**
     * zerlegt die mit Komma getrennten Werte in die int-Window-Parameter
     * @param p String der den gelesenen Parameter enthält
     * @return Rectangle welches die Platzierung / Abmasse beschreibt
     */
    static Rectangle parseWindowParameters(String p) {
        final int i1;
        final int i2;
        final int i3; //Pos der Kommata im Parameterstring
        Rectangle r = new Rectangle();
        try {
            // erstes Komma suchen und den String bis dahin al x-top speichern
            i1 = p.indexOf(','); //1. Komma finden
            r.x = Integer.parseInt(p.substring(0, i1)); // x separieren
            i2 = p.indexOf(',', i1 + 1); //2. Komma finden
            r.y = Integer.parseInt(p.substring(i1 + 1, i2)); // y separieren
            i3 = p.indexOf(',', i2 + 1); //3. Komma finden
            r.width = Integer.parseInt(p.substring(i2 + 1, i3)); // b separieren
            r.height = Integer.parseInt(p.substring(i3 + 1, p.length())); // h separieren
            // Mindestwerte für höhe und Breite einhalten
                if (logger.isDebugEnabled()) {
                    logger.debug("parseWindowParameters: set window parameters: "
                            + r.x
                            + " ,"
                            + r.y
                            + " ,"
                            + r.width
                            + " ,"
                            + r.height);
                }
        } catch (NumberFormatException e) {
                logger.warn("parseWindowParameters: invalid values in window parameters: "
                        + p
                        + "\n set default values.");
            r = createDefaultWindowParameters();
        } catch (StringIndexOutOfBoundsException e) {
                logger.warn("parseWindowParameters: not enough or invalid values: "
                        + p
                        + "\n set internal default values");
            r = createDefaultWindowParameters();
        }
        return r;
    }

    public static int countCommandLineArgs() {
    	if (commandLineArguments != null) {
    		return commandLineArguments.length;
    	} else {
    		return 0;
    	}
    }
    
    public static String getCommandLineArguments() {
    	if (commandLineArguments != null) {
    		final StringBuffer sb = new StringBuffer();
    		for (int i = 0; i < commandLineArguments.length; i++) {
    			if (i > 0) {
        			sb.append('\n');
    			}
    			sb.append(commandLineArguments[i]);
    		}
    		return sb.toString();
    	} else {
    		return "";
    	}
    }
    
    public static String getCommandLineArgument(int index) {
    	if (commandLineArguments == null) {
    		throw new IllegalStateException("no command line args exists");
    	} else if (index > commandLineArguments.length - 1) {
    		throw new IllegalArgumentException("index out of args array bounds");
    	} else {
    		return commandLineArguments[index];
    	}
    }
    
    public static MainFrame getActiveMainFrame() {
    	MainFrame mf = null;
    	for (int i = 0; i < listMainFrames.size(); i++) {
    		mf = listMainFrames.get(i);
    		if (mf.isActive()) {
    			break;
    		} else {
    			mf = null;
    		}
    	}
    	// workaround because not all OS support active windows
    	if (mf == null && listMainFrames.size() > 0) {
    		mf = listMainFrames.get(0);
    	}
    	return mf;
    }
    
    private static void setupMacOSXApplicationListener() {
    	try {
			MacOSXAdapterInterface adapter = (MacOSXAdapterInterface) Class.forName("sqlrunner.MacOSXAdapter").newInstance();
			adapter.setup();
    	} catch (Throwable e) {
			logger.error("Instantiation of MacOS X adapter failed: " + e.getMessage(), e);
		}
    }
    
    public static final boolean isMacOsX() {
    	return osType.equals(OS_MACOSX);
    }
    
    public static void main(final String[] args) {
    	commandLineArguments = args;
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "SQLRunner");
        String osname = System.getProperty("os.name");
        if (osname != null) {
        	if (osname.toLowerCase().indexOf("mac") != -1) {
            	osType = OS_MACOSX;
                setupMacOSXApplicationListener();
        	} else if (osname.toLowerCase().indexOf("win") != -1) {
        		osType = OS_WINDOWS;
        	} else {
        		osType = OS_UNIX;
        	}
        }
        logger.info("SQLRunner Application");
        // unbedingt soweit zum Beginn als möglich den Listener registerieren !!
        // Splashscreen
        //splash = new SplashScreen();
        setupProgramDirectories();
        setupLog4j();
        boolean openOption = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-open")) {
                openOption = true;
            } else {
                if (openOption) {
                    fileToLoad = args[i];
                } else {
                    if ((args[i]).charAt(0) == '-') {
                        //dann Optionen zu erwarten
                        switch (args[i].charAt(1)) {
                            // Hilfe zum Programm-Aufruf
                            case '?':
                            case 'h': {
                                printUsage();
                                System.exit(0); // ENDE
                                break;
                            }
                            // Parameter für die Platzierung des Fensters
                            case 'w': {
                                Rectangle r = parseWindowParameters(args[i].substring(2, args[i].length()));
                                x = r.x;
                                y = r.y;
                                b = r.width;
                                h = r.height;
                                // merken, dass windowparameter mitgegeben
                                editorWindowParameterGiven = true;
                                break;
                            }
                            //Standard-ini-Dateien erstellen
                            case 'i': {
                                createDefaultProp();
                                createDefaultHLCfgFile();
                                createDefaultUserProp();
                                createDefaultDbCfgFile();
                                createDefaultAdminCfgFile();
                                break;
                            }
                            case 'd': {
                                Logger.getRootLogger().setLevel(Level.DEBUG);
                                break;
                            }
                            // autologin anhand übergebener Parameter
                            case 'l': {
                                loginParamsAsURLElements = args[i].substring(2, args[i].length());
                                break;
                            }
                            // alle nicht erwarteten Parameter
                            default: {
                                logger.warn("unknown option: " + args[i]);
                                printUsage();
                            }
                        } // switch (args[i].charAt(1))
                    } else {
                        if (script == null) { // wenn noch nicht erfolgt, dann
                            script = args[i]; // Dateinamen übernehmen
                        } else {
                            // alle weiteren Parameter (ausser Optionen) sind ungültig
                            if (logger.isDebugEnabled()) {
                                logger.debug("unknown parameter: " + args[i]);
                            }
                            printUsage();
                        }
                    } // if (args[i].charAt(0) == '-')
                }
            }
        } // for (int i = 0; i < args.length; i++)
    	SwingUtilities.invokeLater(new Runnable() {
    		@Override
			public void run() {
    	        initialize();
    		}
    	});
    }

}
