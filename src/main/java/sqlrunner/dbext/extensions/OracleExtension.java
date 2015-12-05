package sqlrunner.dbext.extensions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sqlrunner.datamodel.Field;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLProcedure.Parameter;
import sqlrunner.datamodel.SQLSequence;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.dbext.GenericDatabaseExtension;
import sqlrunner.flatfileimport.BasicDataType;
import dbtools.DatabaseSession;

public class OracleExtension extends GenericDatabaseExtension {

	private static Logger logger = Logger.getLogger(OracleExtension.class);
	private static final String name = "Oracle Extension";

	public OracleExtension() {
		super();
		addDriverClassName("oracle.jdbc.driver.OracleDriver");
	}

	@Override
	public boolean hasExplainFeature() {
		return true;
	}

	@Override
	public String getExplainSQL(String currentStatement) {
		StringBuilder sb = new StringBuilder();
		sb.append("explain plan for\n");
		sb.append(currentStatement);
		sb.append(";\n");
		sb.append("select * from table(dbms_xplan.display());");
		return sb.toString();
	}

	@Override
	public String setupViewSQLCode(DatabaseSession session, SQLTable table) {
		StringBuilder sb = new StringBuilder();
		sb.append("select TEXT from USER_VIEWS where VIEW_NAME='");
		sb.append(table.getName().toUpperCase());
		sb.append("'");
		ResultSet rs = session.executeQuery(sb.toString());
		if (session.isSuccessful()) {
			StringBuilder code = new StringBuilder();
			try {
				if (rs.next()) {
					code.append("create or replace view ");
					code.append(table.getName());
					code.append(" as\n");
					code.append(rs.getString(1));
				}
				rs.close();
				if (code.length() > 1) {
					table.setSourceCode(code.toString());
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
		sb.append("select TEXT from user_source where NAME='");
		sb.append(proc.getName().toUpperCase());
		sb.append("' order by LINE");
		ResultSet rs = session.executeQuery(sb.toString());
		if (session.isSuccessful()) {
			StringBuilder code = new StringBuilder();
			try {
				boolean firstLoop = true;
				while (rs.next()) {
					if (firstLoop) {
						code.append("create or replace ");
						firstLoop = false;
					}
					code.append(rs.getString(1));
				}
				rs.close();
				if (code.length() > 1) {
					proc.setCode(code.toString());
				}
			} catch (SQLException e) {
				logger.error("setupProcedureSQLCode for proc=" + proc.getName() + " failed:" + e.getMessage(), e);
			} 
		}
		return sb.toString();
	}

	@Override
	public List<SQLSequence> getSequences(DatabaseSession session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setupDataType(Field field) {
		if ("integer".equalsIgnoreCase(field.getTypeName())) {
            field.setTypeSQLCode("integer");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("bigint".equalsIgnoreCase(field.getTypeName())) {
            field.setTypeSQLCode("bigint");
    		field.setBasicType(BasicDataType.LONG.getId());
        } else if ("number".equalsIgnoreCase(field.getTypeName())) {
        	if (field.getLength() == 22 && field.getDecimalDigits() == 0) {
        		// 22,0 does not means nothing!!
                field.setTypeSQLCode("number");
        	}
    		field.setBasicType(BasicDataType.DOUBLE.getId());
        }
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setupDataType(Parameter parameter) {
		// do nothing
	}

	@Override
	public List<String> getAdditionalSQLKeywords() {
		List<String> list = new ArrayList<String>();
		list.add("nvl");
		list.add("coalesce");
		list.add("last_day");
		return list;
	}

	@Override
	public List<String> getAdditionalSQLDatatypes() {
		List<String> list = new ArrayList<String>();
		return list;
	}

	@Override
	public List<String> getAdditionalProcedureKeywords() {
		List<String> list = new ArrayList<String>();
		return list;
	}

	@Override
	public boolean hasSQLLimitFeature() {
		return true;
	}

	@Override
	public String getLimitExpression(int max) {
		return "rownum <= " + max;
	}

	@Override
	public boolean isLimitExpressionAWhereCondition() {
		return true;
	}
	
	@Override
	public boolean isApplicable(String driverClass) {
		if (driverClass.toLowerCase().contains("oracle")) {
			return true;
		} else {
			return false;
		}
	}
	
}
