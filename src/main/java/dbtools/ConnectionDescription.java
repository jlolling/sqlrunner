package dbtools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import sqlrunner.base64.StringCrypt;

/**
 * encapsulate all describing-informations for a database-connection
 */
public class ConnectionDescription implements Comparable<ConnectionDescription>, Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private String url;
	private String driverClassName;
	private String user;
	private String password;
	private String comment;
	private String propertiesString;
	private String initSQL;
	private static final boolean userInfoInUrl = false;
	/** Oracle Thin-driver */
	static public final int ORACLE_THIN = 0;
	/** Oracle-OCI-driver that use entries in the client-local TNSNAMES.ORA */
	static public final int ORACLE_OCI_USE_LOCAL_TNSNAMES = 1;
	/** Oracle-OCI-driver who specify a separate Net8-keyword-value pair */
	static public final int ORACLE_OCI_WITHOUT_LOCAL_TNSNAMES = 2;
	/** ODBC-driver */
	static public final int ODBC = 3;
	private static final List<DatabaseType> databaseTypes = new ArrayList<DatabaseType>();
	static private boolean showCommentAsDefault = false;
	static private boolean extendsUrlWithProperties = false;
	static private boolean runInitialSQLOnConnect = false;
	private DatabaseType dt;
	private boolean storePasswdEnabled = true;
	private List<URLElement> urlElements;
	private boolean autoCommit = true;
	private String jndiDataSourceName = null;
	private int defaultFetchSize = -1;
    private int maxCountSessions = 0;
    private boolean productive = false;

    public int getMaxCountSessions() {
        return maxCountSessions;
    }

    public void setMaxCountSessions(int maxCountSessions) {
        this.maxCountSessions = maxCountSessions;
    }

	/**
	 * Constructor ConnectionDescription
	 */
	public ConnectionDescription() {
		createDefaults();
	}
	
	public void setProperties(Properties properties) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			sb.append((String) entry.getKey());
			sb.append("=");
			sb.append((String) entry.getValue());
			sb.append(";");
		}
		setPropertiesString(sb.toString());
	}

	/**
	 * Constructor ConnectionDescription
	 * 
	 * @param paramStr
	 *            enthält die konrete Beschreibung einer Connection
	 */
	public ConnectionDescription(String paramStr) {
		createDefaults();
		parseParamStr(paramStr);
		createURL();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url_loc) {
		this.url = url_loc;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName_loc) {
		this.driverClassName = driverClassName_loc;
	}

	public void setAutoCommit(boolean autoCommit_loc) {
		this.autoCommit = autoCommit_loc;
	}

	public boolean getAutoCommit() {
		return autoCommit;
	}

	public void setDatabaseType(DatabaseType dt_loc) {
		if (this.dt != dt_loc) {
			this.dt = dt_loc;
			this.urlElements = dt_loc.cloneURLElementList();
			this.driverClassName = dt_loc.getDriverClassName();
		}
	}

	public void setDatabaseType(int id) {
		final DatabaseType dt_loc = getDatabaseType(id);
		if (dt_loc == null) {
			throw new RuntimeException("kein Datenbanktyp mit id="
					+ id
					+ " vorhanden!");
		}
		if (this.dt != dt_loc) {
			this.dt = dt_loc;
			this.urlElements = dt_loc.cloneURLElementList();
			this.driverClassName = dt_loc.getDriverClassName();
		}
	}

	public DatabaseType getDatabaseType() {
		return dt;
	}

	public String getUser() {
		if (urlElements != null) {
			String userFormURLElements = null;
			for (int i = 0; i < urlElements.size(); i++) {
				if (urlElements.get(i).getName()
						.equals(URLElement.USER_NAME)) {
					userFormURLElements = urlElements.get(i).getValue();
					break;
				}
			}
			if (userFormURLElements != null) {
				return userFormURLElements;
			} else {
				return user;
			}
		} else {
			return user;
		}
	}

	public String getPasswd() {
		if ((urlElements != null) && (urlElements.size() > 0)) {
			String passwdFromUrlElement = null;
			for (int i = 0; i < urlElements.size(); i++) {
				if (urlElements.get(i).getName()
						.equals(URLElement.PASSWORD_NAME)) {
					passwdFromUrlElement = urlElements.get(i).getValue();
					break;
				}
			}
			if (passwdFromUrlElement != null) {
				return passwdFromUrlElement;
			} else {
				return password;
			}
		} else {
			return password;
		}
	}

	public void setUser(String user_loc) {
		if ((urlElements != null) && (urlElements.size() > 0)) {
			URLElement element;
			for (int i = 0; i < urlElements.size(); i++) {
				element = urlElements.get(i);
				if (element.getName().equals(URLElement.USER_NAME)) {
					element.setValue(user_loc);
					break;
				}
			}
		}
		this.user = user_loc;
	}

	public void setPasswd(String password_loc) {
		if ((urlElements != null) && (urlElements.size() > 0)) {
			URLElement element;
			for (int i = 0; i < urlElements.size(); i++) {
				element = urlElements.get(i);
				if (element.getName().equals(URLElement.PASSWORD_NAME)) {
					element.setValue(password_loc);
					break;
				}
			}
		}
		this.password = password_loc;
	}

	public void setComment(String comment_loc) {
		this.comment = comment_loc;
	}

	public void setPropertiesString(String text) {
		this.propertiesString = text;
	}

	public String getComment() {
		return comment;
	}

	public void setDefaultFetchSize(int size) {
		this.defaultFetchSize = size;
	}

	public int getDefaultFetchSize() {
		return defaultFetchSize;
	}

	public String getPropertiesString() {
		return propertiesString;
	}

	@SuppressWarnings("unchecked")
	public void setURLElements(ArrayList<URLElement> urlElements_loc) {
		this.urlElements = (ArrayList<URLElement>) urlElements_loc.clone();
	}

	public List<URLElement> getURLElements() {
		return urlElements;
	}

	private void storeURLElementValue(URLElement element) {
		if (element.getName().equals(URLElement.PRODUCTIVE)) {
			productive = Boolean.parseBoolean(element.getValue());
		} else if (element.getName().equals(URLElement.COMMENT)) {
			comment = element.getValue();
		} else if (element.getName().equals(URLElement.INIT_SQL)) {
			initSQL = element.getValue();
		} else if (element.getName().equals(URLElement.PROPERTIES)) {
			propertiesString = element.getValue();
		} else if (element.getName().equals(URLElement.FETCHSIZE)) {
			String value = element.getValue();
			if (value != null && value.length() > 0) {
				defaultFetchSize = Integer.parseInt(value);
			} else {
				defaultFetchSize = -1;
			}
		} else {
			URLElement urlElement;
			for (int i = 0; i < urlElements.size(); i++) {
				urlElement = urlElements.get(i);
				if (urlElement.equals(element)) {
					urlElement.setValue(element.getValue());
	                if (element.isUserNameElement()) {
	                    user = element.getValue();
	                }
					break;
				}
			}
		}
	}

	private void parseParamStr(String param) {
		// nun an Hand des ersten Parameters den DatabaseType ermitteln
		// type
		int p0 = param.indexOf('|');
		// DatabaseType holen
		dt = getDatabaseType(Integer.parseInt(param.substring(0, p0)));
		if (dt != null) {
			driverClassName = dt.getDriverClassName();
			// die URLElements vorfüllen
			urlElements = new ArrayList<URLElement>();
			for (int i = 0; i < dt.getURLElementeCount(); i++) {
				urlElements.add(new URLElement((URLElement) dt.getURLElementAt(i)));
			}
		} else {
			throw new IllegalStateException("parseParamStr can only work within SQLRunner. It needs DatabaseTypes!");
		}
		// driverClassName setzen
		int p1;
		p0++;
		boolean ready = false;
		while (!ready) {
			p1 = param.indexOf('|', p0);
			if (p1 == -1) {
				ready = true;
				break;
			} else {
				storeURLElementValue(new URLElement(param.substring(p0, p1)));
				p0 = p1 + 1;
			}
		}
		storeURLElementValue(new URLElement(param.substring(p0, param.length())));
	}

	/**
	 * erzeugt aus den URLElements die URL für die DB-Connection
	 */
	public void createURL() {
		String urlTemp = dt.getUrlTemplate();
		String name;
		String value;
		int p0;
		int p1;
		for (int i = 0; i < urlElements.size(); i++) {
			name = urlElements.get(i).getName();
			value = urlElements.get(i).getValue();
			p0 = urlTemp.indexOf("%" + name);
			if (p0 != -1) { // kann ja sein, dass ein Bestandteil nicht
							// erforderlich ist !!
				p1 = urlTemp.indexOf("%", p0 + 1);
				urlTemp = urlTemp.substring(0, p0)
						+ value
						+ urlTemp.substring(p1 + 1, urlTemp.length());
			}
		}
		url = urlTemp;
	}

	public void setStorePasswdEnabled(boolean enable) {
		this.storePasswdEnabled = enable;
	}

	public boolean getStorePasswdEnabled() {
		return storePasswdEnabled;
	}

	/**
	 * creates a string-presentation of these object to stores in config-files
	 * es werden die exakten Bestandteile der URLElements ausgegeben
	 * 
	 * @return string contains all parameters
	 */
	public String getParamStr() {
		if (dt == null || urlElements == null || urlElements.size() == 0) {
			throw new IllegalStateException("parseParamStr can only be used within SQLRunner because it need DatabaseTypes");
		}
		final StringBuilder param = new StringBuilder();
		param.append(String.valueOf(dt.getId()));
		param.append('|');
		// dann die URLElemente
		URLElement element;
		for (int i = 0; i < urlElements.size(); i++) {
			element = urlElements.get(i);
			if (storePasswdEnabled) {
				param.append(element.getParamStr());
			} else {
				if (element.getParamStr().indexOf(URLElement.PASSWORD_NAME) != -1) {
					param.append(URLElement.PASSWORD_NAME + "=");
				} else {
					param.append(element.getParamStr());
				}
			}
			if (i < urlElements.size() - 1) {
				param.append('|');
			}
		}
		param.append('|');
		param.append(URLElement.PRODUCTIVE);
		param.append("=");
		param.append(productive);
		if (initSQL != null && initSQL.length() > 0) {
			param.append('|');
			param.append(URLElement.INIT_SQL);
			param.append("=");
			param.append(initSQL);
		}
		if (comment != null && comment.length() > 0) {
			param.append('|');
			param.append(URLElement.COMMENT);
			param.append("=");
			param.append(comment);
		}
		if (propertiesString != null && propertiesString.length() > 0) {
			param.append('|');
			param.append(URLElement.PROPERTIES);
			param.append("=");
			param.append(propertiesString);
		}
		if (defaultFetchSize > 0) {
			param.append('|');
			param.append(URLElement.FETCHSIZE);
			param.append("=");
			param.append(defaultFetchSize);
		}
		return param.toString();
	}

	/**
	 * Method toString create a sting-representation of these object
	 * 
	 * @return string-representation
	 */
    @Override
	public String toString() {
		if (showCommentAsDefault) {
			final String comment_loc = getComment();
			if ((comment_loc != null) && (comment_loc.length() > 0)) {
				return getComment();
			} else {
				return url + " AS USER " + getUser();
			}
		} else {
			final String propStr = getPropertiesString();
			if (propStr != null && propStr.length() > 0) {
				if (extendsUrlWithProperties) {
					return url + "?" + propStr + " AS USER " + getUser();
				} else {
					return url + " [" + propStr + "] AS USER " + getUser();
				}
			} else {
				return url + " AS USER " + getUser();
			}
		}
	}

	/**
	 * Check for valid Attributes to create a DatabaseSession
	 * 
	 * @return true if all Attributes to create a DatabaseSession are complete.
	 */
	public boolean hasInitdata() {
		boolean ok = false;
		if ((url != null) && (driverClassName != null)) {
			ok = true;
		}
		return ok;
	}

	/**
	 * check for valid user-data
	 * 
	 * @return true if ok
	 */
	public boolean hasUserdata() {
		if ((getUser() != null) && (getPasswd() != null)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Comparing two ConnectionDescriptions.
	 * 
	 * @param cd
	 *            zu vergleichende ConnectionDescription
	 * @return true if objects describes the same connection
	 */
    @Override
	public boolean equals(Object o) {
		if (o instanceof ConnectionDescription) {
			final ConnectionDescription cd = (ConnectionDescription) o;
			if (driverClassName.equals(cd.driverClassName) == false) {
				return false;
			}
			if (getUniqueId().equals(cd.getUniqueId())) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

    @Override
	public int hashCode() {
		return (driverClassName + url + getUser()).hashCode();
	}

	public String getUniqueId() {
		return driverClassName + "_" + url + "_" + getUser();
	}

	// public int compareTo(Object object) {
	// }

	static public List<DatabaseType> getDBTypes() {
		return databaseTypes;
	}

	static public List<DatabaseType> getDatabaseTypes() {
		createDefaults(); // erzeugt nur wenn Vector leer defaults !!
		return databaseTypes;
	}

	static public synchronized void createDefaults() {
		if (databaseTypes.size() == 0) {
			databaseTypes.add(new DatabaseType(0,
					"Oracle THIN",
					"oracle.jdbc.driver.OracleDriver",
					"jdbc:oracle:thin:@%HOST%:%PORT=1521%:%SID%"));
			databaseTypes.add(new DatabaseType(1,
					"Oracle OCI (use local TNSNAMES)",
					"oracle.jdbc.driver.OracleDriver",
					"jdbc:oracle:oci8:@%SERVICENAME%"));
			databaseTypes.add(new DatabaseType(2,
					"Oracle OCI (self created TNSNAMES-entry)",
					"oracle.jdbc.driver.OracleDriver",
					"jdbc:oracle:oci8:@(description=(address=(host=%HOST%)(protocol=tcp)(port=%PORT=1521%))(connect_data=(sid=%SID%)))"));
			databaseTypes.add(new DatabaseType(3,
					"ODBC",
					"sun.jdbc.odbc.JdbcOdbcDriver",
					"jdbc:odbc:%DATASOURCE%"));
		}
	}

	static public synchronized DatabaseType getDatabaseType(int id) {
		DatabaseType databaseType = null;
		boolean gefunden = false;
		for (int i = 0; i < databaseTypes.size(); i++) {
			databaseType = databaseTypes.get(i);
			if (id == databaseType.getId()) {
				gefunden = true;
				break;
			}
		}
		if (gefunden) {
			return databaseType;
		} else {
			return new DatabaseType(-1,
					"unbekannter Typ:" + String.valueOf(id),
					"UNBEKANNT",
					"UNBEKANNT");
		}
	}

	public String getURLElementValue(String urlElementName) {
		for (URLElement elem : urlElements) {
			if (urlElementName.equals(elem.getName())) {
				return elem.getValue();
			}
		}
		return null;
	}


	public static final boolean isShowCommentAsDefault() {
		return showCommentAsDefault;
	}

	public static final void setShowCommentAsDefault(boolean showCommentAsDefault) {
		ConnectionDescription.showCommentAsDefault = showCommentAsDefault;
	}

	public static final boolean isUserInfoInUrl() {
		return userInfoInUrl;
	}

	public static final boolean isExtendsUrlWithProperties() {
		return extendsUrlWithProperties;
	}

	public static final void setExtendsUrlWithProperties(boolean extendsUrlWithProperties) {
		ConnectionDescription.extendsUrlWithProperties = extendsUrlWithProperties;
	}

	public final String getJndiDataSourceName() {
		return jndiDataSourceName;
	}

	public final void setJndiDataSourceName(String jndiDataSourceName) {
		this.jndiDataSourceName = jndiDataSourceName;
	}

	public String getDatabasePropertyString() {
		final StringBuffer sb = new StringBuffer();
		sb.append("DRIVER=");
		sb.append(driverClassName);
		sb.append('\n');
		sb.append("URL=");
		sb.append(url);
		sb.append('\n');
		sb.append("USER=");
		sb.append(getUser());
		sb.append('\n');
		sb.append("PASSWORD=");
		sb.append(getPasswd());
		sb.append('\n');
		return sb.toString();
	}

	public String getInitSQL() {
		if ((urlElements != null) && (urlElements.size() > 0)) {
			String sql = null;
			for (int i = 0; i < urlElements.size(); i++) {
				if (urlElements.get(i)
						.getName()
						.equals(URLElement.INIT_SQL)) {
					sql = urlElements.get(i).getValue();
					break;
				}
			}
			if (sql == null) {
				return this.initSQL;
			} else {
				return sql;
			}
		} else {
			return this.initSQL;
		}
	}

	public void setInitSQL(String sql) {
		if ((urlElements != null) && (urlElements.size() > 0)) {
			for (int i = 0; i < urlElements.size(); i++) {
				if (urlElements.get(i)
						.getName()
						.equals(URLElement.INIT_SQL)) {
					urlElements.get(i).setValue(sql);
					break;
				}
			}
		}
		initSQL = sql;
	}

	public static final boolean isRunInitialSQLOnConnect() {
		return runInitialSQLOnConnect;
	}

	public static final void setRunInitialSQLOnConnect(boolean runInitialSQLOnConnect) {
		ConnectionDescription.runInitialSQLOnConnect = runInitialSQLOnConnect;
	}

	public int compareTo(ConnectionDescription cd) {
		int compareValue = this.url.compareTo(cd.getUrl());
		if (compareValue == 0) {
			compareValue = getUser().compareTo(cd.getUser());
		}
		if (compareValue == 0) {
			compareValue = driverClassName.compareTo(cd.getDriverClassName());
		}
		return compareValue;
	}
    
    /**
     * creates a new ConnectioonDescription from crypted connection parameter string
     * @param encryptedAccessData
     * @param passPhrase
     */
    public static ConnectionDescription create(String encryptedAccessData, String passPhrase) throws Exception {
        String paramStr = StringCrypt.decryptFromBase64(encryptedAccessData, passPhrase);
        return new ConnectionDescription(paramStr);
    }

    /**
     * build an crypt connection parameter string
     * @param passPhrase
     * @return
     * @throws java.lang.Exception
     */
    public String getEncryptedSetupData(String passPhrase) throws Exception {
        return StringCrypt.cryptToBcase64(getParamStr(), passPhrase);
    }
    
    @Override
    public ConnectionDescription clone() {
    	if (urlElements != null) {
            return new ConnectionDescription(getParamStr());
    	} else {
    		ConnectionDescription clone = new ConnectionDescription();
    		clone.setUrl(getUrl());
    		clone.setUser(getUser());
    		clone.setPasswd(getPasswd());
    		clone.setDriverClassName(getDriverClassName());
    		clone.setAutoCommit(getAutoCommit());
    		clone.setComment(getComment());
    		clone.setDefaultFetchSize(getDefaultFetchSize());
    		clone.setInitSQL(getInitSQL());
    		clone.setPropertiesString(getPropertiesString());
    		clone.setProductive(isProductive());
    		return clone;
    	}
    }

	public boolean isProductive() {
		return productive;
	}

	public void setProductive(boolean productive) {
		this.productive = productive;
	}

}