package dbtools;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Encapsulate a complete database.
 */
public class DatabaseSession {

    private static Logger staticLogger = Logger.getLogger(DatabaseSession.class);
	private Logger currentLogger = staticLogger;
    
    /* id to provide access to multiple sessions */
    static private int              lastSessionID         = -1;
    private int                     sessionID;
    static public final int         NORMAL                = 0;
    static public final int         WARNINGS              = 1;
    static public final int         FATALS                = 2;
    private int                     errorCode             = NORMAL;
    protected boolean               isConnected           = false;
    protected boolean               success               = false;
    protected boolean               isPooled              = false;
    /* native session data */
    protected Connection            conn;
    private Statement               currentOpenStatement;
    protected String                lastErrorMessage;
    protected ConnectionDescription desc;
    protected String                linesep               = System.getProperty("line.separator");
    private boolean                 isQuery;
    protected boolean               free                  = true;
    protected String                aliasName;
    private final HashMap<String, PreparedStatement> psMap = new HashMap<String, PreparedStatement>();
    private long                    connectedTimestamp    = 0;
    private long                    lastOccupiedTimestamp = 0;
    private long                    lastUsedTimestamp     = 0;
    private long                    lastErrorTimestamp    = 0;
    private long                    countUsage            = 0;
    @SuppressWarnings("unused")
	private boolean                 isCommitted           = true;
    private Exception               lastException;
    protected String                lastSQL;
    private ArrayList<PreparedStatementDefinition> preparedStatementDefinitions;
    private boolean driverLoaded = false;
    private boolean connectInProgress = false;
    private int lastUpdateCount = -1;
    
    /**
     * default-constructor require,
     * that all necessary initialisations will be proceed later.
     */
    public DatabaseSession() {
        createSessionID();
        desc = new ConnectionDescription();
    }

    /**
     * default-constructor
     * that all necessary initialisations will be proceed later.
     */
    public DatabaseSession(ConnectionDescription desc) {
        this.desc = desc;
        createSessionID();
        if (loadDriver()) {
            connect();
        }
    }
    
    /**
     * to provide an instance logging
     * @param extLogger
     */
    public void setLogger(Logger extLogger) {
        if (extLogger == null) {
            throw new IllegalArgumentException("extLogger cannot be null");
        }
    	this.currentLogger = extLogger;
    }
    
    /**
     * @return the current logger (may be it is static)
     */
    public Logger getLogger() {
    	return currentLogger;
    }
    
    /**
     * rejects the instance logger and use the static class logger
     */
    public void resetLoggerToStaticClassLogger() {
    	currentLogger = staticLogger;
    }

    /**
     * this session is within a pool
     * @param isPooledSession
     */
    protected void setPooled(boolean isPooledSession) {
        this.isPooled = isPooledSession;
    }

    /**
     * @return true when within a pool
     */
    public boolean isPooled() {
        return isPooled;
    }
    
    /**
     * checks the current usage of this session
     * following checks will be proceed:
     * # when session is pooled then this session must be occupied
     * # session must be connected
     * @param isDataModificationAction true if the current sql is an modificating command
     * @throws HandlingException when a check fails
     */
    public final void checkUsage(boolean isDataModificationAction) {
        if (isPooled) {
            if (free) {
                throw new HandlingException(getClass().getName() + ".checkUsage session is not occupied !");
            }
            if (isConnected == false) {
                throw new HandlingException(getClass().getName() + ".checkUsage session is not connected !");
            }
            if (isDataModificationAction && desc.getAutoCommit() == false) {
                isCommitted = false;
            }
        }
        lastUsedTimestamp = System.currentTimeMillis();
        
    }

    /**
     * @return time in seconds how long session are unused
     */
    public long getIdleTime() {
        if (lastOccupiedTimestamp == 0) {
            return (System.currentTimeMillis() - connectedTimestamp) / 1000;
        } else {
            return (System.currentTimeMillis() - lastOccupiedTimestamp) / 1000;
        }
    }

    /**
     * set debug-mode
     * @param debug=true if additional output by the activities of database
     * @deprecated
     */
    public void setDebug(boolean debug_loc) {
        if (debug_loc) {
            currentLogger.setLevel(Level.DEBUG);
        } else {
            currentLogger.setLevel(Level.INFO);
        }
    }

    /**
     * marks this session as free for further usage
     * this method should only be used in class DatabaseSessionPool
     */
    protected void release() {
        if (free) {
            // ist bereits frei, kann nicht nochmal freigegeben werden
            // da es sich um einen massiven Programmdesignfehler handelt
            // es den Programmierer um die Ohren hauen !!
            throw new HandlingException(getClass().getName() + ".release failed: Session is already released !");
        }
        if ((isCommitted = false) && (desc.getAutoCommit() == false)) {
            commit();
            error("release WARNING: Session was not committed - now already done !");
        }
        free = true;
    }

    /**
     * marks a session as used
     * @param changeTimeStamp true change timestamp of usage
     */
    protected void occupy(boolean changeTimeStamp) {
        if (!free) {
            // ist bereits belegt, kann nicht nochmal belegt werden
            // da es sich um einen massiven Programmdesignfehler handelt
            // es den Programmierer um die Ohren hauen !!
            throw new HandlingException("alias=" + getAliasName() +  " occupy failed: Session is already in use !");
        }
        free = false;
        if (changeTimeStamp) {
            lastOccupiedTimestamp = System.currentTimeMillis();
            if (countUsage < Long.MAX_VALUE) {
                countUsage++;
            } else {
                countUsage = 0;
            }
        }
    }

    /**
     * marks this session as used
     */
    protected void occupy() {
        occupy(true);
    }

    /**
     * @return true if session is ready to use
     */
    public boolean isReady() {
        return (free && success) && isConnected;
    }

    /**
     * notify/log an error (and change the internal state)
     * @param message error message
     */
    public void error(String message) {
        success = false;
        this.lastErrorMessage = "alias=" + getAliasName() + ":" + message;
        currentLogger.error(lastErrorMessage);
        lastErrorTimestamp = System.currentTimeMillis();
    }
    
    /**
     * notify/log an error (and change the internal state)
     * @param message error message
     * @param Exception e
     */
    public void error(String message, Throwable e) {
        success = false;
        this.lastErrorMessage = message;
        if (e instanceof Exception) {
        	lastException = (Exception) e;
        }
        currentLogger.error(lastErrorMessage, e);
        lastErrorTimestamp = System.currentTimeMillis();
        if (e instanceof SQLException) {
        	SQLException sqle = (SQLException) e;
        	if (sqle.getNextException() != null) {
        		lastErrorMessage = lastErrorMessage + "\nNext exception:" + sqle.getNextException().getMessage();
                currentLogger.error(sqle.getNextException().getMessage(), sqle.getNextException());
        	}
        }
    }

    public void error(Exception exception) {
        success = false;
        this.lastException = exception;
        this.lastErrorMessage = exception.getMessage();
        currentLogger.error(lastErrorMessage, exception);
        lastErrorTimestamp = System.currentTimeMillis();
        if (exception instanceof SQLException) {
        	SQLException sqle = (SQLException) exception;
        	if (sqle.getNextException() != null) {
        		lastErrorMessage = lastErrorMessage + "\nNext exception:" + sqle.getMessage();
                currentLogger.error(sqle.getNextException().getMessage(), sqle.getNextException());
        	}
        }
    }

    public void warn(String message) {
        this.lastErrorMessage = "alias=" + getAliasName() + ":" + message;
        currentLogger.warn(lastErrorMessage);
        lastErrorTimestamp = System.currentTimeMillis();
    }

    public void warn(String message, Exception e) {
        this.lastErrorMessage = "alias=" + getAliasName() + ":" + message;
        currentLogger.warn(lastErrorMessage, e);
        lastErrorTimestamp = System.currentTimeMillis();
    }

    /**
     * gibt auf der Standardausgabe Statusmeldung aus
     * @param message auszugebende Meldung
     */
    public void status(String message) {
        currentLogger.info("alias=" + getAliasName() + ":" + message);
    }

    /**
     * Status ob session belegt
     * @return true wenn belegt
     */
    public boolean isOccupied() {
        return !free;
    }

    /**
     * return the time when occupied
     * @param ms
     */
    public long getLastOccupiedTimestamp() {
        return lastOccupiedTimestamp;
    }
    
    /**
     * @return the timestamp of last usage
     */
    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }
    
    /**
     * @param count of usage
     */
    public long getCountUsage() {
        return countUsage;
    }

    /**
     * session need an alias name if used in pool
     * @param name aliasName
     */
    public void setAliasName(String name) {
        this.aliasName = name;
    }

    /**
     * @return aliasName
     */
    public String getAliasName() {
    	if (aliasName != null) {
            return aliasName;
    	} else {
    		if (getConnectionDescription() != null) {
    			return getConnectionDescription().getUniqueId();
    		} else {
    			return null;
    		}
    	}
    }

    /**
     * returns the status of the session
     * @return true if the session is connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * @param time when connected
     */
    public long getConnectedTimestamp() {
        return connectedTimestamp;
    }

    /**
     * sets the ConnectionDescription to describe the Connection
     * @param desc description for connection
     */
    public void setConnectionDescription(ConnectionDescription desc_loc) {
        this.desc = desc_loc;
    }

    /**
     * returns the description-object of these session
     * @return description fo the session
     */
    public ConnectionDescription getConnectionDescription() {
        return desc;
    }

    /**
     * load a database-driver.
     * Realize that the driverclass is in classpath !
     * use desc ConnectionDescription that contains information about driver.
     */
    public boolean loadDriver() {
        driverLoaded = false;
        if (desc != null) {
            if (desc.getJndiDataSourceName() != null) {
                driverLoaded = true;
            } else {
                if (desc.getDriverClassName() != null) {
                    driverLoaded = registerDriver(desc.getDriverClassName());
                } else {
                    lastException = new NullPointerException("driver-class-name is null");
                    error("loadDriver failed: driver-class-name is null !");
                }
            }
        } else {
            lastException = new NullPointerException("decription is null");
            error("loadDriver failed: description is null !");
        }
        return driverLoaded;
    }

    public boolean isDriverLoaded() {
        return driverLoaded;
    }
    
    /**
     * register database-JDBC-driver
     * @param driverClassName the full qualified classname of the main-class of the driver
     * @return true if driver load is successful, false if a errors occured
     */
    private boolean registerDriver(String driverClassName) {
        boolean ok = false;
        try {
            Class.forName(driverClassName);
            ok = true;
            success = true;
        } catch (ClassNotFoundException cnfe) {
            lastException = cnfe;
            error("registerDriver "
                    + driverClassName
                    + " failed: driver not available: "
                    + cnfe.getMessage(), cnfe);
        }
        return ok;
    }

    /**
     * sets the timeout for creating session
     * @param timeout in seconds
     */
    static public void setTimeout(int timeout) {
        DriverManager.setLoginTimeout(timeout);
    }

    /**
     * returns the timeout of the DriverManager
     * @return timeout in seconds
     */
    static public int getTimeout() {
        return DriverManager.getLoginTimeout();
    }

    /**
     * connect to the database with the former initialized data
     * @return true if connection was successfully created
     */
    public boolean connect() {
        connectInProgress = true;
        if (desc != null) {
            boolean ok = false;
            try {
            	if (driverLoaded == false) {
            		loadDriver();
            	}
                Properties properties = null;
                if (desc.getPasswd() == null) {
                    warn("connect (using DriverManager): no password provided !");
                }
                if (desc.getPropertiesString() != null
                        && desc.getPropertiesString().length() > 0
                        && ConnectionDescription.isExtendsUrlWithProperties() == false) {
                    properties = new Properties();
                    StringTokenizer st = new StringTokenizer(desc.getPropertiesString(), ";&");
                    String token = null;
                    String key = null;
                    String value = null;
                    int pos = 0;
                    while (st.hasMoreTokens()) {
                        token = st.nextToken();
                        pos = token.indexOf('=');
                        if (pos != -1) {
                            key = token.substring(0, pos).trim();
                            value = token.substring(pos + 1).trim();
                            if (key.length() > 0 && value.length() > 0) {
                                properties.put(key, value);
                            }
                        }
                    }
                }
                if (properties != null && properties.isEmpty() == false) {
                    properties.put("user", desc.getUser());
                    properties.put("password", desc.getPasswd());
                }
                if (desc.getPropertiesString() != null
                        && desc.getPropertiesString().length() > 0
                        && ConnectionDescription.isExtendsUrlWithProperties()) {
                    conn = DriverManager.getConnection(
                            desc.getUrl() + "?" + desc.getPropertiesString(),
                            desc.getUser(),
                            desc.getPasswd());
                } else if (properties != null) {
                    conn = DriverManager.getConnection(desc.getUrl(), properties);
                } else if (ConnectionDescription.isUserInfoInUrl()) {
                    conn = DriverManager.getConnection(desc.getUrl());
                } else {
                    conn = DriverManager.getConnection(
                            desc.getUrl(), 
                            desc.getUser(), 
                            desc.getPasswd());
                }
                ok = true;
                connectedTimestamp = System.currentTimeMillis();
            } catch (Throwable ex) {
                error("connect (using DriverManager) failed: " + ex.getMessage(), ex);
                ok = false;
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(desc.getAutoCommit());
                } catch (SQLException sqle) {
                    warn("connect setAutoCommit failed: " + sqle.getMessage());
                }
                if (ConnectionDescription.isRunInitialSQLOnConnect()) {
                    ok = runInitSQL();
                } else {
                    success = ok;
                }
            }
            isConnected = ok;
            connectInProgress = false;
            return ok;
        } else {
            connectInProgress = false;
            return false;
        }
    }
    
    public boolean isConnectInProgress() {
        return connectInProgress;
    }
    
    private boolean runInitSQL() {
        boolean ok = true;
        if (desc != null) {
            String initSQL = desc.getInitSQL();
            if (initSQL != null) {
                SQLParser parser = new SQLParser(initSQL);
                SQLStatement initStat = null;
                for (int i = 0; i < parser.getStatementCount(); i++) {
                    initStat = parser.getStatementAt(i);
                    execute(initStat.getSQL());
                    if (isSuccessful() == false) {
                        ok = false;
                    }
                }
            }
        }
        return ok;
    }

    /**
     * cancel the current running statement, some times it is not possible to do this and
     * in these cases the session will be marked as damaged and returns false.
     * Cancelation can consume a lot of time depending what resources until now are used by this statement.
     * @return true if successfully canceled
     */
    public boolean cancelStatement() {
        checkUsage(false);
        try {
        	if (currentOpenStatement != null) {
                Statement toCancelStatement = currentOpenStatement;
                currentOpenStatement = null;
                toCancelStatement.cancel();
                toCancelStatement.close();
        	}
        } catch (SQLException sqle) {
            lastException = sqle;
            error("cancelStatement (cancel) failed: " + sqle.getMessage(), sqle);
        }
        try {
            conn.clearWarnings();
            success = true;
        } catch (SQLException sqle) {
            lastException = sqle;
            error("cancelStatement (clear warnings) failed: " + sqle.getMessage(), sqle);
        }
        return success;
    }

    public boolean setAutoCommit(boolean enableAutoCommit) {
        checkUsage(false);
        boolean ok = true;
        desc.setAutoCommit(enableAutoCommit);
        if (isConnected) {
            try {
                conn.setAutoCommit(desc.getAutoCommit());
            } catch (SQLException sqle) {
                lastException = sqle;
                error("setAutoCommit failed: " + sqle.getMessage(), sqle);
                ok = false;
            }
        }
        return ok;
    }

    public boolean isAutoCommit() {
        boolean isAutoCommit = false;
        if (isConnected) {
            try {
                isAutoCommit = conn.getAutoCommit();
            } catch (SQLException sqle) {
                lastException = sqle;
                error("isAutoCommit failed: " + sqle.getMessage(), sqle);
            }
        } else {
            isAutoCommit = desc.getAutoCommit();
        }
        return isAutoCommit;
    }

    /**
     * commit a session
     * @return true if successful
     */
    public boolean commit() {
        checkUsage(false);
        if (currentLogger.isDebugEnabled()) {
            currentLogger.debug("commit()");
        }
        boolean ok = false;
        try {
        	if (conn.getAutoCommit() == false) {
                conn.commit();
        	}
            ok = true;
            success = true;
            isCommitted = true;
        } catch (SQLException sqle) {
            lastException = sqle;
            error("commit failed: " + sqle.getMessage(), sqle);
        }
        return ok;
    }

    void commitUnchecked() {
        warn(" id=" + sessionID + " perform commit unckecked !");
        try {
            conn.commit();
            success = true;
            isCommitted = true;
        } catch (SQLException sqle) {
            lastException = sqle;
            error("commit failed: " + sqle.getMessage(), sqle);
        }
    }

    /**
     * rollback a session
     * @return true if successful
     */
    public boolean rollback() {
        checkUsage(false);
        if (currentLogger.isDebugEnabled()) {
            currentLogger.debug("rollback()");
        }
        boolean ok = false;
        try {
        	if (conn.getAutoCommit() == false) {
                conn.rollback();
        	}
            ok = true;
            success = true;
            isCommitted = true;
        } catch (SQLException sqle) {
            lastException = sqle;
            error("rollback failed: " + sqle.getMessage(), sqle);
        }
        return ok;
    }

    /**
     * close the database-session and all previous created statements
     * @return true if session successful closed.
     */
    public boolean close() {
        if (currentLogger.isDebugEnabled()) {
            currentLogger.debug("close()");
        }
        resetErrorStatus();
        if (isConnected) {
            try {
                if (currentOpenStatement != null) {
                    currentOpenStatement.close();
                    currentOpenStatement = null;
                }
            } catch (SQLException sqle) {
                lastException = sqle;
                error("close: close current statement failed: " + sqle.getMessage(), sqle);
            }
            try {
                if (psMap.isEmpty() == false) {
                    String psName;
                    PreparedStatement ps;
                    for (Iterator<?> it = psMap.keySet().iterator(); it.hasNext();) {
                        psName = (String) it.next();
                        ps = (PreparedStatement) psMap.get(psName);
                        if (ps != null) {
                            ps.close();
                        }
                    }
                }
                if ((preparedStatementDefinitions != null) && preparedStatementDefinitions.isEmpty() == false) {
                    PreparedStatement ps = null;
                    for (Iterator<PreparedStatementDefinition> it = preparedStatementDefinitions.iterator(); it.hasNext();) {
                        ps = it.next().getPreparedStatement();
                        if (ps != null) {
                            ps.close();
                        }
                    }
                }
            } catch (SQLException sqle) {
                lastException = sqle;
                error("close: close prepared statements failed: " + sqle.getMessage(), sqle);
            }
            try {
                conn.close();
                isConnected = false;
            } catch (SQLException sqle) {
                lastException = sqle;
                error("close: close connection failed: " + sqle.getMessage(), sqle);
            }
        }
        return isSuccessful();
    }

    public Statement createStatement() {
    	return createStatement(-1);
    }
    
    /**
     * returns the statement
     * @param fetchSize: if not equals -1 then fetchSize will be set into the statement
     * @return Statement or null if connect() failed
     */
    public Statement createStatement(int fetchSize) {
        checkUsage(false);
        Statement stat = null;
        try {
			stat = conn.createStatement();
			if (fetchSize != -1) {
				stat.setFetchSize(fetchSize);
			} else if (desc.getDefaultFetchSize() != -1) {
				stat.setFetchSize(desc.getDefaultFetchSize());
			}
		} catch (SQLException e) {
			error("createStatement fetchSize=" + fetchSize + " failed: " + e.getMessage(), e);
		}
        return stat;
    }
    
    /**
     * creates a prepared statement and use sql code as name for it
     * @param sql
     * @return PreparedStatement bind to this connection
     * @throws java.sql.SQLException
     */
    public PreparedStatement createPreparedStatement(String sql) throws SQLException {
    	return createPreparedStatement(sql, -1);
    }
    	
    /**
     * returns a prepared statement
     * @param SQL-Statement to prepare statement
     * @param enableCache true: the statement will be chached inside a map, though statement will be closed if connection will be closed
     * @return prepared statement
     * 
     * @throws SQLException
     */
    public PreparedStatement createPreparedStatement(String sql, int fetchSize) throws SQLException {
        checkUsage(false);
        if (sql == null || sql.trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        lastSQL = "create prepared statement "+sql;
        success = false;
        final PreparedStatement ps = conn.prepareStatement(sql);
        if (ps != null) {
        	if (fetchSize != -1) {
        		ps.setFetchSize(fetchSize);
        	} else if (desc.getDefaultFetchSize() != -1) {
        		ps.setFetchSize(desc.getDefaultFetchSize());
        	}
            psMap.put(sql, ps);
        }
        success = true;
        return ps;
    }

    public void createPreparedStatement(PreparedStatementDefinition psDef) throws SQLException {
        createPreparedStatement(psDef, true, -1);
    }
    
    public void createPreparedStatement(PreparedStatementDefinition psDef, boolean enableCache) throws SQLException {
    	createPreparedStatement(psDef, enableCache, -1);
    }
    
    /**
     * returns a prepared statement
     * ist das Statement bereits mit einem JDBC-prepared statement ausgestattet wird
     * kein weiteres erzeugt. Diese Methode kann für das gleuiche Statement somit
     * mehrfach aufgerufen werden
     * @param psDef
     * @param enableCache true dann wird das Statement intern gehalten um es automatisch schliessen zu können
     * @param fetchSize wenn != -1 wird dieser Wert als fetchSize dem Statement mitgegeben
     * @throws SQLException
     */
    public void createPreparedStatement(PreparedStatementDefinition psDef, boolean enableCache, int fetchSize) throws SQLException {
        checkUsage(false);
        success = false;
        // um Fehler zu vermeiden...
        if (psDef instanceof CallableStatementDefinition) {
            createCallableStatement((CallableStatementDefinition) psDef);
        } else {
            if (psDef.getSQL() == null || psDef.getSQL().trim().length() == 0) {
                throw new IllegalArgumentException("sql cannot be null or empty");
            }
            // wenn Prepared Statement bereits erzeugt
            // dann wird davon ausgegangen, dass diese Methode verwendet wurde
            if (psDef.getPreparedStatement() == null) {
                if (psDef.getSQL() == null || psDef.getSQL().trim().length() == 0) {
                    throw new IllegalArgumentException("sql cannot be null or empty");
                }
                final PreparedStatement ps = conn.prepareStatement(psDef.getSQL());
                if (ps == null) {
                    throw new SQLException("unable to create statement " + psDef.getSQL());
                }
            	if (fetchSize != -1) {
            		ps.setFetchSize(fetchSize);
            	} else if (desc.getDefaultFetchSize() != -1) {
            		ps.setFetchSize(desc.getDefaultFetchSize());
            	}
                psDef.setPreparedStatement(ps);
                success = true;
                if (enableCache) {
                    if (preparedStatementDefinitions == null) {
                        preparedStatementDefinitions = new ArrayList<PreparedStatementDefinition>();
                    }
                    preparedStatementDefinitions.add(psDef);
                }
            } else {
                // das statement in jedem Fall neu erstellen !!
                final PreparedStatement ps = conn.prepareStatement(psDef.getSQL());
                if (ps == null) {
                    throw new SQLException("unable to create statement " + psDef.getSQL());
                }
            	if (fetchSize != -1) {
            		ps.setFetchSize(fetchSize);
            	} else if (desc.getDefaultFetchSize() != -1) {
            		ps.setFetchSize(desc.getDefaultFetchSize());
            	}
                psDef.setPreparedStatement(ps);
                success = true;
            }
        }
    }

    /**
     * close prepared statement
     * @param psDef
     * @throws SQLException
     */
    protected void closePreparedStatement(PreparedStatementDefinition psdef) throws SQLException {
        if (psdef.getPreparedStatement() != null) {
            psdef.getPreparedStatement().close();
        }
    }

    public void createCallableStatement(CallableStatementDefinition csDef) throws SQLException {
    	createCallableStatement(csDef, -1);
    }
    
    /**
     * returns a prepared statement
     * @param psDef
     * @throws SQLException
     */
    public void createCallableStatement(CallableStatementDefinition csDef, int fetchSize) throws SQLException {
        checkUsage(false);
        success = false;
        if (csDef.getSQL() == null || csDef.getSQL().trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        if (csDef.getCallableStatement() == null) {
            final CallableStatement cs = conn.prepareCall(csDef.getSQL());
            if (cs == null) {
                throw new SQLException("unable to create statement " + csDef.getSQL());
            }
        	if (fetchSize != -1) {
        		cs.setFetchSize(fetchSize);
        	} else if (desc.getDefaultFetchSize() != -1) {
        		cs.setFetchSize(desc.getDefaultFetchSize());
        	}
            csDef.setCallableStatement(cs);
            if (preparedStatementDefinitions == null) {
                preparedStatementDefinitions = new ArrayList<PreparedStatementDefinition>();
            }
            preparedStatementDefinitions.add(csDef);
            success = true;
        } else {
            final CallableStatement cs = conn.prepareCall(csDef.getSQL());
            if (cs == null) {
                throw new SQLException("unable to create statement " + csDef.getSQL());
            }
        	if (fetchSize != -1) {
        		cs.setFetchSize(fetchSize);
        	} else if (desc.getDefaultFetchSize() != -1) {
        		cs.setFetchSize(desc.getDefaultFetchSize());
        	}
            csDef.setCallableStatement(cs);
            success = true;
        }
    }

    public CallableStatement createCallableStatement(String sql) throws SQLException {
    	return createCallableStatement(sql, -1);
    }
    	
    /**
     * returns a callable prepared statement
     * @param sql
     * @return callable statement
     */
    public CallableStatement createCallableStatement(String sql, int fetchSize) throws SQLException {
        checkUsage(false);
        success = false;
        if (sql == null || sql.trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        lastSQL = "create callable statement " + sql;
        final CallableStatement cs = conn.prepareCall(sql);
        if (cs != null) {
            if (fetchSize != -1) {
        		cs.setFetchSize(fetchSize);
        	} else if (desc.getDefaultFetchSize() != -1) {
        		cs.setFetchSize(desc.getDefaultFetchSize());
        	}
        }
        success = true;
        return cs;
    }

    /**
     * creates a prepared statement
     * @param name name of the statement (for reusage)
     * @param sql of the statement (use ? as parameter)
     * @return true if successfully created
     */
    public final boolean createPreparedStatement(String name, String sql) {
    	return createPreparedStatement(name, sql, -1);
    }    	

    /**
     * creates a prepared statement
     * @param name name of the statement (for reusage)
     * @param sql of the statement (use ? as parameter)
     * @param fetchsize for the result sets created by this statements (can increase the performance)
     * @return true if successfully created
     */
    public final boolean createPreparedStatement(String name, String sql, int fetchSize) {
        checkUsage(false);
        boolean ok = false;
        if (sql == null || sql.trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (psMap.get(name) == null) {
            try {
                final PreparedStatement ps = conn.prepareStatement(sql);
                if (ps != null) {
                	if (fetchSize != -1) {
                		ps.setFetchSize(fetchSize);
                	} else if (desc.getDefaultFetchSize() != -1) {
                		ps.setFetchSize(desc.getDefaultFetchSize());
                	}
                    psMap.put(name, ps);
                    success = true;
                    ok = true;
                }
            } catch (SQLException sqle) {
                lastException = sqle;
                error("createPreparedStatement name="
                        + name
                        + " failed: "
                        + sqle.getMessage(), sqle);
            }
        } else {
            lastException = null;
            error("createPreparedStatement failed: attempt to overwrite existing prepared statement name="
                    + name);
        }
        return ok;
    }

    /**
     * creates a callable statement
     * @param name name of the statement (for reusage)
     * @param sql of the statement (use ? as parameter)
     * @return true if successfully created
     */
    public final boolean createCallableStatement(String name, String sql) {
    	return createCallableStatement(name, sql, -1);
    }
    
    /**
     * creates a callable statement
     * @param name name of the statement (for reusage)
     * @param sql of the statement (use ? as parameter)
     * @param fetchsize for the result sets created by this statements (can increase the performance)
     * @return true if successfully created
     */
    public final boolean createCallableStatement(String name, String sql, int fetchSize) {
        checkUsage(false);
        boolean ok = false;
        if (sql == null || sql.trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        try {
            final CallableStatement cs = conn.prepareCall(sql);
            if (cs != null) {
            	if (fetchSize != -1) {
            		cs.setFetchSize(fetchSize);
            	} else if (desc.getDefaultFetchSize() != -1) {
            		cs.setFetchSize(desc.getDefaultFetchSize());
            	}
                psMap.put(name, cs);
                success = true;
                ok = true;
            }
        } catch (SQLException sqle) {
            lastException = sqle;
            error("createCallableStatement name="
                    + name
                    + " failed: "
                    + sqle.getMessage(), sqle);
        }
        return ok;
    }

    /**
     * returns a PreparedStatement from internal map
     * @param name name of statement
     * @return PreparedStatement aus der Map
     * @see createPreparedStatement(String, String)
     */
    public final PreparedStatement getPreparedStatement(String name) {
        checkUsage(false);
        lastSQL = "use prepared statement "+name;
        return (PreparedStatement) psMap.get(name);
    }

    /**
     * returns a resultset from statement
     * @return result
     */
    public final ResultSet getCurrentResultSet() {
        checkUsage(false);
        ResultSet rs = null;
        try {
            rs = currentOpenStatement.getResultSet();
            success = true;
        } catch (SQLException sqle) {
            lastException = sqle;
            error("getCurrentResultSet failed: " + sqle.getMessage(), sqle);
        }
        return rs;
    }

    public final boolean lastStatementWasAQuery() {
        return isQuery;
    }

    /**
     * returns the used connection
     * @return connection
     */
    public final Connection getConnection() {
        checkUsage(false);
        return conn;
    }

    public final String getUrl() {
        return desc.getUrl();
    }

    public final String getDriverClassName() {
        return desc.getDriverClassName();
    }

    public final String getUser() {
        return desc.getUser();
    }

    public final String getPassword() {
        return desc.getPasswd();
    }

    public final void setUser(String user) {
        desc.setUser(user);
    }

    public final void setPasswd(String passwd) {
        desc.setPasswd(passwd);
    }

    public final void setUrl(String url) {
        desc.setUrl(url);
    }

    public final void setFetchSize(int fetchSize) {
        desc.setDefaultFetchSize(fetchSize);
    }

    public final void setDriverClassName(String driverClassName) {
        desc.setDriverClassName(driverClassName);
    }
    
    private void setCurrentOpenStatement(Statement stat) {
    	closeCurrentOpenStatement();
    	currentOpenStatement = stat;
    }
    
    public void closeCurrentOpenStatement() {
    	if (currentOpenStatement != null) {
			if (currentLogger.isDebugEnabled()) {
				currentLogger.debug("closeCurrentOpenStatement");
			}
    		try {
    			currentOpenStatement.close();
    		} catch (SQLException e) {
                currentLogger.warn("close previous statement failed: " + e.getMessage(), e);
            }
    	}
    }

    /**
     * executes a query with creating a resultset
     * @param ResultSet
     */
    public synchronized final ResultSet executeQuery(String sql) {
        return executeQuery(sql, desc.getDefaultFetchSize());
    }

    /**
     * executes a query with creating a resultset
     * @param ResultSet
     */
    public synchronized final ResultSet executeQuery(String sql, int fetchSize) {
        checkUsage(false);
        lastSQL = sql;
        if (sql == null || sql.trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        if (currentLogger.isDebugEnabled()) {
            currentLogger.debug("executeQuery(sql=" + sql + ")");
        }
        ResultSet rs = null;
        try {
        	final Statement stat = conn.createStatement();
			if (fetchSize > 0) {
				stat.setFetchSize(fetchSize);
				if (stat.getClass().getName().contains("mysql")) {
					try {
						stat.getClass().getMethod("enableStreamingResults", new Class[] {}).invoke(stat, new Object[] {});
					} catch (Exception e) {
						currentLogger.warn(e.getMessage());
					}
				}			
			}
        	setCurrentOpenStatement(stat);
            rs = stat.executeQuery(sql);
            success = true;
            isQuery = true;
        } catch (SQLException sqle) {
            lastException = sqle;
            error("executeQuery=" + sql + " failed: " + sqle.getMessage(), sqle);
        }
        return rs;
    }

    /**
     * executes a SQL-statement without creating a result-set
     * @return true if executing successful
     */
    public synchronized boolean execute(String sql) {
        checkUsage(true);
        if (sql == null || sql.trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        lastSQL = sql;
        if (currentLogger.isDebugEnabled()) {
            currentLogger.debug("execute(sql=" + sql + ")");
        }
        Statement stat = null;
        try {
        	stat = conn.createStatement();
			if (desc.getDefaultFetchSize() != -1) {
				stat.setFetchSize(desc.getDefaultFetchSize());
			}
            isQuery = stat.execute(sql);
            if (isQuery) {
                setCurrentOpenStatement(stat);
            } else {
            	lastUpdateCount = stat.getUpdateCount();
                stat.close();
            }
            success = true;
        } catch (SQLException sqle) {
            if (stat != null) {
                try {
                    stat.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            setCurrentOpenStatement(null);
            lastException = sqle;
            error("execute=" + sql + " failed: " + sqle.getMessage(), sqle);
        }
        return success;
    }

    /**
     * executes a SQL-update-statement
     * @return number of updated datasets
     */
    public synchronized int executeUpdate(String sql) {
        checkUsage(true);
        if (sql == null || sql.trim().length() == 0) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        lastSQL = sql;
        if (currentLogger.isDebugEnabled()) {
            currentLogger.debug("executeUpdate(sql=" + sql + ")");
        }
        int count = -1;
        Statement stat = null;
        try {
            isQuery = false;
            stat = conn.createStatement();
            count = stat.executeUpdate(sql);
            stat.close();
            success = true;
        } catch (SQLException sqle) {
            if (stat != null) {
                try {
                    stat.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            lastException = sqle;
            error("executeUpdate=" + sql + " failed: " + sqle.getMessage(), sqle);
        }
        return count;
    }

    /**
     * returns the message-text of the last Exception
     * @return message-text
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public Exception getLastException() {
        return lastException;
    }

    /**
     * returns th last SQL 
     * only if the SQL are setted before usage - even when subclass execute there own sql code
     * @return SQL
     */
    public String getLastSQL() {
        return lastSQL;
    }
    
    public void setLastSQL(String action) {
    	lastSQL = action;
    }

    /**
     * @return time stamp of the last error
     */
    public long getLastErrorTimestamp() {
        return lastErrorTimestamp;
    }

    /**
     * returns the status of the last action
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return success;
    }

    /**
     * reset the successful flag to true
     */
    public void resetSuccessfulFlag() {
        success = true;
    }

    /**
     * kills all error informations
     */
    public void resetErrorStatus() {
        success = true;
        lastErrorTimestamp = 0;
        lastErrorMessage = null;
        lastException = null;
    }

    /**
     * print out the debug-informations
     * @param debug-info
     */
    public void debug(String message) {
        if (currentLogger.isDebugEnabled()) {
            currentLogger.debug(message);
        }
    }

    // -------------- Methods to provide more than one session -----------------

    private void createSessionID() {
        lastSessionID = lastSessionID + 1;
        sessionID = lastSessionID;
    }

    /**
     * Returns the valid sessionID.
     * @return sessionID
     */
    public int getSessionID() {
        return sessionID;
    }

    /**
     * Returns a String-representation of these object
     * @return string-representation
     */
    @Override
    public String toString() {
        return getClass().getName() + ": sessionID=" + String.valueOf(sessionID) + " alias="+aliasName+" url=" + desc.getUrl() + " user=" + desc.getUser();
    }

    public int getErrorCode() {
        return errorCode;
    }

	public int getLastUpdateCount() {
		return lastUpdateCount;
	}
    
}