package dbtools;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;


/**
 * @author lolling.jan
 * beschreibt eine Definition eines CallableStatements
 */
public abstract class CallableStatementDefinition extends PreparedStatementDefinition {
    
    private static final Logger logger    = Logger.getLogger(CallableStatementDefinition.class);

    protected CallableStatement cs;
    private ArrayList<OutParameter>         outParams = new ArrayList<OutParameter>();

    public final void setCallableStatement(CallableStatement statement) throws SQLException {
        cs = statement;
        OutParameter outParam = null;
        for (int i = 0; i < outParams.size(); i++) {
            outParam = outParams.get(i);
            cs.registerOutParameter(outParam.index, outParam.type);
        }
    }
    
    public final void registerOutParameter(int index, int type) {
        OutParameter outParam = new OutParameter();
        outParam.index = index;
        outParam.type = type;
        outParams.add(outParam);
        if (cs != null) {
            try {
                cs.registerOutParameter(outParam.index, outParam.type);
            } catch (SQLException e) {
                logger.error("registerOutParam: "+e.getMessage(), e);
            }
        }
    }

    public CallableStatement getCallableStatement() {
        return cs;
    }
    
    @Override
    public PreparedStatement getPreparedStatement() {
        return cs;
    }

    @Override
    public final String toString() {
        return sql;
    }

    static final class OutParameter {
        
        protected int index;
        protected int type;
        
    }

}
