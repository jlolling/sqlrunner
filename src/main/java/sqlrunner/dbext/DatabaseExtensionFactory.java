package sqlrunner.dbext;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import dbtools.ConnectionDescription;
import sqlrunner.datamodel.SQLObject;
import sqlrunner.dbext.extensions.DB2Extension;
import sqlrunner.dbext.extensions.DerbyExtension;
import sqlrunner.dbext.extensions.EXASolutionExtension;
import sqlrunner.dbext.extensions.MSSqlExtension;
import sqlrunner.dbext.extensions.MySQLExtension;
import sqlrunner.dbext.extensions.OracleExtension;
import sqlrunner.dbext.extensions.PostgresqlExtension;
import sqlrunner.dbext.extensions.TeradataExtension;

public class DatabaseExtensionFactory {
	
	private static final Logger logger = Logger.getLogger(DatabaseExtensionFactory.class);
	private static List<DatabaseExtension> listExtensions = new ArrayList<DatabaseExtension>();
	private static DatabaseExtension genericExtension = new GenericDatabaseExtension();
	
	public static DatabaseExtension getGenericDatabaseExtension() {
		return genericExtension;
	}
	
	public static synchronized DatabaseExtension getDatabaseExtension(SQLObject object) {
		if (object == null) {
			return genericExtension;
		}
		return getDatabaseExtension(object.getModel().getConnectionDescription());
	}
	
	public static synchronized DatabaseExtension getDatabaseExtension(ConnectionDescription cd) {
		if (cd == null) {
			return genericExtension;
		}
		init();
		for (DatabaseExtension ext : listExtensions) {
			if (ext.isApplicable(cd.getDriverClassName())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Use DatabaseExtension: " + ext.getClass().getCanonicalName() + " for conn desc: " + cd.toString());
				}
				return ext;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Use default DatabaseExtension: " + genericExtension.getClass().getCanonicalName());
		}
		return genericExtension;
	}
	
	public static synchronized DatabaseExtension getDatabaseExtension(String driverClass) {
		if (driverClass == null) {
			return genericExtension;
		}
		init();
		for (DatabaseExtension ext : listExtensions) {
			if (ext.isApplicable(driverClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Use DatabaseExtension: " + ext.getClass().getCanonicalName() + " for driverClass: " + driverClass);
				}
				return ext;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Use default DatabaseExtension: " + genericExtension.getClass().getCanonicalName() + " for driverClass: " + driverClass);
		}
		return genericExtension;
	}

	private static void init() {
		if (listExtensions.size() == 0) {
			listExtensions.add(new PostgresqlExtension());
			listExtensions.add(new OracleExtension());
			listExtensions.add(new DB2Extension());
			listExtensions.add(new DerbyExtension());
			listExtensions.add(new MySQLExtension());
			listExtensions.add(new MSSqlExtension());
			listExtensions.add(new TeradataExtension());
			listExtensions.add(new EXASolutionExtension());
		}
	}

}
