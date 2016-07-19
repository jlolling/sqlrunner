package sqlrunner.dbext;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import sqlrunner.datamodel.Field;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLSequence;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.datamodel.SQLTrigger;
import dbtools.ConnectionDescription;
import dbtools.DatabaseSession;

public interface DatabaseExtension {
	
	/**
	 * setup connections if necessary
	 * @param c connection to tweak
	 */
	public void setupConnection(Connection c);
	
	/**
	 * setup connections if necessary
	 * @param c connection to tweak
	 */
	public void setupStatement(Statement c);

	/**
	 * check if extension fits to the database type
	 * @param cd description of connection
	 * @return true if it matches
	 */
	public boolean isApplicable(ConnectionDescription cd);
	
	/**
	 * check if extension fits to the database type
	 * @param driverClass description of connection
	 * @return true if it matches
	 */
	public boolean isApplicable(String driverClass);

	/**
	 * 
	 * @return true if database has explain capabilities
	 */
	public boolean hasExplainFeature();

	/**
	 * Creates a script to do the explain job
	 * @param currentStatement
	 * @return sql code to explain the current statement
	 */
	public String getExplainSQL(String currentStatement);
	
	/**
	 * sets the code into the view
	 * @param table (must be of type VIEW)
	 * @return the code
	 */
	public String setupViewSQLCode(DatabaseSession session, SQLTable table);
	
	/**
	 * sets the code into the procedure
	 * @param proc
	 * @return the code
	 */
	public String setupProcedureSQLCode(DatabaseSession session, SQLProcedure proc);
	
	/**
	 * sets the code into the procedure
	 * @param proc
	 * @return the code
	 */
	public String setupTriggerSQLCode(DatabaseSession session, SQLTrigger proc);

	/**
	 * sets the code into the procedure
	 * @param conn
	 * @param sequence
	 * @return the code
	 */
	public String setupSequenceSQLCode(Connection conn, SQLSequence sequence);
	
	/**
	 * change data type to more appropriated types
	 * @param field
	 */
	public void setupDataType(Field field);
	
	/**
	 * change data type to more appropriated types
	 * @param field
	 */
	public void setupDataType(SQLProcedure.Parameter parameter);

	public String getName();
	
	/**
	 * returns a list of additional keywords, which are depend in the database vendor
	 * @return always a list (e.g. an empty list if there are no additional keywords) 
	 */
	public List<String> getAdditionalSQLKeywords();

	/**
	 * returns a list of additional data types, which are depend in the database vendor
	 * @return always a list (e.g. an empty list if there are no additional data types) 
	 */
	public List<String> getAdditionalSQLDatatypes();

	/**
	 * returns a list of additional keywords for stored procedures, which are depend in the database vendor
	 * @return always a list (e.g. an empty list if there are no additional keywords for stored procedures) 
	 */
	public List<String> getAdditionalProcedureKeywords();
	
	/**
	 * create SQL to set or update comment for a SQLTable
	 * @param tableName to comment
	 * @param comment 
	 * @return SQL code
	 */
	public String getUpdateCommentStatement(String tableName, String comment);
	
	/**
	 * create SQL to set or update comment for a SQLField
	 * @param tableName
	 * @param fieldName
	 * @param comment
	 * @return SQL code
	 */
	public String getUpdateCommentStatement(String tableName, String fieldName, String comment);

	/**
	 * returns the schema name which is associated with the login
	 * @param cd
	 * @return name of login schema
	 */
	public String getLoginSchema(ConnectionDescription cd);
		
	/**
	 * returns the schema name which is associated with the login
	 * @param cd
	 * @return name of login schema
	 */
	public String getLoginSchema(Connection conn);

	/**
	 * returns true if SQL dialect contains limit keyword
	 * @return
	 */
	public boolean hasSQLLimitFeature();
	
	/**
	 * returns true if SQL dialect use a where condition to limit count datasets
	 * @return
	 */
	public boolean isLimitExpressionAWhereCondition();

	/**
	 * returns the changed statement with dataset limitation
	 * @param max count datasets
	 * @return sql fragement to limit an query
	 */
	public String getLimitExpression(int max);
	
	/**
	 * return the SQL code to convert a date object into a SQL expression
	 * @param value the date object which needs to convert into a SQL expression
	 * @return the necessary SQL code 
	 */
	public String getDateToSQLExpression(java.util.Date value);
	
	/**
	 * return the SQL code template to convert a date object into a SQL expression
	 * @return the necessary SQL code template
	 */
	public String getDateToSQLExpressionPattern();

	/**
	 * return the SQL code to convert a date object into a SQL expression
	 * @param value the date object which needs to convert into a SQL expression
	 * @return the necessary SQL code 
	 */
	public String getTimestampToSQLExpression(java.util.Date value);
	
	/**
	 * return the SQL code template to convert a date object into a SQL expression
	 * @return the necessary SQL code template
	 */
	public String getTimestampToSQLExpressionPattern();
	
	/**
	 * returns the char which should be used to enclosure identifiers
	 * @return
	 */
	public String getIdentifierQuoteString();
	
	public void setIdentifierQuoteString(String quote);
	
	public boolean hasSequenceFeature();
	
	public List<SQLSequence> listSequences(Connection conn, SQLSchema schema);
	
	/**
	 * returns the SQL code to get the next value from the sequence
	 * @param sequence
	 * @return SQL code
	 */
	public String getSequenceNextValSQL(SQLSequence sequence);
	
}
