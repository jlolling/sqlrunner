package sqlrunner.flatfileimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import dbtools.SQLParser;
import dbtools.SQLStatement;
import sqlrunner.generator.SQLCodeGenerator;
import sqlrunner.text.StringReplacer;

/**
 * 
 * @author Jan Lolling
 */
public class FileImporter implements Importer {

    private static final Logger staticLogger = Logger.getLogger(FileImporter.class);
    private Logger instanceLogger = staticLogger;
    static final int ROWS_BETWEEN_COMMIT = 1000;
    private long countRowsBetweenCommit = ROWS_BETWEEN_COMMIT;
    private String descFilename;
    private File currentDataFile;
    private ArrayList<FieldDescription> fields;
    private DatabaseSession session;
    private Properties additionalProperties = new Properties();
    /**
     * error-code for System.exit 0 = normal, 1 = warnings, 2 = fatals the order
     * of values is important
     */
    private int errorCode = NOT_STARTED;
    static String lineSep = System.getProperty("line.separator");
    private String lastErrorMessage;
    private boolean interrupted = false;
    private long startTime;
    private long stopTime;
    private long countLines = 0;
    private long currentLineIndex = 0;
    private long countInsert = 0;
    private long countInsertProcessed = 0;
    private long countUpdate = 0;
    private long countMaxLines = 0;
    private String currentAction;
    private boolean running = false;
    private String errorLogFileName = null;
    private boolean testOnly = false;
    private boolean disableDeleteTable = false;
    private String defaultExtension = "csv";
    private ArrayList<String> csvFileExtensions = new ArrayList<String>();
    private ImportAttributes currentAttributs = new ImportAttributes();
    private HashMap<String, Object> lastValues = new HashMap<String, Object>();
    
    public FileImporter() {
    	csvFileExtensions.add("csv");
    	csvFileExtensions.add("tsv");
    	csvFileExtensions.add("txt");
    }
    
    public void addCSVFileExtension(String extension) {
    	if (extension == null || extension.isEmpty()) {
    		throw new IllegalArgumentException("extension cannot be null or empty");
    	}
    	csvFileExtensions.add(extension.toLowerCase());
    }
    
    private boolean isCSVFileExt(String ext) {
    	if (currentAttributs.isHandleFileAlwaysAsCSV()) {
    		return true;
    	}
    	for (String knownExt : csvFileExtensions) {
    		if (ext.equalsIgnoreCase(knownExt)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean isXLSFileExt(String ext) {
    	if (currentAttributs.isHandleFileAlwaysAsCSV()) {
    		return false;
    	}
    	if (ext == null || ext.isEmpty()) {
    		throw new IllegalArgumentException("ext cannot be null or empty");
    	}
    	return ext.equalsIgnoreCase("xls");
    }
    
    private boolean isXLSXFileExt(String ext) {
    	if (currentAttributs.isHandleFileAlwaysAsCSV()) {
    		return false;
    	}
    	if (ext == null || ext.isEmpty()) {
    		throw new IllegalArgumentException("ext cannot be null or empty");
    	}
    	return ext.equalsIgnoreCase("xlsx");
    }

    public DatasetProvider createDatasetProvider(File dataFile, boolean testMode, String defaultExt, ImportAttributes opts) throws Exception {
    	DatasetProvider provider = null;
        String ext = getFileExtension(dataFile.getName(), defaultExt);
        if (isXLSFileExt(ext)) {
        	getLogger().info("file " + dataFile + " is a xls spreadsheet. Using " + (opts.getSheetName() != null ? opts.getSheetName() : " first sheet"));
            provider = new XLSFileDatasetProvider();
        } else if (isXLSXFileExt(ext)) {
        	getLogger().info("file " + dataFile + " is a xlsx spreadsheet. Using " + (opts.getSheetName() != null ? opts.getSheetName() : " first sheet"));
            provider = new XLSXFileDatasetProvider();
        } else if (isCSVFileExt(ext) || opts.isHandleFileAlwaysAsCSV()) {
        	getLogger().info("file " + dataFile + " is plain text");
            provider = new CSVFileDatasetProvider();
        } else {
            throw new Exception("Unknown or invalid file extension:" + ext);
        }
    	provider.setupDatasetProvider(dataFile, testMode, opts);
    	return provider;
    }
    
    private static String getFileExtension(final String fileName, final String defaultExt) {
    	if (fileName == null || fileName.trim().length() == 0) {
    		throw new IllegalArgumentException("fileName cannot be null or empty");
    	} 
    	if (defaultExt == null || defaultExt.trim().length() == 0) {
    		throw new IllegalArgumentException("defaultExt cannot be null or empty");
    	}
        int pos = fileName.lastIndexOf('.');
        if (pos == -1) {
            return defaultExt;
        } else {
            String name = fileName.trim().toLowerCase();
            String ext = name.substring(pos + 1);
            if (ext != null && ext.length() > 0) {
                return ext;
            } else {
                return defaultExt;
            }
        }
    }
    
    public ImportAttributes getImportAttributes() {
    	return currentAttributs;
    }
    
    public boolean isDisableDeleteTable() {
        return disableDeleteTable;
    }

    public void setDisableDeleteTable(boolean disableDeleteTable) {
        this.disableDeleteTable = disableDeleteTable;
    }

    public boolean isTestOnly() {
        return testOnly;
    }

    public void setTestOnly(boolean testOnly) {
        this.testOnly = testOnly;
    }
    
    public int getBatchCounter() {
    	return batchCounter;
    }

    @Override
	public String getCurrentAction() {
        return currentAction;
    }

    public void setProperty(String key, String value) {
        additionalProperties.setProperty(key, value);
    }

    public String getProperty(String key, String defaultValue) {
        return additionalProperties.getProperty(key, defaultValue);
    }

    public void removeProperty(String key) {
        additionalProperties.remove(key);
    }

    public void clearProperties() {
        additionalProperties.clear();
    }

    protected void resetStatus() {
        startTime = 0;
        stopTime = 0;
        errorCode = NOT_STARTED;
        stopTime = 0; // kennzeichnet dass noch nicht beendet !
        countMaxLines = 0;
        countLines = 0;
        currentLineIndex = 0;
        countInsert = 0;
        countUpdate = 0;
        countInsertProcessed = 0;
        batchCounter = 0;
    }

    public boolean connect(ConnectionDescription cd) {
        if (running) {
            throw new IllegalStateException("importer is already running");
        }
        resetStatus();
        currentAction = "connecting";
        final boolean ok;
        setErrorCode(NORMAL);
        ConnectionDescription localCd = cd.clone();
        localCd.setAutoCommit(false);
        session = new DatabaseSession(localCd);
        ok = session.loadDriver();
        if (ok == false) {
            error("load database-driver failed!", FATALS);
            setErrorCode(FATALS);
        }
        if (instanceLogger != null) {
            session.setLogger(instanceLogger);
        }
        return session.isConnected();
    }

    public void disconnect() {
        if (running) {
            throw new IllegalStateException("importer is already running");
        }
        if (session != null && session.isConnected()) {
            session.close();
            session = null;
            if (instanceLogger != null) {
                instanceLogger.removeAllAppenders();
            }
            instanceLogger = null;
        }
    }

    public void interrupt() {
        interrupted = true;
    }

    public boolean setImportConfig(String descFilename_loc) {
        status("set description-config-file:" + descFilename_loc);
        this.descFilename = descFilename_loc;
        return initConfig(new File(descFilename));
    }

    public void importData(String filePath) {
    	File f = new File(filePath);
    	importData(f);
    }
    
    public void importData(File dataFile) {
        if (running) {
            throw new IllegalStateException("importer is already running");
        }
        this.currentDataFile = dataFile;
        importData();
    }
    
    public String getDataFileName() {
    	return currentDataFile.getName();
    }

    public Logger setLogger(Logger localLogger) {
    	if (localLogger == null) {
    		throw new IllegalArgumentException("localLogger cannot be null");
    	}
        if (running) {
            throw new IllegalStateException("importer is already running");
        }
        if (instanceLogger != null) {
            instanceLogger.removeAllAppenders();
        }
        instanceLogger = localLogger;
        if (session != null) {
            session.setLogger(instanceLogger);
        }
        return instanceLogger;
    }

    public FileAppender createFileAppender(String fileName) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss");
        errorLogFileName = fileName + "-" + sdf.format(new java.util.Date()) + ".import.log";
        FileAppender appender = new FileAppender();
        appender.setFile(errorLogFileName, false, true, 8000);
        final PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("%d %-5p %m%n");
        appender.setLayout(layout);
        appender.setImmediateFlush(true);
        return appender;
    }

    /**
     * creates a logger with name = class name + "-" + sourceFileName
     * @param sourceFileName
     * @return
     */
    public Logger createLogger(String sourceFileName) {
        File f = new File(sourceFileName);
        Logger localLogger = Logger.getLogger(getClass().getName() + "-" + f.getName());
        return localLogger;
    }

    public void setupLocalLoggerWithFileAppender(String sourceFileName) throws IOException {
        setupLocalLogger(sourceFileName).addAppender(createFileAppender(sourceFileName));
    }

    public Logger setupLocalLogger(String sourceFileName) {
        setLogger(createLogger(sourceFileName));
        return getLogger();
    }

    /**
     * use the static class logger as local logger
     * @return local logger
     */
    public Logger setupLoggerAsStaticClassLogger() {
        instanceLogger = staticLogger;
        return instanceLogger;
    }

    public Logger getLogger() {
    	if (instanceLogger == null) {
    		instanceLogger = staticLogger;
    	}
        return instanceLogger;
    }

    public void importData() {
        if (running) {
            throw new IllegalStateException("importer is already running");
        }
        if (instanceLogger == null) {
            throw new IllegalStateException("logger is not configured");
        }
        if (testOnly) {
            instanceLogger.info("run in test mode");
        }
        if (currentDataFile != null) {
            try {
                running = true;
                if (runPreProcessSQL()) {
                    if (runImport()) {
                        runPostProcessSQL();
                    }
                }
                running = false;
            } catch (Exception e) {
            	getLogger().error("importData failed: " + e.getMessage(), e);
                setErrorCode(FATALS);
                running = false;
            }
        } else {
            error("data-file expected !", FATALS);
        }
        stopTime = System.currentTimeMillis();
        switch (errorCode) {
            case NORMAL:
                status("\nJob ends successful.");
                break;
            case WARNINGS:
                status("\nJob ends with warnings !");
                break;
            case FATALS:
                status("\nJob ends with fatals !");
                
        }
    }

    private void runPostProcessSQL() {
        if (currentAttributs.isPostProcessEnabled()) {
            status("run post process SQL");
            currentAction = "Replace SQL parameters";
            String sql = replaceParameters(currentAttributs.getPostProcessSQL());
            currentAction = "Parse post process SQL script";
            final SQLParser parser = new SQLParser(sql);
            SQLStatement stat = null;
            for (int i = 0; i < parser.getStatementCount(); i++) {
                currentAction = "Execute post process SQL (" + i + ")";
                stat = parser.getStatementAt(i);
                if (testOnly == false) {
                    session.execute(stat.getSQL());
                    if (session.isSuccessful()) {
                        session.commit();
                    } else {
                        error(session.getLastErrorMessage(), FATALS);
                        session.rollback();
                        break;
                    }
                }
            }
            currentAction = "Post process SQL proceeded";
            status("finished");
        }
    }

    private boolean runPreProcessSQL() {
        boolean ok = true;
        if (currentAttributs.isPreProcessEnabled()) {
            status("run pre process SQL");
            currentAction = "Replace SQL parameters";
            String sql = replaceParameters(currentAttributs.getPreProcessSQL());
            currentAction = "Parse pre process SQL script";
            final SQLParser parser = new SQLParser(sql);
            SQLStatement stat = null;
            for (int i = 0; i < parser.getStatementCount(); i++) {
                currentAction = "Execute pre process SQL (" + i + ")";
                stat = parser.getStatementAt(i);
                if (testOnly == false) {
                    session.execute(stat.getSQL());
                    if (session.isSuccessful()) {
                        session.commit();
                    } else {
                        ok = false;
                        error(session.getLastErrorMessage(), FATALS);
                        session.rollback();
                        break;
                    }
                }
            }
            currentAction = "Pre process SQL proceeded";
            status("finished");
        }
        return ok;
    }

    private String replaceParameters(String sql) {
        FieldDescription field = null;
        if (sql != null) {
            StringReplacer sr = new StringReplacer(sql);
            for (int i = 0; i < fields.size(); i++) {
                field = fields.get(i);
                sr.replace("{field." + field.getName() + ".defaultValue}", field.getDefaultValue(), false);
                sr.replace("{field." + field.getName() + ".lastValue}", getLastValue(field.getName()).toString(), false);
                sr.replace("{field." + field.getName() + ".generatorStartValue}", field.getGeneratorStartValue(), false);
            }
            if (additionalProperties != null && additionalProperties.isEmpty() == false) {
                Map.Entry<Object, Object> entry = null;
                for (Iterator<Map.Entry<Object, Object>> it = additionalProperties.entrySet().iterator(); it.hasNext();) {
                    entry = it.next();
                    sr.replace("{prop." + ((String) entry.getKey()).toLowerCase() + ".value}", (String) entry.getValue(), false);
                }
            }
            return sr.getResultText();
        } else {
            return null;
        }
    }

    public File getConfigFile() {
        final File f = new File(descFilename);
        if (f.exists()) {
            return f;
        } else {
            return null;
        }
    }

    /**
     * erstellt die Strukturdaten im Speicher (SourceTable, SourceField ...
     * 
     * @return true wenn erfolgreich
     */
    public boolean initConfig(File descFile) {
        currentAction = "Load config file";
        if (descFile == null) {
            throw new IllegalArgumentException("descFile cannot be null");
        }
        if (descFile.exists() == false) {
            error("initConfig failed: descFile=" + descFile.getAbsolutePath() + " doesn't exist!", FATALS);
            return false;
        }
        status("read description-properties from file " + descFile.getAbsolutePath());
        fields = new ArrayList<FieldDescription>();
        boolean ok = initConfig(loadProperties(descFile));
        return ok;
    }

    public Properties loadProperties(File propertiesFile) {
        FileInputStream fin = null;
        final Properties importProperties = new Properties();
        try {
            fin = new FileInputStream(propertiesFile);
            importProperties.load(fin);
        } catch (IOException ioe) {
            error(ioe.getMessage(), FATALS);
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (Exception e) {}
            }
        }
        return importProperties;
    }

    public boolean initConfig(String propertiesAsString) {
        if (propertiesAsString == null) {
            error("initConfig propertiesAsString cannot be null", FATALS);
            return false;
        }
        final Properties importProperties = new Properties();
        try {
            final StringInputStream in = new StringInputStream(propertiesAsString);
            importProperties.load(in);
            in.close();
        } catch (Exception e) {
            error("initConfig with String failed:" + e.getMessage(), FATALS);
            return false;
        }
        return initConfig(importProperties);
    }
    
    private int extractIndexFromKey(String key) {
        int index = -1;
        if (key.startsWith("COLUMN_") && key.endsWith("_NAME")) {
            int p0 = key.indexOf("_");
            if (p0 != -1) {
                int p1 = key.indexOf("_", p0 + 1);
                if (p1 != -1) {
                    try {
                        index = Integer.parseInt(key.substring(p0 + 1, p1));
                    } catch (Exception e) {
                    	getLogger().warn("invalid key, no valid index found. Exception: " + e.getMessage(), e);
                    }
                }
            }
        }
        return index;
    }
    
    private ArrayList<Integer> collectFieldDescriptionIndexes(Properties properties) {
        ArrayList<Integer> listIndexes = new ArrayList<Integer>();
        int index = 0;
        for (Object key : properties.keySet()) {
            index = extractIndexFromKey((String) key);
            if (index != -1) {
                Integer indexObj = Integer.valueOf(index);
                if (listIndexes.contains(indexObj) == false) {
                    listIndexes.add(indexObj);
                }
            }
        }
        Collections.sort(listIndexes);
        return listIndexes;
    }

    public boolean initConfig(Properties importProperties) {
        currentAction = "Setup import configuration";
        resetStatus();
        interrupted = false;
        status("read descriptions from properties...");
        fields = new ArrayList<FieldDescription>();
        if (importProperties == null || importProperties.isEmpty()) {
            error("importProperties cannot be null or empty", FATALS);
            return false;
        }
        boolean ok = true;
        try {
        	currentAttributs = new ImportAttributes();
            currentAttributs.setupFrom(importProperties);
            if (currentAttributs.getBatchSize() > 1) {
            	getLogger().info("Use batch size of " + currentAttributs.getBatchSize());
            }
            ArrayList<Integer> listIndexes = collectFieldDescriptionIndexes(importProperties);
            if (getLogger().isDebugEnabled()) {
            	getLogger().debug("initConfig found " + listIndexes.size() + " columns");
            }
            int index = 0;
            for (Integer propertySearchIndex : listIndexes) {
                fields.add(new FieldDescription(index++, propertySearchIndex.intValue(), importProperties));
            } // for (int i=0; i < columnCount; i++)
            for (int d = 0; d < fields.size(); d++) {
                FieldDescription fd = fields.get(d);
                if (fd.getAlternativeFieldDescriptionName() != null) {
                    fd.setAlternativeFieldDescription(getFieldDescription(fd.getAlternativeFieldDescriptionName()));
                }
                if (fd.validate() == false) {
                    ok = false;
                    error("check FieldDescription:" + fd.toString() + ":" + fd.getErrorMessage(), WARNINGS);
                } else {
                    if (BasicDataType.isNumberType(fd.getBasicTypeId()) && fd.getFieldFormat() == null) {
                        error("field " + fd + " has numeric type without defined number locale. This can result in wrong parsing if value is a fraction number", WARNINGS);
                    }
                }
            }
        } catch (Exception ex) {
            error(ex.getMessage(), FATALS);
            ok = false;
        }
        return ok;
    }

    /**
     * @return ArrayList with objects of type FieldDescription
     */
    public List<FieldDescription> getFieldDescriptions() {
        return fields;
    }

    private void resetAutoValueGenerators() {
        status("reset auto value generators");
        FieldDescription fd = null;
        for (int i = 0; i < fields.size(); i++) {
            fd = fields.get(i);
            fd.resetAutValueGenerator();
        }
    }

    public FieldDescription getFieldDescription(String name) {
        FieldDescription fd = null;
        for (int i = 0; i < fields.size(); i++) {
            fd = fields.get(i);
            if (fd.getName() != null && fd.getName().equalsIgnoreCase(name)) {
                return fd;
            }
        }
        return null;
    }

    public FieldDescription getFieldDescription(int index) {
        if (fields != null) {
            return fields.get(index);
        } else {
            return null;
        }
    }

    public int getCountFieldDescriptions() {
        if (fields != null) {
            return fields.size();
        } else {
            return 0;
        }
    }

    /**
     * based on the previously given field desciptions a select count statement will be created
     * @return statement to check if a dataset exists
     */
    private PreparedStatement createTestStatement() {
        FieldDescription fieldTemp;
        final StringBuffer sqlBf = new StringBuffer();
        boolean isFirstLoop = true;
        for (int f = 0; f < fields.size(); f++) {
            fieldTemp = fields.get(f);
            if (fieldTemp.isPartOfPrimaryKey()) {
                if (isFirstLoop) {
                    isFirstLoop = false;
                    sqlBf.append("select count(");
                    sqlBf.append(SQLCodeGenerator.getInstance().getEncapsulatedName(fieldTemp.getName()));
                    sqlBf.append(") from ");
                    sqlBf.append(SQLCodeGenerator.getInstance().getEncapsulatedName(currentAttributs.getTableName()));
                    sqlBf.append(" where ");
                } else {
                    sqlBf.append(" and ");
                }
                sqlBf.append(SQLCodeGenerator.getInstance().getEncapsulatedName(fieldTemp.getName()));
                sqlBf.append("=?");
            } // if ((fieldTemp.isPartOfPrimaryKey() == false) &&
        } // for (int f=0; f<tempTable.fields.size(); f++)
        if (isDebugEnabled()) {
            debug("test statement SQL:" + sqlBf.toString());
        }
        if (isFirstLoop) {
            status("createTestStatement for table " + currentAttributs.getTableName() + " has in Description NO primary keys defined !");
            return null;
        } else {
            PreparedStatement ps;
            try {
                ps = session.createPreparedStatement(sqlBf.toString());
            } catch (SQLException sqle) {
                error("createTestStatement for table " + currentAttributs.getTableName() + " failed!\n" + sqle.getMessage(), FATALS);
                ps = null;
            }
            return ps;
        } // if (isFirstLoop)
    }

    /**
     * creates a statement to be used for inserts of datasets
     * based on the previously given field descriptions
     * @return prepared statement ready to use for insert
     */
    private PreparedStatement createInsertStatement() {
        FieldDescription fieldTemp;
        final StringBuffer sqlBf = new StringBuffer();
        sqlBf.append("insert into " + currentAttributs.getTableName() + " (");
        boolean isFirstLoop = true;
        for (int f = 0; f < fields.size(); f++) {
            fieldTemp = fields.get(f);
            if (fieldTemp.isEnabled()) {
                if (isFirstLoop) {
                    isFirstLoop = false;
                } else {
                    sqlBf.append(',');
                }
                sqlBf.append(fieldTemp.getName());
            } // if (fieldTemp.enabled())
        }
        sqlBf.append(") values (");
        isFirstLoop = true;
        for (int f = 0; f < fields.size(); f++) {
            fieldTemp = fields.get(f);
            if (fieldTemp.isEnabled()) {
                if (isFirstLoop) {
                    isFirstLoop = false;
                } else {
                    sqlBf.append(',');
                }
                if (fieldTemp.getBasicTypeId() == BasicDataType.SQLEXP.getId()) {
                    sqlBf.append(fieldTemp.getDefaultValue());
                } else {
                    sqlBf.append('?');
                }
            }
        }
        sqlBf.append(')');
        if (isDebugEnabled()) {
            debug("insert statement SQL:" + sqlBf.toString());
        }
        PreparedStatement ps;
        try {
            ps = session.createPreparedStatement(sqlBf.toString());
        } catch (SQLException sqle) {
            error("createInsertStatement for table " + currentAttributs.getTableName() + " failed!\n" + sqle.getMessage(), FATALS);
            ps = null;
        }
        return ps;
    }

    /**
     * creates a statement to be used for updates of datasets
     * based on the previously given field descriptions
     * @return prepared statement ready to use for update
     */
    private PreparedStatement createUpdateStatement() {
        FieldDescription fieldTemp;
        final StringBuffer sqlBf = new StringBuffer();
        sqlBf.append("update ");
        sqlBf.append(currentAttributs.getTableName());
        sqlBf.append(" set ");
        boolean isFirstLoop = true;
        for (int f = 0; f < fields.size(); f++) {
            fieldTemp = fields.get(f);
            if (fieldTemp.isEnabled()) {
                if (fieldTemp.isPartOfPrimaryKey() == false) {
                    if (isFirstLoop) {
                        isFirstLoop = false;
                    } else {
                        sqlBf.append(',');
                    }
                    if (fieldTemp.isAggregateNumberValues() && BasicDataType.isNumberType(fieldTemp.getBasicTypeId())) {
                        sqlBf.append(fieldTemp.getName());
                        sqlBf.append('=');
                        sqlBf.append(fieldTemp.getName());
                        sqlBf.append("+?");
                    } else if (fieldTemp.getBasicTypeId() == BasicDataType.SQLEXP.getId()) {
                        sqlBf.append(fieldTemp.getName());
                        sqlBf.append("=");
                        sqlBf.append(fieldTemp.getDefaultValue());
                    } else {
                        sqlBf.append(fieldTemp.getName());
                        sqlBf.append("=?");
                    }
                } // if ((fieldTemp.isPartOfPrimaryKey() == false) &&
            } // if (fieldTemp.enabled())
        } // for (int f=0; f < fields.size(); f++)
        // ready with content fields
        sqlBf.append(" where ");
        // build where clausel
        isFirstLoop = true;
        for (int x = 0; x < fields.size(); x++) {
            fieldTemp = fields.get(x);
            if (fieldTemp.isPartOfPrimaryKey()) {
                if (isFirstLoop) {
                    isFirstLoop = false;
                } else {
                    sqlBf.append(" and ");
                }
                sqlBf.append(fieldTemp.getName());
                if (fieldTemp.getBasicTypeId() == BasicDataType.SQLEXP.getId()) {
                    sqlBf.append("=");
                    sqlBf.append(fieldTemp.getDefaultValue());
                } else {
                    sqlBf.append("=?");
                }
            } // if (((FieldDescription)fields.elementAt(x)).isPartOfPrimaryKey())
        } // for (int x=0; x < fields.size(); x++)
        if (isDebugEnabled()) {
            debug("update statement SQL:" + sqlBf.toString());
        }
        PreparedStatement ps;
        try {
            ps = session.createPreparedStatement(sqlBf.toString());
        } catch (SQLException sqle) {
            error("createUpdateStatement for table " + currentAttributs.getTableName() + " failed!\n" + sqle.getMessage(), FATALS);
            ps = null;
        }
        return ps;
    }

    /**
     * check if a particular dataset exists
     * @param parser contains the data of dataset to be checked 
     * @param ps test statement
     * @param lineNumber to easier find line in input data when problems happens
     * @return true if data set exists
     * @see #createTestStatement
     */
    private boolean existsDataset(FieldTokenizer parser, PreparedStatement ps, long lineNumber) throws ParserException {
        boolean exist = true;
        // fill the where clausel
        int paramIndex = 1;
        try {
            for (int p = 0; p < fields.size(); p++) {
                FieldDescription fieldDescription = fields.get(p);
                if (fieldDescription.isPartOfPrimaryKey() && fieldDescription.getBasicTypeId() != BasicDataType.SQLEXP.getId()) {
                    Object value = parser.getData(fieldDescription.getIndex());
                    setParameterValue(fieldDescription, ps, paramIndex, value);
                    paramIndex++;
                } // if (fieldTemp.isPartOfPrimaryKey())
            } // for (int f=0; f < rsTable.fields.size();)
            final ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                if (count == 0) {
                    exist = false;
                } else if (count == 1) {
                    exist = true;
                } else {
                    error("isDatasetExists: primary key not correct, more than one dataset found data in line " + lineNumber, FATALS);
                    exist = false;
                }
            } // if ((rs != null) && rs.next())
            rs.close();
        } catch (SQLException sqle) {
            error("isDatasetExists: in line:" + String.valueOf(lineNumber) + " failed " + sqle.getMessage(), WARNINGS);
            exist = false;
        }
        return exist;
    }

    private void setParameterValue(FieldDescription fd, PreparedStatement ps, int paramIndex, Object value) throws SQLException, ParserException {
        if (isDebugEnabled()) {
            debug("param at " + paramIndex + " value: " + value);
        }
        try {
	        if (fd.getBasicTypeId() == BasicDataType.DATE.getId()) {
	            if (value != null) {
                    if (value instanceof Timestamp) {
    	                ps.setTimestamp(paramIndex, (Timestamp) value);
                    } else if (value instanceof java.util.Date) {
    	                ps.setTimestamp(paramIndex, new Timestamp(((java.util.Date) value).getTime()));
                    } else {
                        throw new ParserException("in field " + fd + " date value has not valid class " + value.getClass());
                    }
	            } else {
	                ps.setNull(paramIndex, java.sql.Types.TIMESTAMP);
	            }
	        } else if (fd.getBasicTypeId() == BasicDataType.DOUBLE.getId()) {
	            if (value != null) {
	                ps.setDouble(paramIndex, ((Number) value).doubleValue());
	            } else {
	                ps.setNull(paramIndex, Types.DOUBLE);
	            }
	        } else if (fd.getBasicTypeId() == BasicDataType.INTEGER.getId()) {
	            if (value != null) {
	                ps.setInt(paramIndex, ((Number) value).intValue());
	            } else {
	                ps.setNull(paramIndex, Types.INTEGER);
	            }
	        } else if (fd.getBasicTypeId() == BasicDataType.LONG.getId()) {
	            if (value != null) {
	                ps.setLong(paramIndex, ((Number) value).longValue());
	            } else {
	                ps.setNull(paramIndex, Types.BIGINT);
	            }
	        } else if (fd.getBasicTypeId() == BasicDataType.BOOLEAN.getId()) {
	        	if (value != null) {
	        		ps.setBoolean(paramIndex, (Boolean) value);
	        	} else {
	        		ps.setNull(paramIndex, Types.BOOLEAN);
	        	}
            } else if (fd.getBasicTypeId() == BasicDataType.SQLEXP.getId()) {
                throw new ParserException("FieldDescription with basic type SQL expr cannot be used in this method");
	        } else {
	            // must be string
	            if (value != null) {
	                ps.setString(paramIndex, (String) value);
	            } else {
	                ps.setNull(paramIndex, Types.VARCHAR);
	            }
	        }
	        if (value != null) {
	        	putLastValue(fd.getName(), value);
	        }
        } catch (ClassCastException cce) {
        	throw new ParserException(cce);
        }
    }
    
    private int batchCounter = 0;
    
    /**
     * inserts a dataset
     * @param parser contains the data for one dataset
     * @param ps PreparedStatement to be used for inserts
     * @param lineNumber to help problem reports more helpful
     * @see #createInsertStatement
     */
    private int insertRow(FieldTokenizer parser, PreparedStatement ps, long lineNumber) {
        int count = 0;
        int paramIndex = 1;
        if (isDebugEnabled()) {
            debug("insertRow: for table " + currentAttributs.getTableName() + " line number:" + lineNumber);
        }
        try {
            for (int f = 0; f < fields.size(); f++) {
                final FieldDescription fieldDescription = fields.get(f);
                if (fieldDescription.isEnabled() && fieldDescription.getBasicTypeId() != BasicDataType.SQLEXP.getId()) {
                    Object value = parser.getData(fieldDescription.getIndex());
                    setParameterValue(fieldDescription, ps, paramIndex, value);
                    paramIndex++;
                } // if (fieldTemp.enabled())
            } // for (int f=0; f < fields.size(); f++)
            // execute the prepared statement
            if (testOnly) {
                count = 1;
            } else {
            	if (currentAttributs.getBatchSize() > 1) {
            		ps.addBatch();
            		batchCounter++;
                	if (batchCounter == currentAttributs.getBatchSize()) {
                		int[] counters = ps.executeBatch();
                		for (int i : counters) {
                		    // negative counters are flags!
                			count = count + ((i >= 0) ? i : 1);
                		}
                		batchCounter = 0;
                		session.commit();
                	}
            	} else {
                    count = ps.executeUpdate();
            	}
            }
        } catch (SQLException sqle) {
            count = -1;
            SQLException embedded = sqle.getNextException();
            if (embedded != null) {
                error("insertRow: in line:" + String.valueOf(lineNumber) + " failed:" + sqle.getMessage() + ": " + embedded.getMessage(), WARNINGS);
            } else {
                error("insertRow: in line:" + String.valueOf(lineNumber) + " failed:" + sqle.getMessage(), WARNINGS);
            }
        } catch (ParserException pe) {
            count = -1;
            error("insertRow: in line:" + String.valueOf(lineNumber) + " failed:" + pe.getMessage(), WARNINGS);
        }
        if (count >= 0 && testOnly == false) {
            if (isDebugEnabled()) {
                debug("insertRow: row successful inserted.");
            }
        }
        return count;
    }

    /**
     * updates a dataset
     * @param parser contains the data for one dataset
     * @param ps PreparedStatement to be used for update
     * @param lineNumber to help problem reports more helpful
     * @see #createUpdateStatement
     */
    private int updateRow(FieldTokenizer parser, PreparedStatement ps, long lineNumber) {
        if (isDebugEnabled()) {
            debug("updateRow(ps=" + ps + ", counter=" + lineNumber + ")");
        }
        int count = 0;
        int paramIndex = 1;
        try {
            for (int f = 0; f < fields.size(); f++) {
                FieldDescription fieldTemp = fields.get(f);
                if (fieldTemp.isEnabled() && fieldTemp.isPartOfPrimaryKey() == false && fieldTemp.getBasicTypeId() != BasicDataType.SQLEXP.getId()) {
                    Object value = parser.getData(fieldTemp.getIndex());
                    setParameterValue(fieldTemp, ps, paramIndex, value);
                    paramIndex++;
                } // if (fieldTemp.enabled())
            } // for (int f=0; f < rsTable.fields.size(); f++)
            // build the where clausel
            for (int p = 0; p < fields.size(); p++) {
                FieldDescription fieldTemp = fields.get(p);
                if (fieldTemp.isPartOfPrimaryKey() && fieldTemp.getBasicTypeId() != BasicDataType.SQLEXP.getId()) {
                    Object value = parser.getData(fieldTemp.getIndex());
                    setParameterValue(fieldTemp, ps, paramIndex, value);
                    paramIndex++;
                } // if (fieldTemp.isPartOfPrimaryKey())
            } // for (int f=0; f < rsTable.fields.size();)
            // execute prepared statement
            if (testOnly) {
                count = 1;
            } else {
                count = ps.executeUpdate();
            }
        } catch (SQLException sqle) {
            count = -1;
            error("updateRow: in line:" + String.valueOf(lineNumber) + " failed:" + sqle.getMessage(), WARNINGS);
        } catch (ParserException pe) {
            count = -1;
            error("updateRow: in line:" + String.valueOf(lineNumber) + " failed:" + pe.getMessage(), WARNINGS);
        }
        if (count == 1) {
            // everthing ok !
            if (isDebugEnabled()) {
                debug("update successful.");
            }
        } else if (count == 0) {
            error("updateRow: dataset NOT updated ! in line:\n" + lineNumber, WARNINGS);
        } else if (count > 1) {
            error("updateRow: in line:" + String.valueOf(lineNumber) + " more than ONE (" + count + ") datasets updated !\n", FATALS);
        }
        return count;
    }

    /**
     * write all data into the content-tables
     * 
     * @return true if write is successfuly finished
     */
    private boolean runImport() {
        errorCode = NORMAL;
        resetAutoValueGenerators();
        startTime = System.currentTimeMillis();
        status("run import for table " + currentAttributs.getTableName());
        if (currentAttributs.getBatchSize() > 0) {
        	status("Ignore commit size and commit after each batch execute");
        }
        // testen ob es eine FileMask gibt, dann braucht weiter nichts gemacht
        // werden
        // am Ende gibt es eine Warnung
        if (getFieldDescriptions() == null || getFieldDescriptions().isEmpty()) {
            error("runImport failed: list of fielddescriptions is null or empty", FATALS);
            return false;
        }
        if (currentAttributs.isInsertEnabled()) {
            status("* inserts enabled");
        }
        if (currentAttributs.isUpdateEnabled()) {
            status("* update enabled");
        }
        try {
            // die Statements vorbereiten
            PreparedStatement psTest = null;
            if (psTest == null && currentAttributs.isUpdateEnabled()) {
                psTest = createTestStatement();
            }
            PreparedStatement psUpdate = null;
            if (psTest != null && currentAttributs.isUpdateEnabled()) {
                psUpdate = createUpdateStatement();
            }
            PreparedStatement psInsert = null;
            if (psInsert == null && currentAttributs.isInsertEnabled()) {
                psInsert = createInsertStatement();
            }
            // soll zuerst gelöscht werden ?
            if (currentAttributs.isDeleteBeforeImport() && isTestOnly() == false && isDisableDeleteTable() == false) {
                currentAction = "Delete table data";
                status("delete former table data...");
                session.executeUpdate("delete from " + currentAttributs.getTableName());
                if (session.isSuccessful() == false) {
                    error("delete table data failed: " + session.getLastErrorMessage(), FATALS);
                }
            }
            if (errorCode != FATALS) {
                DatasetProvider datasetProvider = null;
                try {
                    currentAction = "Start import data";
                    status("import file " + currentDataFile.getAbsolutePath());
                    if (currentAttributs.isHandleFileAlwaysAsCSV()) {
                    	status("Ignore file extension and handle file as CSV!");
                    }
                    datasetProvider = createDatasetProvider(currentDataFile, false, defaultExtension, currentAttributs);
                    countMaxLines = datasetProvider.retrieveDatasetCount();
                    if (currentAttributs.isSkipFirstRow()) {
                        countMaxLines--;
                    }
                    // reset counter
                    int countDifference = 0;
                    boolean firstLoop = true;
                    FieldTokenizer parser = datasetProvider.createParser();
                    parser.setFieldDescriptions(getFieldDescriptions());
                    if (parser instanceof CSVFieldTokenizer) {
                        ((CSVFieldTokenizer) parser).setDelimiter(currentAttributs.getDelimiter());
                        ((CSVFieldTokenizer) parser).setEnclosure(currentAttributs.getEnclosure());
                    }
                    Object dataset = null;
                    long currentRowsToSkip = currentAttributs.getCountSkipRows();
                    if (currentRowsToSkip > 0) {
                        status("first " + currentRowsToSkip + " will be skipped");
                        countMaxLines = countMaxLines - currentRowsToSkip;
                    }
                    status("count lines to import=" + countMaxLines);
                    while (true) {
                    	currentLineIndex++;
                    	dataset = datasetProvider.getNextDataset();
                        if (interrupted || Thread.currentThread().isInterrupted()) {
                        	status("Import interrupted");
                            break;
                        }
                        if (currentAttributs.getCountSkipRows() > 0 && currentRowsToSkip > 0) {
                            currentAction = "Skip datasets";
                            currentRowsToSkip--;
                            continue;
                        }
                        if (currentAttributs.isSkipFirstRow() && firstLoop) {
                            firstLoop = false;
                            continue; // erste Zeile überspringen
                        }
                        if (dataset == null) {
                        	break;
                        }
                        countDifference++;
                        try {
                        	currentAction = "Read and import data";
                            // ignore blank lines or the first row if requested
                            if (parser.parseRawData(dataset)) {
                                if (currentAttributs.isUpdateEnabled() == false || psTest == null) {
                                    if (currentAttributs.isInsertEnabled()) {
                                    	int count = insertRow(parser, psInsert, currentLineIndex);
                                        if (count >= 0) {
                                        	countInsertProcessed = countInsertProcessed + count;
                                            countInsert++;
                                        }
                                    }
                                } else {
                                	currentAttributs.setBatchSize(0); // to be sure, that we only batch same kind of statements
                                    if (existsDataset(parser, psTest, currentLineIndex)) {
                                        if (currentAttributs.isUpdateEnabled()) {
                                        	int count = updateRow(parser, psUpdate, currentLineIndex);
                                            if (count >= 0) {
                                                countUpdate++;
                                            }
                                        }
                                    } else {
                                        if (currentAttributs.isInsertEnabled()) {
                                        	int count = insertRow(parser, psInsert, currentLineIndex);
                                            if (count >= 0) {
                                            	countInsertProcessed = countInsertProcessed + count;
                                                countInsert++;
                                            }
                                        }
                                    } // if (isDatasetExists(psTest,line))
                                } // if (psTest == null)
                            }
                        } catch (ParserException ex) {
                            error("parse value in linenumber=" + currentLineIndex + " failed: " + ex.getMessage(), WARNINGS);
                        }
                        countLines++;
                        if (currentAttributs.getBatchSize() == 0 && countDifference == countRowsBetweenCommit) {
                            countDifference = 0;
                            session.commit();
                        }
                        if (errorCode == FATALS) {
                            status(" ERRORS occurse whilst import - reading aborted !");
                            break;
                        }
                    } // while ((line = br.readLine()) != null)
                    if (batchCounter > 0) {
                    	// there are some batches left in prepared statement
                    	int [] counters = psInsert.executeBatch();
                    	for (int i : counters) {
                        	countInsertProcessed = countInsertProcessed + ((i >= 0) ? i : 1);
                    	}
                    }
                    session.commit();
                    if (interrupted) {
                        status("INTERRUPT IMPORT");
                    }
                    status("READY: " + String.valueOf(countLines) + " datasets proceeded:");
                    status("  - inserted: " + String.valueOf(countInsert) + " / insert processed: " + String.valueOf(countInsertProcessed) + " updated: " + String.valueOf(countUpdate) + " ignored: " + String.valueOf((countLines - countInsert) - countUpdate));
                } catch (FileNotFoundException fne) {
                    error("writeData: open file " + currentDataFile + "\n" + fne.getMessage(), FATALS);
                } catch (SQLException e) {
                	session.rollback();
                    error("writeData: read file " + currentDataFile + "\n" + e.getMessage(), FATALS);
                } catch (Exception e) {
                    error("writeData: read file " + currentDataFile + "\n" + e.getMessage(), FATALS);
                } finally {
                	if (datasetProvider != null) {
                		status("close file");
                    	datasetProvider.closeDatasetProvider();
                	}
                }
            } // if (errorCode != FATALS)
            if (psTest != null) {
                psTest.close();
            }
            if (psUpdate != null) {
                psUpdate.close();
            }
            if (psInsert != null) {
                psInsert.close();
            }
            if (session.isSuccessful() == false) {
                error(session.getLastErrorMessage(), WARNINGS);
            }
        } catch (SQLException sqle) {
            error(sqle.getMessage(), FATALS);
        }
        currentAction = "import data finished";
        return errorCode != FATALS && interrupted == false;
    }
    
    public long retrieveDatasetCount(File file) {
    	long count = 0;
        DatasetProvider dsp = null;
    	try {
			dsp = createDatasetProvider(file, false, defaultExtension, currentAttributs);
			count = dsp.retrieveDatasetCount();
		} catch (Exception e) {
			getLogger().error("retrieveDatasetCount file=" + file + " failed: " + e.getMessage(), e);
		} finally {
            if (dsp != null) {
                dsp.closeDatasetProvider();
            }
        }
    	return count;
    }
    
    public long retrieveDatasetCount(File file, ImportAttributes opts) {
    	long count = 0;
        DatasetProvider dsp = null;
    	try {
			dsp = createDatasetProvider(file, false, defaultExtension, opts);
			count = dsp.retrieveDatasetCount();
		} catch (Exception e) {
			getLogger().error("retrieveDatasetCount file=" + file + " failed: " + e.getMessage(), e);
		} finally {
            if (dsp != null) {
                dsp.closeDatasetProvider();
            }
        }
    	return count;
    }

    protected boolean isDebugEnabled() {
        if (instanceLogger != null) {
            return instanceLogger.isDebugEnabled();
        } else {
            return staticLogger.isDebugEnabled();
        }
    }

    protected void debug(String message) {
        if (instanceLogger != null) {
            instanceLogger.debug(message);
        } else {
            staticLogger.debug(message);
        }
    }

    protected void status(String message) {
        if (instanceLogger != null) {
            instanceLogger.info(message);
        } else {
            staticLogger.info(message);
        }
    }

    /**
     * resets al internal states (even the field descriptions !)
     */
    public void reset() {
        setErrorCode(NORMAL);
        startTime = 0;
        stopTime = 0;
        countLines = 0;
        countInsert = 0;
        countInsertProcessed = 0;
        countUpdate = 0;
        batchCounter = 0;
        lastErrorMessage = null;
        fields = new ArrayList<FieldDescription>();
        lastValues.clear();
        errorLogFileName = null;
        if (session != null) {
            session.close();
        }
    }
    
    protected void error(String message, int newErrorCode) {
    	error(message, newErrorCode, null);
    }

    protected void error(String message, int errorcode, Throwable t) {
    	final Logger logger = (instanceLogger != null) ? instanceLogger : staticLogger;
    	if (errorcode == FATALS) {
        	if (t != null) {
        		logger.error(message, t);
                if (t instanceof SQLException) {
                	SQLException ne = ((SQLException) t).getNextException();
                	if (ne != null && ne != t) {
                		logger.error("next exception:", t);
                	}
                }
        	} else {
        		logger.error(message);
        	}
        } else {
        	if (t != null) {
        		logger.warn(message, t);
                if (t instanceof SQLException) {
                	SQLException ne = ((SQLException) t).getNextException();
                	if (ne != null && ne != t) {
                		logger.warn("next exception:", t);
                	}
                }
        	} else {
        		logger.warn(message);
        	}
        }
        lastErrorMessage = message;
        setErrorCode(errorcode);
    }

    @Override
	public String getLogFileName() {
        if (errorCode > NORMAL) {
            return errorLogFileName;
        } else {
            return null;
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public boolean isSuccessful() {
        return (errorCode == NORMAL);
    }

    /**
     * set the error-code if the new value is higher then the former value.
     * 
     * @param errorCode
     *            the new value
     */
    protected void setErrorCode(int code) {
        if (errorCode < code) {
            errorCode = code;
        }
    }

    @Override
	public long getCountMaxInput() {
        return countMaxLines;
    }

    @Override
	public long getCountUpdates() {
        return countUpdate;
    }

    @Override
	public void abort() {
        interrupt();
    }

    @Override
	public long getCountCurrInput() {
        return countLines;
    }

    @Override
	public long getCountIgnored() {
        return countLines - countInsert - countUpdate;
    }

    @Override
	public long getCountInserts() {
        return countInsert;
    }

    @Override
	public long getStartTime() {
        return startTime;
    }

    @Override
	public int getStatusCode() {
        return errorCode;
    }

    @Override
	public long getStopTime() {
        return stopTime;
    }

    @Override
	public boolean isRunning() {
        return startTime > 0 && stopTime == 0;
    }

    @Override
	public boolean isStopped() {
        return stopTime > 0;
    }

    public String getDefaultExtension() {
    	return defaultExtension;
    }
    
    public void setDefaultExtension(String ext) {
        if (ext != null && ext.trim().length() > 0) {
            ext = ext.trim();
            if (ext.endsWith(".")) {
                throw new IllegalArgumentException("ext cannot end with a dot");
            } else {
                int pos = ext.lastIndexOf(".");
                if (pos != -1) {
                    this.defaultExtension = ext.substring(pos + 1);
                } else {
                    this.defaultExtension = ext;
                }
            }
        }
    }
    
    public boolean isXLSFile(File f) {
    	return isXLSFileExt(getFileExtension(f.getName(), defaultExtension));
    }
    
    public boolean isXLSXFile(File f) {
    	return isXLSXFileExt(getFileExtension(f.getName(), defaultExtension));
    }
    
    public boolean isExcelFile(File f) {
    	return isXLSFile(f) || isXLSXFile(f); 
    }
    
    public boolean isDataFile(File f) {
    	return isCSVFile(f) || isExcelFile(f);
    }

    public boolean isCSVFile(File f) {
    	return isCSVFileExt(getFileExtension(f.getName(), defaultExtension));
    }
    
    public FieldTokenizer createFieldTokenizer(String fileName) {
    	if (fileName != null) {
            if (isXLSFileExt(getFileExtension(fileName, defaultExtension)) || isXLSXFileExt(getFileExtension(fileName, defaultExtension))) {
                return new XLSFieldParser();
            } else {
                return new CSVFieldTokenizer();
            }
    	} else {
    		return null;
    	}
    }

    public long getCountRowsBetweenCommit() {
        return countRowsBetweenCommit;
    }

    public void setCountRowsBetweenCommit(long countRowsBetweenCommit) {
        this.countRowsBetweenCommit = countRowsBetweenCommit;
    }
    
    private void putLastValue(String columnName, Object value) {
    	if (columnName == null) {
    		throw new IllegalArgumentException("columnName cannot be null");
    	}
    	if (value != null) {
        	lastValues.put(columnName.toLowerCase(), value);
    	}
    }

	@Override
	public Object getLastValue(String columnName) {
		Object lastValue = lastValues.get(columnName.toLowerCase());
		if (lastValue != null) {
			return lastValue; 
		} else {
			return "";
		}
	}
    
}
