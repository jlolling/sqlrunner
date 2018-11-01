package sqlrunner.datamodel;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;
import dbtools.DatabaseSessionPool;
import sqlrunner.dbext.DatabaseExtension;
import sqlrunner.dbext.DatabaseExtensionFactory;

public final class SQLDataModel extends SQLObject implements Comparable<SQLDataModel> {

	private static final Logger logger = Logger.getLogger(SQLDataModel.class);
	private ConnectionDescription cd;
	private String errorMessage;
	private final List<SQLCatalog> catalogs = new ArrayList<SQLCatalog>();
	private boolean loadingSchemas = false;
	private boolean loadingCatalogs = false;
	private boolean useLowerCaseIdentifiers = false;
	private boolean useUpperCaseIdentifiers = false;
	private boolean userCaseSensitiveIdentifiers = false;
	public final char delimiter = 0x00;
	private boolean schemasLoaded = false;
	private boolean catalogsLoaded = false;
	private SQLSchema currentSQLSchema;
	private SQLSchema loginSchema;
	private String loginSchemaName;
	private ArrayList<DatamodelListener> listener = new ArrayList<DatamodelListener>();
	private DatabaseExtension databaseExtension;
	private Connection connection;
	
	public void addDatamodelListener(DatamodelListener l) {
		if (listener.contains(l) == false) {
			listener.add(l);
		}
	}
	
	public void removeDatamodelListener(DatamodelListener l) {
		listener.remove(l);
	}
	
	public void removeAllDatamodelListener() {
		listener.clear();
	}
	
	private void fireDatamodelEvent(final String message, final int type) {
		if (listener.isEmpty() == false) {
			if (SwingUtilities.isEventDispatchThread()) {
				doFireDatemodelEvent(message, type);
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						doFireDatemodelEvent(message, type);
					}
				});
			}
		}
	}
	
	private void doFireDatemodelEvent(String message, int type) {
		if (logger.isDebugEnabled()) {
			logger.debug(message);
		}
		DatamodelEvent e = new DatamodelEvent(this, message, type);
		for (DatamodelListener l : listener) {
			l.eventHappend(e);
		}
	}
	
	public boolean isUseLowerCaeIdentifiers() {
		return useLowerCaseIdentifiers;
	}

	public boolean isUseUpperCaseIdentifiers() {
		return useUpperCaseIdentifiers;
	}

	public boolean isUserCaseSensitiveIdentifiers() {
		return userCaseSensitiveIdentifiers;
	}

	public SQLDataModel(ConnectionDescription cd) {
		super(null, cd.toString());
		this.cd = cd.clone();
		this.cd.setAutoCommit(true);
		if (DatabaseSessionPool.getConnectionDescription(cd.getUniqueId()) == null) {
			DatabaseSessionPool.addConnectionDescription(cd.getUniqueId(), cd);
		}
		if (DatabaseSessionPool.isPoolCheckThreadRunning() == false) {
			DatabaseSessionPool.setMaxIdleTime(5);
			DatabaseSessionPool.setCheckPoolCyclusTime(5);
			DatabaseSessionPool.startCheckPoolThread();
		}
		databaseExtension = DatabaseExtensionFactory.getDatabaseExtension(cd);
		loginSchemaName = databaseExtension.getLoginSchema(cd);
		if (logger.isDebugEnabled()) {
			logger.debug("loginSchemaName=" + loginSchemaName);
		}
	}

	public SQLDataModel(Connection connection) {
		super(null, "given-connection");
		this.connection = connection;
		String driverClass = null;
		try {
			driverClass = connection.getMetaData().getDriverName();
		} catch (SQLException e) {
			logger.error("Unable to get database metadata: " + e.getMessage(), e);
		}
		databaseExtension = DatabaseExtensionFactory.getDatabaseExtension(driverClass);
		loginSchemaName = databaseExtension.getLoginSchema(connection);
		if (logger.isDebugEnabled()) {
			logger.debug("loginSchemaName=" + loginSchemaName);
		}
	}
	
	public void refresh() {
		loadCatalogs();
	}
	
	public void reloadSchemasAndTables() {
		fireDatamodelEvent("Preloading metadata...", DatamodelEvent.ACTION_MESSAGE_EVENT);
		loadCatalogs();
		for (SQLCatalog cat : catalogs) {
			loadSchemas(cat);
		}
		getCurrentSQLSchema();
		if (currentSQLSchema != null && currentSQLSchema.isTablesLoaded() == false) {
			loadTables(currentSQLSchema);
		}
		fireDatamodelEvent("Preloading metadata finished", DatamodelEvent.ACTION_MESSAGE_EVENT);
	}

	public boolean isSchemaLoaded() {
		return schemasLoaded;
	}
	
	public boolean isCatalogsLoaded() {
		return catalogsLoaded;
	}

	public DatabaseSession getDatabaseSession() {
		return DatabaseSessionPool.getDatabaseSession(cd.getUniqueId());
	}
	
	public boolean loadCatalogs() {
		if (loadingCatalogs) {
			return false;
		}
		loadingCatalogs = true;
		if (Thread.currentThread().isInterrupted()) {
			loadingSchemas = false;
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("loadCatalogs");
		}
		DatabaseSession session = null;
		Connection conn = connection;
		if (conn == null) {
			session = getDatabaseSession();
			if (session == null) {
				return false;
			}
			session.setLastSQL("loadCatalogs");
			conn = session.getConnection();
		}
		if (conn == null) {
			return false;
		}
		boolean ok = false;
		try {
			fireDatamodelEvent("Loading catalogs", DatamodelEvent.ACTION_MESSAGE_EVENT);
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null) {
				useLowerCaseIdentifiers = dbmd.storesLowerCaseIdentifiers();
				useUpperCaseIdentifiers = dbmd.storesUpperCaseIdentifiers();
				userCaseSensitiveIdentifiers = dbmd.storesMixedCaseIdentifiers();
				catalogsLoaded = false;
				ResultSet rsCatalogs = dbmd.getCatalogs();
				catalogs.clear();
				while (rsCatalogs.next()) {
					String catName = rsCatalogs.getString("TABLE_CAT");
					if (catName != null && catName.isEmpty() == false) {
						// sometimes IBM DB" returns null as name 
	 					addCatalog(new SQLCatalog(this, catName));
					}
				}
				rsCatalogs.close();
				if (catalogs.isEmpty()) {
					catalogs.add(new DefaultCatalog(this));
				}
				catalogsLoaded = true;
			}
		} catch (SQLException sqle) {
			try {
				if (conn.getAutoCommit() == false) {
					conn.rollback();
				}
			} catch (SQLException e1) {
				// ignore
			}
			errorMessage = "loadCatalogs failed: " + sqle.getMessage();
			logger.error(errorMessage);
			if (session != null) {
				session.error(errorMessage, sqle);
			}
		} finally {
			if (session != null) {
				DatabaseSessionPool.release(session);
			}
			loadingCatalogs = false;
		}
		fireDatamodelEvent("Loading catalogs finished (" + catalogs.size() + ")", DatamodelEvent.ACTION_MESSAGE_EVENT);
		for (SQLCatalog catalog : catalogs) {
			loadSchemas(catalog);
		}
		return ok;
	}
	
	public boolean loadSchemas(SQLCatalog catalog) {
		if (loadingSchemas) {
			return false;
		}
		catalog.setLoadingSchemas(true);
		loadingSchemas = true;
		if (Thread.currentThread().isInterrupted()) {
			loadingSchemas = false;
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("loadSchemas for catalog: " + catalog.getName());
		}
		DatabaseSession session = null;
		Connection conn = connection;
		if (conn == null) {
			session = getDatabaseSession();
			if (session == null) {
				return false;
			}
			session.setLastSQL("loadSchemas");
			conn = session.getConnection();
		}
		if (conn == null) {
			return false;
		}
		boolean ok = false;
		fireDatamodelEvent("Loading schemas for catalog", DatamodelEvent.ACTION_MESSAGE_EVENT);
		try {
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null) {
				dbmd = conn.getMetaData();
				catalog.clear();
				final ResultSet rsSchemas = dbmd.getSchemas();
				while (rsSchemas.next()) {
					if (Thread.currentThread().isInterrupted()) {
						break;
					}
					String name = rsSchemas.getString("TABLE_SCHEM");
					SQLSchema schema = new SQLSchema(this, name);
					catalog.addSQLSchema(schema);
					if (schema.getName().equalsIgnoreCase(loginSchemaName)) {
						loginSchema = schema;
					}
				}
				rsSchemas.close();
				if (catalog.getCountSchemas() == 0) {
					SQLSchema schema = new DefaultSchema(this);
					catalog.addSQLSchema(schema);
					if (schema.getName().equalsIgnoreCase(loginSchemaName)) {
						loginSchema = schema;
					}
				}
				ok = true;
				schemasLoaded = true;
			}
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// ignore
			}
			errorMessage = "loadSchemas (schemas) for catalog: " + catalog.getName() + " failed: " + e.getMessage();
			logger.error(errorMessage);
			fireDatamodelEvent("Loading schemas failed.", DatamodelEvent.ACTION_MESSAGE_EVENT);
			if (session != null) {
				session.error(errorMessage, e);
			}
			return false;
		} finally {
			if (session != null) {
				DatabaseSessionPool.release(session);
			}
			loadingSchemas = false;
			catalog.setLoadingSchemas(false);
		}
		fireDatamodelEvent("Loading catalogs+schemas finished (" + catalogs.size() + ")", DatamodelEvent.ACTION_MESSAGE_EVENT);
		return ok;
	}
	
	protected void clearCatalogs() {
		catalogs.clear();
	}
	
	public void addCatalog(SQLCatalog catalog) {
		if (catalogs.contains(catalog) == false) {
			catalogs.add(catalog);
		}
	}

	public SQLTable getSQLTable(String schemaName, String tableName) {
		if (isIdentifierName(tableName) == false) {
			return null;
		}
		for (SQLCatalog cat : catalogs) {
			for (SQLSchema schema : cat.getSchemas()) {
				if (schema.getName().equalsIgnoreCase(schemaName)) {
					SQLTable table = schema.getTable(tableName);
					if (table != null) {
						return table;
					}
				}
			}
		}
		return null;
	}
	
	public static boolean isIdentifierName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return false;
		}
		for (int i = 0, n = name.length(); i < n; i++) {
			if (Character.isJavaIdentifierPart(name.charAt(i)) == false) {
				return false;
			}
		}
		return true;
	}

	public SQLSchema getSchema(String schemaName) {
		String currentCatalog = null;
		int pos = schemaName.indexOf('.');
		if (pos > 0) {
			currentCatalog = schemaName.substring(0, pos);
		}
		for (SQLCatalog cat : catalogs) {
			if (currentCatalog != null) {
				if (currentCatalog.equals(cat.getName()) == false) {
					continue;
				}
			}
			for (SQLSchema schema : cat.getSchemas()) {
				if (schema.getName().equalsIgnoreCase(schemaName)) {
					return schema;
				}
			}
		}
		return null;
	}

	public SQLTable getSQLTable(String schemaDotTableName) {
		int pos = schemaDotTableName.indexOf('.');
		if (pos == -1) {
			return currentSQLSchema.getTable(schemaDotTableName);
		} else {
			String schemaName = schemaDotTableName.substring(0, pos);
			String tableName = schemaDotTableName.substring(pos + 1);
			return getSQLTable(schemaName, tableName);
		}
	}

	public SQLField getSQLField(String schemaDotTableDotFieldName) {
		int pos = schemaDotTableDotFieldName.indexOf('.');
		if (pos == -1) {
			throw new IllegalArgumentException(
					schemaDotTableDotFieldName
					+ " is not a absolute name");
		} else {
			String schemaName = schemaDotTableDotFieldName.substring(0, pos);
			SQLSchema schema = getSchema(schemaName);
			if (schema != null) {
				int pos1 = schemaDotTableDotFieldName.indexOf('.', pos + 1);
				if (pos1 == -1) {
					throw new IllegalArgumentException(
							schemaDotTableDotFieldName
									+ " is not a absolute name");
				} else {
					String tableName = schemaDotTableDotFieldName.substring(pos + 1, pos1);
					SQLTable table = schema.getTable(tableName);
					if (table != null) {
						String fieldName = schemaDotTableDotFieldName.substring(pos1);
						return table.getField(fieldName);
					}
				}
			}
		}
		return null;
	}
	
	public SQLCatalog getSQLCatalog(String name) {
		for (SQLCatalog c : catalogs) {
			if (name.equalsIgnoreCase(c.getName())) {
				return c;
			}
		}
		return null;
	}
	
	public List<SQLCatalog> getCatalogs() {
		return catalogs;
	}

	public SQLField getSQLField(
			String schemaName, 
			String tableName,
			String fieldName) {
		SQLSchema schema = getSchema(schemaName);
		if (schema != null) {
			SQLTable table = schema.getTable(tableName);
			if (table != null) {
				return table.getField(fieldName);
			}
		}
		return null;
	}

	public boolean loadTables(SQLSchema schema) {
		if (schema.isLoadingTables()) {
			return false;
		}
		schema.setLoadingTables(true);
		if (Thread.currentThread().isInterrupted()) {
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("loadTables schema=" + schema);
		}
		boolean ok = false;
		DatabaseSession session = null;
		Connection conn = connection;
		if (conn == null) {
			session = getDatabaseSession();
			if (session == null) {
				return false;
			}
			conn = session.getConnection();
			session.setLastSQL("loadTables schema=" + schema);
		}
		if (conn == null) {
			return false;
		}
		try {
			fireDatamodelEvent("Loading tables and views...", DatamodelEvent.ACTION_MESSAGE_EVENT);
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null) {
				SQLTable table;
				schema.clearTables();
				final ResultSet rs = dbmd.getTables(
						schema.getCatalog().getKey(), 
						schema.getKey(), 
						null,
						null);
				if (rs != null) {
					while (rs.next()) {
						if (Thread.currentThread().isInterrupted()) {
							break;
						}
						String tableName = rs.getString("TABLE_NAME");
						table = new SQLTable(schema.getModel(), schema, tableName);
						table.setType(rs.getString("TABLE_TYPE"));
						table.setComment(rs.getString("REMARKS"));
						if (table.isTable() || table.isView()) {
							schema.addTable(table);
						}
					}
					rs.close();
				}
				schema.setTablesLoaded();
				ok = true;
			}	
		} catch (SQLException sqle) {
			try {
				if (conn.getAutoCommit() == false) {
					conn.rollback();
				}
			} catch (SQLException e1) {
				// ignore
			}
			errorMessage = "loadTables for schema=" + schema + " failed: " + sqle.getMessage();
			if (session != null) {
				session.error(errorMessage, sqle);
			}
			fireDatamodelEvent("Loading tables failed.", DatamodelEvent.ACTION_MESSAGE_EVENT);
			return false;
		} finally {
			if (session != null) {
				DatabaseSessionPool.release(session);
			}
			schema.setLoadingTables(false);
		}
		fireDatamodelEvent("Loading tables and views finished: " + schema.getTableCount() + " tables.", DatamodelEvent.ACTION_MESSAGE_EVENT);
		return ok;
	}
	
	public boolean loadSequences(SQLSchema schema) {
		if (databaseExtension.hasSequenceFeature()) {
			if (schema.isLoadingTables()) {
				return false;
			}
			if (Thread.currentThread().isInterrupted()) {
				return false;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("loadSequences schema=" + schema);
			}
			DatabaseSession session = null;
			Connection conn = connection;
			if (conn == null) {
				session = getDatabaseSession();
				if (session == null) {
					return false;
				}
				conn = session.getConnection();
				session.setLastSQL("loadSequences schema=" + schema);
			}
			if (conn == null) {
				return false;
			}
			fireDatamodelEvent("Loading sequences...", DatamodelEvent.ACTION_MESSAGE_EVENT);
			databaseExtension.listSequences(conn, schema);
			fireDatamodelEvent("Loading sequences for " + schema + " finished: " + schema.getSequenceCount() + " sequences.", DatamodelEvent.ACTION_MESSAGE_EVENT);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * load procedure and functions
	 * 
	 * @param schema
	 *            object that represent a schema
	 * @return true if everything went well
	 */
	public boolean loadProcedures(SQLSchema schema) {
		if (databaseExtension.loadProcedures(schema)) {
			return true;
		}
		if (schema.isLoadingProcedures()) {
			return false;
		}
		// makes this thread save
		schema.setLoadingProcedures(true);
		if (logger.isDebugEnabled()) {
			logger.debug("loadProcedures schema=" + schema);
		}
		boolean ok = false;
		DatabaseSession session = null;
		Connection conn = connection;
		if (conn == null) {
			session = getDatabaseSession();
			if (session == null) {
				return false;
			}
			conn = session.getConnection();
			session.setLastSQL("loadProcedures schema=" + schema);
		}
		if (conn == null) {
			return false;
		}
		try {
			fireDatamodelEvent("Load procedures for " + schema, DatamodelEvent.ACTION_MESSAGE_EVENT);
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null) {
				schema.clearProcedures(); // remove any previously loaded procedures
				ResultSet rs = dbmd.getProcedures(
						schema.getCatalog().getKey(),
						schema.getKey(), 
						null);
				if (rs != null) {
					// loading all procedures and functions
					while (rs.next()) {
						if (Thread.currentThread().isInterrupted()) {
							break;
						}
						// PROCEDURE_CAT contains the package name
						String catalogName = rs.getString("PROCEDURE_CAT");
						String name = rs.getString("PROCEDURE_NAME");
						if (catalogName != null && catalogName.length() > 0) {
							name = catalogName + "." + name;
						}
						// create an new one
						if (logger.isDebugEnabled()) {
							logger.debug("   Add procedure schema=" + schema + " name=" + name);
						}
						SQLProcedure procedure = new SQLProcedure(this, schema,	name);
						// decide if it is a function
						procedure.setComment(rs.getString("REMARKS"));
						schema.addProcedure(procedure);
					}
					rs.close();
				} else {
					logger.error("No result returned by query database metadata for procedures.");
				}
				// loading procedure/function parameters
				if (schema.getProcedureCount() > 0) {
					rs = dbmd.getProcedureColumns(
							schema.getCatalog().getKey(), 
							schema.getKey(), 
							null, 
							null);
					if (rs != null) {
						// an index for overloaded procedures
						int procedureIndex = 0;
						String prevProcedureName = "noprocedurehere_xx";
						while (rs.next()) {
							if (Thread.currentThread().isInterrupted()) {
								break;
							}
							// PROCEDURE_CAT contains the package name
							String catalogName = rs.getString("PROCEDURE_CAT");
							String name = rs.getString("PROCEDURE_NAME");
							if (catalogName != null && catalogName.length() > 0) {
								// add the catalog name as package name
								name = catalogName + "." + name;
							}
							int pos = 0;
							try {
								// position of this parameter in the procedure parameter
								// list
								pos = rs.getInt("ORDINAL_POSITION");
							} catch (Exception e) {
								// perhaps this column is not defined in every database
								// type
								pos = -1;
							}
							if (prevProcedureName.equals(name)) {
								if (pos == 0) {
									// if we get the same procedure name and a parameter
									// with position zero increase the procedure index
									// that means point to the next procedure with the
									// same name
									procedureIndex++;
								}
							} else {
								procedureIndex = 0;
							}
							String columnName = rs.getString("COLUMN_NAME");
							List<SQLProcedure> list = schema.getProcedures(name);
							SQLProcedure procedure = list.get(procedureIndex);
							if (procedure != null) {
								int dataType = rs.getShort("DATA_TYPE");
								String dataTypeName = rs.getString("TYPE_NAME");
								int length = rs.getInt("LENGTH");
								int precision = rs.getInt("PRECISION");
								short ioType = rs.getShort("COLUMN_TYPE");
								SQLProcedure.Parameter p = procedure.addParameter(
										columnName,
										dataType,
										dataTypeName,
										length,
										precision, 
										ioType);
								databaseExtension.setupDataType(p);
							}
							// keep the name of the last procedure to detect overloaded
							// procedures
							prevProcedureName = name;
						}
						rs.close();
					}
				}
				schema.setProcedureLoaded();
				schema.sortProcedureList();
				fireDatamodelEvent("Loading procedure source code", DatamodelEvent.ACTION_MESSAGE_EVENT);
				for (int i = 0; i < schema.getProcedureCount(); i++) {
					SQLProcedure p = schema.getProcedureAt(i);
					if (session != null) {
						databaseExtension.setupProcedureSQLCode(conn, p);
					}
				}
				ok = true;
				fireDatamodelEvent("Load procedures for " + schema + " finished: " + schema.getProcedureCount() + " procedures.", DatamodelEvent.ACTION_MESSAGE_EVENT);
			}
		} catch (SQLException sqle) {
			try {
				if (conn.getAutoCommit() == false) {
					conn.rollback();
				}
			} catch (SQLException e1) {
				// ignore
			}
			// creates a error message to show it in GUI
			errorMessage = "loadProcedures schema=" 
				+ schema 
				+ " failed: "
				+ sqle.getMessage();
			if (session != null) {
				session.error(errorMessage, sqle);
			}
			fireDatamodelEvent("Load procedures failed.", DatamodelEvent.ACTION_MESSAGE_EVENT);
			return false;
		} finally {
			if (session != null) {
				// put the session back in the pool
				DatabaseSessionPool.release(session);
			}
			schema.setLoadingProcedures(false);
		}
		return ok;
	}
	
	boolean loadColumns(SQLTable table) {
		return loadColumns(table, false);
	}

	boolean loadColumns(SQLTable table, boolean ignoreIndices) {
		if (table.isLoadingColumns()) {
			return false;
		}
		table.setLoadingColumns(true);
		if (Thread.currentThread().isInterrupted()) {
			table.setLoadingColumns(false);
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("loadColumns table=" + table);
		}
		DatabaseSession session = null;
		Connection conn = connection;
		if (conn == null) {
			session = getDatabaseSession();
			if (session == null) {
				return false;
			}
			conn = session.getConnection();
			session.setLastSQL("loadColumns table=" + table);
		}
		if (conn == null) {
			table.setLoadingColumns(false);
			return false;
		}
		try {
			fireDatamodelEvent("Load columns for " + table, DatamodelEvent.ACTION_MESSAGE_EVENT);
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null) {
				SQLField field = null;
				try {
					table.clearFields();
					final ResultSet rs = dbmd.getColumns(
							table.getSchema().getCatalog().getKey(), 
							table.getSchema().getKey(), 
							table.getName(), 
							null);
					if (rs != null) {
						while (rs.next()) {
							if (Thread.currentThread().isInterrupted()) {
								break;
							}
							String tableName = rs.getString("TABLE_NAME");
							if (table.getName().equalsIgnoreCase(tableName)) {
								String name = rs.getString("COLUMN_NAME");
								field = new SQLField(this, table, name);
								field.setType(rs.getInt("DATA_TYPE"));
								field.setTypeName(rs.getString("TYPE_NAME"));
								field.setLength(rs.getInt("COLUMN_SIZE"));
								field.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
								field.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
								field.setNullValueAllowed(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
								field.setComment(rs.getString("REMARKS"));
								field.setDefaultValue(rs.getString("COLUMN_DEF"));
								field.setSerial("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")) || "true".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
								databaseExtension.setupDataType(field);
								table.addField(field);
							}
						}
						rs.close();
					}
					fireDatamodelEvent("Loading columns finished", DatamodelEvent.ACTION_MESSAGE_EVENT);
					if (table.isView()) {
						if (table.isMaterializedView()) {
							if (ignoreIndices == false) {
								loadIndexes(table);
							}
						}
						if (session != null && ignoreIndices == false) {
							databaseExtension.setupViewSQLCode(conn, table);
						}
					} else {
						loadConstraints(table);
						if (ignoreIndices == false) {
							loadIndexes(table);
						}
					}
				} catch (SQLException sqle) {
					try {
						if (conn.getAutoCommit() == false) {
							conn.rollback();
						}
					} catch (SQLException e1) {
						// ignore
					}
					logger.error("loadColumns (get columns) for table=" + table + " failed: " + sqle.getMessage());
					if (session != null) {
						session.error(errorMessage, sqle);
					}
					fireDatamodelEvent("Loading columns for table=" + table + " failed", DatamodelEvent.ACTION_MESSAGE_EVENT);
					table.setLoadingColumns(false);
					return false;
				} finally {
					table.setLoadingColumns(false);
					table.setFieldsLoaded();
				}
			}	
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// ignore
			}
			logger.error("loadColumns failed: " + e.getMessage(), e);
			if (session != null) {
				session.error(errorMessage);
			}
		} finally {
			if (session != null) {
				DatabaseSessionPool.release(session);
			}
			table.setLoadingColumns(false);
			table.setFieldsLoaded();
		}
		return true;
	}
	
	public Long countDatasets(SQLTable table) {
		Long count = null;
		DatabaseSession session = getDatabaseSession();
		if (session != null) {
			fireDatamodelEvent("Count datasets for table " + table, DatamodelEvent.ACTION_MESSAGE_EVENT);
			try {
				ResultSet rs = session.executeQuery(databaseExtension.getSelectCountRows(table));
				if (rs.next()) {
					count = rs.getLong(1);
				}
				rs.close();
			} catch (SQLException sqle) {
				logger.error("countDatasets for table " + table.getName() + " failed:" + sqle.getMessage(), sqle);
			} finally {
				DatabaseSessionPool.release(session);
			}
		}
		return count;
	}

	private boolean loadConstraints(SQLTable table) {
		if (table.isLoadingConstraints()) {
			return false;
		}
		table.setLoadingConstraints(true);
		if (Thread.currentThread().isInterrupted()) {
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("loadConstraints table=" + table);
		}
		DatabaseSession session = null;
		Connection conn = connection;
		if (conn == null) {
			session = getDatabaseSession();
			if (session == null) {
				return false;
			}
			conn = session.getConnection();
			session.setLastSQL("loadColumns table=" + table);
		}
		if (conn == null) {
			return false;
		}
		try {
			fireDatamodelEvent("Load primary key constraints for table " + table, DatamodelEvent.ACTION_MESSAGE_EVENT);
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null) {
				table.clearConstraints();
				try {
					final ResultSet rs = dbmd.getPrimaryKeys(
							table.getSchema().getCatalog().getKey(), 
							table.getSchema().getKey(), 
							table.getName());
					if (rs != null) {
						while (rs.next()) {
							if (Thread.currentThread().isInterrupted()) {
								break;
							}
							String tableName = rs.getString("TABLE_NAME");
							if (table.getName().equalsIgnoreCase(tableName)) {
								String name = rs.getString("PK_NAME");
								SQLConstraint constraint = table.getPrimaryKeyConstraint();
								if (constraint == null) {
									constraint = new SQLConstraint(
											this, 
											table,
											SQLConstraint.PRIMARY_KEY, 
											name);
									table.setPrimaryKeyConstraint(constraint);
								}
								constraint.addPrimaryKeyFieldName(
										rs.getString("COLUMN_NAME"), 
										rs.getShort("KEY_SEQ"));
								if (logger.isDebugEnabled()) {
									logger.debug("pk constraint changed: " + constraint);
								}
							}
						}
						rs.close();
					}
				} catch (SQLException sqle) {
					try {
						if (conn.getAutoCommit() == false) {
							conn.rollback();
						}
					} catch (SQLException e1) {
						// ignore
					}
					logger.error("loadConstraints (pk) for table=" + table + " failed: " + sqle.getMessage());
					if (session != null) {
						session.error(errorMessage, sqle);
					}
					try {
						connection.rollback();
					} catch (SQLException e1) {
						// ignore
					}
					table.setLoadingConstraints(false);
					return false;
				}
				if (Thread.currentThread().isInterrupted()) {
					return false;
				}
				try {
					if (session != null) {
						session.setLastSQL("loadConstraints (imported keys) table=" + table);
					}
					fireDatamodelEvent("Load foreign key constraints for table " + table, DatamodelEvent.ACTION_MESSAGE_EVENT);
					final ResultSet rs = dbmd.getImportedKeys(
							table.getSchema().getCatalog().getKey(), 
							table.getSchema().getKey(), 
							table.getName());
					if (rs != null) {
						while (rs.next()) {
							if (Thread.currentThread().isInterrupted()) {
								break;
							}
							String name = rs.getString("FK_NAME");
							String referencedSchema = rs.getString("PKTABLE_SCHEM");
							String referencedTable = rs.getString("PKTABLE_NAME");
							SQLConstraint constraint = table.getConstraint(name);
							if (constraint == null) {
								constraint = new SQLConstraint(
										this, 
										table,
										SQLConstraint.FOREIGN_KEY, 
										name);
								if (referencedSchema == null || referencedSchema.isEmpty()) {
									referencedSchema = table.getSchema().getName();
								}
								constraint.setReferencedTableName(referencedSchema + "." + referencedTable);
								table.addConstraint(constraint);
							}
							constraint.addForeignKeyColumnNamePair(
									rs.getString("FKCOLUMN_NAME"), 
									rs.getString("PKCOLUMN_NAME"), 
									rs.getShort("KEY_SEQ"));
							if (logger.isDebugEnabled()) {
								logger.debug("fk constraint changed: " + constraint);
							}
						}
						rs.close();
						table.setConstraintsLoaded();
					}
					if (logger.isDebugEnabled()) {
						List<SQLConstraint> list = table.getConstraints();
						for (int i = 0; i < list.size(); i++) {
							logger.debug(" * " + list.get(i));
						}
					}
				} catch (SQLException sqle) {
					logger.error("loadConstraints (fk) for table=" + table + " failed: " + sqle.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
					if (session != null) {
						session.error(errorMessage);
					}
					table.setLoadingConstraints(false);
					return false;
				}
			}
		} catch (Exception e) {
			logger.error("loadConstraints failed: " + e.getMessage(), e);
			if (session != null) {
				session.error(errorMessage);
			}
			return false;
		} finally {
			if (session != null) {
				DatabaseSessionPool.release(session);
			}
			table.setLoadingConstraints(false);
		}
		fireDatamodelEvent("Loading constraints finished", DatamodelEvent.ACTION_MESSAGE_EVENT);
		return true;
	}
	
	private boolean loadIndexes(SQLTable table) {
		if (table.isLoadingIndexes()) {
			return false;
		}
		table.setLoadingIndexes(true);
		if (Thread.currentThread().isInterrupted()) {
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("loadIndexes table=" + table);
		}
		DatabaseSession session = null;
		Connection conn = connection;
		if (conn == null) {
			session = getDatabaseSession();
			if (session == null) {
				return false;
			}
			conn = session.getConnection();
			session.setLastSQL("loadIndexes table=" + table);
		}
		if (conn == null) {
			return false;
		}
		try {
			fireDatamodelEvent("Load indexes for table " + table, DatamodelEvent.ACTION_MESSAGE_EVENT);
			DatabaseMetaData dbmd = conn.getMetaData();
			if (dbmd != null) {
				table.clearIndexes();
				SQLIndex index = null;
				ResultSet rs = dbmd.getIndexInfo(null, table.getSchema().getName(), table.getName(), false, true);
				if (rs != null) {
					while (rs.next()) {
						if (Thread.currentThread().isInterrupted()) {
							break;
						}
						String tableName = rs.getString("TABLE_NAME");
						if (table.getName().equalsIgnoreCase(tableName)) {
							String indexName = rs.getString("INDEX_NAME");
							if (indexName == null) {
								continue; // because Oracle sends here null some times
							}
							boolean unique = false;
							Object nonUnique = rs.getObject("NON_UNIQUE");
							if (nonUnique instanceof Boolean) {
								unique = !((Boolean) nonUnique).booleanValue();
							} else if (nonUnique instanceof Integer) {
								unique = ((Integer) nonUnique).intValue() == 0;
							} else if (nonUnique instanceof String) {
								unique = !Boolean.parseBoolean((String) nonUnique);
							}
							short type = rs.getShort("TYPE");
							short ordinalPosition = rs.getShort("ORDINAL_POSITION");
							String columnName = rs.getString("COLUMN_NAME");
							String sortOrder = rs.getString("ASC_OR_DESC");
							int cardinality = rs.getInt("CARDINALITY");
							String filterCondition = rs.getString("FILTER_CONDITION");
							index = table.getIndexByName(indexName);
							if (index == null) {
								if (logger.isDebugEnabled()) {
									logger.debug("add index " + indexName);
								}
								index = new SQLIndex(table.getModel(), indexName, table);
								index.setUnique(unique);
								index.setType(type);
								index.setCardinality(cardinality);
								index.setFilterCondition(filterCondition);
								table.addIndex(index);
							}
							index.addIndexField(columnName, ordinalPosition, sortOrder);
						}
					}
					table.setIndexesLoaded();
					rs.close();
					return true;
				}
			}
		} catch (Exception e) {
			try {
				if (conn.getAutoCommit() == false) {
					conn.rollback();
				}
			} catch (SQLException e1) {
				// ignore
			}
			logger.error("loadIndexes failed: " + e.getMessage(), e);
			if (session != null) {
				session.error(errorMessage, e);
			}
			fireDatamodelEvent("Load indexes for table " + table + " failed.", DatamodelEvent.ACTION_MESSAGE_EVENT);
			return false;
		} finally {
			if (session != null) {
				DatabaseSessionPool.release(session);
			}
			table.setLoadingIndexes(false);
		}
		return false;
	}

	public String getLastErrorMessage() {
		return errorMessage;
	}

	public static void removeViews(SQLSchema schema) {
		SQLTable sqlTable = null;
		for (int x = 0; x < schema.getTableCount(); x++) {
			sqlTable = schema.getTableAt(x);
			if (sqlTable.getType().equals(SQLTable.TYPE_VIEW)) {
				schema.removeSQLTable(sqlTable);
				x--;
			}
		}
	}

	@Override
	public String toString() {
		if (cd != null) {
			return cd.toString();
		} else {
			return "not connected";
		}
	}

	@Override
	public int compareTo(SQLDataModel o) {
		if (o.cd != null) {
			return o.cd.compareTo(cd);
		}
		return 0;
	}

	@Override
	public int hashCode() {
		if (cd != null) {
			return cd.hashCode();
		} else {
			return super.hashCode();
		}
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof SQLDataModel) {
			if (cd != null) {
				return cd.equals(((SQLDataModel) object).cd);
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public List<SQLSchema> getSchemas() {
		List<SQLSchema> list = new ArrayList<SQLSchema>();
		for (SQLCatalog c : catalogs) {
			for (SQLSchema s : c.getSchemas()) {
				if (list.contains(s) == false) {
					list.add(s);
				}
			}
		}
		return list;
	}
	
	public DatabaseExtension getDatabaseExtension() {
		return databaseExtension;
	}
	
	public SQLSchema getCurrentSQLSchema() {
		if (currentSQLSchema == null) {
			currentSQLSchema = getSchema(databaseExtension.getLoginSchema(cd));
		}
		return currentSQLSchema;
	}
	
	public ConnectionDescription getConnectionDescription() {
		return cd.clone();
	}

	public SQLSchema getLoginSchema() {
		return loginSchema;
	}

	public String getLoginSchemaName() {
		return loginSchemaName;
	}

	/** this method is only allowed if the data model already had a connection and use a new one
	 * to continue working!
	 * @param connection
	 */
	public void setConnection(Connection connection) {
		if (this.connection == null) {
			throw new IllegalStateException("Use the method setConnection only to renew an existing connection!");
		}
		if (connection != null) {
			this.connection = connection;
		} else {
			throw new IllegalArgumentException("connection cannot be null!");
		}
	}

	public void setCurrentSQLSchema(SQLSchema currentSQLSchema) {
		this.currentSQLSchema = currentSQLSchema;
	}
	
}
