package sqlrunner.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import sqlrunner.BinaryDataFile;
import sqlrunner.datamodel.SQLField;
import sqlrunner.flatfileimport.BasicDataType;
import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import dbtools.SQLParser;

/**
 *
 * @author jan
 */
public class ExporterToTextFile implements Exporter {

    private static Logger staticClassLogger = Logger.getLogger(ExporterToTextFile.class);
	private Logger logger = staticClassLogger;
    private String sqlCode;
    private String delim = "|";
    private String enclosure;
    private String insertStatementBegin;
    private String dateFormatTemplate;
    private BufferedWriter bw;
    private DatabaseSession session;
    private ConnectionDescription cd; 
    private File outputFile;
    private long maxDatasetsPerFile = 0;
    public static final int FILE = 1;
    public static final int INSERT_FORMAT = 2;
    private int exportType = FILE;
    private SimpleDateFormat sdf = new SimpleDateFormat();
    private String[] columnNames;
    private File currentFile;
    private int fileIndex = 0;
    private int rowCountOverAll = 0;
	private int rowCountPerFile = 0;
    private boolean createHeaderLine = false;
    private boolean changePointToComma = false;
    private String fileCharSet = null;
    private String lineSeparator = System.getProperty("line.separator");
    private boolean abort = false;
    private String currentAction = null;
    private boolean connected = false;
    private String lastErrorMessage;
    private int status = 0;
    
    public ExporterToTextFile(
    		ConnectionDescription cd, 
    		String sqlCode, 
    		File outputFile) {
    	this.cd = cd;
    	this.sqlCode = sqlCode;
    	this.outputFile = new File(outputFile.getAbsolutePath());
    }
    
	/**
	 * set connection description
	 * @param connDesc defines the database
	 */
    @Override
	public void setConnectionDescription(ConnectionDescription connDesc) {
		this.cd = connDesc;
	}
	
    @Override
    public void setQuery(String sql) {
    	this.sqlCode = sql;
	}

	public void setCharSet(String charSet) {
    	this.fileCharSet = charSet;
    }
    
    public void setExportType(int type) {
    	if (type == FILE || type == INSERT_FORMAT) {
    		this.exportType = type;
    	} else {
    		throw new IllegalArgumentException("type=" + type + " unknown");
    	}
    }
    
    public void setCreateHeader(boolean createHeader) {
    	this.createHeaderLine = createHeader;
    }
    
    public void useCommaAsDecimalDelimiter(boolean useCommaAsDecimalDelimiter) {
    	this.changePointToComma = useCommaAsDecimalDelimiter;
    }
    
    public void setMaxDatasetsPerFile(long maxDatasetsPerFile) {
    	this.maxDatasetsPerFile = maxDatasetsPerFile;
    }
    
    public void setDelimiter(String delimiter) {
    	this.delim = delimiter;
    }
    
    public void setEnclosure(String enclosure) {
    	this.enclosure = enclosure;
    }
    
    public void setLineSeparator(String sep) {
    	this.lineSeparator = sep;
    }
    
    public void setDateFormat(String pattern) {
        if (pattern == null || pattern.trim().length() == 0) {
            throw new IllegalArgumentException("pattern cannot be null or empty");
        }
        sdf = new SimpleDateFormat(pattern);
    }
    
    /* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#setLogger(org.apache.log4j.Logger)
	 */
    @Override
    public void setLogger(Logger logger) {
    	this.logger = logger;
    }
    
    /* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getLogger()
	 */
    @Override
    public Logger getLogger() {
    	return logger;
    }
    
    /* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#resetToStaticLogger()
	 */
    @Override
    public void resetToStaticLogger() {
    	this.logger = staticClassLogger;
    }

    /* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#exportData()
	 */
    @Override
    public void exportData() throws IOException, SQLException {
    	abort = false;
        if (connected) {
        	currentAction = "parsing sql";
        	status = PARSING;
            final SQLParser sqlParser = new SQLParser(sqlCode);
            String singleStatement = sqlParser.getStatementAt(0).getSQL();
            currentAction = "selecting data";
            status = SELECTING;
            final ResultSet rs = session.executeQuery(singleStatement);
            if (session.isSuccessful() == false) {
                lastErrorMessage = session.getLastErrorMessage();
            	logger.error("running statement:\n" + singleStatement + "\nfailed: " + session.getLastErrorMessage());
                throw new SQLException(lastErrorMessage);
            }
            if (rs == null) {
            	lastErrorMessage = "no resultset available";
            	logger.error("running statement:\n" + singleStatement + "\nfailed: " + lastErrorMessage);
            	throw new SQLException(lastErrorMessage);
            }
            final Object[] resultRow;
            try {
            	currentAction = "getting metadata for result";
                final ResultSetMetaData rsmd = rs.getMetaData();
                final int cols = rsmd.getColumnCount();
                resultRow = new Object[cols];
                // aus den Metadaten die Splatennamen lesen
                columnNames = new String[cols];
                for (int i = 0; i < cols; i++) {
                    columnNames[i] = rsmd.getColumnName(i + 1);
                }
                currentAction = "fetching data";
            	status = FETCHING;
				int[] types = new int[cols];
				for (int c = 0; c < cols; c++) {
					types[c] = BasicDataType.getBasicTypeByTypes(rsmd.getColumnType(c + 1));
				}
                while (rs.next()) {
                    if (abort || Thread.currentThread().isInterrupted()) {
                    	status = ABORTED;
                    	break;
                    } else {
                    	for (int i = 0; i < cols; i++) {
                            if (types[i] == BasicDataType.DATE.getId()) {
                                resultRow[i] = rs.getTimestamp(i + 1);
                            } else if (BasicDataType.isNumberType(types[i])) {
                                resultRow[i] = rs.getObject(i + 1);
                            } else if (types[i] == BasicDataType.BOOLEAN.getId()) {
                            	if (rs.getObject(i + 1) != null) {
                                    resultRow[i] = rs.getBoolean(i + 1);
                            	} else {
                                    resultRow[i] = null;
                            	}
                            } else if (types[i] == BasicDataType.CHARACTER.getId()) {
                                resultRow[i] = rs.getString(i + 1);
                            } else if (types[i] == BasicDataType.CLOB.getId()) {
                                if (rs.getObject(i + 1) != null) {
                                    final Clob clob = rs.getClob(i + 1);
                                    resultRow[i] = clob.getSubString(1, (int) clob.length());
                                } else {
                                    resultRow[i] = null;
                                }
                            } else if (types[i] == BasicDataType.BINARY.getId()) {
                                // offensichtlich bringt diese Abfrage bei Oracle immer ein Objekt
                                // zurück, so dass man erst durch das Lesen der Daten festellen kann
                                // ob diese leer sind.
                                if (rs.getObject(i + 1) != null) {
                                    resultRow[i] = new BinaryDataFile();
                                } else {
                                    resultRow[i] = null;
                                }
                            } else if (types[i] == SQLField.ORACLE_ROWID) {
                                resultRow[i] = rs.getString(i + 1);
                                break;
                            }
                        } // for
                        processDataSet(resultRow, types);
                    }
                } // while (rs.next())
            } catch (SQLException sqle) {
            	lastErrorMessage = "fetching data failed: " + sqle.getMessage();
                logger.error(lastErrorMessage, sqle);
                throw sqle;
            } catch (IOException ioe) {
            	lastErrorMessage = "writing data failed: " + ioe.getMessage();
                logger.error(lastErrorMessage, ioe);
                throw ioe;
            } finally {
                currentAction = "closing streams";
                status = CLOSING;
                try {
                    if (bw != null) {
                        bw.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                } catch (SQLException sqle) {
                    logger.error("closing resultset failed: " + sqle.getMessage(), sqle);
                } catch (IOException ioe) {
                    logger.error("closing file failed: " + ioe.getMessage(), ioe);
                }
                session.close();
            }
            status = FINISHED;
        } else {
            logger.error("not connected");
        }
    }

    private void processDataSet(Object[] resultRow, int[] types)
        throws IOException {
        if (rowCountOverAll == 0) {
            currentFile = outputFile;
            if (fileCharSet != null) {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile), fileCharSet));
            } else {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile)));
            }
        } else if ((maxDatasetsPerFile > 0) && (rowCountPerFile == maxDatasetsPerFile)) {
            rowCountPerFile = 0;
            fileIndex++;
            currentFile = createNextFile(outputFile, fileIndex);
            bw.flush();
            bw.close();
            if (fileCharSet != null) {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile), fileCharSet));
            } else {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile)));
            }
        }
        if (exportType == INSERT_FORMAT) {
            if (rowCountOverAll == 0) {
                insertStatementBegin = createInsertStatementText(sqlCode);
            }
            bw.write(createInsertLine(resultRow, types, insertStatementBegin, dateFormatTemplate));
            bw.write(lineSeparator);
        } else {
            if (rowCountPerFile == 0) {
                if (createHeaderLine) {
                    bw.write(createDelimitedHeaderLine());
                    bw.write(lineSeparator);
                }
                bw.write(createDelimitedLine(resultRow, types));
                bw.write(lineSeparator);
            } else {
                bw.write(createDelimitedLine(resultRow, types));
                bw.write(lineSeparator);
            }
        }
        rowCountOverAll++;
        rowCountPerFile++;
    }

    private String createDelimitedHeaderLine() {
        final StringBuffer sb = new StringBuffer();
        boolean firstLoop = true;
        for (int c = 0; c < columnNames.length; c++) {
            if (firstLoop) {
                firstLoop = false;
            } else {
                sb.append(delim);
            }
            if (enclosure != null) {
                sb.append(enclosure);
            }
            sb.append(columnNames[c]);
            if (enclosure != null) {
                sb.append(enclosure);
            }
        }
        return sb.toString();
    }

    private String createDelimitedLine(Object[] resultRow, int[] types) {
        final StringBuffer sb = new StringBuffer();
        for (int c = 0; c < resultRow.length; c++) {
            if (enclosure != null) {
                sb.append(enclosure);
            }
            if (types[c] == BasicDataType.DATE.getId()) {
                if (sdf == null) {
                    throw new IllegalStateException("date format not defined");
                }
                if (resultRow[c] != null) {
                    sb.append(sdf.format((java.util.Date) resultRow[c]));
                }
            } else if (BasicDataType.isNumberType(types[c])) {
                if (resultRow[c] != null) {
                    if (changePointToComma) {
                        sb.append(resultRow[c].toString().replace('.', ','));
                    } else {
                        sb.append(resultRow[c].toString());
                    }
                }
            } else {
                if (resultRow[c] != null) {
                    sb.append(resultRow[c].toString());
                }
            }
            if (enclosure != null) {
                sb.append(enclosure);
            }
            if (c < resultRow.length - 1) {
                // noch nicht die letzte Spalte ?
                sb.append(delim);
            }
        } // for (int c=0; c < getColumnCount(); c++)
        return sb.toString().replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * erzeugt einen insert-String für die momentan angezeigte Tabelle
     * @return - enthält insert-Statement-Text
     */
    private String createInsertStatementText(String sql) {
        // Tabellen/View-Namen ermitteln
        sql = sql.toLowerCase();
        int p0 = sql.indexOf("from"); //$NON-NLS-1$
        p0 = p0 + 4; // Zeiger nach dem from
        sql = sql.substring(p0, sql.length()).trim();
        // testen ob es sich um eine View handelt oder um eine table
        if (sql.charAt(0) == '(') {
            // View gefunden, dann Ende der View finden
            p0 = sql.indexOf(')') + 1;
        } else {
            // Tabelle gefunden, dann das Ende finden
            p0 = sql.indexOf(' '); // wird durch Leerzeichen abgeschlossen
            if (p0 == -1) {
                // keine Leerzeichen , dann nach Semikolon testen
                p0 = sql.indexOf(';');
                if (p0 == -1) {
                    // auch kein semikolon, dann Ende des Textes nutzen
                    p0 = sql.length();
                }
            }
        } // if (sql.charAt(0) == '(')
        if (p0 != -1) {
            sql = sql.substring(0, p0);
        } else {
            sql = null;
        }
        // insert-Statement zusammensetzen
        if (sql != null) {
            final StringBuffer sb = new StringBuffer();
            sb.append("insert into "); //$NON-NLS-1$
            sb.append(sql);
            sb.append(" ("); //$NON-NLS-1$
            for (int i = 0; i < columnNames.length; i++) {
                sb.append(columnNames[i]);
                if (i < columnNames.length - 1) {
                    // noch nicht das Ende erreicht
                    sb.append(',');
                }
            }
            sb.append(") values ("); //$NON-NLS-1$ //$NON-NLS-2$
            return sb.toString();
        } else {
            return null;
        }
    }

    /**
     * erzeugt Textzeilen die ein Insert-Statement enthalten für den Tabellenexport
     */
    private String createInsertLine(Object[] resultRow, int[] types, String insertStart, String dateFormatTemplate_loc) {
        final StringBuffer sb = new StringBuffer();
        sb.append(insertStart);
        // jetzt ist alles fertig bis zu values ( ...
        for (int c = 0; c < resultRow.length; c++) {
            if (types[c] == BasicDataType.DATE.getId()) {
                if (resultRow[c] != null) {
                    final String s = sdf.format((java.util.Date) resultRow[c]);
                    final int p0 = dateFormatTemplate_loc.indexOf("<"); //$NON-NLS-1$
                    if (p0 != -1) {
                        final int p1 = dateFormatTemplate_loc.indexOf(">", p0 + 1); //$NON-NLS-1$
                        dateFormatTemplate_loc = dateFormatTemplate_loc.substring(0, p0) + s + dateFormatTemplate_loc.substring(p1 + 1, dateFormatTemplate_loc.length());
                    }
                    // dateFormatTemplate ist nun kein Template mehr
                    sb.append(dateFormatTemplate_loc);
                } else {
                    sb.append("null"); //$NON-NLS-1$
                }
            } else if (BasicDataType.isStringType(types[c])) {
                sb.append("'"); //$NON-NLS-1$
                if (resultRow[c] != null) {
                    sb.append(dublicateSingleQuotas(resultRow[c].toString().replace('\n', ' ').replace('\r', ' ')));
                }
                sb.append("'"); //$NON-NLS-1$
            } else {
                if (resultRow[c] != null) {
                    sb.append(resultRow[c].toString().replace('\n', ' ').replace('\r', ' '));
                } else {
                    sb.append("null"); //$NON-NLS-1$
                }
            } // if (getColumnBasicType(c) == BASICTYPE_DATE)
            if (c < resultRow.length - 1) {
                // noch nicht die letzte Spalte ?
                sb.append(',');
            } else {
                sb.append(");"); // schliessende Klammer //$NON-NLS-1$
            }
        }
        return sb.toString();
    }

    /**
     * dubliziert in einem String die einfachen Hochkomma,
     * um diesen String innerhalb eines SQL-Statements einsetzen zu können
     */
    private String dublicateSingleQuotas(String text) {
        final StringBuffer sb = new StringBuffer();
        char c;
        for (int i = 0; i < text.length(); i++) {
            c = text.charAt(i);
            if (c == '\'') {
                sb.append(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#connect()
	 */
    @Override
    public boolean connect() {
    	if (cd == null) {
    		throw new IllegalStateException("no ConnectionDescription set");
    	}
    	status = CONNECTING;
        session = new DatabaseSession(cd);
        connected = session.isConnected();
        if (connected == false) {
            lastErrorMessage = session.getLastErrorMessage();
        }
        return connected;
    }

    private File createNextFile(File originalTargetFile, int index) {
        String path = originalTargetFile.getParent();
        final String fullname = originalTargetFile.getName();
        final int p0 = fullname.lastIndexOf(".");
        String name;
        if (p0 != -1) {
            name = fullname.substring(0, p0) 
            	+ "_"
                + String.valueOf(index) 
                + fullname.substring(p0, fullname.length());
        } else {
            name = fullname 
            	+ "_" 
            	+ String.valueOf(index);
        }
        if (path != null) {
            return new File(path, name);
        } else {
            return new File(name);
        }
    }
    
    /* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#abort()
	 */
    @Override
    public void abort() {
    	abort = true;
    }
    
    /* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getCurrentFile()
	 */
    public File getCurrentFile() {
		return currentFile;
	}

	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getRowCountOverAll()
	 */
    @Override
	public int getCurrentRowNum() {
		return rowCountOverAll;
	}

	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getRowCountPerFile()
	 */
	public int getRowCountPerFile() {
		return rowCountPerFile;
	}

	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getCurrentAction()
	 */
    @Override
	public String getCurrentAction() {
		return currentAction;
	}
	
	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getStatus()
	 */
    @Override
	public int getStatus() {
		return status;
	}
	
	/* (non-Javadoc)
	 * @see sqlrunner.export.Exporter#getLastErrorMessage()
	 */
	public String getLastErrorMessage() {
		return lastErrorMessage;
	}

    @Override
	public void setOutputFile(File outputFile) {
    	this.outputFile = new File(outputFile.getAbsolutePath());
	}
	
}
