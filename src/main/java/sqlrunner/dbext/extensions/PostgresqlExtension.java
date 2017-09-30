package sqlrunner.dbext.extensions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sqlrunner.datamodel.Field;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLProcedure.Parameter;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLSequence;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.dbext.GenericDatabaseExtension;
import sqlrunner.flatfileimport.BasicDataType;

public class PostgresqlExtension extends GenericDatabaseExtension {

	private static Logger logger = Logger.getLogger(PostgresqlExtension.class);
	private static final String driverClassName = "org.postgresql.Driver";
	private static final String name = "PostgreSQL Extension";
	
	public PostgresqlExtension() {
		addDriverClassName(driverClassName);
		addSQLDatatype("json");
		addSQLKeywords("on", "conflict");
	}

	@Override
	public boolean hasExplainFeature() {
		return true;
	}

	@Override
	public String getExplainSQL(String currentStatement) {
		StringBuilder sb = new StringBuilder();
		sb.append("explain\n");
		sb.append(currentStatement);
		return sb.toString();
	}

	@Override
	public String setupViewSQLCode(Connection conn, SQLTable table) {
		if (table.isView()) {
			if (logger.isDebugEnabled()) {
				logger.debug("setupViewSQLCode view=" + table.getAbsoluteName());
			}
			String source = null;
			try {
				Statement stat = conn.createStatement();
				ResultSet rs = stat.executeQuery("select pg_get_viewdef('" + table.getAbsoluteName() + "', true)");
				if (rs.next()) {
					source = rs.getString(1);
					if (source != null && source.isEmpty() == false) {
						if (table.isMaterializedView()) {
							source = "create materialized view " + table.getName() + " as\n" + source;
							table.setSourceCode(source);
						} else {
							source = "create view " + table.getName() + " as\n" + source;
							table.setSourceCode(source);
						}
					}
				}
				rs.close();
				stat.close();
			} catch (SQLException sqle) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
					// ignore
				}
				logger.error("setupViewSQLCode for table " + table.getAbsoluteName() + " failed: " + sqle.getMessage(), sqle);
			}
			return source;
		}
		return null;
	}

	@Override
	public String setupProcedureSQLCode(Connection conn, SQLProcedure proc) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("setupProcedureSQLCode proc=" + proc.getAbsoluteName());
			}
			StringBuilder query = new StringBuilder();
			query.append("select p.prosrc, l.lanname");
			query.append(" from");
			query.append(" pg_catalog.pg_proc p, ");
			query.append(" pg_catalog.pg_language l,");
			query.append(" pg_catalog.pg_namespace n");
			query.append(" where p.proname = '");
			query.append(proc.getName());
			query.append("'");
			query.append(" and p.prolang = l.oid");
			query.append(" and p.pronamespace = n.oid");
			query.append(" and n.nspname = '");
			query.append(proc.getSchema().getName());
			query.append("'");
			if (proc.getParameterCount() > 0) {
				query.append(" and p.proargnames = ");
				query.append("'{");
				for (int i = 0; i < proc.getParameterCount(); i++) {
					Parameter p = proc.getParameterAt(i);
					if (i > 0) {
						query.append(",");
					}
					query.append("\"");
					query.append(p.getName());
					query.append("\"");
				}
				query.append("}'");
			}
			StringBuilder code = new StringBuilder();
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(query.toString());
			if (rs.next()) {
				String source = rs.getString(1);
				String language = rs.getString(2);
				if (source != null && source.isEmpty() == false) {
					code.append("create or replace ");
					if (proc.isFunction()) {
						code.append("function ");
					} else {
						code.append("procedure ");
					}
					code.append(proc.getName());
					code.append("(\n");
					for (int i = 0; i < proc.getParameterCount(); i++) {
						Parameter p = proc.getParameterAt(i);
						if (i > 0) {
							code.append(",\n");
						}
						code.append("    ");
						code.append(p.getName());
						code.append(" ");
						code.append(p.getTypeName());
					}
					code.append(")\n\n");
					if (proc.isFunction()) {
						code.append("returns ");
						code.append(proc.getReturnParameter().getTypeName());
						code.append(" as $$");
					}
					code.append(source);
					code.append("$$ LANGUAGE ");
					code.append(language);
					proc.setCode(code.toString());
				}
			}
			rs.close();
			stat.close();
			return code.toString();
		} catch (SQLException sqle) {
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// ignore
			}
			logger.error("setupProcedureSQLCode for proc " + proc.getAbsoluteName() + " failed: " + sqle.getMessage(), sqle);
		}
		return null;
	}

	@Override
	public void setupDataType(Field field) {
        if ("int2".equalsIgnoreCase(field.getTypeName())) {
            field.setTypeSQLCode("smallint");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("int4".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("integer");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("integer".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("integer");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("serial".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("serial");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("bigserial".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("bigserial");
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if ("int8".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("bigint");
    		field.setBasicType(BasicDataType.LONG.getId());
        } else if ("float8".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("double precision");
    		field.setBasicType(BasicDataType.DOUBLE.getId());
        } else if ("float4".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("single precision");
    		field.setBasicType(BasicDataType.DOUBLE.getId());
        } else if ("varchar".equalsIgnoreCase(field.getTypeName())) {
        	if (field.getLength() > 2048) {
        		field.setTypeSQLCode("text");
        		field.setBasicType(BasicDataType.CLOB.getId());
        	}
        } else if ("bool".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("boolean");
    		field.setBasicType(BasicDataType.BOOLEAN.getId());
        } else if ("text".equalsIgnoreCase(field.getTypeName())) {
        	field.setTypeSQLCode("text");
    		field.setBasicType(BasicDataType.CLOB.getId());
        }
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setupDataType(Parameter parameter) {
        if ("int2".equalsIgnoreCase(parameter.getTypeName())) {
            parameter.setTypeName("smallint");
            parameter.setLength(0);
        } else if ("int4".equalsIgnoreCase(parameter.getTypeName())) {
        	parameter.setTypeName("integer");
        	parameter.setLength(0);
        } else if ("integer".equalsIgnoreCase(parameter.getTypeName())) {
            parameter.setLength(0);
        } else if ("bigint".equalsIgnoreCase(parameter.getTypeName())) {
            parameter.setLength(0);
        } else if ("serial".equalsIgnoreCase(parameter.getTypeName())) {
            parameter.setLength(0);
        } else if ("bigserial".equalsIgnoreCase(parameter.getTypeName())) {
            parameter.setLength(0);
        } else if ("int8".equalsIgnoreCase(parameter.getTypeName())) {
        	parameter.setTypeName("bigint");
            parameter.setLength(0);
        } else if ("float8".equalsIgnoreCase(parameter.getTypeName())) {
        	parameter.setTypeName("double precision");
            parameter.setLength(0);
        } else if ("float4".equalsIgnoreCase(parameter.getTypeName())) {
        	parameter.setTypeName("single precision");
            parameter.setLength(0);
        } else if ("varchar".equalsIgnoreCase(parameter.getTypeName())) {
        	if (parameter.getLength() > 2048) {
        		parameter.setTypeName("text");
        		parameter.setLength(0);
        	}
        } else if ("bool".equalsIgnoreCase(parameter.getTypeName())) {
        	parameter.setTypeName("boolean");
            parameter.setLength(0);
        } else if ("text".equalsIgnoreCase(parameter.getTypeName())) {
    		parameter.setLength(0);
        }
	}

	@Override
	public List<String> getAdditionalSQLKeywords() {
		List<String> list = new ArrayList<String>();
		list.add("date_trunc");
		list.add("substring");
		list.add("regexp_replace");
		list.add("regexp_matches");
		list.add("regexp_split_to_array");
		list.add("position");
		list.add("overlay");
		list.add("overlay");
		list.add("bit_length");
		list.add("char_length");
		list.add("character_length");
		list.add("btrim");
		list.add("format");
		list.add("do");
		list.add("instead");
		list.add("conflict");
		list.add("excluded");
		return list;
	}

	@Override
	public List<String> getAdditionalSQLDatatypes() {
		List<String> list = new ArrayList<String>();
		list.add("interval");
		return list;
	}

	@Override
	public List<String> getAdditionalProcedureKeywords() {
		List<String> list = new ArrayList<String>();
		list.add("returns");
		return list;
	}

	@Override
	public boolean hasSQLLimitFeature() {
		return true;
	}

	@Override
	public String getLimitExpression(int max) {
		return "limit " + max;
	}

	@Override
	public boolean isLimitExpressionAWhereCondition() {
		return false;
	}

	@Override
	public boolean isApplicable(String driverClass) {
		if (driverClass.toLowerCase().contains("postgre")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public List<SQLSequence> listSequences(Connection conn, SQLSchema schema) {
		schema.setLoadingSequences(true);
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT sequence_name,start_value,maximum_value,increment FROM information_schema.sequences\n");
		sb.append("where sequence_schema='");
		sb.append(schema.getName().toLowerCase());
		sb.append("'");
		try {
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery(sb.toString());
			while (rs.next()) {
				SQLSequence seq = new SQLSequence(schema, rs.getString(1));
				seq.setStartsWith(rs.getLong(2));
				seq.setEndsWith(rs.getLong(3));
				seq.setStepWith(rs.getLong(4));
				setupSequenceSQLCode(conn, seq);
				schema.addSequence(seq);
			}
			rs.close();
			stat.close();
			schema.setSequencesLoaded();
		} catch (SQLException sqle) {
			try {
				if (conn.getAutoCommit() == false) {
					conn.rollback();
				}
			} catch (SQLException e1) {
				// ignore
			}
			logger.error("listSequences for schema=" + schema + " failed: " + sqle.getMessage(), sqle);
		}
		schema.setLoadingSequences(false);
		return schema.getSequences();
	}

	@Override
	public boolean hasSequenceFeature() {
		return true;
	}

	@Override
	public String getSequenceNextValSQL(SQLSequence sequence) {
		StringBuilder sql = new StringBuilder();
		sql.append("nextval('");
		sql.append(sequence.getSchema().getName());
		sql.append(".");
		sql.append(sequence.getName());
		sql.append("')");
		return sql.toString();
	}

/*
	@Override
	public String getSelectCountRows(SQLTable table) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT reltuples::bigint AS estimate from pg_class where oid = to_regclass('");
		sb.append(table.getAbsoluteName());
		sb.append("')");
		return sb.toString();
	}
*/
	
	
}
