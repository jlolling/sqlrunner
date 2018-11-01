package sqlrunner.dbext.extensions;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sqlrunner.datamodel.Field;
import sqlrunner.dbext.GenericDatabaseExtension;
import sqlrunner.flatfileimport.BasicDataType;
import dbtools.ConnectionDescription;

public class MSSqlExtension extends GenericDatabaseExtension {
	
	private static final Logger logger = Logger.getLogger(MSSqlExtension.class);
	
	public MSSqlExtension() {
		addDriverClassName("net.sourceforge.jtds.jdbc.Driver");
		addDriverClassName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
		addDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	}
	
	@Override
	public String getLoginSchema(ConnectionDescription cd) {
		String catalog = cd.getURLElementValue("DATABASE");
		if (catalog != null) {
			return catalog + ".dbo";
		} else {
			return "dbo";
		}
	}

	@Override
	public String getLoginSchema(Connection conn) {
		String catalog = null;
		try {
			catalog = conn.getCatalog();
		} catch (SQLException e) {
			logger.error("getCatalog failed: " + e.getMessage(), e);
		}
		if (catalog != null) {
			return catalog + ".dbo";
		} else {
			return "dbo";
		}
	}

	@Override
	public boolean isApplicable(ConnectionDescription cd) {
		return isApplicable(cd.getDriverClassName());
	}

	@Override
	public void setupDataType(Field field) {
		String typeName = field.getTypeName().toLowerCase();
        if (typeName.indexOf("int") != -1) {
        	if (typeName.indexOf("identity") != -1) {
        		field.setTypeSQLCode("integer indentity(1,1)");
        	} else {
        		field.setTypeSQLCode("integer");
        	}
    		field.setBasicType(BasicDataType.INTEGER.getId());
        } else if (typeName.indexOf("double") != -1) {
            field.setTypeSQLCode("double");
    		field.setBasicType(BasicDataType.DOUBLE.getId());
        } else if (typeName.indexOf("float") != -1) {
            field.setTypeSQLCode("float");
    		field.setBasicType(BasicDataType.DOUBLE.getId());
        } else if ("bool".equals(typeName)) {
        	field.setTypeName("boolean");
            field.setLength(0);
    		field.setBasicType(BasicDataType.DATE.getId());
        }
	}

	@Override
	public List<String> getAdditionalSQLKeywords() {
		List<String> list = new ArrayList<String>();
		list.add("identity");
		list.add("coalesce");
		list.add("len");
		return list;
	}

}
