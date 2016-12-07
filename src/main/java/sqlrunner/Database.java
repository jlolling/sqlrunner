package sqlrunner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import dbtools.DatabaseSessionPool;
import dbtools.SQLPSParam;
import dbtools.SQLParser;
import dbtools.SQLStatement;
import sqlrunner.datamodel.SQLDataModel;
import sqlrunner.datamodel.SQLField;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.dbext.DatabaseExtension;
import sqlrunner.dbext.DatabaseExtensionFactory;
import sqlrunner.editor.WildcardSearch;
import sqlrunner.export.ExporterToSpreadsheetFile;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.FieldDescription;
import sqlrunner.generator.SQLCodeGenerator;
import sqlrunner.text.GenericDateUtil;

public final class Database implements TableModel {

	private static final Logger logger = Logger.getLogger(Database.class);
	private ResultSetMetaData rsmd;
	private DatabaseMetaData dbmd;
	private DatabaseSession session;
	private TableColumnModel tableColumnModel;
	private String[] columnNames;
	private String[] columnSourceClasses;
	private Class<?>[] columnClasses;
	private String[] columnTypeNames;
	private Boolean[] columnPkInfo;
	private Boolean[] columnRefFromInfo;
	private Integer[] columnPrecision;
	private Integer[] columnScale;
	private HashMap<String, StringBuilder> mapRefFromColumns;
	private String[][] columnRefToInfo;
	private int[] columnTypesValues;
	private Object[] newRow;
	private int newRowIndex = -1;
	private JTable table;
	private String lastTable;
	private ValueEditor currentCellEditor;
	private SimpleDateFormat sdf; // only for format date, not for parsing
	static public final int CSV_FORMAT = 0;
	static public final int INSERT_FORMAT = 1;
	private int maxRowsToRead;
	private int maxRowsToWarning;
	private int lastSearchPosRow = 0;
	private int lastSearchPosCol = 0;
	private SQLDataModel dataModel;
	private boolean isAdditionalInfoReady = false;
	private MainFrame mainFrame;
	private int refreshTime = 2000;
	private int defaultPreferedColumnWidth = 80;
	static Properties columnsWidth = new Properties();
	private int preferredColumnWidthSum = 0;
	private final ColumnPropertyChangeListener cpcl = new ColumnPropertyChangeListener();
	private SQLStatement lastSelectStatement;
	private boolean verticalView = false;
	private boolean showsResultSet = true;
	private List<Object[]> resultSetResults = new ArrayList<Object[]>();
	private List<SQLPSParam> outputParameters = new ArrayList<SQLPSParam>();
	private DatabaseExtension databaseExtension;

	public Database(JTable table, MainFrame mainFrame) {
		super(); // den Vector instanzieren.
		databaseExtension = DatabaseExtensionFactory.getGenericDatabaseExtension();
		this.table = table;
		this.table.setModel(this);
		this.mainFrame = mainFrame;
		tableColumnModel = table.getColumnModel();
		session = new DatabaseSession();
		maxRowsToWarning = Integer.parseInt(Main.getDefaultProperty("MAX_ROWS_TO_READ_IN", "10000"));
		createSDF();
		try {
			refreshTime = Integer.parseInt(Main.getDefaultProperty("DB_FETCH_REFRESH_CYCLE", "2000"));
		} catch (NumberFormatException nfe) {
			refreshTime = 2000;
		}
	}

	public Database() {
		super();
		session = new DatabaseSession();
		databaseExtension = DatabaseExtensionFactory.getGenericDatabaseExtension();
	}
    
    public boolean isConnected() {
        return session != null && session.isConnected();
    }
    
    public String getCurrentUserName() {
    	return getDatabaseSession().getUser();
    }

    public DatabaseExtension getDatabaseExtension() {
    	return databaseExtension;
    }
	/**
	 * wechselt in die vertikale Darstellung der Datensätze Die Datenfelder
	 * werden untereinander geschrieben
	 * 
	 * @param enable
	 *            true = vertikale Darstellung
	 */
	public void setVerticalView(boolean enable) {
		if (enable) {
			logger.debug("change to vertical view");
		} else {
			logger.debug("change to horizontal view");
		}
		this.verticalView = enable;
		// tableHeaderRenderer are get lost after that !
		fireTableStructureChanged();
		if (enable == false) {
			prepareColumns();
		}
		mainFrame.resultTable.getTableHeader()
				.setDefaultRenderer(titleRenderer);
	}
	
	public boolean isVerticalView() {
		return verticalView;
	}

	public void createSDF() {
		sdf = new SimpleDateFormat(MainFrame.getDateFormatMask());
	}

	public int getLogicalColumnCount() {
		if (columnNames != null) {
			return columnNames.length;
		} else {
			return 0;
		}
	}

	public void setTable(JTable table_loc) {
		this.table = table_loc;
		this.table.setModel(this);
		tableColumnModel = table_loc.getColumnModel();
	}

	public void setDatabaseSession(DatabaseSession session_loc) {
		this.session = session_loc;
	}

	public DatabaseSession getDatabaseSession() {
		return session;
	}

	public String getTableName() {
		return lastTable;
	}

	public void refreshMetadata() {
		if (session.isConnected()) {
			dataModel.refresh();
		}
	}

	public SQLDataModel getSQLDataModel() {
		return dataModel;
	}

	public DatabaseMetaData getDatabaseMetaData() {
		return dbmd;
	}

	public String getSQLDatamodelErrorMessage() {
		return dataModel.getLastErrorMessage();
	}

	/**
	 * kapselt die Methode aus DatabaseSession
	 * 
	 * @param desc -
	 *            Beschreibung für die Session
	 * @see DatabaseSession.setConnectionDescription
	 */
	public void setConnectionDescription(ConnectionDescription desc) {
		session.setConnectionDescription(desc);
	}

	/**
	 * kapselt die Methode aus DatabaseSession
	 * 
	 * @return true wenn Treiber erfolgreich geladen
	 * @see DatabaseSession.loadDriver
	 */
	public boolean loadDriver() {
		return session.loadDriver();
	}

	/**
	 * kapselt die Methode aus DatabaseSession füllt zusätzlich noch die
	 * Referenz dbmd auf DatabaseMetaData
	 * 
	 * @return true erfolgreich verbunden
	 * @see DatabaseSession.connect
	 */
	public boolean connect() {
		// we have to do this before we connect because we are able to set env variables before
		databaseExtension = DatabaseExtensionFactory.getDatabaseExtension(session.getConnectionDescription());
		final boolean ok = session.connect();
		dataModel = null;
		if (Thread.currentThread().isInterrupted()) {
			return false;
		}
		if (ok) {
			try {
				logger.debug("setup meta data...");
				dbmd = session.getConnection().getMetaData();
				dataModel = new SQLDataModel(session.getConnectionDescription());
				databaseExtension.setupConnection(session.getConnection());
				databaseExtension.setIdentifierQuoteString(dbmd.getIdentifierQuoteString());
				SQLCodeGenerator.getInstance().setEnclosureChar(databaseExtension.getIdentifierQuoteString());
				startPreloadingDataModel();
				registerAdditionalKeywords();
			} catch (SQLException sqle) {
				mainFrame.showWarningMessage("No metadata available: "
						+ sqle.getMessage(), "connect");
			}
		} else {
			dbmd = null;
		}
		return ok;
	}
	
	private Thread preloadDataModelThread;
	
	private void startPreloadingDataModel() {
		preloadDataModelThread = new Thread() {
			@Override
			public void run() {
				dataModel.reloadSchemasAndTables();
			}
		};
		preloadDataModelThread.start();
	}

	private void registerAdditionalKeywords() {
		if (mainFrame.getSyntaxScanner() != null) {
			final StringBuilder sb = new StringBuilder();
			try {
				String keywords = dbmd.getSQLKeywords();
				if ((keywords != null) && (keywords.length() > 0)) {
					sb.append(keywords);
					sb.append(',');
				}
				keywords = dbmd.getStringFunctions();
				if ((keywords != null) && (keywords.length() > 0)) {
					sb.append(keywords);
					sb.append(',');
				}
				keywords = dbmd.getSystemFunctions();
				if ((keywords != null) && (keywords.length() > 0)) {
					sb.append(keywords);
					sb.append(',');
				}
				keywords = dbmd.getTimeDateFunctions();
				if ((keywords != null) && (keywords.length() > 0)) {
					sb.append(keywords);
					sb.append(',');
				}
				keywords = dbmd.getNumericFunctions();
				if ((keywords != null) && (keywords.length() > 0)) {
					sb.append(keywords);
				}
				mainFrame.getSyntaxScanner().addAdditionalKeywords(sb.toString());
				if (databaseExtension != null) {
					mainFrame.getSyntaxScanner().addAdditionalKeywords(databaseExtension.getAdditionalSQLKeywords());
					mainFrame.getSyntaxScanner().addAdditionalSQLDataTypes(databaseExtension.getAdditionalSQLDatatypes());
					mainFrame.getSyntaxScanner().addAdditionalPLSQLKeywords(databaseExtension.getAdditionalProcedureKeywords());
				}
			} catch (SQLException sqle) {
				logger.error("registerAdditionalKeywords:" + sqle.getMessage(),
						sqle);
			}
		} // if (mainFrame.lexer != null)
	}

	private void deregisterKeywords() {
		if (mainFrame.getSyntaxScanner() != null) {
			mainFrame.getSyntaxScanner().deregisterAdditionalKeywords();
		}
	}

	public boolean close() {
		if (preloadDataModelThread != null && preloadDataModelThread.isAlive()) {
			preloadDataModelThread.interrupt();
		}
		dataModel = null;
		dbmd = null;
		databaseExtension = DatabaseExtensionFactory.getGenericDatabaseExtension();
		deregisterKeywords();
		DatabaseSessionPool.close(session.getAliasName());
        boolean ok = session.close();
		return ok;
	}

	public List<FieldDescription> selectFieldDescriptions(String table_loc) {
		if (dbmd != null) {
			final List<FieldDescription> fields = new ArrayList<FieldDescription>();
			String schema;
			final int pos = table_loc.indexOf('.');
			if (pos != -1) {
				// dann ist der Tabelle das Schema mitgegeben worden
				schema = table_loc.substring(0, pos);
				table_loc = table_loc.substring(pos + 1, table_loc.length());
			} else {
				schema = session.getUser().toUpperCase();
			}
			ResultSet rsc = null;
			try {
				// use the internal datamodel to create the field descriptions
				rsc = dbmd.getColumns(null, schema, table_loc, null); // (null,null,table,null);
				int i = 0;
				while (rsc.next()) {
					fields.add(new FieldDescription(rsc.getString("COLUMN_NAME"),
							BasicDataType.getBasicTypeByTypes(rsc.getShort("DATA_TYPE")),
							"",
							rsc.getInt("ORDINAL_POSITION") - 1,
							2,
							// positionType
							rsc.getInt("ORDINAL_POSITION") - 1,
							-1,
							rsc.getInt("COLUMN_SIZE"),
							false,
							true,
							"",
							false,
							true,
							null));
				}
				rsc.close();
				// primary keys festellen und markieren
				rsc = dbmd.getPrimaryKeys(null, schema, table_loc);
				String columnName;
				FieldDescription fd;
				while (rsc.next()) {
					// im Vector das Feld suchen und dieses als PK kennzeichnen
					columnName = rsc.getString("COLUMN_NAME");
					for (i = 0; i < fields.size(); i++) {
						fd = fields.get(i);
						if ((fd.getName()).equalsIgnoreCase(columnName)) {
							fd.setIsPartOfPrimaryKey(true);
							break;
						}
					} // for (int i=0; i < fields.size(); i++)
				} // while (rsc.next())
                rsc.close();
			} catch (SQLException sqle) {
				logger.error("getFieldDescriptions:" + sqle.getMessage());
			} finally {
				try {
					if (rsc != null) {
						rsc.close();
					}
				} catch (Exception e) {
					logger.error("failed to close result set " + e.getMessage());
				}
            }
			if (fields.size() == 0) {
				return null;
			} else {
				return fields;
			}
		} else {
			return null;
		}
	}

	public List<FieldDescription> selectFieldDescriptions(SQLTable table) {
		final List<FieldDescription> fields = new ArrayList<FieldDescription>();
		for (int i = 0; i < table.getFieldCount(); i++) {
			SQLField field = table.getFieldAt(i);
			FieldDescription fd = new FieldDescription(field);
			fields.add(fd);
		}
		return fields;
	}

	/**
	 * der Viewer muss hier sich bekannt geben um nach einer Fehlermeldung
	 * wieder in den Fordergrund geholt zu werden. Die Fehlermeldungen setzen
	 * ihn hinter MainFrame !
	 * 
	 * @param cv -
	 *            zu behandelnder CellEditor
	 */
	public void setCurrentCellEditor(ValueEditor cv) {
		this.currentCellEditor = cv;
	}

	/**
	 * erzeugt einen insert-String für die momentan angezeigte Tabelle
	 * 
	 * @return - enthält insert-Statement-Text
	 */
	public String createInsertStatementText() {
		if (lastTable != null) {
			final StringBuilder sb = new StringBuilder();
			sb.append("insert into ");
			sb.append(lastTable);
			sb.append(" (");
			for (int i = 0; i < columnNames.length; i++) {
				sb.append(columnNames[i]);
				if (i < columnNames.length - 1) { // noch nicht das Ende
					// erreicht
					sb.append(',');
				}
			}
			sb.append(")\nvalues (");
			return sb.toString();
		} else {
			return null;
		}
	}

	public String createSelectStatementText() {
		if (lastTable != null) {
			final StringBuilder sb = new StringBuilder();
			sb.append("select ");
			for (int i = 0; i < columnNames.length; i++) {
				sb.append(columnNames[i]);
				if (i < columnNames.length - 1) { // noch nicht das Ende
					// erreicht
					sb.append(",\n       ");
				}
			}
			sb.append("\nfrom ");
			sb.append(lastTable);
			return sb.toString();
		} else {
			return null;
		}
	}

	private String createWherePartOfPreparedStatement(int row) {
		// letztes Feld suchen, welches als PK genutzt werden soll
		int lastPkFieldIndex = -1;
		int x;
		final boolean error = false;
		for (x = 0; x < columnPkInfo.length; x++) {
			if (columnPkInfo[x].booleanValue()) {
				lastPkFieldIndex = x;
			}
		}
		// where Bedingung zusammenbauen
		final StringBuilder whereClause = new StringBuilder();
		whereClause.append(" where ");
		for (x = 0; x < columnPkInfo.length; x++) {
			if (columnPkInfo[x].booleanValue()) {
				whereClause.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnNames[x]));
				if (getValueAt(row, x) != null) {
					whereClause.append("=?");
				} else {
					whereClause.append(" is null");
				} // if (getValueAt(row,x) != null)
				if (x < lastPkFieldIndex) { // noch nicht das Ende erreicht
					// dann "and" dazwischen setzen
					whereClause.append(" and ");
				}
			} // if (columnPkInfo[x].booleanValue() == true)
		} // for (x=0; x < columnPkInfo.length; x++)
		if (error) {
			return null;
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("where-condition = " + whereClause);
			}
			return whereClause.toString();
		}
	}

	private boolean completeWherePartOfPreparedStatement(
			PreparedStatement ps,
			int row,
			int lastParamIndex,
			Object constraint) {
		boolean ok = true;
		int paramIndex = lastParamIndex + 1;
		Object value;
		for (int i = 0; i < columnPkInfo.length; i++) {
			if (columnPkInfo[i].booleanValue()) {
				value = getValueAtLogicalIndexes(row, i);
				// Null-values has no parameter-replacements
				if (value != null) {
					ok = setParameterValue(ps, paramIndex, i, value, constraint);
					paramIndex++;
					if (ok == false) {
						break;
					}
				} // if (value != null)
			} // if (columnPkInfo[i].booleanValue() == true)
		} // for (int i=0; i < columnPkInfo.length; i++)
		return ok;
	}

	private final class DeleteDataSetThread extends Thread {

		private int[] rows;

		DeleteDataSetThread(int[] rows) {
			this.rows = rows;
		}

        @Override
		public void run() {
			if (mainFrame.isDatabaseBusy() == false) {
				int row = rows[0];
				for (int i = 0; i < rows.length; i++) {
					// use always the same row index because
					// we delete that row and the next row has exactly this index
					mainFrame.setDatabaseBusyFiltered(true, "delete dataset");
					if (performDeleteDataset(row) == false) {
						mainFrame.setDatabaseBusyFiltered(false, null);
						break;
					} else {
						mainFrame.setDatabaseBusyFiltered(false, null);
					}
				}
			}
		}
	}

	/**
	 * löscht einen Datensatz asynchron
	 * 
	 * @param row
	 *            zu löschende zeile in der Tabelle (DB)
	 */
	public void deleteDataset(int[] rows) {
		final DeleteDataSetThread thread = new DeleteDataSetThread(rows);
		thread.start();
	}

	/**
	 * erzeugt einen where-clause-String
	 * 
	 * @param row
	 *            Zeile für den die where-Bedingung erstellt werden soll
	 * @return whereClause
	 */
	private boolean performDeleteDataset(int row) {
		boolean ok = true;
		if (getDatabaseSession().isConnected()) {
			// Indexfelder ermitteln an Hand der ausgewählten Zeile
			final String whereBed = createWherePartOfPreparedStatement(row);
			if (whereBed != null) {
				// Test-select ausfähren
				StringBuilder sql_loc = new StringBuilder("select count(");
				int x;
				int countPk = 0;
				for (x = 0; x < columnPkInfo.length; x++) {
					if (columnPkInfo[x].booleanValue()) {
						sql_loc.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnNames[x]));
						sql_loc.append(") from ");
						sql_loc.append(SQLCodeGenerator.getInstance().getEncapsulatedName(lastTable));
						sql_loc.append(' ');
						sql_loc.append(whereBed);
						countPk++;
						break; // Schleife abbrechen, ein Feldname reicht für
						// count(..)
					}
				}
				// ist Statement vollständig ?
				if (countPk == 0) { // so muss auch der Feldname vorhanden sein
					// !
					mainFrame.showDBMessageWithoutContinueAction("No primary key definied!\n"
							+ "define primary keys with context menu in the table header",
							"delete dataset");
					ok = false;
				} else {
					// Warnung wenn mehr als eine Zeile selektiert
					int testCount = -1;
					ResultSet rs_loc = null;
					PreparedStatement psTest = null;
					try {
						psTest = getDatabaseSession().createPreparedStatement(sql_loc.toString());
						completeWherePartOfPreparedStatement(psTest, row, 0, null);
						rs_loc = psTest.executeQuery();
						if (rs_loc != null) {
							if (rs_loc.next()) {
								testCount = rs_loc.getInt(1);
							}
						}
					} catch (SQLException sqle) {
						mainFrame.showDBMessageWithoutContinueAction(sqle.getMessage(),
								"test of primary key");
						ok = false;
					} finally {
						try {
							if (rs_loc != null) {
								rs_loc.close();
							}
							if (psTest != null) {
								psTest.close();
							}
						} catch (Exception e) {
							logger.error("performDeleteDataset close cursors failed: "
									+ e.toString());
						}
					}
					if (testCount == -1) {
						// Fehler ausgeben, dass Testzählung nicht erfolgreich
						mainFrame.showDBMessageWithoutContinueAction(
								getDatabaseSession().getLastErrorMessage(),
								"Test count for check primary key");
						ok = false;
					} else if (testCount == 0) {
						mainFrame.showDBMessageWithoutContinueAction("Test count got 0 datasets ! unable to perform delete",
								"Delete dataset");
						ok = false;
					} else if (testCount > 1) {
						mainFrame.showDBMessageWithoutContinueAction("Test count got more than one dataset \n("
								+ String.valueOf(testCount)
								+ ") !",
								"Delete dataset");
						ok = false;
					}
					if (testCount == 1) {
						// Wertzuweisung zusammensetzen
						final StringBuilder sb = new StringBuilder();
						sb.append("delete from ");
						sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(lastTable));
						sb.append(whereBed);
						// delete durchfähren
						int count = -1;
						PreparedStatement ps = null;
						try {
							ps = session.createPreparedStatement(sb.toString());
							completeWherePartOfPreparedStatement(ps, row, 0, null);
							count = ps.executeUpdate();
						} catch (SQLException sqle) {
							mainFrame.showDBMessage(
									sqle.getMessage(),
									"Delete dataset");
							ok = false;
						} finally {
							try {
								ps.close();
							} catch (Exception e) {
								logger.error("Database.performDeleteDataset close statement failed: "
										+ e.toString());
							}
						}
						if (count == 1) { // alles ok !
							resultSetResults.remove(row);
							fireTableRowsDeleted(row, row);
						}
					}
				} // else if (countPk == 0)
			} else { // if (whereBed != null)
				ok = false;
			}
		} else { // if (getDatabaseSession().isConnected())
			mainFrame.showDBMessageWithoutContinueAction(
					"Database connection disconnected!",
					"Delete");
			ok = false;
		}
		if (ok) {
			// die Anzahl der Datensätze im Vector ist nun um 1 kleiner und der
			// Index für eine
			// ggf. neu eingefügte Zeile ist nun um 1 zu hoch !
			if (newRowIndex > 0) {
				newRowIndex--;
			}
		} // if (ok)
		return ok;
	}

	private final class DeleteValueThread extends Thread {
		private int row;
		private int col;

		DeleteValueThread(int row, int col) {
			this.row = row;
			this.col = col;
		}

        @Override
		public void run() {
			if (mainFrame.isDatabaseBusy() == false) {
				mainFrame.setDatabaseBusyFiltered(true, "delete value");
				performDeleteValue(row, col);
				mainFrame.setDatabaseBusyFiltered(false, null);
			}
		}
	}

	/**
	 * löscht einen Datensatz asynchron
	 * 
	 * @param row
	 *            zu löschende zeile in der Tabelle (DB)
	 */
	public Thread deleteValue(
			int row,
			int col) {
		final DeleteValueThread thread = new DeleteValueThread(row, col);
		thread.start();
		return thread;
	}

	/**
	 * löscht den Inhalt eines Feldes innerhalb eines Datensatzes
	 * 
	 * @param row =
	 *            Zeile in der Tabelle
	 * @param col =
	 *            Index der Spalte
	 * @return true wenn erfolgreich gelöscht
	 */
	public boolean performDeleteValue(
			int row,
			int col) {
		boolean ok = true;
		if (getDatabaseSession().isConnected()) {
			// Indexfelder ermitteln an Hand der ausgewählten Zeile
			final String whereBed = createWherePartOfPreparedStatement(row);
			if (whereBed != null) {
				// Test-select ausführen
				StringBuilder sql_loc = new StringBuilder("select count(");
				int x;
				int countPk = 0;
				for (x = 0; x < columnPkInfo.length; x++) {
					if (columnPkInfo[x].booleanValue()) {
						sql_loc.append(columnNames[x]);
						sql_loc.append(") from ");
						sql_loc.append(lastTable);
						sql_loc.append(' ');
						sql_loc.append(whereBed);
						countPk++;
						break; // Schleife abbrechen, ein Feldname reicht für
						// count(..)
					}
				}
				// ist Statement vollständig ?
				if (countPk == 0) { // so muss auch der Feldname vorhanden sein
					// !
					mainFrame.showDBMessageWithoutContinueAction("no primary key definied!\n"
							+ "define primary key with context menu from table header",
							"delete field");
					ok = false;
				} else {
					// Warnung wenn mehr als eine Zeile selektiert
					int testCount = -1;
					ResultSet rs_loc = null;
					PreparedStatement psTest = null;
					try {
						psTest = getDatabaseSession().createPreparedStatement(sql_loc.toString());
						completeWherePartOfPreparedStatement(psTest, row, 0, null);
						rs_loc = psTest.executeQuery();
						if (rs_loc != null) {
							if (rs_loc.next()) {
								testCount = rs_loc.getInt(1);
							}
						}
					} catch (SQLException sqle) {
						mainFrame.showDBMessageWithoutContinueAction(sqle.getMessage(),	"test of primary key");
						ok = false;
					} finally {
						try {
							if (rs_loc != null) {
								rs_loc.close();
							}
							if (psTest != null) {
								psTest.close();
							}
						} catch (Exception e) {
							logger.error("Database.performDeleteValue close cursors failed: " + e.toString(), e);
						}
					}
					if (ok && (testCount == -1)) {
						// Fehler ausgeben, dass Testzählung nicht erfolgreich
						mainFrame.showDBMessageWithoutContinueAction(getDatabaseSession().getLastErrorMessage(),
								"delete field");
						ok = false;
					} else if (testCount == 0) {
						mainFrame.showDBMessageWithoutContinueAction("test count got 0 datasets", "delete field");
						ok = false;
					} else if (testCount > 1) {
						mainFrame.showDBMessageWithoutContinueAction("test count got more than one datasets \n("
								+ String.valueOf(testCount)
								+ ")",
								"delete field");
						ok = false;
					}
					if (testCount == 1) {
						// Wertzuweisung zusammensetzen
						final StringBuilder sb = new StringBuilder();
						sb.append("update ");
						sb.append(lastTable);
						sb.append(" set ");
						sb.append(columnNames[col]);
						sb.append("=null ");
						sb.append(whereBed);
						// update durchfähren
						PreparedStatement ps = null;
						try {
							ps = session.createPreparedStatement(sb.toString());
							completeWherePartOfPreparedStatement(ps, row, 0, null);
							final int count = ps.executeUpdate();
							if (count != 1) {
								ok = false;
							}
						} catch (SQLException sqle) {
							mainFrame.showDBMessage(sqle.getMessage(), "delete field");
							ok = false;
						} finally {
							try {
								ps.close();
							} catch (Exception e) {
								logger.error("deleteValue close prepared statement failed: " + e.toString(), e);
							}
						}
					}
				} // else if (countPk == 0)
			} else { // if (whereBed != null)
				currentCellEditor.toFront();
				ok = false;
			}
		} else { // if (getDatabaseSession().isConnected())
			mainFrame.showDBMessageWithoutContinueAction(
					"database connection disconnected!",
					"delete field");
			ok = false;
		}
		if (ok) {
			setValueAt(null, row, col);
		}
		return ok;
	}

	private final class UpdateValueExecuter {

		private Object value;
		private int row;
		private int col;
		private JFrame frame;
		private Object constraint;

		UpdateValueExecuter(Object value, int row, int col, JFrame frame, Object constraint) {
			this.value = value;
			this.row = row;
			this.col = col;
			this.frame = frame;
			this.constraint = constraint;
		}

		public void execute() {
			if (mainFrame.isDatabaseBusy() == false) {
				if (performUpdateValue(value, row, col, constraint)) {
					frame.dispose();
				} else {
					frame.toFront();
				}
				mainFrame.setDatabaseBusyFiltered(false, null);
			}
		}

	}

	/**
	 * schreibt Wert in Datenbank, benutzt aber eine eigenen Thread hierfür
	 * 
	 * @param value -
	 *            zuschreibender Wert als String, bei LOB-Feldern steht hier der
	 *            Filename aus dem gelesen werden soll.
	 * @param row -
	 *            kennzeichnet die Zeile (row wird benutzt um den primary key zu
	 *            ermitteln)
	 * @param col -
	 *            kennzeichnet in welches Feld
	 * @return true - wenn update erfolgreich
	 */
	public void updateValue(Object value,
			int row,
			int col,
			JFrame frame,
			Object constraint) {
		if (logger.isDebugEnabled()) {
			logger.debug("updateValue(value="
					+ value
					+ ", row="
					+ row
					+ ", col="
					+ col
					+ ", constraint="
					+ constraint
					+ ")");
		}
		final UpdateValueExecuter uvt = new UpdateValueExecuter(
				value,
				row,
				col,
				frame,
				constraint);
		uvt.execute();
	}

	/**
	 * schreibt Wert in Datenbank
	 * 
	 * @param value -
	 *            zuschreibender Wert als String, bei LOB-Feldern steht hier der
	 *            Filename aus dem gelesen werden soll.
	 * @param row -
	 *            kennzeichnet die Zeile (row wird benutzt um den primary key zu
	 *            ermitteln)
	 * @param col -
	 *            kennzeichnet in welches Feld
	 * @return true - wenn update erfolgreich
	 */
	public boolean performUpdateValue(Object value,
			int row,
			int col,
			Object constraint) {
		boolean ok = true;
		if (getDatabaseSession().isConnected()) {
			// Indexfelder ermitteln an Hand der ausgewählten Zeile
			final String whereBed = createWherePartOfPreparedStatement(row);
			if (whereBed != null) {
				// Test-select ausfähren
				StringBuilder sqlBuffer = new StringBuilder("select count(");
				int x;
				int countPk = 0;
				for (x = 0; x < columnPkInfo.length; x++) {
					if (columnPkInfo[x].booleanValue()) {
						sqlBuffer.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnNames[x]));
						sqlBuffer.append(") from ");
						sqlBuffer.append(SQLCodeGenerator.getInstance().getEncapsulatedName(lastTable));
						sqlBuffer.append(' ');
						sqlBuffer.append(whereBed);
						countPk++;
						break; // Schleife abbrechen, ein Feldname reicht für
						// count(..)
					}
				}
				// is statement complete ?
				if (countPk == 0) { // there must be a field name
					// !
					mainFrame.showDBMessageWithoutContinueAction("no primary key definied\n"
							+ "define primary keys with context menu from table header",
							"update value");
					ok = false;
				} else {
					// warning if more than one row will be affected
					int testCount = -1;
					ResultSet rs = null;
					PreparedStatement psTest = null;
					try {
						psTest = getDatabaseSession().createPreparedStatement(sqlBuffer.toString());
						completeWherePartOfPreparedStatement(psTest, row, 0, constraint);
						rs = psTest.executeQuery();
						if (rs != null) {
							if (rs.next()) {
								testCount = rs.getInt(1);
							}
						}
					} catch (SQLException sqle) {
						logger.error("update test count failed: " + sqlBuffer.toString(), sqle);
						mainFrame.showDBMessageWithoutContinueAction(
								sqle.getMessage(),
								"test of primary key");
						ok = false;
					} finally {
						try {
							if (rs != null) {
								rs.close();
							}
							if (psTest != null) {
								psTest.close();
							}
						} catch (Exception e) {
							logger.error("performUpdateValue close cursors failed: " + e.toString());
						}
					}
					if (ok && (testCount == -1)) {
						// Fehler ausgeben, dass Testzählung nicht erfolgreich
						mainFrame.showDBMessageWithoutContinueAction(getDatabaseSession().getLastErrorMessage(),
								"update value");
						currentCellEditor.toFront();
						ok = false;
					} else if (testCount == 0) {
						mainFrame.showDBMessageWithoutContinueAction("test count got 0 datasets!", "update value");
						currentCellEditor.toFront();
						ok = false;
					} else if (testCount > 1) {
						mainFrame.showDBMessageWithoutContinueAction("test count got more than one datasets\n("
								+ String.valueOf(testCount)
								+ ")",
								"update value");
						currentCellEditor.toFront();
						ok = false;
					}
					if (testCount == 1) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("update ");
                        sb.append(lastTable);
                        sb.append(" set ");
                        sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnNames[col]));
                        sb.append("=? ");
                        sb.append(whereBed);
                        // update durchfähren
                        PreparedStatement ps = null;
                        try {
                            ps = getDatabaseSession().createPreparedStatement(sb.toString());
                            ok = setParameterValue(ps, 1, col, value, constraint);
                            if (ok) { // nur weitermachen wenn bis hier
                                // alles in Ordnung
                                completeWherePartOfPreparedStatement(
                                		ps,
                                        row,
                                        1,
                                        constraint);
                                final int count = ps.executeUpdate();
                                if (count == 1) { // alles ok !
                                    if (columnTypeNames[col].contains("BINARY")
                                            || (columnTypeNames[col]).equals("BLOB")) {
                                    	if (value instanceof String) {
                                            setValueAtLogicalIndexes(
                                            		row,
                                                    col,
                                                    new BinaryDataFile((String) value),
                                                    null);
                                    	} else {
                                            setValueAtLogicalIndexes(
                                            		row,
                                                    col,
                                                    value,
                                                    null);
                                    	}
                                    } else {
                                        setValueAtLogicalIndexes(
                                        		row,
                                                col,
                                                value,
                                                constraint);
                                    }
                                } else {
                                    ok = false;
                                }
                            }
                        } catch (SQLException sqle) {
    						logger.error("update failed: " + sb.toString(), sqle);
                            mainFrame.showDBMessageWithoutContinueAction(sqle.getMessage(), "update value");
                            currentCellEditor.toFront();
                            ok = false;
                        } finally {
                            try {
                                ps.close();
                            } catch (Exception e) {
                                logger.error("performUpdateValue close prepared statement failed: " + e.toString());
                            }
                        }
					} // if (testCount == 1)
				} // else if (countPk == 0)
			} else { // if (whereBed != null)
				currentCellEditor.toFront();
				ok = false;
			}
		} else { // if (getDatabaseSession().isConnected())
			mainFrame.showDBMessageWithoutContinueAction("database connection is disconnected",	"update value");
			currentCellEditor.toFront();
			ok = false;
		}
		return ok;
	}

	private boolean setParameterValue(PreparedStatement ps,
			int paramIndex,
			int columnIndex,
			Object value,
			Object constraint) {
		boolean ok = true;
		if (logger.isDebugEnabled()) {
			logger.debug("setParameterValue(paramIndex="
					+ paramIndex
					+ ", columnIndex="
					+ columnIndex
					+ ", constraint="
					+ constraint
					+ ", value="
					+ value);
		}
		try {
			// den IO-Paramater abhängig vom Datentyp setzen
			if (getColumnBasicType(columnIndex) == BasicDataType.CHARACTER.getId()) {
                if ((value != null) && (value.toString().length() > 0)) {
    				ps.setString(paramIndex, value.toString());
                } else {
                    ps.setNull(paramIndex, Types.VARCHAR);
                }
//			} else if (getColumnBasicType(columnIndex) == BasicDataType.BINARY.getId()) {
//				StringReplacer sr = new StringReplacer(value.toString());
//				sr.replace("\n", "");
//				sr.replace(" ", "");
//				sr.replace("\t", "");
//				ps.setString(paramIndex, sr.getResultText());
			} else if (getColumnBasicType(columnIndex) == BasicDataType.INTEGER.getId()) {
				if ((value != null) && (value.toString().length() > 0)) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug(" type=BASICTYPE_INTEGER");
						}
						ps.setInt(
								paramIndex,
								Integer.parseInt(value.toString().trim()));
					} catch (NumberFormatException nfe) {
						ok = false;
						mainFrame.showDBMessageWithoutContinueAction("interpretation of integer value failed\n"
								+ nfe.getMessage(),
								"update");
					}
				} else {
					ps.setNull(paramIndex, Types.NUMERIC);
				}
			} else if (getColumnBasicType(columnIndex) == BasicDataType.DOUBLE.getId()) {
				if ((value != null) && (value.toString().length() > 0)) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug(" type=BASICTYPE_DOUBLE");
						}
						ps.setDouble(
								paramIndex,
								Double.parseDouble(value.toString().trim()));
					} catch (NumberFormatException nfe) {
						ok = false;
						mainFrame.showDBMessageWithoutContinueAction("interpretation of double value failed\n"
								+ nfe.getMessage(),
								"update");
					}
				} else {
					ps.setNull(paramIndex, Types.NUMERIC);
				}
			} else if (getColumnBasicType(columnIndex) == BasicDataType.LONG.getId()) {
				if ((value != null) && (value.toString().length() > 0)) {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug(" type=BASICTYPE_LONG");
						}
						ps.setLong(
								paramIndex,
								Long.parseLong(value.toString().trim()));
					} catch (NumberFormatException nfe) {
						ok = false;
						mainFrame.showDBMessageWithoutContinueAction("interpretation of long value failed\n"
								+ nfe.getMessage(),
								"update");
					}
				} else {
					ps.setNull(paramIndex, Types.NUMERIC);
				}
			} else if (getColumnBasicType(columnIndex) == BasicDataType.BOOLEAN.getId()) {
				if ((value != null) && (value.toString().length() > 0)) {
					if (logger.isDebugEnabled()) {
						logger.debug(" type=BASICTYPE_BOOLEAN");
					}
					ps.setBoolean(
							paramIndex,
							Boolean.parseBoolean(value.toString().trim()));
				} else {
					ps.setNull(paramIndex, Types.BOOLEAN);
				}
			} else if (getColumnBasicType(columnIndex) == BasicDataType.DATE.getId()) {
				try {
					if (value != null) {
						if (logger.isDebugEnabled()) {
							logger.debug(" type=BASICTYPE_DATE");
						}
						if (value instanceof String	&& (value.toString().length() > 0)) {
							if (constraint instanceof String) {
								SimpleDateFormat sdf2 = new SimpleDateFormat((String) constraint);
								Date date = null;
								try {
									date = sdf2.parse(value.toString().trim());
								} catch (ParseException pe) {
									date = GenericDateUtil.parseDate(value.toString().trim());
								}
								ps.setTimestamp(
										paramIndex,
										new Timestamp(date.getTime()));
							} else {
								java.util.Date date = GenericDateUtil.parseDate(value.toString().trim());
								ps.setTimestamp(
										paramIndex,
										new Timestamp(date.getTime()));
							}
						} else if (value instanceof Timestamp) {
							ps.setTimestamp(paramIndex, (Timestamp) value);
						}
					} else {
						ps.setNull(paramIndex, Types.DATE);
					}
				} catch (java.text.ParseException pe) {
					mainFrame.showDBMessageWithoutContinueAction(pe.getMessage()
							+ "\nCheck date format in preferences or in value editor.",
							"validate date value");
					ok = false;
				}
			} else if (getColumnBasicType(columnIndex) == BasicDataType.CLOB.getId()) {
				// testen ob noch setString genutzt werden kann
                if (value != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(" type=BASICTYPE_CLOB");
                    }
                    ps.setCharacterStream(
                    		paramIndex,
                            new StringReader(value.toString()),
                            value.toString().length());
                } else {
                    ps.setNull(paramIndex, Types.CLOB);
                }
			} else if (getColumnBasicType(columnIndex) == BasicDataType.BINARY.getId()) {
				if (value instanceof String) {
					if ((value != null) && (value.toString().length() > 0)) {
						try {
							// value muss hier den Namen der Datei enthalten, deren
							// Inhalt als Binary in die DB geschrieben werden soll
							final File f = new File(value.toString());
							if (logger.isDebugEnabled()) {
								logger.debug(" type=BASICTYPE_BINARY");
							}
							ps.setBinaryStream(
									paramIndex,
									new FileInputStream(f),
									(int) f.length());
						} catch (FileNotFoundException fnfe) {
							mainFrame.showDBMessageWithoutContinueAction(
									fnfe.getMessage(),
									"File upload");
							ok = false;
						}
					} else {
						ps.setNull(paramIndex, Types.LONGVARBINARY);
					}
				} else if (value instanceof byte[]) {
					ps.setBytes(paramIndex, (byte[]) value);
				}
			} // if ((columnTypeNames[col].equals....
		} catch (SQLException sqle) {
			mainFrame.showDBMessageWithoutContinueAction(
					sqle.getMessage(),
					"set value");
			currentCellEditor.toFront();
			ok = false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("\n");
		}
		return ok;
	}

	private void setupParameterValues(
			CallableStatement callableStatement,
			SQLStatement sqlps) throws Exception {
		SQLPSParam parameter = null;
		String value = null;
		for (int i = 0; i < sqlps.getParams().size(); i++) {
			parameter = sqlps.getParams().get(i);
			value = parameter.getValue();
			try {
				// den IO-Paramater abhängig vom Datentyp setzen
				if (parameter.getBasicType() == BasicDataType.CHARACTER.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.VARCHAR);
					} else {
						if ((value != null) && (value.length() > 0)) {
							callableStatement.setString(
									parameter.getIndex(),
									value);
						} else {
							callableStatement.setNull(
									parameter.getIndex(),
									Types.VARCHAR);
						}
					}
				} else if (parameter.getBasicType() == BasicDataType.DOUBLE.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.NUMERIC);
					} else {
						if ((value != null) && (value.trim().length() > 0)) {
							try {
								callableStatement.setDouble(
										parameter.getIndex(),
										Double.parseDouble(value.trim()));
							} catch (NumberFormatException nfe) {
								throw new Exception("Interpretation of double value failed\n" + nfe.getMessage(), nfe);
							}
						} else {
							callableStatement.setNull(
									parameter.getIndex(),
									Types.NUMERIC);
						}
					}
				} else if (parameter.getBasicType() == BasicDataType.LONG.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.NUMERIC);
					} else {
						if ((value != null) && (value.trim().length() > 0)) {
							try {
								callableStatement.setLong(
										parameter.getIndex(),
										Long.parseLong(value.trim()));
							} catch (NumberFormatException nfe) {
								throw new Exception("Interpretation of long value failed\n" + nfe.getMessage(), nfe);
							}
						} else {
							callableStatement.setNull(
									parameter.getIndex(),
									Types.NUMERIC);
						}
					}
				} else if (parameter.getBasicType() == BasicDataType.INTEGER.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.NUMERIC);
					} else {
						if ((value != null) && (value.trim().length() > 0)) {
							try {
								callableStatement.setInt(
										parameter.getIndex(),
										Integer.parseInt(value.trim()));
							} catch (NumberFormatException nfe) {
								throw new Exception("Interpretation of integer value failed\n" + nfe.getMessage(), nfe);
							}
						} else {
							callableStatement.setNull(
									parameter.getIndex(),
									Types.NUMERIC);
						}
					}
				} else if (parameter.getBasicType() == BasicDataType.BOOLEAN.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.BOOLEAN);
					} else {
						if ((value != null) && (value.length() > 0)) {
							callableStatement.setBoolean(
									parameter.getIndex(),
									Boolean.parseBoolean(value));
						} else {
							callableStatement.setNull(
									parameter.getIndex(),
									Types.BOOLEAN);
						}
					}
				} else if (parameter.getBasicType() == BasicDataType.DATE.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.DATE);
					} else {
						try {
							if (value != null) {
								if (value.length() > 0) {
									java.util.Date date = GenericDateUtil.parseDate(value.trim());
									callableStatement.setTimestamp(
											parameter.getIndex(),
											new Timestamp(date.getTime()));
								}
							} else {
								callableStatement.setNull(
										parameter.getIndex(),
										Types.DATE);
							}
						} catch (java.text.ParseException pe) {
							throw new Exception(pe.getMessage()	+ ": check date format in preferences", pe);
						}
					}
				} else if (parameter.getBasicType() == BasicDataType.CLOB.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.CLOB);
					} else {
						// testen ob noch setString genutzt werden kann
						if ((value != null) && (value.length() > 0)) {
                            callableStatement.setCharacterStream(
                                    parameter.getIndex(),
                                    new StringReader(value),
                                    value.length());
						} else {
							callableStatement.setNull(
									parameter.getIndex(),
									Types.CLOB);
						}
					}
				} else if (parameter.getBasicType() == BasicDataType.BINARY.getId()) {
					if (parameter.isOutParam()) {
						callableStatement.registerOutParameter(
								parameter.getIndex(),
								Types.LONGVARBINARY);
					} else {
						if ((value != null) && (value.length() > 0)) {
							try {
								// value muss hier den Namen der Datei
								// enthalten, deren Inhalt als Binary in die DB
								// geschrieben werden soll
								final File f = new File(value);
								callableStatement.setBinaryStream(
										parameter.getIndex(),
										new FileInputStream(f),
										(int) f.length());
							} catch (FileNotFoundException fnfe) {
								throw new Exception("File upload for binary db upload failed: " + fnfe.getMessage(), fnfe);
							}
						} else {
							callableStatement.setNull(
									parameter.getIndex(),
									Types.LONGVARBINARY);
						}
					}
				} // if ((columnTypeNames[col].equals....
			} catch (SQLException sqle) {
				throw new Exception("set parameter failed:" + sqle.getMessage(), sqle);
			}
		}
	}

	private final class WriteLobInFileThread extends Thread {

		private File f;
		private int row;
		private int col;
		private JFrame frame;

		WriteLobInFileThread(File f, int row, int col, JFrame frame) {
			this.f = f;
			this.row = row;
			this.col = col;
			this.frame = frame;
		}

        @Override
		public void run() {
			if (mainFrame.isDatabaseBusy() == false) {
				mainFrame.setDatabaseBusyFiltered(true, "perform write LOB in file");
				performWriteLOBValueInFile(f, row, col);
				frame.toFront();
				mainFrame.setDatabaseBusyFiltered(false, null);
			}
		}

	}

	public void writeLOBValueInFile(
			File file,
			int row,
			int col,
			JFrame frame) {
		final WriteLobInFileThread thread = new WriteLobInFileThread(
				file,
				row,
				col,
				frame);
		thread.start();
	}

	/**
	 * schreibt LOB-Wert aus Datenbank in File
	 * 
	 * @param file -
	 *            File in den geschrieben werden soll
	 * @param row ,
	 *            col kennzeichnet in welches Zeile, Feld row wird benutzt um
	 *            den primary key zu ermitteln
	 */
	public boolean performWriteLOBValueInFile(
			File file,
			int row,
			int col) {
		boolean ok = false;
		if (getDatabaseSession().isConnected()) {
			// Indexfelder ermitteln an Hand der ausgewählten Zeile
			final String whereBed = createWherePartOfPreparedStatement(row);
			// Test-select ausfähren
			StringBuilder sql_loc = new StringBuilder("select count(");
			int x;
			int countPk = 0;
			for (x = 0; x < columnPkInfo.length; x++) {
				if (columnPkInfo[x].booleanValue()) {
					sql_loc.append(columnNames[x]);
					sql_loc.append(") from ");
					sql_loc.append(lastTable);
					sql_loc.append(' ');
					sql_loc.append(whereBed);
					countPk++;
					break; // Schleife abbrechen, ein Feldname reicht für
					// count(..)
				}
			}
			// ist Statement vollständig ?
			if (countPk == 0) { // so muss auch der Feldname vorhanden sein !
				mainFrame.showDBMessageWithoutContinueAction("No primary key definied\n"
						+ "define primary keys with context menu of table header.",
						"select LOB");
				ok = false;
			} else {
				// Warnung wenn mehr als eine Zeile selektiert
				int testCount = -1;
				ResultSet rs_loc = null;
				PreparedStatement psTest = null;
				try {
					psTest = getDatabaseSession().createPreparedStatement(sql_loc.toString());
					completeWherePartOfPreparedStatement(psTest, row, 0, null);
					rs_loc = psTest.executeQuery();
					if (rs_loc != null) {
						if (rs_loc.next()) {
							testCount = rs_loc.getInt(1);
						}
					}
				} catch (SQLException sqle) {
					mainFrame.showDBMessageWithoutContinueAction(sqle.getMessage(), "test primary key");
					ok = false;
				} finally {
					try {
						if (rs_loc != null) {
							rs_loc.close();
						}
						if (psTest != null) {
							psTest.close();
						}
					} catch (Exception e) {
						logger.error("writeLOBValueInFile close cursors failed: " + e.toString(), e);
					}
				}
				if (testCount == -1) {
					// Fehler ausgeben, dass Testzählung nicht erfolgreich
					mainFrame.showDBMessageWithoutContinueAction(getDatabaseSession().getLastErrorMessage(), "select LOB");
					currentCellEditor.toFront();
					ok = false;
				} else if (testCount == 0) {
					mainFrame.showDBMessageWithoutContinueAction("test count got 0 datasets", "select LOB");
					currentCellEditor.toFront();
					ok = false;
				} else if (testCount > 1) {
					mainFrame.showDBMessageWithoutContinueAction("test count got more than one datasets ("
							+ String.valueOf(testCount)
							+ ")",
							"select LOB");
					currentCellEditor.toFront();
					ok = false;
				}
				if (testCount == 1) {
					// Wertzuweisung zusammensetzen
					final StringBuilder sb = new StringBuilder();
					sb.append("select ");
					sb.append(columnNames[col]);
					sb.append(" from ");
					sb.append(lastTable);
					sb.append(whereBed);
					// update durchfähren
					PreparedStatement ps = null;
					InputStream is = null;
					FileOutputStream os = null;
					try {
						ps = session.createPreparedStatement(sb.toString());
						completeWherePartOfPreparedStatement(ps, row, 0, null);
						rs_loc = ps.executeQuery();
						if ((rs_loc != null) && rs_loc.next()) {
							if (columnTypeNames[col].equals("BLOB") || columnTypeNames[col].equals("LONGVARBINARY")) {
								Object test = rs_loc.getBlob(1);
								if (test instanceof Blob) {
									Blob blob =  (Blob) test;
									//is = rs_loc.getBinaryStream(1);
									is = blob.getBinaryStream();
								} else if (test != null) {
									logger.error("Get anything else than an Blob object:" + test.getClass().getName());
								}
							}
							try {
								if (is != null) {
									if (logger.isDebugEnabled()) {
										logger.debug("write in File " + file.getAbsolutePath());
									}
									os = new FileOutputStream(file);
									final byte[] buffer = new byte[1024];
									int length = -1;
									while ((length = is.read(buffer)) != -1) {
										os.write(buffer, 0, length);
									}
									ok = true;
								} else { // if (is != null)
									logger.error("write lob in file failed: no inputstream received");
								}
							} catch (IOException ioe) {
								mainFrame.showDBMessageWithoutContinueAction(
										ioe.getMessage(),
										"write cell data");
								currentCellEditor.toFront();
							}
						} // if ((rs != null) && (rs.next()))
					} catch (SQLException sqle) {
						mainFrame.showDBMessageWithoutContinueAction(
								sqle.getMessage(),
								"read cell data");
						currentCellEditor.toFront();
					} finally {
						try {
							if (os != null) {
								os.close();
							}
							if (is != null) {
								is.close();
							}
						} catch (Exception e) {}
						try {
							rs_loc.close();
							ps.close();
						} catch (Exception e) {}
					}
				} // if (testCount == 1)
			} // if (countPk == 0)
		} else { // if (getDatabaseSession().isConnected())
			mainFrame.showDBMessageWithoutContinueAction("database connection disconnected", "read data from db");
			currentCellEditor.toFront();
		} // if (getDatabaseSession().isConnected())
		return ok;
	}

	private final class InsertNewRowInDBThread extends Thread {

        @Override
		public void run() {
			if (!mainFrame.isDatabaseBusy()) {
				mainFrame.setDatabaseBusyFiltered(true, "perform insert");
				if (performInsertNewRowInDB()) {
					if (getNewRowIndex() == -1) { // wenn -1 dann ist das
						// Einfügen erfolgreich
						// verlaufen
						mainFrame.setCreateNewRowEnabled(true);
					}
					mainFrame.resultTable.repaint();
				}
				mainFrame.setDatabaseBusyFiltered(false, null);
			}
		}

	}

	public void insertNewRowInDB() {
		final InsertNewRowInDBThread thread = new InsertNewRowInDBThread();
		thread.start();
	}

	/**
	 * insert für eine neue Zeile in der DB-Tabelle
	 */
	public boolean performInsertNewRowInDB() {
		boolean ok = true;
		if (newRowIndex != -1) {
			final Object[] row = resultSetResults.get(newRowIndex);
			// nun die Feld des Types ROWID auszählen
			int rowCount = 0;
			for (int i = 0; i < getColumnCount(); i++) {
				if (getColumnBasicType(i) != SQLField.ORACLE_ROWID) {
					rowCount++;
				}
			}
			// wenn ausser rowid noch beschreibbare Felder da sind neue Array
			// bilden
			if (rowCount > 0) {
				final Object[] newRow_loc = new Object[rowCount];
				final int[] columnIndexes = new int[rowCount];
				int x = 0;
				for (int i = 0; i < row.length; i++) {
					if (getColumnBasicType(i) != SQLField.ORACLE_ROWID) {
						newRow_loc[x] = row[i];
						columnIndexes[x] = i;
						x++;
					}
				}
				// prepared-statement zusammenbauen
				final StringBuilder sb = new StringBuilder();
				sb.append("insert into ");
				sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(lastTable));
				int valueCount = 0;
				boolean firstLoop = true;
				// nun die Felder selektieren, die im Array auch Inhalt haben
				for (int i = 0; i < newRow_loc.length; i++) {
					if (newRow_loc[i] != null) {
						if (firstLoop) {
							sb.append(" (");
							firstLoop = false;
						} else {
							sb.append(',');
						}
						sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnNames[columnIndexes[i]]));
						valueCount++;
					}
				} // for (int i=0; i < newRow.length; i++)
				firstLoop = true;
				if (valueCount == 0) {
					// keine Inhalte eizufügen, dann null für jedes Feld
					// einfügen
					sb.append(" values (");
					for (int i = 0; i < newRow_loc.length; i++) {
						if (!firstLoop) {
							sb.append(',');
						} else {
							firstLoop = false;
						}
						sb.append("null"); // Parameter-Platzhalter hinzufügen
					}
					sb.append(')');
				} else { // if (valueCount == 0)
					// wenn Inhalte vorhanden dann
					// die Parameter hierfür vorsehen
					sb.append(") values (");
					for (int i = 0; i < newRow_loc.length; i++) {
						if (newRow_loc[i] != null) {
							if (firstLoop == false) {
								sb.append(',');
							} else {
								firstLoop = false;
							}
    						sb.append('?'); // Parameter-Platzhalter
						}
					} // for (int i=0; i < newRow.length; i++)
					sb.append(')');
				} // if (valueCount == 0)
				// nun PreparedStatement erzeugen
				PreparedStatement ps = null;
				try {
					ps = getDatabaseSession().createPreparedStatement(sb.toString());
					// nun die Werte setzen
					int paramIndex = 1;
					for (int i = 0; i < newRow_loc.length; i++) {
						if (newRow_loc[i] != null) {
                            ok = setParameterValue(ps,
                                    paramIndex,
                                    columnIndexes[i],
                                    newRow_loc[i],
                                    null);
                            paramIndex++;
							if (ok == false) {
								break;
							}
						}
					} // for (int i=0; i < newRow.length; i++)
					// Statement abfeuern
					if (ok) {
						ps.executeUpdate();
					}
				} catch (SQLException sqle) {
					logger.error("insert failed: " + sb.toString(), sqle);
					mainFrame.showDBMessage(sqle.getMessage(), "insert row");
					ok = false;
				} finally {
					try {
						ps.close();
					} catch (Exception e) {
						logger.error("insertNewRowInDB close statement failed: " + e.toString());
					}
				}
			} // if (rowCount > 0)
			if (ok) {
				newRowIndex = -1; // so wird das nächste Einfügen freigegeben
			}
		} else { // if (newRowIndex != -1)
			ok = false;
		} // if (newRowIndex != -1)
		return ok;
	}

	/**
	 * bringt den Zeileindex der neu eingefügten Zeile zurück
	 * 
	 * @return index der neu eingefügten und noch nicht gespeicherten Zeile
	 */
	public int getNewRowIndex() {
		return newRowIndex;
	}

	/**
	 * fügt eine neue Zeile in die Tabelle ein und setzt newRowIndex
	 * 
	 * @return true wenn neue Zeile eingefügt, false wenn keine Zeile eingefügt
	 *         (keine Tabelle zuvor aufgebaut ?)
	 */
	public boolean insertRowInTable() {
		if (getColumnCount() > 0) {
			newRow = new Object[getColumnCount()];
			resultSetResults.add(newRow);
			newRowIndex = resultSetResults.indexOf(newRow);
			fireTableRowsInserted(newRowIndex, newRowIndex);
			if (logger.isDebugEnabled()) {
				logger.debug("new line with index=" + newRowIndex + " appended");
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean insertRowInTableAsCopy() {
		if (getColumnCount() > 0) {
			final int currentRowIndex = table.getSelectedRow();
			if (currentRowIndex != -1) {
				final Object[] temp = resultSetResults.get(currentRowIndex);
				newRow = new Object[getColumnCount()];
				for (int i = 0; i < newRow.length; i++) {
					newRow[i] = temp[i];
				}
				resultSetResults.add(newRow);
			} else {
				newRow = new Object[getColumnCount()];
				resultSetResults.add(newRow);
			}
			newRowIndex = resultSetResults.indexOf(newRow);
			fireTableRowsInserted(newRowIndex, newRowIndex);
			if (logger.isDebugEnabled()) {
				logger.debug("new line with index=" + newRowIndex + " appended");
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * löscht die neu eingefügte Zeile wieder
	 */
	public void removeNewRow() {
		if (newRowIndex != -1) {
			resultSetResults.remove(newRowIndex);
			fireTableRowsDeleted(newRowIndex, newRowIndex);
			newRowIndex = -1;
		}
	}

	/**
	 * exportiert die Daten des TableModel in ein File
	 */
	public boolean exportTableInCSVFile(
			File file,
			String delim,
			String enclosure,
			int exportType,
			String dateFormatTemplate,
			boolean withHeader,
			boolean setComma4Point) {
		boolean ok = false;
		try {
			mainFrame.setStatusMessage("export table in file:"	+ file.getName());
			BufferedWriter bw = null;
			if (Main.currentCharSet != null) {
				bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(file),
						Main.currentCharSet));
			} else {
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			}
			if (exportType == CSV_FORMAT) {
				for (int r = 0; r < resultSetResults.size(); r++) {
					if ((r == 0) && withHeader) {
						bw.write(createDelimitedHeaderLine(delim, enclosure));
						bw.newLine();
					}
					bw.write(createDelimitedLine(r, delim, enclosure, setComma4Point));
					bw.newLine();
				}
			} else if (exportType == INSERT_FORMAT) {
				final String insertStatementBegin = createInsertStatementText();
				for (int r = 0; r < resultSetResults.size(); r++) {
					bw.write(createInsertLine(
							r,
							insertStatementBegin,
							dateFormatTemplate));
					bw.newLine();
				}
			} // if (exportType==CSV_FORMAT)
			bw.flush();
			bw.close();
			ok = true;
			mainFrame.setStatusMessage("ready");
		} catch (FileNotFoundException fnfe) {
			mainFrame.showDBMessageWithoutContinueAction(fnfe.getMessage(),	"open export file");
		} catch (IOException ioe) {
			mainFrame.showDBMessageWithoutContinueAction(ioe.getMessage(), "write in export file");
		}
		return ok;
	}
	
	public void exportTableToSpreadSheetFile(File f, boolean withHeader) throws Exception {
		ExporterToSpreadsheetFile exporter = new ExporterToSpreadsheetFile();
		exporter.setOutputFile(f);
		exporter.setDateFormat(MainFrame.getDateFormatMask());
		for (int r = 0; r < resultSetResults.size(); r++) {
			if (r == 0) {
				if (withHeader) {
					exporter.writeRow(columnNames);
				}
			}
			Object[] oneRow = resultSetResults.get(r);
			exporter.writeRow(oneRow);
		}
		exporter.writeWorkbook();
		exporter.close();
	}

	public String exportTableToString(
			String delim,
			String enclosure,
			int exportType,
			String dateFormatTemplate,
			boolean withHeader,
			boolean setComma4Point) {
		final StringBuilder sb = new StringBuilder();
		if (exportType == CSV_FORMAT) {
			for (int r = 0; r < resultSetResults.size(); r++) {
				if ((r == 0) && withHeader) {
					sb.append(createDelimitedHeaderLine(delim, enclosure));
					sb.append('\n');
				}
				sb.append(createDelimitedLine(r, delim, enclosure, setComma4Point));
				sb.append('\n');
			}
		} else if (exportType == INSERT_FORMAT) {
			final String insertStatementBegin = createInsertStatementText();
			for (int r = 0; r < resultSetResults.size(); r++) {
				sb.append(createInsertLine(r,
						insertStatementBegin,
						dateFormatTemplate));
				sb.append('\n');
			}
		} // if (exportType==CSV_FORMAT)
		mainFrame.setStatusMessage("ready");
		return sb.toString();
	}

	public String createImportScriptText(
			int limit,
			String dateFormatTemplate) {
		final StringBuilder sb = new StringBuilder();
		final String insertStatementBegin = createInsertStatementText();
		for (int r = 0; r < resultSetResults.size(); r++) {
			if (r == limit) {
				break;
			}
			sb.append(createInsertLine(r,
					insertStatementBegin,
					dateFormatTemplate));
			sb.append(Main.LINE_FEED);
		}
		return sb.toString();
	}

	/**
	 * exportiert die Daten des TableModel in ein File
	 */
	public boolean exportTableInMultipleFiles(
			File fileTemplate,
			String delim,
			String enclosure,
			int exportType,
			String dateFormatTemplate,
			long maxDatasetsPerFile,
			boolean withHeader,
			boolean setComma4Point) {
		boolean ok = false;
		int fileIndex = 0;
		int rowCountPerFile = 0;
		try {
			File f;
			BufferedWriter bw = null;
			boolean firstLoop = true;
			if (exportType == CSV_FORMAT) {
				for (int r = 0; r < resultSetResults.size(); r++) {
					if (firstLoop) {
						firstLoop = false;
						if (fileIndex > 0) {
							f = createNextFile(fileTemplate, fileIndex);
						} else {
							f = fileTemplate;
						}
						mainFrame.setStatusMessage("export table in file: " + f.getName());
						bw = new BufferedWriter(new FileWriter(f));
						if (withHeader) {
							bw.write(createDelimitedHeaderLine(delim, enclosure));
							bw.newLine();
						}
					} else if (rowCountPerFile == maxDatasetsPerFile) {
						rowCountPerFile = 0;
						fileIndex++;
						f = createNextFile(fileTemplate, fileIndex);
						mainFrame.setStatusMessage("export table in file: " + f.getName());
						bw.flush();
						bw.close();
						bw = new BufferedWriter(new FileWriter(f));
						if (withHeader) {
							bw.write(createDelimitedHeaderLine(delim, enclosure));
							bw.newLine();
						}
					}
					bw.write(createDelimitedLine(r, delim, enclosure, setComma4Point));
					bw.newLine();
					rowCountPerFile++;
				}
			} else if (exportType == INSERT_FORMAT) {
				final String insertStatementBegin = createInsertStatementText();
				for (int r = 0; r < resultSetResults.size(); r++) {
					if (firstLoop) {
						firstLoop = false;
						if (fileIndex > 0) {
							f = createNextFile(fileTemplate, fileIndex);
						} else {
							f = fileTemplate;
						}
						mainFrame.setStatusMessage("export table in file: " + f.getName());
						bw = new BufferedWriter(new FileWriter(f));
					} else if (rowCountPerFile == maxDatasetsPerFile) {
						rowCountPerFile = 0;
						fileIndex++;
						f = createNextFile(fileTemplate, fileIndex);
						mainFrame.setStatusMessage("export table in file: " + f.getName());
						bw.flush();
						bw.close();
						bw = new BufferedWriter(new FileWriter(f));
					}
					bw.write(createInsertLine(r,
							insertStatementBegin,
							dateFormatTemplate));
					bw.newLine();
					rowCountPerFile++;
				}
			} // if (exportType==CSV_FORMAT)
			if (bw != null) {
				bw.flush();
				bw.close();
			}
			ok = true;
			mainFrame.setStatusMessage("Fertig");
		} catch (FileNotFoundException fnfe) {
			mainFrame.showDBMessageWithoutContinueAction(fnfe.getMessage(), "open export file");
		} catch (IOException ioe) {
			mainFrame.showDBMessageWithoutContinueAction(ioe.getMessage(), "write in export file");
		}
		return ok;
	}

	public File createNextFile(
			File f,
			int index) {
		String path = f.getParent();
		if (path == null) {
			path = "./";
		} else {
			path = path + "/";
		}
		final String fullname = f.getName();
		final int p0 = fullname.lastIndexOf(".");
		String name;
		if (p0 != -1) {
			name = fullname.substring(0, p0)
					+ "_"
					+ String.valueOf(index)
					+ fullname.substring(p0, fullname.length());
		} else {
			name = fullname + "_" + String.valueOf(index);
		}
		return new File(path + name);
	}

	private String createDelimitedLine(
			int row,
			String delim,
			String enclosure,
			boolean setComma4Point) {
		final StringBuilder sb = new StringBuilder();
		for (int c = 0; c < getColumnCount(); c++) {
			if (enclosure != null) {
				sb.append(enclosure);
			}
			if (getColumnBasicType(c) == BasicDataType.DATE.getId()) {
				if (getValueAt(row, c) != null) {
					sb.append(sdf.format((java.util.Date) getValueAt(row, c)));
				}
			} else if (BasicDataType.isNumberType(getColumnBasicType(c))) {
				if (getValueAt(row, c) != null) {
					if (setComma4Point) {
						sb.append(getValueAt(row, c).toString().replace('.', ','));
					} else {
						sb.append(getValueAt(row, c).toString());
					}
				}
			} else {
				if (getValueAt(row, c) != null) {
					sb.append(getValueAt(row, c).toString()
							.replace('\n', ' ')
							.replace('\r', ' '));
				}
			}
			if (enclosure != null) {
				sb.append(enclosure);
			}
			if (c < getColumnCount() - 1) { // noch nicht die letzte Spalte ?
				sb.append(delim);
			}
		} // for (int c=0; c < getColumnCount(); c++)
		return sb.toString();
	}

	private String createDelimitedHeaderLine(String delim, String enclosure) {
		final StringBuilder sb = new StringBuilder();
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

	/**
	 * erzeugt Textzeilen die ein Insert-Statement enthalten für den
	 * Tabellenexport
	 */
	private String createInsertLine(
			int row,
			String insertStart,
			String dateFormatTemplate) {
		final StringBuilder sb = new StringBuilder();
		sb.append(insertStart); // jetzt ist alles fertig bis zu values ( ...
		for (int c = 0; c < getColumnCount(); c++) {
			if (getColumnBasicType(c) == BasicDataType.DATE.getId()) {
				if (getValueAt(row, c) != null) {
					final String s = sdf.format((java.util.Date) getValueAt(row,
							c));
					final int p0 = dateFormatTemplate.indexOf("<");
					if (p0 != -1) {
						final int p1 = dateFormatTemplate.indexOf(">", p0 + 1);
						dateFormatTemplate = dateFormatTemplate.substring(0, p0)
								+ s
								+ dateFormatTemplate.substring(p1 + 1,
										dateFormatTemplate.length());
					}
					// dateFormatTemplate ist nun kein Template mehr
					sb.append(dateFormatTemplate);
				} else {
					sb.append("null");
				}
			} else if (BasicDataType.isStringType(getColumnBasicType(c))) {
				sb.append('\'');
				if (getValueAt(row, c) != null) {
					sb.append(dublicateSingleQuotas(getValueAt(row, c).toString()
							.replace('\n', ' ')
							.replace('\r', ' ')));
				}
				sb.append('\'');
			} else {
				if (getValueAt(row, c) != null) {
					sb.append(getValueAt(row, c).toString()
							.replace('\n', ' ')
							.replace('\r', ' '));
				} else {
					sb.append("null");
				}
			} // if (getColumnBasicType(c) == BASICTYPE_DATE)
			if (c < getColumnCount() - 1) { // noch nicht die letzte Spalte ?
				sb.append(',');
			} else {
				sb.append(");"); // schliessende Klammer
			}
		}
		return sb.toString();
	}

	/**
	 * dubliziert in einem String die einfachen Hochkomma, um diesen String
	 * innerhalb eines SQL-Statements einsetzen zu können
	 */
	private String dublicateSingleQuotas(String text) {
		final StringBuilder sb = new StringBuilder();
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

	public String getSelectedColumnName() {
		final int col = table.getSelectedColumn();
		if (col != -1) {
			return columnNames[col];
		} else {
			return null;
		}
	}

	public boolean findValue(
			String value,
			final boolean caseSensitive,
			boolean inSelectedColumn) {
		lastSearchPosRow = table.getSelectedRow() - 1; // wird in findNextValue
		// incrementiert !
		if (lastSearchPosRow == -2) {
			lastSearchPosRow = -1;
		}
		lastSearchPosCol = table.getSelectedColumn();
		if (lastSearchPosCol == -2) {
			lastSearchPosCol = -1;
		}
		return findNextValue(value, caseSensitive, inSelectedColumn);
	}

	public boolean findNextValue(
			String value,
			boolean caseSensitive,
			boolean inSelectedColumn) {
		boolean founded = false;
		Object cellObject;
		String cellValue;
		// WildCardSearch nutzen...
		final WildcardSearch comparer = new WildcardSearch();
		// soll nur in aktueller Spalte gesucht werden ?
		if (inSelectedColumn) {
			// nur in aktueller Spalte suchen
			final int col = lastSearchPosCol;
			if (lastSearchPosRow < resultSetResults.size() - 1) {
				lastSearchPosRow++;
			}
			int r = lastSearchPosRow;
			for (; r < resultSetResults.size(); r++) {
				cellObject = getValueAt(r, col);
				if (cellObject != null) {
					if (cellObject instanceof Timestamp) {
						cellValue = sdf.format(cellObject);
					} else {
						cellValue = cellObject.toString();
					}
					if (caseSensitive) {
						if (comparer.patternSearch(value, cellValue)) {
							// gefunden !
							lastSearchPosRow = r;
							lastSearchPosCol = col;
							table.setRowSelectionInterval(r, r);
							founded = true;
							break;
						}
					} else {
						if (comparer.patternSearch(value.toUpperCase(),
								cellValue.toUpperCase())) {
							// gefunden !
							lastSearchPosRow = r;
							lastSearchPosCol = col;
							table.setRowSelectionInterval(r, r);
							founded = true;
							break;
						}
					} // if (caseSensitive)
				} // if (cellObject != null)
			} // for (; r < size(); r++)
		} else { // if (inSelectedColumn)
			// alle Spalten durchsuchen
			if (lastSearchPosCol < getColumnCount() - 1) {
				lastSearchPosCol++;
			} else if (lastSearchPosRow < resultSetResults.size() - 1) {
				lastSearchPosRow++;
			}
			if (lastSearchPosRow < 0) {
				lastSearchPosRow = 0;
			}
			int r = lastSearchPosRow;
			int c = lastSearchPosCol;
			for (; r < resultSetResults.size(); r++) {
				for (; c < getColumnCount(); c++) {
					cellObject = getValueAt(r, c);
					if (cellObject != null) {
						if (cellObject instanceof Timestamp) {
							cellValue = sdf.format(cellObject);
						} else {
							cellValue = cellObject.toString();
						}
						if (caseSensitive) {
							if (comparer.patternSearch(value, cellValue)) {
								// gefunden !
								lastSearchPosRow = r;
								lastSearchPosCol = c;
								table.setRowSelectionInterval(r, r);
								table.setColumnSelectionInterval(c, c);
								founded = true;
								break;
							}
						} else {
							if (comparer.patternSearch(
									value.toUpperCase(),
									cellValue.toUpperCase())) {
								// gefunden !
								lastSearchPosRow = r;
								lastSearchPosCol = c;
								table.setRowSelectionInterval(r, r);
								table.setColumnSelectionInterval(c, c);
								founded = true;
								break;
							}
						} // if (caseSensitive)
					} // if (cellObject != null)
				} // for (; c < getColumnCount(); c++)
				if (c < getColumnCount()) { // innere Schleife wurde abgebrochen
					// dann die äussere auch abbrechen
					break;
				} else {
					c = 0; // Zähler für nächste Zeile neu initialisieren
				}
			} // for (; r < size(); r++)
		} // if (inSelectedColumn)
		return founded;
	}

	public void sortByNewSelect(int columnIndex) {
		final SQLStatement sortStat = new SQLStatement();
		sortStat.setSQL(createSQLWithOrderBy(columnIndex));
		executeStatement(sortStat);
	}

	public String createSQLWithOrderBy(int col) {
		final String columnName = getColumnName(col).toUpperCase();
		final String sql_loc = lastSelectStatement.getSQL();
		// nun nachsehen ob eine order by-clause vorhanden war
		// wenn ja dann die order by clause so verändern, dass die
		// gewünschte Spalte als erstes dort erscheint und sonst nicht weiter
		final String ORDER_BY = " order by ";
		final int p0 = sql_loc.toLowerCase().lastIndexOf(ORDER_BY);
		if (p0 != -1) {
			final StringBuilder sb = new StringBuilder();
			// den teil bis zum order by in den sb speichern
			sb.append(sql_loc.substring(0, p0 + ORDER_BY.length()));
			sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnName));
			// nun die ggf. anderen Spalten in einlesen
			final StringTokenizer st = new StringTokenizer(sql_loc.substring(p0
					+ ORDER_BY.length(), sql_loc.length()), ",", false);
			String token;
			boolean hasColumnRemoved = false;
			while (st.hasMoreTokens()) {
				token = st.nextToken().trim();
				// nun nachsehen ob in diesem Token der zu sortierende
				// Spaltenname steckt,
				// dann diesen token nicht anfügen
				if (!hasColumnRemoved) {
					if (!token.equals(columnName)) {
						if (token.startsWith(columnName)) {
							// dann denn teil mit dem columnName ausklammern un
							// den rest aktzeptieren
							sb.append(',');
							sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(token.substring(
									columnName.length(),
									token.length())));
							hasColumnRemoved = true;
						} else {
							sb.append(',');
							sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(token));
						}
					} else {
						hasColumnRemoved = true;
					}
				} else {
					sb.append(',');
					sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(token));
				}
			}
			return sb.toString();
		} else {
			return sql_loc + " order by " + SQLCodeGenerator.getInstance().getEncapsulatedName(columnName);
		}
	}

	public boolean enabledForSorting(int columnIndex) {
		boolean ok;
		if (getColumnNameByLogicalColIndex(columnIndex).indexOf('(') != -1) {
			ok = false;
		} else {
			final int basicType = getColumnBasicType(columnIndex);
			if (basicType == BasicDataType.CHARACTER.getId()) {
				ok = true;
			} else if (basicType == BasicDataType.DATE.getId()) {
				ok = true;
			} else if (BasicDataType.isNumberType(basicType)) {
				ok = true;
			} else {
				ok = false;
			}
		}
		return ok;
	}

	public void sortVector(int columnIndex) {
		bubbleSort(columnIndex);
	}

	/**
	 * sortiert den Vector nach der Spalte (Index in resultRow-Array)
	 * 
	 * @param columnIndex -
	 *            Index im Array welches die Zeile aufnimmt
	 */
	private void bubbleSort(int columnIndex) {
		// hier wird das bubble-sort-Verfahren angewendet
		// jedes Element wird mit seinem Nachbarn verglichen und wenn e0 > e1
		// dann wird ausgetauscht
		Object[] row1 = null;
		Object[] row2 = null;
		Object o1 = null; // für die Sortierung
		double n1 = 0;
		java.util.Date d1 = null;
		java.util.Date d2 = null;
		String s1 = null;
		String s2 = null;
		int maxIndex = resultSetResults.size() - 1;
		// der Vegleich geht auf n ?
		// n+1, und das letzte Vectorelement hat kein n+1 !
		int cResult = 1;
		final int basicType = this.getColumnBasicType(columnIndex);
		int i;
		boolean hasChanged = true;
		// Schleife über alle Durchläufe durch den Vector
		// die Verfahrensweise ist abhängig vom Datentyp
		if (BasicDataType.isNumberType(basicType)) {
			// um nicht für jedem Vergleich immer wieder neu die double-Wete
			// bilden zu müssen
			// die Umwandlung von String in double ist sehr zeitaufwendig !!
			// deshalb wir ein array double[] gebildet um die Umwandlung nur ein
			// einziges mal zu machen
			double[] numbers = new double[resultSetResults.size()];
			for (i = 0; i < numbers.length; i++) {
				row1 = resultSetResults.get(i);
				o1 = row1[columnIndex];
				if (o1 != null) {
					if (o1 instanceof Number) {
						numbers[i] = ((Number) o1).doubleValue();
					} else if (o1 instanceof String) {
						numbers[i] = Double.parseDouble((String) o1);
					}
				} else {
					numbers[i] = 0;
				}
			}
			while (hasChanged) {
				// einzelner Durchlauf durch den Vector mit
				// Vertauschungsaktionen
				hasChanged = false;
				for (i = 0; i < maxIndex; i++) {
					if (i == 0) {
						row1 = resultSetResults.get(i);
						row2 = resultSetResults.get(i + 1);
					} else {
						if (cResult <= 0) {
							row1 = row2;
						}
						row2 = resultSetResults.get(i + 1);
					}
					cResult = ((numbers[i] < numbers[i + 1]) ? -1 : ((numbers[i] > numbers[i + 1]) ? 1 : 0));
					if (cResult > 0) { // Wert in row1 ist grösser als in rwo2
						// -> Vertauschen !!
						resultSetResults.set(i, row2);
						resultSetResults.set(i + 1, row1);
						n1 = numbers[i];
						numbers[i] = numbers[i + 1];
						numbers[i + 1] = n1;
						hasChanged = true;
					}
				}
				maxIndex--; // an der letzten Stelle steht nun das
				// höchstwertigste Element
			}
		} else if (BasicDataType.isStringType(basicType)) {
			while (hasChanged) {
				// einzelner Durchlauf durch den Vector mit
				// Vertauschungsaktionen
				hasChanged = false;
				for (i = 0; i < maxIndex; i++) {
					if (i == 0) {
						row1 = resultSetResults.get(i);
						row2 = resultSetResults.get(i + 1);
						s1 = (String) row1[columnIndex];
						s2 = (String) row2[columnIndex];
					} else {
						if (cResult <= 0) {
							row1 = row2;
							s1 = s2;
						}
						row2 = resultSetResults.get(i + 1);
						s2 = (String) row2[columnIndex];
					}
					if (s1 == null) {
						cResult = -1;
					} else if (s2 == null) {
						cResult = 1;
					} else {
						cResult = s1.compareToIgnoreCase(s2);
					}
					if (cResult > 0) { // Wert in row1 ist grösser als in rwo2
						// -> Vertauschen !!
						resultSetResults.set(i, row2);
						resultSetResults.set(i + 1, row1);
						hasChanged = true;
					}
				}
				maxIndex--; // an der letzten Stelle steht nun das
				// höchstwertigste Element
			}
		} else if (basicType == BasicDataType.DATE.getId()) {
			while (hasChanged) {
				// einzelner Durchlauf durch den Vector mit
				// Vertauschungsaktionen
				hasChanged = false;
				for (i = 0; i < maxIndex; i++) {
					if (i == 0) {
						row1 = resultSetResults.get(i);
						row2 = resultSetResults.get(i + 1);
						d1 = (java.util.Date) row1[columnIndex];
						d2 = (java.util.Date) row2[columnIndex];
					} else {
						if (cResult <= 0) {
							row1 = row2;
							d1 = d2;
						}
						row2 = resultSetResults.get(i + 1);
						d2 = (java.util.Date) row2[columnIndex];
					}
					if (d1 == null) {
						cResult = -1;
					} else if (d2 == null) {
						cResult = 1;
					} else {
						cResult = d1.compareTo(d2);
					}
					if (cResult > 0) { // Wert in row1 ist grösser als in rwo2
						// -> Vertauschen !!
						resultSetResults.set(i, row2);
						resultSetResults.set(i + 1, row1);
						hasChanged = true;
					}
				}
				maxIndex--; // an der letzten Stelle steht nun das
				// höchstwertigste Element
			}
		}
		fireTableRowsUpdated(0, resultSetResults.size() - 1);
	}

	// ------------- Methoden von TableModel
	// ----------------------------------------------

	/** List of listeners */
	protected EventListenerList tabelModelListeners = new EventListenerList();

	@Override
	public int getRowCount() {
		if (showsResultSet) {
			// aus den Metadaten bilden
			if (verticalView) {
				if (resultSetResults.size() > 0) {
					// Spalten * Zeilen
					if (columnNames != null) {
						return resultSetResults.size() * columnNames.length;
					} else {
						return 0;
					}
				} else {
					if (columnNames != null) {
						return columnNames.length; // für leeren Datensatz
					} else {
						return 0;
					}
				}
			} else {
				return resultSetResults.size();
			}
		} else {
			if (outputParameters != null) {
				return outputParameters.size();
			} else {
				return 0;
			}
		}
	}

	@Override
	public int getColumnCount() {
		if (showsResultSet) {
			if (verticalView) {
				return 2; // Spalte + Wert
			} else {
				if (columnNames == null) {
					return 0;
				} else {
					return columnNames.length;
				}
			}
		} else {
			return 3;
		}
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (showsResultSet) {
			if (verticalView) {
				switch (columnIndex) {
				case 0:
					return "column";
				case 1:
					return "value";
				default:
					logger.error("getColumnName: invalid column index="
							+ columnIndex);
					return null;
				}
			} else {
				return columnNames[columnIndex];
			}
		} else {
			switch (columnIndex) {
			case 0:
				return "index";
			case 1:
				return "name";
			case 2:
				return "value";
			default:
				logger.error("getColumnName: invalid column index="
						+ columnIndex);
				return null;
			}
		}
	}

	public String getColumnNameByLogicalColIndex(int logicalColumnIndex) {
		if (columnNames != null && columnNames.length > logicalColumnIndex) {
			return columnNames[logicalColumnIndex];
		} else {
			return null;
		}
	}

	/**
	 * gibt die class der Spalte basierend auf den ResultSetMeta-Daten
	 * 
	 * @return class der Spalte
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (showsResultSet) {
			if (verticalView) {
				// die erste Spalte ist ertsmla vom Typ String damit wir den
				// Spaltennamen rausschreiben
				if (columnIndex == 0) {
					return String.class;
				} else {
					return Object.class;
				}
			} else {
				return columnClasses[columnIndex];
			}
		} else {
			switch (columnIndex) {
			case 0:
				return Integer.class;
			case 1:
				return String.class;
			case 2:
				return Object.class;
			default:
				return Object.class;
			}
		}
	}

	@Override
	public Object getValueAt(int rowIndex,
			int columnIndex) {
		if (rowIndex == -1 || columnIndex == -1) {
			return null;
		}
		if (showsResultSet) {
			if (verticalView) {
				if (columnIndex == 0) {
					if (resultSetResults.size() == 0) {
						// leere Zeile
						return columnNames[rowIndex];
					} else {
						// den viruellen Spaltenindex ermitteln
						// die eigentlichen Tabellenspalten wiederholen sich nun
						// mit jedem Datensatz
						int verticalColumnIndex = rowIndex % columnNames.length;
						return columnNames[verticalColumnIndex];
					}
				} else {
					if (resultSetResults.size() == 0) {
						return null;
					} else {
						// jede Zeile stellt ein Feld dar
						int logicalColumnIndex = rowIndex % columnNames.length;
						int logicalRowIndex = rowIndex / columnNames.length;
						return getValueAtLogicalIndexes(logicalRowIndex, logicalColumnIndex);
					}
				}
			} else {
				return getValueAtLogicalIndexes(rowIndex, columnIndex);
			}
		} else {
			final SQLPSParam param = outputParameters.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return Integer.valueOf(param.getIndex());
			case 1:
				return param.getName();
			case 2:
				return param.getValue();
			default:
				return null;
			}
		}
	}

	Object getValueAtLogicalIndexes(
			int logicalRowIndex,
			int logicalColIndex) {
		if (logicalRowIndex < resultSetResults.size()) {
			Object[] tableRow = resultSetResults.get(logicalRowIndex);
			if (logicalColIndex < tableRow.length) {
				return tableRow[logicalColIndex];
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public void setValueAt(
			Object value,
			int rowIndex,
			int columnIndex) {
		if (showsResultSet) {
			int internalColumnIndex = 0;
			int internalRowIndex = 0;
			if (verticalView) {
				internalColumnIndex = rowIndex % columnNames.length;
				internalRowIndex = rowIndex / columnNames.length;
			} else {
				internalColumnIndex = columnIndex;
				internalRowIndex = rowIndex;
			}
			setValueAtLogicalIndexes(
					internalRowIndex,
					internalColumnIndex,
					value,
					null);
		}
	}

	void setValueAtLogicalIndexes(
			int logicalRow,
			int logicalCol,
			Object value,
			Object constraint) {
		if (value == null) {
		    resultSetResults.get(logicalRow)[logicalCol] = null;
		} else {
			// Datumswerte auch wieder als Datumswerte in den Vector
			// schreiben !!
			if (getColumnBasicType(logicalCol) == BasicDataType.DATE.getId()) {
				// Datumswert erzeugen
				try {
					if (constraint instanceof String) {
						SimpleDateFormat sdf2 = new SimpleDateFormat((String) constraint);
						resultSetResults.get(logicalRow)[logicalCol] = new Timestamp((sdf2.parse(value.toString())).getTime());
					} else {
						java.util.Date date = GenericDateUtil.parseDate(value.toString());
						resultSetResults.get(logicalRow)[logicalCol] = new Timestamp(date.getTime());
					}
				} catch (java.text.ParseException pe) {
					mainFrame.showDBMessageWithoutContinueAction(pe.getMessage()
							+ "\nThe date format can be adjusted in preferences",
							"validate date value");
				}
			} else {
				resultSetResults.get(logicalRow)[logicalCol] = value;
			}
		}
		fireTableRowsUpdated(logicalRow, logicalCol);
	}

	/**
	 * Returns true regardless of parameter values.
	 * 
	 * @param row
	 *            the row whose value is to be queried
	 * @param column
	 *            the column whose value is to be queried
	 * @return true
	 * @see #setValueAt
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	// ----------------- Methoden aus javax.swing.table.AbstractTableModel
	// ---------------

	public String getColumnTypename(int columnIndex) {
		if (verticalView) {
			switch (columnIndex) {
			case 0:
				return "";
			default:
				return "";
			}
		} else {
			return columnTypeNames[columnIndex];
		}
	}

	/**
	 * ordnet die unterschiedlichen Datentypen zu Basic-Typen und gibt für die
	 * ausgewählte Spalte den Basictyp zurück
	 * 
	 * @param index
	 *            der Spalte
	 * @return Basic-Type
	 */
	public int getColumnBasicType(int columnIndex) {
		if (showsResultSet == false
				|| (columnTypeNames[columnIndex]).equals("VARCHAR")
				|| (columnTypeNames[columnIndex]).equals("CHAR")) {
			return BasicDataType.CHARACTER.getId();
		} else {
			return BasicDataType.getBasicTypeByTypes(columnTypesValues[columnIndex]);
		}
	}

	public boolean isPrimaryKey(int columnIndex) {
		// das array kann nach dem Neuladen der tabelle ggf zu klein sein !
		// am Ende des Ladens wird es komplett restauriert!
		if ((columnPkInfo != null) && (columnIndex < columnPkInfo.length)) {
			return (columnPkInfo[columnIndex]).booleanValue();
		} else {
			return false;
		}
	}

	public void setPrimaryKey(int columnIndex,
			boolean isPrimaryKey) {
		if (columnIndex < columnPkInfo.length) {
			columnPkInfo[columnIndex] = Boolean.valueOf(isPrimaryKey);
		}
	}

	public boolean isExportedKey(int columnIndex) {
		// das array kann nach dem Neuladen der tabelle ggf zu klein sein !
		// am Ende des Ladens wird es komplett restauriert!
		if ((columnRefFromInfo != null)
				&& (columnIndex < columnRefFromInfo.length)) {
			return (columnRefFromInfo[columnIndex]).booleanValue();
		} else {
			return false;
		}
	}

	public void setExportedKey(int columnIndex,
			boolean isExportedKey) {
		if (columnIndex < columnRefFromInfo.length) {
			columnRefFromInfo[columnIndex] = Boolean.valueOf(isExportedKey);
		}
	}

	public void setPrimaryKey(String columnName,
			boolean isPrimaryKey) {
		for (int i = 0; i < columnNames.length; i++) {
			if (columnNames[i].equals(columnName)) {
				setPrimaryKey(i, isPrimaryKey);
				break; // weitere Spalten gleichen Namens kann es nicht geben
			}
		}
	}

	public void togglePrimaryKey(int columnIndex) {
		if (isPrimaryKey(columnIndex)) {
			setPrimaryKey(columnIndex, false);
		} else {
			setPrimaryKey(columnIndex, true);
		}
	}

	/** List of listeners */
	protected EventListenerList listenerList = new EventListenerList();

	/**
	 * Add a listener to the list that's notified each time a change to the data
	 * model occurs.
	 * 
	 * @param l
	 *            the TableModelListener
	 */
	@Override
	public void addTableModelListener(TableModelListener l) {
		listenerList.add(TableModelListener.class, l);
	}

	/**
	 * Remove a listener from the list that's notified each time a change to the
	 * data model occurs.
	 * 
	 * @param l
	 *            the TableModelListener
	 */
	@Override
	public void removeTableModelListener(TableModelListener l) {
		listenerList.remove(TableModelListener.class, l);
	}

	/**
	 * Forward the given notification event to all TableModelListeners that
	 * registered themselves as listeners for this table model. Notify that
	 * TableModelEvent concerning only changes of rows ! If the table-structure
	 * has been changed you must fire TableColumnModelEvents !
	 * 
	 * @see #addTableModelListener
	 * @see TableModelEvent
	 * @see EventListenerList
	 */
	public void fireTableChanged(final TableModelEvent e) {
		if (SwingUtilities.isEventDispatchThread()) {
			final Object[] listeners = listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == TableModelListener.class) {
					((TableModelListener) listeners[i + 1]).tableChanged(e);
				}
			}
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					final Object[] listeners = listenerList.getListenerList();
					// Process the listeners last to first, notifying
					// those that are interested in this event
					for (int i = listeners.length - 2; i >= 0; i -= 2) {
						if (listeners[i] == TableModelListener.class) {
							((TableModelListener) listeners[i + 1]).tableChanged(e);
						}
					}
				}
			});
		}
	}

	/**
	 * Notify all listeners that rows in the (inclusive) range [<I>firstRow</I>,
	 * <I>lastRow</I>] have been inserted.
	 * 
	 * @see TableModelEvent
	 * @see EventListenerList
	 */
	public void fireTableRowsInserted(int firstRow,
			int lastRow) {
		fireTableChanged(new TableModelEvent(this,
				firstRow,
				lastRow,
				TableModelEvent.ALL_COLUMNS,
				TableModelEvent.INSERT));
	}

	/**
	 * Notify all listeners that rows in the (inclusive) range [<I>firstRow</I>,
	 * <I>lastRow</I>] have been updated.
	 * 
	 * @see TableModelEvent
	 * @see EventListenerList
	 */
	public void fireTableRowsUpdated(int firstRow,
			int lastRow) {
		fireTableChanged(new TableModelEvent(this,
				firstRow,
				lastRow,
				TableModelEvent.ALL_COLUMNS,
				TableModelEvent.UPDATE));
	}

	public void fireTableStructureChanged() {
		fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
	}

	/**
	 * Notify all listeners that rows in the (inclusive) range [<I>firstRow</I>,
	 * <I>lastRow</I>] have been deleted.
	 * 
	 * @see TableModelEvent
	 * @see EventListenerList
	 */
	public void fireTableRowsDeleted(int firstRow, int lastRow) {
		// thats a workaround !
		boolean hack = false;
		if ((lastRow > 10) && (lastRow == getRowCount())) { // lastRow ==
			// getRowCount() ->
			// last rows is
			// deleted yet
			// dieser Trick arbeitet nur wenn lastRow nicht die letzte Zeile in
			// der Tabelle war !!
			// Problem: if the last row will be deleted than JTable read ALL
			// values from whole tablemodel
			// in these model i have 100.000 rows an now hang my application
			// solution: deceive that the row is not the last row !
			firstRow--;
			lastRow--;
			hack = true;
		}
		fireTableChanged(new TableModelEvent(this,
				firstRow,
				lastRow,
				TableModelEvent.ALL_COLUMNS,
				TableModelEvent.DELETE));
		if (hack) {
			// its necessary to clear the selection !
			table.clearSelection();
		}
	}

	public synchronized void removeAllElements() {
		if (verticalView == false) {
			removeAllColumns();
		}
		resultSetResults.clear();
	}

	// --------- Methods to change TableColumnModel-properties
	// ----------------------------

	private void removeAllColumns() {
		// das löschen einzelner Spalten aus einen festen TableColumnModel hat
		// nicht funktioniert.
		// dabei wurde immer eine Spalte zu wenig gelöscht :-(
		// daher nun bei jeder neuen Query ein neues TableColumnModel erzeugen
		tableColumnModel = new DefaultTableColumnModel();
		table.setColumnModel(tableColumnModel);
	}

	private final ResultTableHeaderRenderer titleRenderer = new ResultTableHeaderRenderer();

	/**
	 * fügt in die Ergebnistabelle Spalten hinzu
	 * 
	 * @param count
	 *            Anzahl der Spalten
	 */
	private void addColumns(int count) {
		if (verticalView == false) {
			setupColumnWidthAdditive(count);
			for (int i = 0; i < count; i++) {
				final TableColumn column = new TableColumn(i);
				column.setHeaderValue(getColumnName(i));
				final int width = Integer.parseInt(columnsWidth.getProperty(
						getColumnName(i),
						String.valueOf(defaultPreferedColumnWidth)));
                if (width > 20) {
    				column.setPreferredWidth(width + columnWidthAdditive);
                }
				column.setHeaderRenderer(titleRenderer);
				column.addPropertyChangeListener(cpcl);
				tableColumnModel.addColumn(column);
			}
		}
	}

	private int columnWidthAdditive = 0;
	
	private void setupColumnWidthAdditive(int count) {
		int width = 0;
		for (int i = 0; i < count; i++) {
			width = width + Integer.parseInt(columnsWidth.getProperty(
					getColumnName(i),
					String.valueOf(defaultPreferedColumnWidth)));
		}
		columnWidthAdditive = preferredColumnWidthSum - width;
		if (columnWidthAdditive < 0) {
			columnWidthAdditive = 0;
		}
		columnWidthAdditive = columnWidthAdditive / count;
		if (logger.isDebugEnabled()) {
			logger.debug("setupColumnWidthAdditive count=" + count + " preferredColumnWidthSum=" + preferredColumnWidthSum + " set columnWidthAdditive=" + columnWidthAdditive);
		}
	}
	
	private boolean disablePropertyChangeListener = false;

	public void setColumnWidthForAllColumns(int newWidth) {
        if (newWidth > 20) {
            defaultPreferedColumnWidth = newWidth;
            disablePropertyChangeListener = true;
            for (int i = 0; i < tableColumnModel.getColumnCount(); i++) {
                tableColumnModel.getColumn(i).setPreferredWidth(newWidth);
            }
            table.repaint();
            disablePropertyChangeListener = false;
        }
	}
	
	private void prepareColumns() {
		disablePropertyChangeListener = true;
		TableColumn column = null;
		int width = 80;
		for (int i = 0; i < tableColumnModel.getColumnCount(); i++) {
			column = tableColumnModel.getColumn(i);
			width = Integer.parseInt(columnsWidth.getProperty(
					getColumnName(i),
					String.valueOf(defaultPreferedColumnWidth)));
            if (width > 20) {
    			column.setPreferredWidth(width);
            }
			column.setHeaderRenderer(titleRenderer);
			column.removePropertyChangeListener(cpcl);
			column.addPropertyChangeListener(cpcl);
		}
		table.repaint();
		disablePropertyChangeListener = false;
	}

	public int getDefaultColumnWidth() {
		return defaultPreferedColumnWidth;
	}

	private final class ColumnPropertyChangeListener implements	PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (!disablePropertyChangeListener) {
				final TableColumn column = (TableColumn) evt.getSource();
				if (column.getWidth() != defaultPreferedColumnWidth) {
					columnsWidth.setProperty(getColumnName(
							column.getModelIndex()),
							String.valueOf((column.getWidth() > 20 ? column.getWidth() : 20) - columnWidthAdditive));
				}
			} else {
				final TableColumn column = (TableColumn) evt.getSource();
				if (column.getWidth() != defaultPreferedColumnWidth) {
					columnsWidth.remove(getColumnName(column.getModelIndex()));
				}
			}
		}

	}
	
	private void configColumnClassInfo() {
		try {
			final int cols = rsmd.getColumnCount();
			columnClasses = new Class[cols];
			columnSourceClasses = new String[cols];
			columnPkInfo = new Boolean[cols];
			columnTypeNames = new String[cols];
			columnTypesValues = new int[cols];
			columnPrecision = new Integer[cols];
			columnScale = new Integer[cols];
			int type;
			for (int i = 0; i < cols; i++) {
				columnPkInfo[i] = Boolean.valueOf(false); // Initialisierung
				columnPrecision[i] = rsmd.getPrecision(i + 1);
				columnScale[i] = rsmd.getScale(i + 1);
				// des Arrays mit
				// Defaultwerten
				type = rsmd.getColumnType(i + 1);
				columnTypeNames[i] = rsmd.getColumnTypeName(i + 1);
				columnSourceClasses[i] = rsmd.getColumnClassName(i + 1);
				columnTypesValues[i] = type;
				switch (type) {
				case Types.DATE: {
					columnClasses[i] = java.sql.Date.class;
					break;
				}
				case Types.TIMESTAMP: {
					columnClasses[i] = java.sql.Timestamp.class; // so wird
					// korrekt
					// angezeigt
					break;
				}
				case Types.TIME: {
					columnClasses[i] = Time.class;
					break;
				}
				case Types.NUMERIC: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.INTEGER: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.DOUBLE: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.FLOAT: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.SMALLINT: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.DECIMAL: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.REAL: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.BIGINT: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.TINYINT: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.CHAR: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.VARCHAR: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.CLOB: {
					columnClasses[i] = String.class;
					break;
				}
				case Types.BLOB: {
					columnClasses[i] = Object.class; // noch kein Renderer
					// vorhanden
					break;
				}
				case Types.LONGVARCHAR: {
					columnClasses[i] = String.class; // noch kein Renderer
					// vorhanden
					break;
				}
				case Types.BINARY: {
					columnClasses[i] = String.class; // noch kein Renderer
					// vorhanden
					break;
				}
				case Types.BIT:
				case Types.BOOLEAN: {
					columnClasses[i] = Boolean.class; // noch kein Renderer
					// vorhanden
					break;
				}
				case Types.VARBINARY: {
					columnClasses[i] = Object.class; // noch kein Renderer
					// vorhanden
					break;
				}
				case Types.LONGVARBINARY: {
					columnClasses[i] = Object.class; // noch kein Renderer
					// vorhanden
					break;
				}
				case SQLField.ORACLE_ROWID: {
					columnClasses[i] = String.class;
					break;
				}
				default: {
					columnClasses[i] = Object.class;
				}
				} // switch (type)
			} // for (int i = 0; i < cols; i++)
		} catch (Exception sqle) {
			logger.error(sqle.getMessage(), sqle);
		}
	}

	private void configColumnNameInfo() {
		// aus den Metadaten bilden
		try {
			if (rsmd != null) {
				final int cols = rsmd.getColumnCount();
				columnNames = new String[cols];
				for (int i = 0; i < cols; i++) {
					columnNames[i] = rsmd.getColumnName(i + 1);
				}
			} // if (rsmd != null)
		} catch (SQLException sqle) {
			logger.error(sqle.getMessage(), sqle);
		}
	}

	public int size() {
		return resultSetResults.size();
	}

	/*
	 * konfiguriert das Array in dem alle primary-key-felder enthalten sind.
	 * diese Methode darf nicht innerhalb des Durchlaufes durch ein anderes
	 * ResultSet genutzt werden.
	 */
	private void configColumnPkInfo() {
		if (logger.isDebugEnabled()) {
			logger.debug("configColumnPkInfo");
		}
        String schemaName = null;
        String tableName = null;
        final int pos = lastTable.indexOf('.');
        if (pos != -1) {
            schemaName = lastTable.substring(0, pos);
            tableName = lastTable.substring(pos + 1);
        } else {
            schemaName = databaseExtension.getLoginSchema(session.getConnectionDescription());
            tableName = lastTable;
        }
        if (dataModel != null) {
            if (dataModel.isCatalogsLoaded() == false) {
                dataModel.loadCatalogs();
            }
            SQLTable sqlTable = dataModel.getSQLTable(schemaName, tableName);
            if (sqlTable != null) {
                if (sqlTable.isFieldsLoaded() == false) {
                    sqlTable.loadColumns();
                }
                for (int f = 0; f < sqlTable.getFieldCount(); f++) {
                    SQLField sqlField = sqlTable.getFieldAt(f);
                    if (sqlField.isPrimaryKey()) {
                        setPrimaryKey(sqlField.getName(), true);
                    }
                }
            } else {
                logger.warn("configColumnPkInfo failed: table=" + schemaName + "." + tableName + " not found in metadata");
            }
        }
	}

	private void configColumnRefFromInfo() {
		final DatabaseSession localSession = DatabaseSessionPool.getDatabaseSession(session.getAliasName());
		if (localSession != null) {
			columnRefFromInfo = new Boolean[columnNames.length];
			mapRefFromColumns = new HashMap<String, StringBuilder>();
			try {
				DatabaseMetaData localDbmd = localSession.getConnection().getMetaData();
				if (localDbmd != null) {
					String schemaName = null;
					String tableName = null;
					// testen ob last table das Schema enthält
					final int pos = lastTable.indexOf('.');
					if (pos != -1) {
						schemaName = lastTable.substring(0, pos);
						tableName = lastTable.substring(pos + 1);
					} else {
			            schemaName = databaseExtension.getLoginSchema(session.getConnectionDescription());
						tableName = lastTable;
					}
					if (localDbmd.getDatabaseProductName()
							.toLowerCase()
							.indexOf("oracle") != -1) {
						schemaName = schemaName.toUpperCase();
						tableName = tableName.toUpperCase();
					} else if (localDbmd.getDatabaseProductName()
							.toLowerCase()
							.indexOf("informix") != -1) {
						schemaName = schemaName.toLowerCase();
						tableName = tableName.toLowerCase();
					}
					final ResultSet rsc = localDbmd.getExportedKeys(null,
							schemaName,
							tableName);
					if (rsc != null) {
						Object object;
						StringBuilder sb;
						String column;
						String fTable;
						String fColumn;
						String fSchema;
						while (rsc.next()) {
							// im Vector das Feld suchen und dieses als FK
							// kennzeichnen
							column = rsc.getString("PKCOLUMN_NAME");
							fSchema = rsc.getString("FKTABLE_SCHEM");
							fTable = rsc.getString("FKTABLE_NAME");
							fColumn = rsc.getString("FKCOLUMN_NAME");
							if (logger.isDebugEnabled()) {
								logger.debug(column + ": " + fTable + "." + fColumn);
							}
							// die FKTABLE und KKCOLUMN in einer HashMap zur
							// PKCOLUMN merken
							object = mapRefFromColumns.get(column);
							if (object == null) {
								sb = new StringBuilder();
								mapRefFromColumns.put(column, sb);
							} else {
								sb = (StringBuilder) object;
							}
	                        if (fSchema != null && fSchema.length() > 0) {
	    						sb.append(fSchema);
	        					sb.append('.');
	                        }
							sb.append(fTable);
							sb.append('.');
							sb.append(fColumn);
							sb.append('|'); // Trennzeichen
							// merken, dass es dort was gab in einem Array aus
							// boolean
							for (int i = 0; i < columnNames.length; i++) {
								if (columnNames[i].equalsIgnoreCase(column)) {
									columnRefFromInfo[i] = Boolean.valueOf(true);
									break;
								}
							} // for (int i=0; i < fields.size(); i++)
						} // while (rsc.next())
						rsc.close();
					}
				}
			} catch (SQLException sqle) {
				logger.error("configColumnRefFromInfo: read foreign keys from metadata:" + sqle.getMessage(), sqle);
				localSession.error(sqle.getMessage(), sqle);
			} finally {
				DatabaseSessionPool.release(localSession);
			}
		}
	}

	public boolean isReferencedColumn(int columnIndex) {
		final Boolean b = columnRefFromInfo[columnIndex];
		if (b != null) {
			return b.booleanValue();
		} else {
			return false;
		}
	}

	public String[] getReferencingColumns(int columnIndex) {
		String[] list = null;
		if (columnIndex != -1) {
			final String column = getColumnName(columnIndex);
			final StringBuilder sb = mapRefFromColumns.get(column);
			if (sb != null) {
				final StringTokenizer st = new StringTokenizer(sb.toString(),
						"|",
						false);
				list = new String[st.countTokens()];
				int i = 0;
				while (st.hasMoreTokens()) {
					list[i] = st.nextToken();
					i++;
				}
			}
		}
		return list;
	}

	private void configColumnRefToInfo() {
		final DatabaseSession localSession = DatabaseSessionPool.getDatabaseSession(session.getConnectionDescription().getUniqueId());
		if (localSession != null) {
			columnRefToInfo = new String[columnNames.length][2];
			try {
				DatabaseMetaData localDbmd = localSession.getConnection().getMetaData();
				if (localDbmd != null) {
					String schemaName = null;
					String tableName = null;
					// testen ob last table das Schema enthält
					final int pos = lastTable.indexOf('.');
					if (pos != -1) {
						schemaName = lastTable.substring(0, pos);
						tableName = lastTable.substring(pos + 1);
					} else {
			            schemaName = databaseExtension.getLoginSchema(session.getConnectionDescription());
						tableName = lastTable;
					}
					final ResultSet rsc = localDbmd.getImportedKeys(
							null,
							schemaName.toUpperCase(),
							tableName.toUpperCase());
					if (rsc != null) {
						String column;
						String fSchema;
						String fTable;
						String fColumn;
						while (rsc.next()) {
							// im Vector das Feld suchen und dieses als PK
							// kennzeichnen
							column = rsc.getString("FKCOLUMN_NAME");
							fSchema = rsc.getString("PKTABLE_SCHEM");
							fTable = rsc.getString("PKTABLE_NAME");
							fColumn = rsc.getString("PKCOLUMN_NAME");
							for (int i = 0; i < columnNames.length; i++) {
								if (columnNames[i].equalsIgnoreCase(column)) {
	                                if (fSchema != null && fSchema.length() > 0) {
	    								columnRefToInfo[i][0] = fSchema + "." + fTable;
	                                } else {
	                                    columnRefToInfo[i][0] = fTable;
	                                }
									columnRefToInfo[i][1] = fColumn;
									break;
								}
							} // for (int i=0; i < fields.size(); i++)
						} // while (rsc.next())
						rsc.close();
					}
				}
			} catch (SQLException sqle) {
				logger.error("configColumnRefToInfo: read foreign keys from metadata: " + sqle.getMessage(), sqle);
				localSession.error(sqle.getMessage(), sqle);
			} finally {
				DatabaseSessionPool.release(localSession);
			}
		}
	}

	public boolean isReferencingColumn(int columnIndex) {
		return (columnRefToInfo[columnIndex][0] != null);
	}

	public String createSelectForReferencingTable(String tableColumn,
			int row,
			int col) {
		final int pos = tableColumn.lastIndexOf(".");
		final String table_loc = tableColumn.substring(0, pos);
		final String column = tableColumn.substring(pos + 1,
				tableColumn.length());
		final StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(table_loc));
		sb.append(" where ");
		sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(column));
		if (BasicDataType.isNumberType(getColumnBasicType(col))) {
			sb.append('=');
			sb.append(getValueAt(row, col));
		} else {
			sb.append("='");
			sb.append(getValueAt(row, col));
			sb.append('\'');
		}
		return sb.toString();
	}

	public String getReferencedColumn(int columnIndex) {
		return columnRefToInfo[columnIndex][0]
				+ "."
				+ columnRefToInfo[columnIndex][1];
	}

	public String createSelectForReferencedTable(int row,
			int col) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select * from ");
		sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnRefToInfo[col][0]));
		sb.append(" where ");
		sb.append(SQLCodeGenerator.getInstance().getEncapsulatedName(columnRefToInfo[col][1]));
		if (BasicDataType.isNumberType(getColumnBasicType(col))) {
			sb.append('=');
			sb.append(getValueAt(row, col));
		} else {
			sb.append("='");
			sb.append(getValueAt(row, col));
			sb.append('\'');
		}
		return sb.toString();
	}

	private boolean cancelStatementEnabled = true;
	private Statement currentLocalStatement = null;
	private boolean cancelled = false;
	private int answer = DBMessageDialog.CONTINUE;

	public void cancel() {
        cancelled = true;
        answer = DBMessageDialog.INGORE_ERRORS; // avoid more error messages
		if (cancelStatementEnabled) {
			if (currentLocalStatement != null) {
				try {
                    final Statement toCancelStatement = currentLocalStatement;
                    currentLocalStatement = null;
					toCancelStatement.cancel();
				} catch (Exception e) {
					logger.warn("cancel currentLocalStatement failed: " + e.getMessage(), e);
				}
			}
			session.cancelStatement();
		}
	}

	private class SQLExecuter {
		
		private SQLParser parser;
		private boolean refreshNow = false;
		private boolean isScript = false;
		private boolean prevHistoryFreezState = false;
		
		private void execute(boolean noMetaData) {
			answer = 0;
			cancelled = false;
			if (parser != null) {
				isScript = parser.getStatementCount() > 1;
				prevHistoryFreezState = MainFrame.sqlHistory.isFreesed();
				if (parser.getStatementCount() > 1000) {
					MainFrame.sqlHistory.setFreezed(true);
				}
				runScript(parser, noMetaData);
				if (isScript) {
					mainFrame.setDatabaseBusyFiltered(false, parser.getStatementCount()	+ " statements proceeded.");
					MainFrame.sqlHistory.setFreezed(prevHistoryFreezState);
				}
			} else {
				throw new IllegalStateException("parser cannot be null");
			}
		}

		private void runScript(final SQLParser parser, boolean noMetaData) {
			if (logger.isDebugEnabled()) {
				logger.debug("runScript noMetaData=" + noMetaData);
			}
			try {
				long lastUpdatedAt = 0;
				SQLStatement currStatement = null;
				if (parser.getStatementCount() == 0) {
					logger.warn("Parser has no statements");
				}
				for (int i = 0; i < parser.getStatementCount(); i++) {
					if (cancelled) {
						break;
					}
					currStatement = parser.getStatementAt(i);
					final SQLStatement sqlStat = currStatement;
					if (noMetaData == false && (System.currentTimeMillis() - lastUpdatedAt) > 1000) {
						lastUpdatedAt = System.currentTimeMillis();
			            SwingUtilities.invokeLater(new Runnable() {

			                @Override
							public void run() {
								if (!mainFrame.isTextSelected()
										&& parser.getStatementCount() > 1) {
									mainFrame.setCaretPos(sqlStat.getStartPos());
								}
			                }
			                
			            });
					}
					runStatement(sqlStat, noMetaData);
				} // for (int=0; ....
				if (noMetaData == false) {
					final SQLStatement lastStatement = currStatement;
					SwingUtilities.invokeLater(new Runnable() {

		                @Override
						public void run() {
							if (!mainFrame.isTextSelected()
									&& parser.getStatementCount() > 1) {
								mainFrame.setCaretPos(lastStatement.getStartPos());
							}
		                }
		                
		            });
				}
			} finally {
				mainFrame.setDatabaseBusyFiltered(false, null);
			}
		}

		private void runStatement(final SQLStatement sqlStat, boolean noMetaData) {
			if (logger.isDebugEnabled()) {
				logger.debug("runStatement sqlStat=" + sqlStat);
			}
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					mainFrame.setDatabaseBusyFiltered(true, "exec statement("
							+ String.valueOf(sqlStat.getIndex())
							+ ")...");
				}
				
			});
			currentLocalStatement = null;
			cancelStatementEnabled = true;
			sqlStat.setStartTime();
			sqlStat.setCurrentUrl(session.getUrl());
			sqlStat.setCurrentUser(session.getUser());
			if (noMetaData == false && MainFrame.sqlHistory.isFreesed() == false) {
				if (logger.isDebugEnabled()) {
					logger.debug("sqlHistory add statement");
				}
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						MainFrame.sqlHistory.addSQLStatement(sqlStat, isScript);
					}
					
				});
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("sqlHistory skipped");
				}
			}
			// Unterscheiden was mit dem Statement gemacht werden soll
			if (sqlStat.getType() == SQLStatement.OTHER || sqlStat.getType() == SQLStatement.EXPLAIN) { // alles was
				runOtherStatement(sqlStat, noMetaData);
			} else if (sqlStat.getType() == SQLStatement.UPDATE) {
				runUpdateStatement(sqlStat);
			} else if (sqlStat.getType() == SQLStatement.START) {
				runStartStatement(sqlStat);
			} else { // bleibt ja nur noch die QUERY übrig ;-), nur die
				// letzte sollte die Tabelle beeinflussen
				runQueryStatement(sqlStat, noMetaData);
			} // if (sqlStat.getType() == ...
			session.closeCurrentOpenStatement();
			if (noMetaData == false && MainFrame.sqlHistory.isFreesed() == false) {
	            MainFrame.sqlHistory.refresh();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("runStatement ends.");
			}
		} // fireStatement

		private void runStartStatement(final SQLStatement sqlStat) {
			if (logger.isDebugEnabled()) {
				logger.debug("runStartStatement " + sqlStat);
			}
			final String currentDirectory = mainFrame.getCurrentDirectory();
			String fileName = SQLParser.extractFileName(
				sqlStat.getSQL(),
				currentDirectory);
			if (fileName.toLowerCase().endsWith(".sql") == false) {
				fileName = fileName + ".sql";
			}
			File scriptFile = new File(fileName);
			if (scriptFile.exists()) {
				try {
					SQLParser subParser = new SQLParser(scriptFile, Main.getFileEnoding());
					runScript(subParser, false);
					sqlStat.setSuccessful(true);
					sqlStat.setMessage("file " + fileName + " successfully loaded.");
				} catch (IOException e) {
					sqlStat.setSuccessful(false);
					sqlStat.setMessage("Load file " + fileName + " failed:" + e.getMessage());
				}
			} else {
				sqlStat.setSuccessful(false);
				sqlStat.setMessage("file " + fileName + " does not exists!");
			}
			sqlStat.setExecStopTime();
		}

		private void runOtherStatement(final SQLStatement sqlStat, boolean noMetaData) {
			if (logger.isDebugEnabled()) {
				logger.debug("runOtherStatement " + sqlStat);
			}
			int updateCount = -1;
			if (sqlStat.isPrepared()) {
				try {
					final CallableStatement cs = session.createCallableStatement(sqlStat.getSQL());
					currentLocalStatement = cs;
					try {
						setupParameterValues(cs, sqlStat);
	                    if (cs.execute() && sqlStat.getType() != SQLStatement.EXPLAIN) {
	                        // return true if a resultset is the result
	                        processResultSet(sqlStat, cs.getResultSet(), noMetaData);
	                    } else {
	                        processOutputParameters(cs, sqlStat);
	            			updateCount = cs.getUpdateCount(); 
	                        showOutputParameters(sqlStat);
	                        // Erfolgsmeldung
	                        sqlStat.setExecStopTime();
	                        sqlStat.setSuccessful(true);
	                        String message = "statement("
	                                + String.valueOf(sqlStat.getIndex())
	                                + ") successful."
	                                + (updateCount >= 0 ? " Rows affected: " + updateCount : "");
	                        sqlStat.setMessage(message);
	                        mainFrame.setStatusMessage(message);
	                    }
					} finally {
						cs.close();
					}
				} catch (final Exception sqle) {
					cancelStatementEnabled = false;
					// Fehlermeldung
					sqlStat.setExecStopTime();
					sqlStat.setSuccessful(false);
					sqlStat.setMessage(sqle.getMessage());
			    	mainFrame.setStatusMessage("statement("
							+ String.valueOf(sqlStat.getIndex())
							+ ") failed !");
					if (answer != DBMessageDialog.INGORE_ERRORS) {
						answer = mainFrame.showDBMessage(sqle.getMessage(),
								"execution statement("
										+ String.valueOf(sqlStat.getIndex())
										+ ")");
						if (answer == DBMessageDialog.CANCEL) {
							cancelled = true;
							session.rollback();
						}
					}
				}
			} else {
				if (session.execute(sqlStat.getSQL())) {
					if (session.lastStatementWasAQuery() && sqlStat.getType() != SQLStatement.EXPLAIN) {
						processResultSet(sqlStat, session.getCurrentResultSet(), noMetaData);
					} else {
						updateCount = session.getLastUpdateCount();
						// Erfolgsmeldung
						sqlStat.setExecStopTime();
						sqlStat.setSuccessful(true);
                        String message = "statement("
                                + String.valueOf(sqlStat.getIndex())
                                + ") successful."
                                + (updateCount >= 0 ? " Rows affected: " + updateCount : "");
                        sqlStat.setMessage(message);
						if (isScript == false) {
					    	mainFrame.setStatusMessage(message);
						}
					}
				} else {
					cancelStatementEnabled = false;
					// Fehlermeldung
					sqlStat.setExecStopTime();
					sqlStat.setSuccessful(false);
					sqlStat.setMessage(session.getLastErrorMessage());
			    	mainFrame.setStatusMessage("statement("
							+ String.valueOf(sqlStat.getIndex())
							+ ") failed !");
					if (answer != DBMessageDialog.INGORE_ERRORS) {
						answer = mainFrame.showDBMessage(session.getLastErrorMessage(),
								"execution statement("
										+ String.valueOf(sqlStat.getIndex())
										+ ")");
						if (answer == DBMessageDialog.CANCEL) {
							cancelled = true;
							session.rollback();
						}
					}
				} // if (session.execute(sqlStat.getSQL()))
			}
		}

		private void processOutputParameters(
				final CallableStatement callableStatement,
				final SQLStatement sqlStat) throws SQLException {
			if (sqlStat.hasOutputParams()) {
				SQLPSParam parameter = null;
				List<SQLPSParam> listOutputParams = sqlStat.getOutputParams();
				for (int i = 0; i < listOutputParams.size(); i++) {
					parameter = listOutputParams.get(i);
					if (BasicDataType.isStringType(parameter.getBasicType())) {
						parameter.setValue(callableStatement.getString(parameter.getIndex()));
					} else if (BasicDataType.isNumberType(parameter.getBasicType())) {
						parameter.setValue(callableStatement.getString(parameter.getIndex()));
					} else if (parameter.getBasicType() == BasicDataType.DATE.getId()) {
						parameter.setValue(callableStatement.getString(parameter.getIndex()));
					} else if (parameter.getBasicType() == BasicDataType.BOOLEAN.getId()) {
						parameter.setValue(String.valueOf(callableStatement.getBoolean(parameter.getIndex())));
					} // if ((columnTypeNames[col].equals....
				}
			}
		}

		private void runUpdateStatement(final SQLStatement sqlStat) {
			if (logger.isDebugEnabled()) {
				logger.debug("runUpdateStatement " + sqlStat);
			}
			int updateCount = -1;
			if (sqlStat.isPrepared()) {
				try {
					final CallableStatement ps = session.createCallableStatement(sqlStat.getSQL());
					currentLocalStatement = ps;
					setupParameterValues(ps, sqlStat);
				    updateCount = ps.executeUpdate();
					sqlStat.setExecStopTime();
				    ResultSet rs = ps.getResultSet();
				    if (rs != null) {
				    	processResultSet(sqlStat, rs, true);
				    }
					ps.close();
				} catch (Exception sqle) {
					session.error(sqle.getMessage(), sqle);
				}
			} else {
				updateCount = session.executeUpdate(sqlStat.getSQL());
				cancelStatementEnabled = false;
				sqlStat.setExecStopTime();
			}
			if (updateCount == -1) {
				// Fehlermeldung
				sqlStat.setSuccessful(false);
				sqlStat.setMessage(session.getLastErrorMessage());
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						mainFrame.setStatusMessage("modification("
								+ String.valueOf(sqlStat.getIndex())
								+ ") failed !");
					}
					
				});
				if (answer != DBMessageDialog.INGORE_ERRORS) {
					answer = mainFrame.showDBMessage(session.getLastErrorMessage(),
							"execution modification("
									+ String.valueOf(sqlStat.getIndex())
									+ ')');
					if (answer == DBMessageDialog.CANCEL) {
						cancelled = true;
						session.rollback();
					}
				} // if (answer != DBMessageDialog.INGORE_ERRORS)
			} else {
				// Anzahl geänderter Datensätze ausgeben
				sqlStat.setSuccessful(true);
				sqlStat.setMessage("modification("
						+ String.valueOf(sqlStat.getIndex())
						+ ") proceeded for "
						+ String.valueOf(updateCount)
						+ " datasets");
				if (isScript == false) {
					final int c = updateCount;
			    	mainFrame.setStatusMessage("modification("
							+ String.valueOf(sqlStat.getIndex())
							+ ") proceeded for "
							+ String.valueOf(c)
							+ " datasets");
				}
			} // if (updateCount == -1)
		}

		private void runQueryStatement(final SQLStatement sqlStat, boolean noMetaData) {
			if (logger.isDebugEnabled()) {
				logger.debug("runQueryStatement " + sqlStat);
			}
			maxRowsToRead = 0;
			if (sqlStat.isPrepared()) {
				try {
					final CallableStatement ps = session.createCallableStatement(sqlStat.getSQL());
					currentLocalStatement = ps;
					databaseExtension.setupStatement(ps);
					setupParameterValues(ps, sqlStat);
	                final ResultSet rs = ps.executeQuery();
	                processResultSet(sqlStat, rs, noMetaData);
					ps.close();
				} catch (Exception sqle) {
					// Fehlermeldung ausgeben
			    	mainFrame.setStatusMessage("query("
							+ String.valueOf(sqlStat.getIndex())
							+ ") failed !");
					sqlStat.setMessage(sqle.getMessage());
					sqlStat.setSuccessful(false);
					if (answer != DBMessageDialog.INGORE_ERRORS) {
						answer = mainFrame.showDBMessage(sqle.getMessage(),
								"execution prepared query("
										+ String.valueOf(sqlStat.getIndex())
										+ ")");
						if (answer == DBMessageDialog.CANCEL) {
							cancelled = true;
							session.rollback();
						}
					}
				}
			} else {
				final ResultSet rs = session.executeQuery(sqlStat.getSQL());
				sqlStat.setExecStopTime();
				if (session.isSuccessful()) {
					processResultSet(sqlStat, rs, noMetaData);
				} else { // if (session.isSuccessful())
					// Fehlermeldung ausgeben
					mainFrame.setStatusMessage("query("
							+ String.valueOf(sqlStat.getIndex())
							+ ") failed !");
					sqlStat.setMessage(session.getLastErrorMessage());
					sqlStat.setSuccessful(false);
					if (answer != DBMessageDialog.INGORE_ERRORS) {
						answer = mainFrame.showDBMessage(session.getLastErrorMessage(),
								"execution query("
										+ String.valueOf(sqlStat.getIndex())
										+ ")");
						if (answer == DBMessageDialog.CANCEL) {
							cancelled = true;
							session.rollback();
						}
					}
				} // if (session.isSuccessful())
			}
		}

		private void setRefreshNow(boolean ok) {
			refreshNow = true;
		}

		private void processResultSet(final SQLStatement sqlStat, final ResultSet rs, boolean noMetaData) {
			if (logger.isDebugEnabled()) {
				logger.debug("processResultSet stat=" + sqlStat + " noMetaData=" + noMetaData);
			}
			showsResultSet = true;
			cancelStatementEnabled = false;
			newRowIndex = -1; // nach dem Neulesen kann keine Zeile momentan
			// eingefügt sein
			lastTable = SQLParser.getTableForQuery(sqlStat);
			if (lastTable != null) {
				SwingUtilities.invokeLater(new Runnable() {

				    @Override
					public void run() {
						mainFrame.status.setTablename(lastTable);
						mainFrame.status.setToolTipText(lastTable);
				    }
				    
				});
			}
			int currentColumnIndex = 0;
			if (rs != null) {
	            mainFrame.ensureResultTableIsVisible();
				isAdditionalInfoReady = false;
				final javax.swing.Timer timer = new javax.swing.Timer(refreshTime,
	                new java.awt.event.ActionListener() {
	                    // wenn Timeout erreicht ...
	                    @Override
						public void actionPerformed(ActionEvent e) {
	                        setRefreshNow(true);
	                    }
	                });
				timer.setRepeats(true); // immer wieder
				timer.start(); // Timer starten
				// Abarbeitung starten
				//table.requestFocus();
				removeAllElements();
				columnTypeNames = null; // zurücksetzen, damit im TitleRenderer
				// der Wechsel erkannt werden kann
				mainFrame.setDatabaseBusyFiltered(true, "read data from query("
						+ String.valueOf(sqlStat.getIndex())
						+ ")...");
				int rowCount = 0; // zähler über alles
				int i = -1;
				try {
					rsmd = rs.getMetaData();
					configColumnClassInfo();
					configColumnNameInfo();
					final int cols = rsmd.getColumnCount();
					
					addColumns(cols);
					// die Metadaten für das Resultset sind bekannt also kann
					// die Tabelle über die neue Struktur informiert werden.
					if (cols > 0) {
						int deltaRows = 0; // zähler zwischen zwei
						// Tabellen-Events eingefügte Zeilen
						logger.debug("gather column types");
						int[] types = new int[cols];
						for (int c = 0; c < cols; c++) {
							types[c] = BasicDataType.getBasicTypeByTypes(rsmd.getColumnType(c + 1));
						}
						while (rs.next()) {
							if (cancelled) {
								try {
									if (logger.isDebugEnabled()) {
										logger.debug("cancel current statement...");
									}
									rs.getStatement().close();
								} catch (Exception e) {
									logger.warn("close statement failed: " + e.getMessage(), e);
								}
								mainFrame.setStatusMessage("abort read results from statement("
										+ String.valueOf(sqlStat.getIndex())
										+ ")");
								break; // while-Schleife abbrechen
							}
							Object[] resultRow = new Object[cols];
							deltaRows++;
							rowCount++;
							currentColumnIndex = 0;
							for (i = 0; i < cols; i++) {
								currentColumnIndex = i;
								// type-gerecht einfügen in die
								// Ergebnismenge
								if (types[i] == BasicDataType.DATE.getId()) {
									// hier auf keinen Fall getDate
									// verwenden, da sonst die Uhrzeit
									// verloren geht !!
									try {
										resultRow[i] = rs.getTimestamp(i + 1);
									} catch (Exception fe) {
										resultRow[i] = rs.getString(i + 1);
									}
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
									resultRow[i] = rs.getString(i + 1);
								} else if (types[i] == BasicDataType.BINARY.getId()) {
									Object v = rs.getObject(i + 1);
									if (v instanceof Blob) {
										final Blob blob = (Blob) v;
										if (blob.length() > 0) {
											resultRow[i] = new BinaryDataFile(blob);
										} else {
											resultRow[i] = null;
										}
									} else if (v instanceof byte[]) {
										resultRow[i] = v;
									} else if (v != null) {
										try {
											resultRow[i] = rs.getObject(i + 1);
										} catch (Exception e) {
										}
									} else {
										resultRow[i] = null;
									}
								} else if (types[i] == SQLField.ORACLE_ROWID) {
									resultRow[i] = rs.getString(i + 1);
								} else {
									try {
										resultRow[i] = rs.getObject(i + 1);
									} catch (Exception e) {
									}
								} 
							} // for
							// Zeile zum Model hinzufügen
							resultSetResults.add(resultRow);
							// die Tabelle über neue Zeilen informieren
							if (refreshNow) {
								fireTableRowsInserted(rowCount - deltaRows,
										rowCount);
								Thread.yield(); // den anderen Threads auch
								// Zeit lassen
								deltaRows = 0;
								refreshNow = false;
								final int cRowCount = rowCount;
								mainFrame.setStatusMessage("query("
										+ String.valueOf(sqlStat.getIndex())
										+ "): until now "
										+ String.valueOf(cRowCount)
										+ " datasets read.");
							}
							// Abbruch bei maximaler Anzahl zu lesender
							// Datensätze
							if ((maxRowsToRead != 0)
									&& (rowCount >= maxRowsToRead)) {
								cancelled = true;
							}
							if (rowCount == maxRowsToWarning) {
								// Warnung ausgeben
								while (true) {
									final String answer_loc = mainFrame.showInputDialog(String.valueOf(rowCount)
											+ " datasets read.\nlimit count datasets to read ? (0=no limitation)",
											"read data",
											String.valueOf(rowCount));
									if (answer_loc == null) {
										cancelled = true;
										break;
									} else if (answer_loc.length() > 0) {
										try {
											maxRowsToRead = Integer.parseInt(answer_loc);
											break;
										} catch (NumberFormatException nfe) {
											maxRowsToRead = 1000;
										}
									} else {
										maxRowsToRead = 0;
										break;
									} // if (answer == null)
								} // while (true)
							} // if (rowCount == maxRowsToWarning)
						} // while (rs.next())
						// die Tabelle über neue Zeilen informieren
						if ((deltaRows > 0)
								|| Thread.currentThread().isInterrupted()) {
							fireTableRowsInserted(rowCount - deltaRows,
									rowCount);
						}
					} // if (cols > 0)
					sqlStat.setGetStopTime();
					sqlStat.setSuccessful(true);
					lastSelectStatement = sqlStat;
					// erst hier abfragen, da es sonst Probleme mit dem Auslesen
					// von LONGVARBINARY-Feldern gibt.
					// offensichtlich wird ein Stream für LOB-Felder vorzeitig
					// geschlossen
					// diese Problem kann hier nach dem Durchlaufen des
					// ResultSets nicht mehr wirken.
					if (noMetaData == false) {
						configColumnPkInfo();
						configColumnRefToInfo();
						configColumnRefFromInfo();
					}
					isAdditionalInfoReady = true;
					table.getTableHeader().repaint();
					if (cancelled) {
						sqlStat.setMessage("query("
								+ String.valueOf(sqlStat.getIndex())
								+ "):"
								+ String.valueOf(rowCount)
								+ " datasets read. ABORT");
						final int cRowCount = rowCount;
						mainFrame.setStatusMessage("query("
								+ String.valueOf(sqlStat.getIndex())
								+ "):"
								+ String.valueOf(cRowCount)
								+ " datasets read. ABORT");
					} else {
						sqlStat.setMessage("query("
								+ String.valueOf(sqlStat.getIndex())
								+ "):"
								+ String.valueOf(rowCount)
								+ " datasets read. READY");
						final int cRowCount = rowCount;
						mainFrame.setStatusMessage("query("
								+ String.valueOf(sqlStat.getIndex())
								+ "):"
								+ String.valueOf(cRowCount)
								+ " datasets read. READY");
					}
				} catch (final SQLException sqle) {
					// hier nur Fehler beim Zugriff auf das ResultSet
					logger.error("processResultSet failed:" + sqle.getMessage(), sqle);
					sqlStat.setGetStopTime();
					sqlStat.setSuccessful(false);
					sqlStat.setMessage(sqle.getMessage());
					final int ci = currentColumnIndex;
					final int cRowCount = rowCount; 
					SwingUtilities.invokeLater(new Runnable() {

					    @Override
						public void run() {
							mainFrame.showDBMessageWithoutContinueAction("column="
									+ String.valueOf(ci + 1)
									+ " row="
									+ String.valueOf(cRowCount)
									+ "\n"
									+ sqle.getMessage(), "error in statement("
									+ String.valueOf(sqlStat.getIndex())
									+ ")");
					    }
					    
					});
				} catch (final java.lang.OutOfMemoryError error) {
	    			setRefreshNow(false);
					timer.stop();
	                resultSetResults.clear();
					System.gc();
	                final String message = "query("
	                        + String.valueOf(sqlStat.getIndex())
	                        + "):"
	                        + " out of memory error: " + error.getMessage() + " . ABORT !";
	                mainFrame.setStatusMessage(message);
	                sqlStat.setMessage(message);
					logger.error("out of memory error", error);
				} finally {
					timer.stop();
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("close result set");
						}
						rs.close();
					} catch (Exception e) {
						logger.error("processResultSet close cursor failed: "
								+ e.toString());
					}
				}
			} else { // if (rs != null)
				mainFrame.setStatusMessage("query("
						+ String.valueOf(sqlStat.getIndex())
						+ ") returns none result set !");
				sqlStat.setMessage(session.getLastErrorMessage());
				sqlStat.setSuccessful(false);
				if (answer != DBMessageDialog.INGORE_ERRORS) {
					answer = mainFrame.showDBMessage("No resultset received.\n"
							+ session.getLastErrorMessage(), "execution query("
							+ String.valueOf(sqlStat.getIndex())
							+ ")");
					if (answer == DBMessageDialog.CANCEL) {
						cancelled = true;
					}
				}
			} // if (rs != null)
			setRefreshNow(false);
		}
		
	}
	
	/**
	 * lastSelectStatement wird in processResultSet gefüllt, wenn erfolgreich
	 */
	public SQLStatement getLastSelectStatement() {
		return lastSelectStatement;
	}

	public void executeScript(SQLParser parser, boolean noMetaData) {
		SQLExecuter executer = new SQLExecuter();
		executer.parser = parser;
		executer.execute(noMetaData);
	}

	public void executeStatement(SQLStatement statement) {
		executeScript(new SQLParser(statement.getSQL()), false);
	}



	// Renderer fuer den Tabellenkopf
	@SuppressWarnings("serial")
	private final class ResultTableHeaderRenderer extends JLabel implements TableCellRenderer {

		private String text;
		private String toTable;
		private String toColumn;
		private boolean isReferenced;
		private boolean isPrimaryKey;
		private final transient Color bg = new Color(220, 220, 220);

		public ResultTableHeaderRenderer() {
			super();
			setOpaque(true); // sonst wird der Hintergrund als durchlössig
			// angesehen und Einstellungen zum Hintergrund ignoriert!!
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object cellValue,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {
			setFont(new Font("Dialog", 0, 10));
			setForeground(Color.black);
			setBorder(BorderFactory.createBevelBorder(0));
			setHorizontalAlignment(SwingConstants.CENTER);
			text = (String) cellValue;
			setText(text);
			if (isAdditionalInfoReady && verticalView == false) {
				if (isPrimaryKey(column)) {
					setBackground(Color.yellow);
					isPrimaryKey = true;
				} else {
					setBackground(bg);
					isPrimaryKey = false;
				}
				if (columnRefFromInfo != null && columnRefFromInfo.length > column) {
					if (columnRefFromInfo[column] != null) {
						setText(">" + getText());
						isReferenced = true;
					} else {
						isReferenced = false;
					}
				} else {
					isReferenced = false;
				}
				if (columnRefToInfo != null && columnRefToInfo.length > column) {
					if (columnRefToInfo[column][0] != null) {
						setText("<" + getText());
						toTable = columnRefToInfo[column][0];
						toColumn = columnRefToInfo[column][1];
					} else {
						toTable = "--";
						toColumn = "--";
					}
				} else {
					toTable = "--";
					toColumn = "--";
				}
				if (columnTypeNames != null) {
					setToolTipText("<HTML><BODY><b>index</b>="
							+ String.valueOf(column + 1)
							+ "<br>"
							+ "<b>name</b>="
							+ text
							+ "<br>"
							+ "<b>type name=</b>"
							+ columnTypeNames[column]
							+ " <b>id=</b>" 
							+ columnTypesValues[column]
							+ "<br>"
							+ "<b>Basic type=</b>" 
							+ BasicDataType.getBasicTypeByTypeObjects(columnTypesValues[column])
							+ "<br>"
							+ "<b>Class=</b>" 
							+ columnSourceClasses[column]
							+ "<br>"
							+ "<b>is primary key</b>="
							+ String.valueOf(isPrimaryKey)
							+ "<br>"
							+ "<b>is referenced</b>="
							+ String.valueOf(isReferenced)
							+ "<br>"
							+ "<b>reference to</b> "
							+ toTable
							+ "."
							+ toColumn
							+ "</BODY></HTML>");
				}
			} else {
				setBackground(bg);
			}
			return this;
		}

	} // TitleRenderer

	public void showOutputParameters(SQLStatement sqlStat) {
		if (sqlStat.hasOutputParams()) {
			outputParameters = sqlStat.getOutputParams();
			showsResultSet = false;
			fireTableStructureChanged();
            mainFrame.ensureResultTableIsVisible();
		}
	}

	public boolean isShowingResultSet() {
		return showsResultSet;
	}

	public int convertFromVerticalToLogicalRowIndex(int verticalRowIndex) {
		return verticalRowIndex / columnNames.length;
	}

	public int convertFromVerticalToLogicalColIndex(int verticalRowIndex) {
		return verticalRowIndex % columnNames.length;
	}

	public int convertFromLogicalToVerticalRowSelection(int verticalRowIndex) {
		return verticalRowIndex * columnNames.length;
	}
	
	public double calculateColumnValueSum(int[] rows, int column) {
        double result = 0;
        if (rows.length > 1) {
            for (int row : rows) {
                result = result + getFailSaveNumberValue(row, column);
            }
        }
        return result;
    }
    
	public Statistic getRowStatistics(int[] rows, int column) {
		Statistic stat = null;
		if (rows.length > 1) {
			Object first =  resultSetResults.get(rows[0])[column];
			if (first instanceof Date) {
				stat = new StatisticDate();
				for (int r : rows) {
					stat.addValue(resultSetResults.get(r)[column]);
				}
			} else {
				stat = new StatisticDouble();
				for (int r : rows) {
					stat.addValue(resultSetResults.get(r)[column]);
				}
			}
		}
		return stat;
	}
	
    public double calculateRowValueSum(int row, int[] columns) {
        double result = 0;
        if (columns.length > 1) {
            for (int column : columns) {
                result = result + getFailSaveNumberValue(row, column);
            }
        }
        return result;
    }

    private double getFailSaveNumberValue(int row, int column) {
        double numberValue = 0;
        try {
            Object value = resultSetResults.get(row)[column];
            if (value != null) {
                numberValue = Double.parseDouble(value.toString());
            }
        } catch (Exception e) {
            // intentionally empty
        }
        return numberValue;
    }

	public SQLDataModel getDataModel() {
		return dataModel;
	}

	public int getPreferredColumnWidthSum() {
		return preferredColumnWidthSum;
	}

	public void setPreferredColumnWidthSum(int preferredColumnWidthSum) {
		if (logger.isDebugEnabled()) {
			logger.debug("setPreferredColumnWidthSum:" + preferredColumnWidthSum);
		}
		this.preferredColumnWidthSum = preferredColumnWidthSum;
	}
	
	public SQLTable getSQLTableFromCurrentQuery() throws SQLException {
        if (dataModel != null) {
            String schemaName = null;
            String tableName = null;
            final int pos = lastTable.indexOf('.');
            if (pos != -1) {
                schemaName = lastTable.substring(0, pos);
                tableName = lastTable.substring(pos + 1);
            } else {
                schemaName = databaseExtension.getLoginSchema(session.getConnectionDescription());
                tableName = lastTable;
            }
            if (dataModel.isCatalogsLoaded() == false) {
                dataModel.loadCatalogs();
            }
            SQLSchema schema = dataModel.getSchema(schemaName);
    		SQLTable table = new SQLTable(dataModel, schema, tableName);
    		table.setType(SQLTable.TYPE_TABLE);
    		table.setFieldsLoaded();
    		for (int i = 0; i < columnNames.length; i++) {
    			SQLField field = new SQLField(dataModel, table, columnNames[i]);
    			field.setBasicType(BasicDataType.getBasicTypeByTypes(columnTypesValues[i]));
    			field.setPrimaryKey(columnPkInfo[i]);
    			field.setType(columnTypesValues[i]);
    			field.setTypeName(columnTypeNames[i]);
    			if (rsmd != null) {
    				field.setLength(columnPrecision[i]);
    				if (BasicDataType.isNumberType(field.getBasicType())) {
        				field.setDecimalDigits(columnScale[i]);
//    				} else if (BasicDataType.isStringType(field.getBasicType())) {
//        				field.setLength(rsmd.getPrecision(i + 1));
//    				} else if (BasicDataType.isDateType(field.getBasicType())) {
//        				field.setLength(rsmd.getPrecision(i + 1));
    				}
    			}
    			table.addField(field);
    		}
    		return table;
        } else {
        	return null;
        }
	}

	public String createSQLList(int column, int[] rows) {
		// collect selected values
		List<Object> values = new ArrayList<Object>();
		for (int row : rows) {
			values.add(getValueAtLogicalIndexes(row, column));
		}
		// check which data type column has
		int columnType = BasicDataType.getBasicTypeByTypes(columnTypesValues[column]);
		StringBuilder sql = new StringBuilder();
		if (columnType == BasicDataType.LONG.getId() || columnType == BasicDataType.INTEGER.getId() || columnType == BasicDataType.DOUBLE.getId()) {
			boolean firstLoop = true;
			for (Object value : values) {
				if (firstLoop) {
					firstLoop = false;
					sql.append("(");
				} else {
					sql.append(",\n");
				}
				sql.append(value);
			}
			if (firstLoop == false) {
				sql.append(")");
			}
		} else if (columnType == BasicDataType.DATE.getId()) {
			boolean firstLoop = true;
			for (Object value : values) {
				if (firstLoop) {
					firstLoop = false;
					sql.append("(");
				} else {
					sql.append(",\n");
				}
				sql.append("'");
				sql.append(sdf.format((Date) value));
				sql.append("'");
			}
			if (firstLoop == false) {
				sql.append(")");
			}
		} else if (columnType == BasicDataType.CHARACTER.getId()) {
			boolean firstLoop = true;
			for (Object value : values) {
				if (firstLoop) {
					firstLoop = false;
					sql.append("(");
				} else {
					sql.append(",\n");
				}
				sql.append("'");
				sql.append((String) value);
				sql.append("'");
			}
			if (firstLoop == false) {
				sql.append(")");
			}
		}
		return sql.toString();
	}
	
}
