package sqlrunner.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import dbtools.DatabaseSession;
import dbtools.SQLPSParam;
import dbtools.SQLStatement;
import sqlrunner.base64.Base64;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.Importer;
import sqlrunner.generator.SQLCodeGenerator;

/**
 * @author lolling.jan
 * Handler der die eigentlichen Importe in die DB triggert.
 */
public class TableImportHandler extends DefaultHandler {

    private static final Logger staticLogger = Logger.getLogger(TableImportHandler.class);
    private Logger logger = staticLogger;
    private ImportDescription impDesc;
    private DatabaseSession session;
    private SQLStatement sqlPsCount;
    private SQLStatement sqlPsInsert;
    private SQLStatement sqlPsUpdate;
    private PreparedStatement psCount;
    private PreparedStatement psInsert;
    private PreparedStatement psUpdate;
    private String tableName;
    private String columnName;
    private String columnCode;
    private final StringBuffer value = new StringBuffer();
    private boolean isFirstElement = true;
    private boolean inRow = false;
    private final HashMap<String, ValueDesc> valueMap = new HashMap<String, ValueDesc>();
    private boolean updateEnabled;
    private boolean tableHasPrimaryKey;
    private long countAll;
    private long countCurrDatasets;
    private long countDatasetInserted;
    private long countDatasetUpdated;
    private javax.swing.Timer timer;
    private Locator locator;
    private boolean testOnly = false;
    private int status = Importer.NOT_STARTED;
    public static final int COUNT_DS_UNTIL_COMMIT = 1000;
    private int countUntilCommit = 0;
    private boolean tableAlreadyDeletedBefore = false;
    
    TableImportHandler(
            DatabaseSession session, 
            ImportDescription impDesc, 
            boolean updateEnabled,
            boolean tableAlreadyDeletedBefore,
            boolean testOnly) {
        this.session = session;
        this.impDesc = impDesc;
        this.tableName = impDesc.getTable().getAbsoluteName().toLowerCase();
        this.updateEnabled = updateEnabled;
        this.tableHasPrimaryKey = impDesc.getTable().hasPrimaryKeyFields();
        this.testOnly = testOnly;
        this.tableAlreadyDeletedBefore = tableAlreadyDeletedBefore;
        logger.debug("import handler for " + impDesc.toString() + " created...");
    }

    public void setLogger(Logger instanceLogger) {
        if (instanceLogger == null) {
            throw new IllegalArgumentException("instanceLogger cannot be null");
        }
        this.logger = instanceLogger;
    }
    
    public Logger getLogger() {
        return logger;
    }

    public void resetToStaticLogger() {
        this.logger = staticLogger;
        if (session != null) {
            session.resetLoggerToStaticClassLogger();
        }
    }
    
    public int getStatus() {
        return status;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startDocument() throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new SAXStoppedException("stopped");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("startDocument() - import file " + impDesc.getXmlFile().getAbsolutePath());
        }
        if (testOnly) {
            logger.info("import in test only mode");
        }
        if (locator == null) {
            logger.info("no xml locator are available");
        }
        try {
            sqlPsInsert = SQLCodeGenerator.getInstance().buildPSInsertSQLStatement(impDesc.getTable(), true);
            psInsert = session.createPreparedStatement(sqlPsInsert.getSQL());
            if (tableHasPrimaryKey) {
                if ((tableAlreadyDeletedBefore && updateEnabled == false) == false) {
                    sqlPsCount = SQLCodeGenerator.getInstance().buildPSCountSQLStatement(impDesc.getTable(), true);
                    if (sqlPsCount.isSqlCodeValid()) {
                        psCount = session.createPreparedStatement(sqlPsCount.getSQL());
                    } else {
                        logger.warn("no valid count statement (count disabled):" + sqlPsCount.getSQL());
                    }
                }
                sqlPsUpdate = SQLCodeGenerator.getInstance().buildPSUpdateSQLStatement(impDesc.getTable(), true);
                if (sqlPsUpdate.isSqlCodeValid()) {
                    psUpdate = session.createPreparedStatement(sqlPsUpdate.getSQL());
                } else {
                    status = Importer.WARNINGS;
                    logger.warn("no valid update statement (update disabled):" + sqlPsUpdate.getSQL());
                }
            }
        } catch (SQLException e) {
            status = Importer.FATALS;
            logger.error("startDocument():" + e.getMessage(), e);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            if (testOnly == false) {
                commit(false);
            }
            if (psInsert != null) {
                psInsert.close();
            }
            if (psCount != null) {
                psCount.close();
            }
            if (psUpdate != null) {
                psUpdate.close();
            }
        } catch (SQLException e) {
            status = Importer.FATALS;
            logger.error("endDocument():" + e.getMessage(), e);
            throw new SAXException(e);
        }
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        logger.info("import finished: " + countCurrDatasets + " proceeded, " + countDatasetInserted + " inserted, " + countDatasetUpdated + " udated, " + (countCurrDatasets - countDatasetInserted - countDatasetUpdated) + " ignored.");
    }

    @Override
    public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new SAXStoppedException("stopped");
        }
        if (isFirstElement) {
            isFirstElement = false;
            if (qName.equals("table")) {
            	logger.info("parsing import file by generic tag names");
            	if (attributes.getValue("name").equals(tableName) == false) {
                    throw new SAXException("wrong table found in attribute name=" + attributes.getValue("name") + " : " + tableName + " expected!");
            	}
            } else if (qName.equals(tableName) == false) {
                throw new SAXException("wrong table tag found: " + qName + ", tag " + tableName + " expected!");
            } else {
            	logger.info("parsing import file by table name as tag name");
            }
        } else {
            value.setLength(0);
            if (qName.equals("row")) {
                inRow = true;
                valueMap.clear();
            } else {
            	if (qName.equals("column")) {
            		columnName = attributes.getValue("name");
                    columnCode = attributes.getValue("code");
            	} else {
                    columnName = qName;
                    columnCode = attributes.getValue("code");
            	}
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if ((length > 0) && (ch != null)) {
            value.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new SAXStoppedException("stopped");
        }
        if (inRow) {
            if (qName.equals("row")) {
                storeDataset();
                inRow = false;
            } else {
                final ValueDesc valueDesc = new ValueDesc();
                valueDesc.code = columnCode;
                valueDesc.value = value.toString().trim();
                valueMap.put(columnName.trim(), valueDesc);
            }
        }
    }

    private int countDatasets() {
        int count = -1;
        if (sqlPsCount != null && sqlPsCount.isSqlCodeValid()) {
            SQLPSParam psParam = null;
            ValueDesc valueDesc = null;
            for (int i = 0; i < sqlPsCount.getParams().size(); i++) {
                psParam = sqlPsCount.getParams().get(i);
                valueDesc = valueMap.get(psParam.getName().toLowerCase());
                if (valueDesc != null) {
                    psParam.setValue(valueDesc.value);
                    psParam.setValueCode(valueDesc.code);
                }
            }
            if (setupParameterValues(psCount, sqlPsCount)) {
                try {
                    final ResultSet rs = psCount.executeQuery();
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                    rs.close();
                } catch (SQLException sqle) {
                    status = Importer.FATALS;
                    logger.error("countDatasets() - countDatasets failed: " + sqle.getMessage(), sqle);
                }
            }
        }
        return count;
    }
    
    private int getCurrentLineNumber() {
        if (locator != null) {
            return locator.getLineNumber();
        } else {
            return 0;
        }
    }

    private boolean insertDataset(boolean usebatch) {
        boolean ok = false;
        if (sqlPsInsert != null) {
            SQLPSParam psParam = null;
            ValueDesc valueDesc = null;
            for (int i = 0; i < sqlPsInsert.getParams().size(); i++) {
                psParam = sqlPsInsert.getParams().get(i);
                valueDesc = valueMap.get(psParam.getName().toLowerCase());
                if (valueDesc != null) {
                    psParam.setValue(valueDesc.value);
                    psParam.setValueCode(valueDesc.code);
                } else {
                    psParam.setValue(null);
                    psParam.setValueCode(null);
                }
            }
            if (setupParameterValues(psInsert, sqlPsInsert)) {
                try {
                    if (testOnly == false) {
                    	if (usebatch) {
                            psInsert.addBatch();
                    	} else {
                            psInsert.executeUpdate();
                    	}
                    }
                    ok = true;
                } catch (SQLException sqle) {
                    status = Importer.FATALS;
                    logger.error("insertDataset in " + tableName + ".xml at line=" + locator.getLineNumber() + " failed: " + sqle.getMessage(), sqle);
                }
            }
        }
        return ok;
    }

    private boolean updateDataset() {
        boolean ok = false;
        if (sqlPsUpdate != null) {
            if (sqlPsUpdate.isSqlCodeValid()) {
                SQLPSParam psParam = null;
                ValueDesc valueDesc = null;
                for (int i = 0; i < sqlPsUpdate.getParams().size(); i++) {
                    psParam = sqlPsUpdate.getParams().get(i);
                    valueDesc = valueMap.get(psParam.getName().toLowerCase());
                    if (valueDesc != null) {
                        psParam.setValue(valueDesc.value);
                        psParam.setValueCode(valueDesc.code);
                    } else {
                        psParam.setValue(null);
                        psParam.setValueCode(null);
                    }
                }
                if (setupParameterValues(psUpdate, sqlPsUpdate)) {
                    try {
                        if (testOnly == false) {
                            psUpdate.executeUpdate();
                        }
                        ok = true;
                    } catch (SQLException sqle) {
                        status = Importer.FATALS;
                        logger.error("updateDataset in " + tableName + ".xml at line=" + locator.getLineNumber() + " failed: " + sqle.getMessage(), sqle);
                    }
                }
            }
        }
        return ok;
    }

    private void storeDataset() {
        final int count = countDatasets();
        boolean usebatch = (updateEnabled == false) || (tableHasPrimaryKey == false);
        if (count != -1 && updateEnabled && tableHasPrimaryKey) {
            if (count == 1) {
                if (updateDataset()) {
                    countDatasetUpdated++;
                }
            } else if (count == 0) {
                if (insertDataset(usebatch)) {
                    countDatasetInserted++;
                }
            } else {
                status = Importer.FATALS;
                logger.error(
                        "storeDataset() - primary key invalid ! more then ONE dataset found for pk condition !",
                        null);
            }
        } else if (count < 1) {
            if (insertDataset(usebatch)) {
                countDatasetInserted++;
            }
        }
        countCurrDatasets++;
        if (countUntilCommit == COUNT_DS_UNTIL_COMMIT) {
            if (testOnly == false) {
                commit(usebatch);
            }
            countUntilCommit = -1;
        }
        countUntilCommit++;
    }

    private void commit(boolean usebatch) {
    	if (usebatch) {
        	try {
            	psInsert.executeBatch();
        	} catch (SQLException sqle) {
        		SQLException ne = sqle.getNextException();
        		if (ne != null) {
                    logger.error("insertDataset in " + tableName + ".xml at line=" + locator.getLineNumber() + " failed: " + ne.getMessage(), sqle);
        		} else {
                    logger.error("insertDataset in " + tableName + ".xml at line=" + locator.getLineNumber() + " failed: " + sqle.getMessage(), sqle);
        		}
                status = Importer.FATALS;
        	}
    	}
        session.commit();
    }

    private String convertBase64ToText(String base64String) throws UnsupportedEncodingException {
        return Base64.getText(base64String, Base64.DEFAULT_CHARSET);
    }

    private byte[] convertBase64ToByteArray(String base64String) throws UnsupportedEncodingException {
        return Base64.decodeFromBase64String(base64String);
    }

    static class ValueDesc {

        public String value;
        public String code;
    }

    private boolean setupParameterValues(PreparedStatement ps, SQLStatement sqlps) {
        boolean ok = true;
        for (int i = 0; i < sqlps.getParams().size(); i++) {
            SQLPSParam parameter = sqlps.getParams().get(i);
            String paramValue = parameter.getValue();
            try {
                if (parameter.getBasicType() == BasicDataType.CHARACTER.getId()) {
                    if ((paramValue != null) && (paramValue.length() > 0)) {
                        if ("base64".equals(parameter.getValueCode())) {
                            paramValue = convertBase64ToText(paramValue);
                        }
                        ps.setString(parameter.getIndex(), paramValue);
                    } else {
                        ps.setNull(parameter.getIndex(), Types.VARCHAR);
                    }
                } else if (BasicDataType.isNumberType(parameter.getBasicType())) {
                    if ((paramValue != null) && (paramValue.length() > 0)) {
                        try {
                            ps.setDouble(parameter.getIndex(), Double.parseDouble(paramValue.trim()));
                        } catch (NumberFormatException nfe) {
                            ok = false;
                            status = Importer.WARNINGS;
                            logger.error("error while interpreting value as number " + nfe.getMessage() + " in linenumber " + getCurrentLineNumber());
                        }
                    } else {
                        ps.setNull(parameter.getIndex(), Types.NUMERIC);
                    }
                } else if (BasicDataType.isBooleanType(parameter.getBasicType())) {
                    if ((paramValue != null) && (paramValue.length() > 0)) {
                        ps.setBoolean(parameter.getIndex(), Boolean.parseBoolean(paramValue.trim()));
                    } else {
                        ps.setNull(parameter.getIndex(), Types.BOOLEAN);
                    }
                } else if (BasicDataType.isDateType(parameter.getBasicType())) {
                    if (paramValue != null) {
                        if (paramValue.length() > 0) {
                            if (parameter.getValueCode() != null && "string".equals(parameter.getValueCode()) == false) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat(parameter.getValueCode());
                                    java.util.Date d = sdf.parse(paramValue);
                                    ps.setTimestamp(parameter.getIndex(), new Timestamp(d.getTime()));
                                } catch (Exception e) {
                                    logger.error("invalid date format code=" + parameter.getValueCode() + " in linenumber " + getCurrentLineNumber());
                                }
                            } else {
                                try {
                                    ps.setTimestamp(parameter.getIndex(), new Timestamp(Long.parseLong(paramValue)));
                                } catch (NumberFormatException e) {
                                    status = Importer.WARNINGS;
                                    logger.error("invalid number format " + e.getMessage() + " in linenumber " + getCurrentLineNumber());
                                    ok = false;
                                }
                            }
                        }
                    } else {
                        ps.setNull(parameter.getIndex(), Types.DATE);
                    }
                } else if (parameter.getBasicType() == BasicDataType.CLOB.getId()) {
                    if ((paramValue != null) && (paramValue.length() > 0)) {
                        if ("base64".equals(parameter.getValueCode())) {
                            paramValue = convertBase64ToText(paramValue);
                        }
                        ps.setCharacterStream(parameter.getIndex(), new StringReader(paramValue), paramValue.length());
                    } else {
                        ps.setNull(parameter.getIndex(), Types.CLOB);
                    }
                } else if (parameter.getBasicType() == BasicDataType.BINARY.getId()) {
                    if ((paramValue != null) && (paramValue.length() > 0)) {
                        if ("base64".equals(parameter.getValueCode())) {
                            byte[] binaryValue = convertBase64ToByteArray(paramValue);
                            ps.setBytes(parameter.getIndex(), binaryValue);
                        } else {
                            try {
                                final File f = new File(paramValue);
                                ps.setBinaryStream(parameter.getIndex(), new FileInputStream(f), (int) f.length());
                            } catch (FileNotFoundException fnfe) {
                                status = Importer.WARNINGS;
                                logger.error("setupParameterValues " + fnfe.getMessage(), fnfe);
                                ok = false;
                            }
                        }
                    } else {
                        ps.setNull(parameter.getIndex(), Types.LONGVARBINARY);
                    }
                } // if ((columnTypeNames[col].equals....
            } catch (SQLException sqle) {
                status = Importer.WARNINGS;
                logger.error("set parameter=" + parameter + "value=" + paramValue + " failed: " + sqle.getMessage() + " in linenumber " + getCurrentLineNumber(), sqle);
                ok = false;
            } catch (UnsupportedEncodingException e) {
                status = Importer.WARNINGS;
                logger.error("set parameter=" + parameter + "value=" + paramValue + " failed: " + e.getMessage() + " in linenumber " + getCurrentLineNumber(), e);
                ok = false;
			}
        }
        return ok;
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        logger.error("xml error in line=" + e.getLineNumber() + " column=" + e.getColumnNumber());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        logger.error("xml fatalError in line=" + e.getLineNumber() + " column=" + e.getColumnNumber());
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        logger.warn("xml warning in line=" + e.getLineNumber() + " column=" + e.getColumnNumber());
    }
    
    public void setCountAll(long count) {
        this.countAll = count;
    }
    
    public long getCountAll() {
        return countAll;
    }

    public long getCountCurrDatasets() {
        return countCurrDatasets;
    }

    public long getCountDatasetInserted() {
        return countDatasetInserted;
    }

    public long getCountDatasetUpdated() {
        return countDatasetUpdated;
    }
    
}
