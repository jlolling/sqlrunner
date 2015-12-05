package sqlrunner.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Properties;

import org.apache.log4j.Logger;

import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import sqlrunner.base64.Base64;
import sqlrunner.export.Exporter;
import sqlrunner.flatfileimport.BasicDataType;

/**
 * Exporter for a single table into a output stream
 * @author jan
 *
 */
public class XMLExporter implements Exporter {
	
	private static final Logger defaultLogger = Logger.getLogger(XMLExporter.class);
	private Logger exportLogger = defaultLogger;
	public static final String PROP_ENCODING = "encoding";
	public static final String PROP_XML_INDENTION = "indention";
	public static final String PROP_BASE64_CODED_STRINGS = "base64CodedStrings";
	public static final String PROP_DATE_FORMAT = "dateFormat";
	public static final String PROP_NUMBERFORMAT_LOCALE = "numberFormatLocale";
	private boolean base64CodedStrings = true;
	private final static String DEFAULT_DATE_FORMAT = "yyyyMMdd";
	private String dateFormat = DEFAULT_DATE_FORMAT;
	private boolean xmlIndention = false;
	private final String DEFAULT_ENCODING = "UTF-8";
	private String encoding = DEFAULT_ENCODING;
	private final Locale DEFAULT_NUMBERFORMAT_LOCALE = Locale.US;
	private Locale numberFormatLocale = DEFAULT_NUMBERFORMAT_LOCALE;
	private String currentAction = "";
	private long currentCount = 0;
	private String query = null;
    private DatabaseSession  exportSession;
    private ConnectionDescription cd;
    private boolean          codeStringInBase64 = false;
    private File outputFile;
    private String tableName;
    private String sql;
    private boolean interrupted;
    
	public void setQuery(String sql) {
		this.query = sql;
	}
	
	public String getQuery() {
		return query;
	}
	
	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getCurrentAction()
	 */
	public String getCurrentAction() {
		return currentAction;
	}

	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getCurrentCount()
	 */
	public long getCurrentCount() {
		return currentCount;
	}

	public boolean connect() throws Exception {
		if (cd == null) {
			throw new Exception("No connection description set");
		}
		exportSession = new DatabaseSession();
		exportSession.setConnectionDescription(cd);
		return exportSession.connect();
	}

	public ConnectionDescription getConnectionDescription() {
		return cd;
	}
	
	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#setConnectionDescription(dbtools.ConnectionDescription)
	 */
	public void setConnectionDescription(ConnectionDescription connDesc) {
		if (connDesc == null) {
			throw new IllegalArgumentException("connDesc cannot be null");
		}
		this.cd = connDesc.clone();
		this.cd.setAutoCommit(false);
	}

	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#setProperties(java.util.Properties)
	 */
	public void setProperties(Properties properties) throws Exception {
		this.base64CodedStrings = Boolean.getBoolean(properties.getProperty(PROP_BASE64_CODED_STRINGS, "true"));
		this.xmlIndention = Boolean.getBoolean(properties.getProperty(PROP_XML_INDENTION, "false"));
		this.dateFormat = properties.getProperty(PROP_DATE_FORMAT, "dd.MM.yyyy");
		this.encoding = properties.getProperty(PROP_ENCODING, DEFAULT_ENCODING);
		this.numberFormatLocale = new Locale(properties.getProperty(PROP_NUMBERFORMAT_LOCALE, DEFAULT_NUMBERFORMAT_LOCALE.toString()));
	}
	
	public boolean usesBase64CodingForStrings() {
		return base64CodedStrings;
	}
	
	public boolean isUseXmlIndention() {
		return xmlIndention;
	}
	
	public String getDateFormat() {
		return dateFormat;
	}
	
	public String getEncoding() {
		return encoding;
	}
	
	public Locale getNumberFormatLocale() {
		return numberFormatLocale;
	}

	public void setMaxDatasetsPerFile(long maxDatasetsPerFile) {
		// TODO Auto-generated method stub
		
	}

	public void setLogger(Logger logger) {
		this.exportLogger = logger;
	}

	public Logger getLogger() {
		return exportLogger;
	}

	public void resetToStaticLogger() {
		exportLogger = defaultLogger;
	}

	public void exportData() throws IOException, SQLException {
        currentAction = "select " + tableName;
        if (sql == null) {
            sql = "select * from " + tableName;
        }
        final ResultSet rs = exportSession.executeQuery(sql);
        if (exportSession.isSuccessful() == false) {
        	throw new SQLException(exportSession.getLastErrorMessage());
        } else {
            XMLWriter xmlWriter = null;
            try {
                xmlWriter = new XMLWriter(new FileOutputStream(outputFile));
                // write the document header
                xmlWriter.setFormatOutput(false);
                xmlWriter.writeXMLBegin();
                xmlWriter.writeTagBegin(tableName.toLowerCase(), null);
            } catch (IOException ioe) {
                exportLogger.error("Create XMLWriter failed: " + ioe.toString());
                throw ioe;
            }
            try {
                final ResultSetMetaData rsmd = rs.getMetaData();
                final int cols = rsmd.getColumnCount();
                Object[] resultRow = new Object[cols];
                int[] types = new int[cols];
                // read column name from meta data
                String[] columnNames = new String[cols];
                for (int i = 0; i < cols; i++) {
                    columnNames[i] = rsmd.getColumnName(i + 1).toLowerCase();
                    types[i] = BasicDataType.getBasicTypeByTypes(rsmd.getColumnType(i + 1));
                }
                while (rs.next()) {
                    if (interrupted) {
                    	exportLogger.warn("Export interrupted");
                        break;
                    } else {
                        for (int i = 0; i < cols; i++) {
                            // build a result row dependend on type
                        	if (BasicDataType.isDateType(types[i])) {
                        		resultRow[i] = rs.getTimestamp(i + 1);
                        	} else if (BasicDataType.isNumberType(types[i])) {
                        		resultRow[i] = rs.getString(i + 1);
                        	} else if (types[i] == BasicDataType.CLOB.getId()) {
                        		 if (rs.getObject(i + 1) != null) {
                                     final Clob clob = rs.getClob(i + 1);
                                     resultRow[i] = clob.getSubString(1, (int) clob.length());
                                 } else {
                                     resultRow[i] = null;
                                 }
                        	} else if (BasicDataType.isStringType(types[i])) {
                        		resultRow[i] = rs.getString(i + 1);
                        	} else if (BasicDataType.isBooleanType(types[i])) {
                        		resultRow[i] = rs.getBoolean(i + 1);
                        	} else if (BasicDataType.BINARY.getId() == types[i]) {
                        		if (rs.getObject(i + 1) != null) {
                                    resultRow[i] = rs.getBinaryStream(i + 1);
                                } else {
                                    resultRow[i] = null;
                                }
                        	} else {
                        		resultRow[i] = null;
                        	}
                        } // for (int i = 0; i < cols; i++)
                        processDataSet(xmlWriter, resultRow, columnNames, types);
                    } // if (interrupted)
                } // while (rs.next())
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                } catch (SQLException sqle) {}
                if (xmlWriter != null) {
                    try {
                        xmlWriter.writeTagEnd();
                        xmlWriter.close();
                    } catch (IOException e) {
                        exportLogger.error("write document end tag failed: " + e.getMessage(), e);
                        throw e;
                    }
                }
            }
        } // else if (session.isSuccessful() == false)
	}

    private void processDataSet(
            XMLWriter xmlWriter, 
            Object[] resultRow, 
            String[] columnNames,
            int[] types) throws IOException {
        xmlWriter.writeTagBegin("row", null);
        final String[][] attributes = new String[1][2];
        for (int i = 0; i < columnNames.length; i++) {
            Object content = resultRow[i];
            if (content != null) {
            	if (BasicDataType.isDateType(types[i])) {
                    xmlWriter.writeTagBegin(columnNames[i], null);
                    if (content instanceof Timestamp) {
                        xmlWriter.writeTextContent(String.valueOf(((Timestamp) content).getTime()));
                    } else if (content instanceof java.sql.Time) {
                        xmlWriter.writeTextContent(String.valueOf(((java.sql.Time) content).getTime()));
                    }
            	} else if (BasicDataType.isNumberType(types[i])) {
                    xmlWriter.writeTagBegin(columnNames[i], null);
                    if (content != null) {
                        xmlWriter.writeTextContent(content.toString());
                    }
            	} else if (types[i] == BasicDataType.CLOB.getId()) {
                    attributes[0][0] = "code";
                    attributes[0][1] = "base64";
                    xmlWriter.writeTagBegin(columnNames[i], attributes);
                    if (content != null) {
                        xmlWriter.writeCDATAContent(Base64.toString(
                                Base64.encode(((String) content).getBytes(Base64.DEFAULT_CHARSET)),
                                true));
                    }
            	} else if (BasicDataType.isStringType(types[i])) {
                    if (codeStringInBase64) {
                        attributes[0][0] = "code";
                        attributes[0][1] = "base64";
                        xmlWriter.writeTagBegin(columnNames[i], attributes);
                        if (content != null) {
                            xmlWriter.writeCDATAContent(Base64.toString(
                                    Base64.encode(((String) content).getBytes(Base64.DEFAULT_CHARSET)),
                                    true));
                        }
                    } else {
                        xmlWriter.writeTagBegin(columnNames[i], null);
                        if (content != null) {
                            xmlWriter.writeTextContent((String) content);
                        }
                    }
            	} else if (BasicDataType.isBooleanType(types[i])) {
                    xmlWriter.writeTagBegin(columnNames[i], null);
                    if (content != null) {
                        xmlWriter.writeTextContent(content.toString());
                    }
            	} else if (BasicDataType.BINARY.getId() == types[i]) {
                    attributes[0][0] = "code";
                    attributes[0][1] = "base64";
                     xmlWriter.writeTagBegin(columnNames[i], attributes);
                     if (content != null) {
                         final byte[] buffer = new byte[Base64.TWENTYFOURBITGROUP];
                         int length = -1;
                         final StringBuffer sb = new StringBuffer();
                         final InputStream is = (InputStream) content;
                         while ((length = is.read(buffer)) != -1) {
                             sb.append(Base64.toString(Base64.encode(buffer, length)));
                         }
                         is.close();
                         xmlWriter.writeCDATAContent(sb.toString());
                     }
            	} else {
                    xmlWriter.writeTagBegin(columnNames[i], null);
                    if (content != null) {
                        xmlWriter.writeTextContent(content.toString());
                    }
            	}
                xmlWriter.writeTagEnd();
            }
        }
        xmlWriter.writeTagEnd();
    }

    @Override
	public void abort() {
    	interrupted = true;
		if (exportSession != null) {
			exportSession.cancelStatement();
		}
	}

	@Override
	public int getCurrentRowNum() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public void setDateFormat(String pattern) {
		this.dateFormat = pattern;
	}
	
}
