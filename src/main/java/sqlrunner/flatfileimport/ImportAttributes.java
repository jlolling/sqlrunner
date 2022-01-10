package sqlrunner.flatfileimport;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import sqlrunner.base64.Base64;

public class ImportAttributes {
	
	private static Logger logger = LogManager.getLogger(ImportAttributes.class);
	private String tableName;
	private boolean insertEnabled;
	private boolean updateEnabled;
	private boolean deleteBeforeImport;
	private boolean skipFirstRow;
	private boolean handleFileAlwaysAsCSV;
	private long countSkipRows;
	private String delimiter;
	private String enclosure;
	private boolean ignoreLineBreakInEnclosedValues = false;
	private String preProcessSQL;
	private String postProcessSQL;
	private boolean preProcessEnabled;
	private boolean postProcessEnabled;
	private int columnCount;
	private String charsetName;
	public static final String DEFAULT_CHARSET = System.getProperty("file.encoding");
	private String sheetName;
	private int batchSize;
	
	public void setLogger(Logger newLogger) {
		logger = newLogger;
	}
	
	public void storeInto(Properties properties) {
		if (tableName != null) {
			properties.put("TABLE_NAME", tableName);
		}
		properties.put("INSERT_ENABLED", insertEnabled ? "true" : "false");
		properties.put("UPDATE_ENABLED", updateEnabled ? "true" : "false");
		properties.put("DELETE_BEFORE_IMPORT", (deleteBeforeImport ? "true" : "false"));
		properties.put("SKIP_FIRST_ROW", skipFirstRow ? "true" : "false");
		properties.put("HANDLE_ALWAYS_AS_CSV", handleFileAlwaysAsCSV ? "true" : "false");
		properties.put("SKIP_ROWS", String.valueOf(countSkipRows));
		properties.put("DELIMITER", getTokenFromDelimiter(delimiter));
        if (enclosure != null) {
        	properties.put("ENCLOSURE", enclosure);
        	properties.put("IGNORE_ENCLOSED_LINE_BREAK", ignoreLineBreakInEnclosedValues);
        } else {
        	properties.remove("ENCLOSURE");
        }
        properties.put("COLUMN_COUNT", String.valueOf(columnCount));
        if (preProcessSQL != null && preProcessSQL.trim().length() > 1) {
            try {
            	properties.put("PRE_PROCESS_SQL", Base64.toString(Base64.encode(preProcessSQL.getBytes(Base64.DEFAULT_CHARSET)), false));
            	properties.put("PRE_PROCESS_SQL_ENABLED", String.valueOf(preProcessEnabled));
            } catch (UnsupportedEncodingException ex) {
                properties.put("PRE_PROCESS_SQL_ENABLED", String.valueOf(false));
                logger.error("PRE_PROCESS_SQL_ENABLED cannot be encoded", ex);
            }
        } else {
        	properties.remove("PRE_PROCESS_SQL");
        	properties.remove("PRE_PROCESS_SQL_ENABLED");
        }
        if (postProcessSQL != null && postProcessSQL.trim().length() > 1) {
            try {
            	properties.put("POST_PROCESS_SQL", Base64.toString(Base64.encode(postProcessSQL.getBytes(Base64.DEFAULT_CHARSET)), false));
            	properties.put("POST_PROCESS_SQL_ENABLED", String.valueOf(postProcessEnabled));
            } catch (UnsupportedEncodingException ex) {
            	properties.put("POST_PROCESS_SQL_ENABLED", String.valueOf(false));
                logger.error("POST_PROCESS_SQL_ENABLED cannot be encoded", ex);
            }
        }
		if (charsetName != null) {
			properties.put("CHARSET", charsetName);
		} else {
			properties.put("CHARSET", DEFAULT_CHARSET);
		}
		if (sheetName != null) {
			properties.put("SHEET_NAME", sheetName);
		} else {
			properties.remove("SHEET_NAME");
		}
		if (batchSize > 1) {
			properties.put("BATCH_SIZE", String.valueOf(batchSize));
		} else {
			properties.remove("BATCH_SIZE");
		}
	}

	private static String getDelimiterFromToken(String token) {
        if (token == null) {
            return null;
        }
        if (token.equals("{TAB}")) {
            return "\t";
        } else if (token.equals("{SPACE}")) {
            return " ";
        } else {
            return token;
        }
    }
	
	private static String getTokenFromDelimiter(String delimiter) {
		if (delimiter == null) {
			return null;
		} else if ("\t".equals(delimiter)) {
			return "{TAB}";
		} else if (" ".equals(delimiter)) {
			return "{SPACE}";
		} else {
			return delimiter;
		}
	}
	
	public String getDelimiterToken() {
		return getTokenFromDelimiter(delimiter);
	}
	
	public void clear() {
		tableName = null;
		Properties p = new Properties();
		try {
			setupFrom(p);
		} catch(Exception e) {
			logger.warn(e.getMessage(), e);
		}
	}

	public void setupFrom(Properties properties) throws Exception {
        setTableName(properties.getProperty("TABLE_NAME"));
        preProcessEnabled = "true".equalsIgnoreCase(properties.getProperty("PRE_PROCESS_SQL_ENABLED", "false"));
        String preProcessSQLBase64 = properties.getProperty("PRE_PROCESS_SQL");
        if (preProcessSQLBase64 != null && preProcessSQLBase64.length() > 2) {
            preProcessSQL = Base64.getText(preProcessSQLBase64, Base64.DEFAULT_CHARSET);
        } else {
            preProcessSQL = null;
        }
        postProcessEnabled = "true".equalsIgnoreCase(properties.getProperty("POST_PROCESS_SQL_ENABLED", "false"));
        String postProcessSQLBase64 = properties.getProperty("POST_PROCESS_SQL");
        if (postProcessSQLBase64 != null && postProcessSQLBase64.length() > 2) {
            postProcessSQL = Base64.getText(postProcessSQLBase64, Base64.DEFAULT_CHARSET);
        } else {
            postProcessSQL = null;
        }
        insertEnabled = properties.getProperty("INSERT_ENABLED", "true").trim().equals("true");
        handleFileAlwaysAsCSV = properties.getProperty("HANDLE_ALWAYS_AS_CSV", "false").trim().equals("true");
        updateEnabled = properties.getProperty("UPDATE_ENABLED", "true").trim().equals("true");
        deleteBeforeImport = properties.getProperty("DELETE_BEFORE_IMPORT", "false").trim().equals("true");
        delimiter = getDelimiterFromToken(properties.getProperty("DELIMITER"));
        enclosure = properties.getProperty("ENCLOSURE");
        ignoreLineBreakInEnclosedValues = "true".equals(properties.getProperty("IGNORE_ENCLOSED_LINE_BREAK", "false"));
        skipFirstRow = properties.getProperty("SKIP_FIRST_ROW", "true").trim().equals("true");
        countSkipRows = Long.parseLong(properties.getProperty("SKIP_ROWS", "0"));
		setCharsetName(properties.getProperty("CHARSET", DEFAULT_CHARSET));
		setSheetName(properties.getProperty("SHEET_NAME"));
		setBatchSize(Integer.parseInt(properties.getProperty("BATCH_SIZE", "0")));
	}
	
	public String getTableName() {
		return tableName;
	}
	public String getSheetName() {
		return sheetName;
	}
	public void setSheetName(String sheetName) {
		if (sheetName != null && sheetName.trim().length() > 0) {
			this.sheetName = sheetName.trim();
		} else {
			this.sheetName = null;
		}
	}
	public String getCharsetName() {
		return charsetName != null ? charsetName : DEFAULT_CHARSET;
	}
	public void setCharsetName(String charsetName) {
		if (charsetName != null && charsetName.trim().length() > 0) {
			this.charsetName = charsetName.trim();
		} else {
			this.charsetName = null;
		}
	}
	public void setTableName(String tableName) {
		if (tableName != null && tableName.trim().length() > 0) {
			this.tableName = tableName.trim();
		} else {
			this.tableName = null;
		}
	}
	public boolean isInsertEnabled() {
		return insertEnabled;
	}
	public void setInsertEnabled(boolean insertEnabled) {
		this.insertEnabled = insertEnabled;
	}
	public boolean isUpdateEnabled() {
		return updateEnabled;
	}
	public void setUpdateEnabled(boolean updateEnabled) {
		this.updateEnabled = updateEnabled;
	}
	public boolean isDeleteBeforeImport() {
		return deleteBeforeImport;
	}
	public void setDeleteBeforeImport(boolean deleteBeforeImport) {
		this.deleteBeforeImport = deleteBeforeImport;
	}
	public boolean isSkipFirstRow() {
		return skipFirstRow;
	}
	public void setSkipFirstRow(boolean skipFirstRow) {
		this.skipFirstRow = skipFirstRow;
	}
	public boolean isHandleFileAlwaysAsCSV() {
		return handleFileAlwaysAsCSV;
	}
	public void setHandleFileAlwaysAsCSV(boolean handleFileAlwaysAsCSV) {
		this.handleFileAlwaysAsCSV = handleFileAlwaysAsCSV;
	}
	public long getCountSkipRows() {
		return countSkipRows;
	}
	public void setCountSkipRows(long countSkipRows) {
		this.countSkipRows = countSkipRows;
	}
	public String getDelimiter() {
		return delimiter;
	}
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	public String getEnclosure() {
		return enclosure;
	}
	public void setEnclosure(String enclosure) {
		this.enclosure = enclosure;
	}
	public String getPreProcessSQL() {
		return preProcessSQL;
	}
	public void setPreProcessSQL(String preProcessSQL) {
		this.preProcessSQL = preProcessSQL;
	}
	public String getPostProcessSQL() {
		return postProcessSQL;
	}
	public void setPostProcessSQL(String postProcessSQL) {
		this.postProcessSQL = postProcessSQL;
	}
	public boolean isPreProcessEnabled() {
		return preProcessEnabled && preProcessSQL != null && preProcessSQL.length() > 1;
	}
	public void setPreProcessEnabled(boolean preProcessEnabled) {
		this.preProcessEnabled = preProcessEnabled;
	}
	public boolean isPostProcessEnabled() {
		return postProcessEnabled && postProcessSQL != null && postProcessSQL.length() > 1;
	}
	public void setPostProcessEnabled(boolean postProcessEnabled) {
		this.postProcessEnabled = postProcessEnabled;
	}
	public int getColumnCount() {
		return columnCount;
	}
	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	public int getBatchSize() {
		return batchSize;
	}
	
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean isIgnoreLineBreakInEnclosedValues() {
		return ignoreLineBreakInEnclosedValues;
	}

	public void setIgnoreLineBreakInEnclosedValues(
			boolean ignoreLineBreakInEnclosedValues) {
		this.ignoreLineBreakInEnclosedValues = ignoreLineBreakInEnclosedValues;
	}
	
}
