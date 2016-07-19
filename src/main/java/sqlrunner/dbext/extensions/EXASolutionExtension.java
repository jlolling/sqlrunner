package sqlrunner.dbext.extensions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import dbtools.DatabaseSession;
import dbtools.SQLStatement;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.dbext.GenericDatabaseExtension;

public class EXASolutionExtension extends GenericDatabaseExtension {
	
	private Logger logger = Logger.getLogger(EXASolutionExtension.class);
	private static final String driverClassName = "com.exasol.jdbc.EXADriver";
	
	public EXASolutionExtension() {
        addDriverClassName(driverClassName);
        addSQLKeyword("flush statistics");
        addSQLKeyword("distribute");
        addSQLKeyword("profile");
        addSQLDatatype("geometry");
        addSQLDatatype("timestamp with local time zone");
        addSQLDatatype("interval year to month");
        addSQLDatatype("interval day to second");
	}

	@Override
	public String setupViewSQLCode(DatabaseSession session, SQLTable table) {
		StringBuilder sb = new StringBuilder();
		sb.append("select VIEW_TEXT from SYS.EXA_ALL_VIEWS where VIEW_SCHEMA='");
		sb.append(table.getName().toUpperCase());
		sb.append("' and VIEW_NAME='");
		sb.append(table.getSchema().getName().toUpperCase());
		sb.append("'");
		ResultSet rs = session.executeQuery(sb.toString());
		if (session.isSuccessful()) {
			String code = null;
			try {
				if (rs.next()) {
					code = rs.getString(1);
				}
				rs.close();
				if (code != null && code.length() > 1) {
					table.setSourceCode(code);
				}
			} catch (SQLException e) {
				logger.error("setupViewSQLCode for view=" + table.getName() + " failed:" + e.getMessage(), e);
			} 
		}
		return sb.toString();
	}

	@Override
	public String setupProcedureSQLCode(DatabaseSession session, SQLProcedure proc) {
		StringBuilder sb = new StringBuilder();
		sb.append("select FUNCTION_TEXT from SYS.EXA_ALL_FUNCTIONS where FUNCTION_NAME='");
		sb.append(proc.getName().toUpperCase());
		sb.append("' and FUNCTION_SCHEMA='");
		sb.append(proc.getSchema().getName().toUpperCase());
		sb.append("'");
		ResultSet rs = session.executeQuery(sb.toString());
		if (session.isSuccessful()) {
			String code = null;
			try {
				if (rs.next()) {
					code = rs.getString(1);
				}
				rs.close();
				if (code != null && code.length() > 2) {
					proc.setCode(code);
				}
			} catch (SQLException e) {
				logger.error("setupProcedureSQLCode for proc=" + proc.getName() + " failed:" + e.getMessage(), e);
			} 
		}
		return sb.toString();
	}

	@Override
	public boolean hasExplainFeature() {
		return true;
	}

	@Override
	public String getExplainSQL(String currentStatement) {
		if (currentStatement != null) {
			currentStatement = currentStatement.trim();
			StringBuilder sb = new StringBuilder();
			sb.append("alter session set profile='on';");
			sb.append(SQLStatement.ignoreResultSetComment + "\n");
			sb.append(currentStatement);
			if (currentStatement.endsWith(";") == false) {
				sb.append(";\n");
			}
			sb.append("alter session set profile='off';\n");
			sb.append("flush statistics;\n");
			sb.append("select * from EXA_STATISTICS.EXA_USER_PROFILE_LAST_DAY\n");
			sb.append("where session_id = current_session order by stmt_id desc;");
			return sb.toString();
		} else {
			return "";
		}
	}

}