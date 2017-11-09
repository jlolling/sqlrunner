package sqlrunner.dbext;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dbtools.ConnectionDescription;
import sqlrunner.datamodel.Field;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLProcedure.Parameter;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLSequence;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.datamodel.SQLTrigger;
import sqlrunner.flatfileimport.BasicDataType;

public class GenericDatabaseExtension implements DatabaseExtension {

	private List<String> listkeywords = new ArrayList<String>();
	private List<String> listdatatypes = new ArrayList<String>();
	private List<String> listprockeywords = new ArrayList<String>();
	private List<String> listDriverClasses = new ArrayList<String>();
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat sdfTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
	private String quote = "\"";
	
	public GenericDatabaseExtension() {
		addSQLKeyword("user");
		addSQLKeywords("password");
	}

	public void addDriverClassName(String driverClass) {
		listDriverClasses.add(driverClass);
	}
	
	@Override
	public boolean isApplicable(ConnectionDescription cd) {
		return isApplicable(cd.getDriverClassName());
	}

	@Override
	public boolean isApplicable(String driverClass) {
		for (String dc : listDriverClasses) {
			if (dc.equalsIgnoreCase(driverClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasExplainFeature() {
		return false;
	}

	@Override
	public String getExplainSQL(String currentStatement) {
		return null;
	}

	@Override
	public String setupViewSQLCode(Connection conn, SQLTable table) {
		return null;
	}

	@Override
	public String setupProcedureSQLCode(Connection conn, SQLProcedure proc) {
		return null;
	}

	@Override
	public void setupDataType(Field field) {
		if ("integer".equalsIgnoreCase(field.getTypeName()) || "int".equalsIgnoreCase(field.getTypeName())) {
            field.setTypeSQLCode("integer");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("bigint".equalsIgnoreCase(field.getTypeName())) {
            field.setTypeSQLCode("bigint");
    		field.setBasicType(BasicDataType.LONG.getId());
        } else if ("smallint".equalsIgnoreCase(field.getTypeName())) {
            field.setTypeSQLCode("smallint");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("double".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("double");
    		field.setBasicType(BasicDataType.DOUBLE.getId());
        } else if ("bool".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("boolean");
    		field.setBasicType(BasicDataType.BOOLEAN.getId());
        }
	}

	@Override
	public String getName() {
		return "No Extension";
	}

	@Override
	public void setupDataType(Parameter parameter) {
		if ("integer".equalsIgnoreCase(parameter.getTypeName())) {
            parameter.setLength(0);
        } else if ("bool".equalsIgnoreCase(parameter.getTypeName())) {
        	parameter.setTypeName("boolean");
            parameter.setLength(0);
        }
	}
	
	protected void addSQLKeyword(String word) {
		listkeywords.add(word);
	}

	protected void addSQLKeywords(String ... words) {
		if (words != null && words.length > 0) {
			for (int i = 0; i < words.length; i++) {
				listkeywords.add(words[i]);
			}
		}
	}

	protected void addSQLDatatype(String word) {
		listdatatypes.add(word);
	}
	
	protected void addProcedureKeyword(String word) {
		listprockeywords.add(word);
	}
	
	@Override
	public List<String> getAdditionalSQLKeywords() {
		return listkeywords;
	}

	@Override
	public List<String> getAdditionalSQLDatatypes() {
		return listdatatypes;
	}

	@Override
	public List<String> getAdditionalProcedureKeywords() {
		return listprockeywords;
	}

	@Override
	public String getUpdateCommentStatement(String tableName, String comment) {
    	StringBuilder sb = new StringBuilder();
		if (comment != null && comment.trim().isEmpty() == false) {
			sb.append("comment on table ");
			sb.append(tableName);
			sb.append(" is '");
			sb.append(comment.replace("'", "''"));
			sb.append("'");
    	}
		return sb.toString();
	}

	@Override
	public String getUpdateCommentStatement(String tableName, String fieldName, String comment) {
		StringBuilder sb = new StringBuilder();
    	if (comment != null && comment.trim().isEmpty() == false) {
            sb.append("comment on column ");
            sb.append(tableName);
            sb.append(".");
            sb.append(fieldName);
            sb.append(" is '");
            sb.append(comment.replace('\n', ' ').replace("'", "''"));
            sb.append("'");
    	}
    	return sb.toString();
	}

	@Override
	public String getLoginSchema(ConnectionDescription cd) {
		return cd.getUser();
	}
	
	@Override
	public String getLoginSchema(Connection conn) {
		try {
			return conn.getMetaData().getUserName();
		} catch (SQLException e) {
			return "";
		}
	}

	@Override
	public boolean hasSQLLimitFeature() {
		return false;
	}

	@Override
	public String getLimitExpression(int max) {
		return "";
	}

	@Override
	public boolean isLimitExpressionAWhereCondition() {
		return false;
	}

	@Override
	public void setupConnection(Connection c) {
		// do nothing intentionally
	}
	
	@Override
	public void setupStatement(Statement stat) {
		// do nothing intentionally
	}

	@Override
	public String getDateToSQLExpression(Date value) {
		String template = getDateToSQLExpressionPattern();
		return template.replace("{date}", sdfDate.format(value));
	}

	@Override
	public String getDateToSQLExpressionPattern() {
		return "to_date('{date}','DD.MM.YYYY')";
	}
	
	@Override
	public String getTimestampToSQLExpression(Date value) {
		String template = getDateToSQLExpressionPattern();
		return template.replace("{date}", sdfTimestamp.format(value));
	}

	@Override
	public String getTimestampToSQLExpressionPattern() {
		return "to_date('{date}','DD.MM.YYYY')";
	}

	@Override
	public String getIdentifierQuoteString() {
		return quote;
	}

	@Override
	public void setIdentifierQuoteString(String quote) {
		if (quote != null && " ".equals(quote) == false) {
			this.quote = quote;
		}
	}

	@Override
	public String setupTriggerSQLCode(Connection conn, SQLTrigger trigger) {
		return null;
	}

	@Override
	public boolean hasSequenceFeature() {
		return false;
	}

	@Override
	public List<SQLSequence> listSequences(Connection conn, SQLSchema schema) {
		List<SQLSequence> result = new ArrayList<SQLSequence>();
		return result;
	}

	@Override
	public String setupSequenceSQLCode(Connection conn, SQLSequence sequence) {
		StringBuilder sql = new StringBuilder();
		sql.append("create sequence ");
		if (sequence.getSchema() != null) {
			sql.append(sequence.getSchema().getName());
			sql.append(".");
		}
		sql.append(sequence.getName());
		sql.append(" start with ");
		sql.append(sequence.getStartsWith());
		if (sequence.getStepWith() > 0) {
			sql.append(" maxvalue ");
			sql.append(sequence.getEndsWith());
		}
		if (sequence.getStepWith() > 0) {
			sql.append(" increment by ");
			sql.append(sequence.getStepWith());
		}
		sequence.setCreateCode(sql.toString());
		sequence.setNextvalCode(getSequenceNextValSQL(sequence));
		return null;
	}
	
	@Override
	public String getSequenceNextValSQL(SQLSequence sequence) {
		return "";
	}

	@Override
	public String getSelectCountRows(SQLTable table) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(1) from "); 
		sb.append(table.getAbsoluteName());
		return sb.toString();
	}

	@Override
	public boolean loadProcedures(SQLSchema schema) {
		return false;
	}

}
