package dbtools;

import java.lang.management.ManagementFactory;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

/**
 * A database pool with static access and config methods which can be used in standalone applications.
 * @author Jan Lolling
 */
public class DatabaseSessionPool {

    private static final Logger logger = LogManager.getLogger(DatabaseSessionPool.class);
    private static final Vector<DatabaseSession> pool = new Vector<DatabaseSession>();
    private static String lastErrorMessage = null;
    private static final Object monitor = new Object();
    // key = aliasName value = ConnectionDescription
    private static final ConnectionMap connDescMap = new ConnectionMap();
    // key = sessionAlias value = Properties (key statementAlias value = sql)
    private static final HashMap<String, HashMap<String, String>> sessionStatementMap = new HashMap<String, HashMap<String, String>>();
    // key = sessionAlias value = Test-SQL
    private static final HashMap<String, String> testSQLs = new HashMap<String, String>();
    static public final String UNIVERSAL_TEST_SQL = "select * from dual";
    static private boolean checkInProcess = false;
    private static volatile Timer timer = null;
    private static CheckPoolTask checkPoolTask = null;
    private static final HashMap<String, String> aliasConnectionClassMap = new HashMap<String, String>();
    static private int maxIdleTime = 60;                                
    private static int checkPoolCyclusTime = 60;
    private static String lastPoolCheckMessage = null;
    private static long lastOccuranceOfPoolCheckMessage = 0;
    private static long lastOccuranceOfPoolCheck = 0;

    private DatabaseSessionPool() {}

    private static void error(String message, Exception e) {
        if (message == null) {
            message = "undeclared error";
        }
        lastErrorMessage = message;
        if (e != null) {
            logger.error(message, e);
        } else {
            logger.error(message);
        }
    }
    
    private static void error(String message) {
        error(message, null);
    }
    
    /**
     * 
     * @param aliasName DatabaseSession
     * @param testSql used to test the Connection
     */
    static public void setTestSQL(String aliasName, String testSql) {
        testSQLs.put(aliasName, testSql);
    }

    /**
     * set the time, how long a session can be idle before session will be closed
     * @param idletime in s
     */
    public static void setMaxIdleTime(int idleTime) {
        maxIdleTime = idleTime;
    }

    public static int getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * checks if it is possible to have such kind of session from pool
     * it is not necessary, that those kind of session already established.
     * If the pool has such kind of description, for every request the pool
     * take care of session wil be established and delivered
     * @param alias
     * @return true if such a kind of session will be available if requested
     */
    public static boolean existDatabaseSession(String alias) {
        return (getConnectionDescriptionFromMap(alias) != null);
    }

    /**
     * this method provide a possibility to define prepared statements which will be established 
     * after session are connected. This avoids the needs to subclass Databasesession to implement
     * they own prepared statements
     * @param sessionAlias session alias
     * @param statementAlias name for statement
     * @param sql SQL code of statement
     */
    static public void definePreparedStatementSQL(String sessionAlias, String statementAlias, String sql) {
        HashMap<String, String> statMap = sessionStatementMap.get(sessionAlias);
        if (statMap == null) {
            statMap = new HashMap<String, String>();
            sessionStatementMap.put(sessionAlias, statMap);
        }
        statMap.put(statementAlias, sql);
    }

    /**
     * create a number of DatabaseSessions with given Parameters within Properties
     * @param properties necessary information to create the session
     * @param autoCommit autoCommit
     * @param dbName alias name of the database
     * @return number of created sessions
     */
    static public int createSessions(Properties properties, boolean autoCommit, String dbName) {
        if (dbName == null) {
            throw new IllegalArgumentException("dbName cannot be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null");
        }
        return createSessions(
            properties.getProperty("URL", "").trim(),
            properties.getProperty("DRIVER", "").trim(),
            properties.getProperty("USER", "").trim(),
            properties.getProperty("PASSWORD", "").trim(),
            autoCommit,
            Integer.parseInt(properties.getProperty("FETCHSIZE", "-1")),
            Integer.parseInt(properties.getProperty("INITIAL_SESSION_COUNT", "1")),
            dbName,
            properties.getProperty("DB_API_CLASS"));
    }

    static public int createSessions(ConnectionDescription cd, int countSessions, String dbName, Class<? extends DatabaseSession> sourceClass) {
        return createSessions(cd, countSessions, dbName, sourceClass.getName());
    }

    /**
     * creates a number of connected DatabaseSession
     * @param url url in jdbc format
     * @param driver driver class name (with package)
     * @param user user or schema name
     * @param passwd password for use schema or user (in DB2 use the OS password)
     * @param count amount of session to be added
     * @param autoCommit true for automaticly commits all statements
     * @param aliasName Name aliasname, if nul then url will be used as alias
     * @return count of successfully created sessions. -1 if some errors occurse
     */
    static public int createSessions(
        String url,
        String driver,
        String user,
        String passwd,
        boolean autoCommit,
        int fetchSize,
        int count,
        String sessionAliasName) {
        return createSessions(
            url,
            driver,
            user,
            passwd,
            autoCommit,
            fetchSize,
            count,
            sessionAliasName,
            DatabaseSession.class.getName());
    }

    /**
     * creates a number of connected DatabaseSession
     * @param url url in jdbc format
     * @param driver driver class name (with package)
     * @param user user or schema name
     * @param passwd password for use schema or user (in DB2 use the OS password)
     * @param count amount of session to be added
     * @param autoCommit true for automaticly commits all statements
     * @param aliasName Name aliasname, if nul then url will be used as alias
     * @param className name of a subclass from DatabaseSession. Sessions will be created by using Class.forName(className)
     * @return count of successfully created sessions. -1 if some errors occurse
     */
    static public int createSessions(
        String url,
        String driver,
        String user,
        String passwd,
        boolean autoCommit,
        int fetchSize,
        int count,
        String sessionAliasName,
        String className) {
        final ConnectionDescription cd = new ConnectionDescription();
        cd.setUser(user);
        cd.setUrl(url);
        cd.setDriverClassName(driver);
        cd.setPasswd(passwd);
        cd.setAutoCommit(autoCommit);
        cd.setDefaultFetchSize(fetchSize);
        return createSessions(cd, count, sessionAliasName, className);
    }

    /**
     * creates a number of connected DatabaseSession
     * @param cd a complete description of the sessions to be created
     * @param count amount of session to be added
     * @param autoCommit true for automaticly commits all statements
     * @param aliasName Name aliasname, if nul then url will be used as alias
     * @return count of successfully created sessions. -1 if some errors occurse
     */
    static public int createSessions(
        ConnectionDescription cd,
        int count,
        String sessionAliasName) {
        return createSessions(cd, count, sessionAliasName, DatabaseSession.class);
    }
    
    static public int createSession(String sourceAlias, String newAlias, String className) {
        if (sourceAlias == null) {
            throw new IllegalArgumentException("sourceAlias cannot be null");
        }
        if (newAlias == null) {
            throw new IllegalArgumentException("newAlias cannot be null");
        }
        if (sourceAlias.equalsIgnoreCase(newAlias)) {
            throw new IllegalArgumentException("sourceAlias cannot be equal to newAlias");
        }
        if (existDatabaseSession(newAlias)) {
            throw new IllegalStateException("connection with new alias " + newAlias + " already exists");
        }
        ConnectionDescription cd = getConnectionDescription(sourceAlias);
        if (cd != null) {
            return createSessions(cd, 1, newAlias, className);
        } else {
            return -1;
        }
    }

    /**
     * creates a number of connected DatabaseSession
     * @param cd a complete description of the sessions to be created
     * @param count amount of session to be added
     * @param autoCommit true for automatically commits all statements
     * @param aliasName Name aliasname, if null then url will be used as alias
     * @param className name of a subclass from DatabaseSession. Sessions will be created by using Class.forName(className)
     * @return count of successfully created sessions. -1 if some errors occurse
     */
    static public int createSessions(
        ConnectionDescription cd,
        int count,
        String sessionAliasName,
        String className) {
        if (sessionAliasName == null) {
            lastErrorMessage = "ERROR: failed to create DatabaseSession: no alias given !";
            error(lastErrorMessage);
            return -1;
        }
        int counter = 0;
        if (className == null) {
            className = DatabaseSession.class.getName();
        }
        aliasConnectionClassMap.put(sessionAliasName.toLowerCase(), className);
        for (int i = 0; i < count; i++) {
            DatabaseSession session = null;
            try {
                session = (DatabaseSession) Class.forName(className).newInstance();
            } catch (Exception e) {
                lastErrorMessage = "ERROR: instantiation (initial) of DatabaseSession subclass " + className + " for alias " + sessionAliasName + " failed:" + e.toString();
                error(lastErrorMessage, e);
            }
            if (session != null) {
                session.setAliasName(sessionAliasName);
                session.setConnectionDescription(cd);
                logger.info("DatabaseSessionPool: create (initial) " + session.toString() + "... ");
                if (session.loadDriver() && session.connect()) {
                    if (prepareStatements(session)) {
                        logger.info("READY.");
                        session.setPooled(true);
                        pool.addElement(session);
                        counter++;
                        addConnectionDescriptionToMap(sessionAliasName, cd);
                    }
                } else {
                    error("ERROR: load driver + connect: " + session.getLastErrorMessage());
                    break;
                }
            }
        }
        return counter;
    }

    private static void addConnectionDescriptionToMap(String alias, ConnectionDescription cd) {
    	connDescMap.put(alias, cd);
    }
    
    private static ConnectionDescription getConnectionDescriptionFromMap(String alias) {
    	return connDescMap.get(alias);
    }
    
    /**
     * creates a number of connected DatabaseSession
     * @param jndiDataSourceName to retrieve connections from JNDI repository (like commons pools)
     * @param count amount of session to be added
     * @param autoCommit true for automaticly commits all statements
     * @param aliasName Name aliasname, if nul then url will be used as alias
     * @param className name of a subclass from DatabaseSession. Sessions will be created by using Class.forName(className)
     * @return count of successfully created sessions. -1 if some errors occurse
     */
    static public int createSessions(
        String jndiDataSourceName,
        int count,
        boolean autoCommit,
        String className) {
        if (jndiDataSourceName == null) {
            error("failed to create DatabaseSession: no jndiDataSourceName given !");
            return -1;
        }
        int counter = 0;
        if (className == null) {
            className = DatabaseSession.class.getName();
        }
        aliasConnectionClassMap.put(jndiDataSourceName.toLowerCase(), className);
        for (int i = 0; i < count; i++) {
            DatabaseSession session = null;
            try {
                session = (DatabaseSession) Class.forName(className).newInstance();
            } catch (Exception e) {
                error("ERROR: instantiation (initial) of DatabaseSession subclass " + className + " for jndiDataSourceName " + jndiDataSourceName + " failed:" + e.toString(), e);
            }
            if (session != null) {
                final ConnectionDescription cd = new ConnectionDescription();
                cd.setJndiDataSourceName(jndiDataSourceName);
                cd.setAutoCommit(autoCommit);
                addConnectionDescriptionToMap(jndiDataSourceName, cd);
                session.setAliasName(jndiDataSourceName);
                session.setConnectionDescription(cd);
                logger.info("DatabaseSessionPool: create (initial) " + session.getClass().getName() + " id=" + session.getSessionID() + " jndiDataSourceName=" + jndiDataSourceName + " alias=" + session.getAliasName() + "... ");
                if (session.loadDriver() && session.connect()) {
                    if (prepareStatements(session)) {
                        logger.info("READY.");
                        session.setPooled(true); // erst hier sonst gibt es Ärger im connect u.U.
                        pool.addElement(session);
                        counter++;
                    } else {
                        error("prepare statements failed:" + session.getLastErrorMessage());
                        counter = -1;
                        break;
                    }
                } else {
                    error("connecting failed: " + session.getLastErrorMessage());
                    counter = -1;
                    break;
                }
            }
        }
        return counter;
    }

    /**
     * closes all connection without take care of there using
     */
    static public void close() {
        logger.info("DatabaseSessionPool: removing all sessions");
        stopCheckPoolThread();
        DatabaseSession session;
        for (int i = 0, n = pool.size(); i < n; i++) {
            session = pool.get(i);
            try {
                session.close();
            } catch (HandlingException e) {
                logger.warn("session was already in use !");
            } catch (Exception e) {
                logger.warn("session was already in use !");
            }
        }
        pool.clear();
    }

    /**
     * closes all session for a given alias
     * @param alias 
     */
    static public void close(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("alias cannot be null");
        }
        logger.info("DatabaseSessionPool: removing all sessions for alias=" + alias);
        DatabaseSession session;
        for (int i = 0; i < pool.size(); i++) {
            session = pool.get(0);
            if (alias.equals(session.getAliasName())) {
                try {
                    session.occupy();
                } catch (HandlingException he) {
                    logger.warn("session " + session + " will be teminated !");
                } finally {
                    session.close();
                }
                pool.remove(session);
                i--;
            }
        }
    }

    private static class CheckPoolTask extends TimerTask {

        /**
         * Logger for this class
         */
        private static final Logger logger = LogManager.getLogger(CheckPoolTask.class);

        public void run() {
            if (checkInProcess == false) {
                // to avoid parallel checks
                checkInProcess = true;
                final Vector<DatabaseSession> wrongSessions = new Vector<DatabaseSession>();
                final Vector<DatabaseSession> idleSessions = new Vector<DatabaseSession>();
                int countRemovedDamaged = 0;
                int countReset = 0;
                DatabaseSession session = null;
                for (int i = 0; i < pool.size(); i++) {
                    session = pool.elementAt(i);
                    // check for idle
                    if (session.isReady()) {
                        session.occupy(false);
                        if (session.getIdleTime() > maxIdleTime) {
                            pool.remove(session);
                            i--;
                            idleSessions.addElement(session);
                            if (logger.isDebugEnabled()) {
                                logger.debug("DatabaseSessionPool: mark session as idle: " + session.toString());
                            }
                            continue;
                        } else {
                            session.release();
                        }
                    }
                    // check for damages
                    if (session.isOccupied() == false) { // don't ask with method ready() here !
                        session.occupy(false); // now thats mine session without changing timestamp
                        if (session.isSuccessful()) {
                            // test only if not recently used
                            if (session.getIdleTime() > checkPoolCyclusTime) {
                                // session was longer than checkPoolCycleTime not in use
                                if (checkSession(session)) {
                                    // move the session out of view from pool
                                    pool.removeElement(session);
                                    // remember session
                                    wrongSessions.addElement(session);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("DatabaseSessionPool: mark session as wrong because test SQL failed: " + session.toString());
                                    }
                                } else {
                                    // if everthing is ok than release this session for further usage
                                    session.release();
                                }
                            } else {
                                // no test, release
                                session.release();
                            }
                        } else {
                            if (checkSession(session)) {
                                pool.removeElement(session);
                                wrongSessions.addElement(session);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("DatabaseSessionPool: mark damaged session as wrong because test SQL failed: " + session.toString());
                                }
                            } else {
                                countReset++;
                                session.resetErrorStatus();
                                session.release();
                            }
                        }
                    }
                }
                // remove the idle sessions
                int countRemovedIdle = 0;
                for (int i = 0; i < idleSessions.size(); i++) {
                    // these sessions are already occupied
                    session = idleSessions.get(i);
                    session.setPooled(false);
                    session.close();
                    countRemovedIdle++;
                }
                idleSessions.clear();
                // close and replace the damaged sessions
                for (int i = 0; i < wrongSessions.size(); i++) {
                    session = wrongSessions.get(i);
                    countRemovedDamaged++;
                    session.setPooled(false);
                    session.close();
                }
                wrongSessions.clear();
                if ((countRemovedDamaged > 0) || (countRemovedIdle > 0) || (countReset > 0)) {
                    lastPoolCheckMessage = "INFO: checkPool: removed damaged:" + countRemovedDamaged + ", removed idle:" + countRemovedIdle + ", reset:" + countReset + ",  current pool size:" + pool.size();
                    logger.info(lastPoolCheckMessage);
                    lastOccuranceOfPoolCheckMessage = System.currentTimeMillis();
                }
                lastOccuranceOfPoolCheck = System.currentTimeMillis();
                checkInProcess = false;
            }
        }
    }

    /**
     * kill a particular session
     * @param sessionId ID of session
     */
    static void killSession(int sessionId) {
        final DatabaseSession session = getDatabaseSession(sessionId);
        pool.remove(session);
        session.close();
    }

    /**
     * tests if an connection to a particular database is used
     * @param alias name of the database
     * @return true if any connection is used
     */
    public static boolean isConnectionUsed(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("alias cannot be null");
        }
        boolean used = false;
        DatabaseSession session = null;
        for (int i = 0; i < pool.size(); i++) {
            session = pool.elementAt(i);
            // als erstes prüfen auf Untätigkeit
            if (session.isReady() == false && session.isSuccessful() && session.getAliasName().equalsIgnoreCase(alias)) {
                used = true;
                break;
            }
        }
        return used;
    }

    /**
     * tests if an connection to a particular database is used
     * @param alias name of the database
     * @return true if any connection is used
     */
    public static boolean isConnectionUsed() {
        boolean used = false;
        DatabaseSession session = null;
        for (int i = 0; i < pool.size(); i++) {
            session = pool.elementAt(i);
            // als erstes prüfen auf Untätigkeit
            if (session.isReady() == false && session.isSuccessful()) {
                used = true;
                break;
            }
        }
        return used;
    }

    /**
     * starts pool check thread
     * @param refreshTime time between two checks
     */
    static public void startCheckPoolThread(int refreshTimeInSeconds) {
        if ((refreshTimeInSeconds > 0) || (maxIdleTime < refreshTimeInSeconds)) {
            checkPoolCyclusTime = refreshTimeInSeconds;
            stopCheckPoolThread();
            timer = new Timer(true);
            checkPoolTask = new CheckPoolTask();
            timer.schedule(checkPoolTask, 1000, checkPoolCyclusTime * 1000);
            logger.info("DatabaseSessionPool: start pool check all " + checkPoolCyclusTime + "s, check max idle time=" + maxIdleTime + "s.");
        } else {
            logger.warn("DatabaseSessionPool: start pool check failed: invalid cyclus time or invalid max idle time !");
        }
    }
    
    public static boolean isPoolCheckThreadRunning() {
        return timer != null;
    }
    
    /**
     * starts pool check thread
     * @param refreshTime time between two checks
     */
    static public void startCheckPoolThread() {
        if (checkPoolCyclusTime > 0) {
            stopCheckPoolThread();
            timer = new Timer(true);
            checkPoolTask = new CheckPoolTask();
            timer.schedule(checkPoolTask, 1000, checkPoolCyclusTime * 1000);
            logger.info("DatabaseSessionPool: start pool check all " + checkPoolCyclusTime + "s, check max idle time=" + maxIdleTime + "s.");
        } else {
            logger.warn("checkPoolCyclusTime has invalid value (must be greater 0)");
        }
    }

    /**
     * @return cyclus time = time between pool checks
     */
    public static int getCheckPoolCyclusTime() {
        return checkPoolCyclusTime;
    }
    
    public static void setCheckPoolCyclusTime(int secondsBetweenChecks) {
        checkPoolCyclusTime = secondsBetweenChecks;
    }

    /**
     * @return expected timestamp of next pool check
     */
    public static long getPoolCheckScheduledExecutionTime() {
        if (checkPoolTask != null) {
            return checkPoolTask.scheduledExecutionTime();
        } else {
            return 0;
        }
    }

    /**
     * stops the pool check
     */
    static public void stopCheckPoolThread() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static long getLastOccuranceOfPoolCheckMessage() {
        return lastOccuranceOfPoolCheckMessage;
    }

    public static long getLastOccuranceOfPoolCheck() {
        return lastOccuranceOfPoolCheck;
    }

    public static String getLastPoolCheckMessage() {
        return lastPoolCheckMessage;
    }

    /**
     * @param session session to be checked
     * @return true if session is affected by some problems and should be removed/replaced
     */
    static public boolean checkSession(DatabaseSession session) {
        boolean inValid = false;
        String testSql = testSQLs.get(session.getAliasName());
        if (testSql == null) {
            testSql = UNIVERSAL_TEST_SQL;
        }
        final ResultSet rs = session.executeQuery(testSql);
        if (session.isSuccessful()) {
            try {
                rs.close();
            } catch (Exception e) {
                inValid = true;
                logger.warn("ERROR: checkPool: db alias:" + session.getAliasName() + ", test statement: " + testSql + " execution failed (handling with result set):" + e.toString());
            }
        } else {
            inValid = true;
            logger.warn("ERROR: checkPool: db alias:" + session.getAliasName() + ", test statement: " + testSql + " execution failed (executing sql):" + session.getLastErrorMessage());
        }
        return inValid;
    }

    /**
     * @return last error message
     */
    static public String getErrorMessage() {
        return lastErrorMessage;
    }
    
    /**
     * returns a session for a alias
     * @param aliasName alias is a short name for a description of a particular connection
     * @return DatabaseSession a session (a new one (will be if no other - in this case the pool is growing) is free or a session from pool)
     */
    static public DatabaseSession getDatabaseSession(String aliasName) {
        return getDatabaseSession(aliasName, null);
    }
    
    /**
     * returns a session for a alias
     * @param aliasName alias is a short name for a description of a particular connection
     * @param alternativeClass alternative subclass from DatabaseSession. Normaly a subclass from DatabaseSession is already defined. In circumstances it can be
     * that an different class is needed. In this case, internally a new alias will be created and added to pool configuration.
     * alternativeClass can be null
     * @return DatabaseSession a session (a new one (will be if no other - in this case the pool is growing) is free or a session from pool)
     */
    static public DatabaseSession getDatabaseSession(String aliasName, Class<? extends DatabaseSession> alternativeClass) {
        if (aliasName == null) {
            throw new IllegalArgumentException("aliasName cannot be null");
        }
        DatabaseSession session = null;
        do {
            String alternativeAliasName = null;
            if (alternativeClass != null) {
                alternativeAliasName = aliasName + "_" + alternativeClass.getName();
            } else {
                alternativeAliasName = aliasName;
            }
            int sessionCount = 0;
            for (int i = 0; i < pool.size(); i++) {
                synchronized (monitor) {
                    session = pool.elementAt(i);
                    if (alternativeAliasName.equals(session.getAliasName())) {
                        sessionCount++;
                        if (session.isReady()) {
                            session.occupy(); // gather
                            break; // wwe are ready
                        } else {
                            session = null;
                        }
                    } else {
                        session = null;
                    }
                }
            }
            if (session == null) {
                // create a new one but using the original ConnectionDescription
                // assuming, that this kind of connection is formaly created
                final ConnectionDescription cd = getConnectionDescriptionFromMap(aliasName);
                if (cd != null) {
                    if (cd.getMaxCountSessions() == 0 || sessionCount < cd.getMaxCountSessions()) {
                        String className = null;
                        try {
                            if (alternativeClass != null) {
                                className = alternativeClass.getName();
                                if (aliasConnectionClassMap.containsKey(alternativeAliasName.toLowerCase()) == false) {
                                    aliasConnectionClassMap.put(alternativeAliasName.toLowerCase(), alternativeClass.getName());
                                }
                            } else {
                                className = aliasConnectionClassMap.get(aliasName.toLowerCase());
                                if (className == null) {
                                    className = DatabaseSession.class.getName();
                                }
                            }
                            session = (DatabaseSession) Class.forName(className).newInstance();
                        } catch (Exception e) {
                            error("instantiation (add) of DatabaseSession subclass " + className + " for alias=" + alternativeAliasName + " failed:" + e.toString());
                            break;
                        }
                        if (session != null) {
                            session.setConnectionDescription(cd);
                            session.setAliasName(alternativeAliasName);
                            if (session.loadDriver() && session.connect()) {
                                if (prepareStatements(session)) {
                                    logger.info("DatabaseSessionPool: add (ondemand) " + session.toString());
                                    session.setPooled(true);
                                    session.occupy();
                                    pool.addElement(session);
                                }
                            } else {
                                error("failed to add DatabaseSession (alias=" + alternativeAliasName + "):" + session.getLastErrorMessage());
                                break;
                            }
                        }
                    }
                } else {
                    error("getDatabaseSession: unknown db-alias=" + alternativeAliasName);
                    break;
                }
            }
        } while (session == null);
        if (session != null && session.isConnected() && session.isSuccessful()) {
            return session;
        } else {
            return null;
        }
    }

    /**
     * get a message by id (not to use in applications)
     * this session will NOT be occupied !!
     * @param id
     * @return session
     */
    static DatabaseSession getDatabaseSession(int id) {
        DatabaseSession session = null;
        for (int i = 0; i < pool.size(); i++) {
            session = pool.elementAt(i);
            if (session.getSessionID() == id) {
                break;
            } else {
                session = null;
            }
        }
        return session;
    }

    /**
     * returns the connection description for an alias
     * @param alias name of connection
     * @return description of connection
     */
    public static ConnectionDescription getConnectionDescription(String alias) {
        return getConnectionDescriptionFromMap(alias);
    }
    
    /**
     * release a DatabaseSession
     * @param session to be released
     */
    static public void release(DatabaseSession session) {
        if (session.isSuccessful() == false) {
            lastErrorMessage = session.getLastErrorMessage();
            pool.remove(session);
            session.close();
        }
        session.release();
    }

    /**
     * release a DatabaseSession
     * @param session to be released
     */
    static public void close(DatabaseSession session) {
        session.release();
        lastErrorMessage = session.getLastErrorMessage();
        pool.remove(session);
        session.close();
    }

    /**
     * creates prepared statements
     * @param session to be prepared with statements
     * @return true if all prepared statements successfully created
     */
    static public boolean prepareStatements(DatabaseSession session) {
        boolean ok = true;
        final HashMap<String, String> statMap = sessionStatementMap.get(session.getAliasName());
        if (statMap != null && statMap.isEmpty() == false) {
            Map.Entry<String, String> entry = null;
            for (Iterator<Map.Entry<String, String>> it = statMap.entrySet().iterator(); it.hasNext();) {
                entry = it.next();
                if (session.isReady()) {
                    session.occupy();
                    if (session.createPreparedStatement(entry.getKey(), entry.getValue())) {
                        session.release();
                    } else {
                        ok = false;
                        error("DatabaseSessionPool.prepareStatements for session (id=" 
                            + session.getSessionID() 
                            + " sessionAlias=" 
                            + session.getAliasName() 
                            + ") and statementAlias=" 
                            + entry.getKey() 
                            + " failed: " 
                            + session.getLastErrorMessage());
                    }
                } else {
                    ok = false;
                }
            }
        } else {
            ok = true;
        }
        return ok;
    }

    static Vector<DatabaseSession> getPool() {
        return pool;
    }

    public static void addConnectionDescription(String alias, String cryptedParameter) throws Exception {
        ConnectionDescription cd = ConnectionDescription.create(alias, cryptedParameter);
        addConnectionDescription(alias, cd);
    }
    
    /**
     * initialized the internal connection descriptions
     * when getDatabaseSession is called, a new session can be created
     * because all neccessary information are available at this point
     * This method is not useful, if a particular subclass of DatabaseSession is needed.
     * @param alias
     * @param driverClassName
     * @param url
     * @param user
     * @param password
     * @param maxConnections
     * @param fetchSize size of preloaded rows in result sets
     * @param autoCommit auto commit stat of connection
     */
    public static void addConnectionDescription(
        String alias, 
        String driverClassName, 
        String url, 
        String user, 
        String password,
        int maxConnections,
        int fetchSize,
        boolean autoCommit) {
        final ConnectionDescription cd = new ConnectionDescription();
        cd.setDriverClassName(driverClassName);
        cd.setUrl(url);
        cd.setUser(user);
        cd.setPasswd(password);
        cd.setMaxCountSessions(maxConnections);
        cd.setDefaultFetchSize(fetchSize);
        cd.setAutoCommit(autoCommit);
        addConnectionDescription(alias, cd);
    }
    
    public static void addConnectionDescription(String alias, ConnectionDescription connDesc) {
        if (existDatabaseSession(alias)) {
            throw new IllegalStateException("alias " + alias + " already exists !");
        } else {
            connDescMap.put(alias, connDesc);
            logger.info("add connection alias=" + alias + " with " + connDesc.toString());
        }
    }
    
    public static void removeAllConnectionDescriptions() {
        connDescMap.clear();
        logger.info("remove all connection descriptions");
    }
    
    public static void removeConnectionDescription(String alias) {
        connDescMap.remove(alias);
        logger.info("remove connection description for alias=" + alias);
    }
    
    public static boolean hasConfigurations() {
        return connDescMap.isEmpty() == false;
    }
    
    public static String[] getAllAliases() {
        String[] aliasArray = new String[connDescMap.size()];
        int i = 0;
        for (String alias : connDescMap.keySet()) {
            aliasArray[i] = alias.toLowerCase();
            i++;
        }
        return aliasArray;
    }
    
    public static String[] getSessionsStatus() {
        String[] statusArray = new String[getPoolSize()];
        for (int i = 0, n = getPoolSize(); i < n; i++) {
            DatabaseSession session = pool.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(session.toString());
            sb.append(" in use=");
            sb.append(session.isOccupied());
            sb.append(" successful=");
            sb.append(session.isSuccessful());
            statusArray[i] = sb.toString();
        }
        return statusArray;
    }
    
    public static int getPoolSize() {
        return pool.size();
    }
    
    public static void registerAtPlatformJMXServer() throws Exception {
        if (System.getProperty("com.sun.management.jmxremote") == null) {
            logger.warn("System property: com.sun.management.jmxremote is not set, probably current VM is not enabled for JMX.");
        }
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        registerAtJMXServer(mbs);
    }
    
    public static void registerAtJMXServer(MBeanServer mbs) throws Exception {
        ObjectName beanName = new ObjectName(DatabaseSessionPoolInfo.class.getPackage().getName()+":type="+DatabaseSessionPoolInfo.class.getSimpleName());
        DatabaseSessionPoolInfo poolBean = new DatabaseSessionPoolInfo();
        if (mbs.isRegistered(beanName) == false) {
            mbs.registerMBean(poolBean, beanName);
        }
    }
    
    private static class ConnectionMap extends HashMap<String, ConnectionDescription> {

		private static final long serialVersionUID = 1L;

		@Override
    	public ConnectionDescription put(String key, ConnectionDescription cd) {
    		return super.put(key.toLowerCase(), cd);
    	}
    	
    	@Override
    	public ConnectionDescription remove(Object key) {
    		return super.remove(key.toString().toLowerCase());
    	}
    	
    	@Override
    	public ConnectionDescription get(Object key) {
    		return super.get(key.toString().toLowerCase());
    	}
    	
    }
    
}
