package dbtools;

import java.sql.PreparedStatement;

/**
 * @author lolling.jan
 * beschreibt eine Definition eines CallableStatements 
 */
public abstract class PreparedStatementDefinition {

    protected String            sql;
    protected PreparedStatement ps;

    public final String getSQL() {
        return sql;
    }

    public void setPreparedStatement(PreparedStatement statement) {
        ps = statement;
    }

    public PreparedStatement getPreparedStatement() {
        return ps;
    }

    @Override
    public String toString() {
        return sql;
    }

}
