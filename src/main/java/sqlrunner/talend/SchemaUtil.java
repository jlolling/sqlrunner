package sqlrunner.talend;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import sqlrunner.datamodel.SQLField;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.FieldDescription;

public class SchemaUtil {
	
	private static final Logger logger = Logger.getLogger(SchemaUtil.class);
	private static Properties dbmsIds = new Properties();
	
	public SchemaUtil() {
		loadDbmsIds();
	}
	
	private static void loadDbmsIds() {
		if (dbmsIds == null) {
			dbmsIds = new Properties();
	        try {
	            if (logger.isDebugEnabled()) {
	                logger.debug("load look and feels from archive");
	            }
	            final InputStream is = SchemaUtil.class.getResourceAsStream("/talend_dbmsids.properties");
	            dbmsIds.load(is);
	            is.close();
	        } catch (Exception e) {
	            logger.error("loadDbmsIds failed: " + e.getMessage(), e);
	        }
		}
	}
	
	public String getSchemaXMLFromTable(SQLTable table, String databaseId) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("\n");
		sb.append("<schema");
		if (databaseId != null && databaseId.isEmpty() == false) {
			sb.append(" dbmsId=\"");
			sb.append(databaseId);
			sb.append("\"");
		}
		sb.append(">");
		sb.append("\n");
		for (int i = 0; i < table.getFieldCount(); i++) {
			SQLField field = table.getFieldAt(i);
			sb.append("    <column comment=\"" + getNullSaveString(field.getComment()) + "\"");
			sb.append("\n");
			sb.append("            default=\"" + getNullSaveString(field.getDefaultValue()) + "\"");
			sb.append("\n");
			sb.append("            key=\"" + field.isPrimaryKey() + "\"");
			sb.append("\n");
			sb.append("            label=\"" + field.getName() + "\"");
			sb.append("\n");
			sb.append("            length=\"" + field.getLength() + "\"");
			sb.append("\n");
			sb.append("            nullable=\"" + field.isNullValueAllowed() + "\"");
			sb.append("\n");
			sb.append("            originalDbColumnName=\"" + field.getName() + "\"");
			sb.append("\n");
			sb.append("            talendType=\"id_" + getJavaClassForDbType(field) + "\"");
			sb.append("\n");
			if (field.getBasicType() == BasicDataType.DATE.getId()) {
				sb.append("            pattern=\"&quot;dd-MM-yyyy&quot;\"");
			} else {
				sb.append("            pattern=\"\"");
			}
			sb.append("\n");
			sb.append("            type=\"" + (field.getTypeName() != null ? field.getTypeName().toUpperCase() : "") + "\"");
			sb.append("\n");
			sb.append("            precision=\"" + field.getDecimalDigits() + "\"/>");
			sb.append("\n");
		}
		sb.append("</schema>");
		return sb.toString();
	}

	public String getSchemaXMLFromFieldDescriptions(List<FieldDescription> fields) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("\n");
		sb.append("<schema>");
		sb.append("\n");
		for (FieldDescription field : fields) {
			sb.append("    <column comment=\"\"");
			sb.append("\n");
			sb.append("            default=\"" + getNullSaveString(field.getDefaultValue()) + "\"");
			sb.append("\n");
			sb.append("            key=\"" + field.isPartOfPrimaryKey() + "\"");
			sb.append("\n");
			sb.append("            label=\"" + field.getName() + "\"");
			sb.append("\n");
			sb.append("            length=\"" + field.getLength() + "\"");
			sb.append("\n");
			sb.append("            nullable=\"" + field.isNullEnabled() + "\"");
			sb.append("\n");
			sb.append("            originalDbColumnName=\"" + field.getName() + "\"");
			sb.append("\n");
			sb.append("            talendType=\"id_" + getJavaClassForType(field) + "\"");
			sb.append("\n");
			if (field.getBasicTypeId() == BasicDataType.DATE.getId()) {
				sb.append("            pattern=\"&quot;dd-MM-yyyy&quot;\"");
			} else {
				sb.append("            pattern=\"\"");
			}
			sb.append("\n");
			sb.append("            type=\"\"");
			sb.append("\n");
			sb.append("            precision=\"\"/>");
			sb.append("\n");
		}
		sb.append("</schema>");
		return sb.toString();
	}
	
	public String getSchemaXMLFromColumns(List<Column> columnList) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("\n");
		sb.append("<schema>");
		sb.append("\n");
		for (Column c : columnList) {
			sb.append("    <column");
			sb.append("\n");
			if (c.getComment() != null && c.getComment().isEmpty() == false) {
				sb.append("            comment=\"" + c.getComment().replace("\n", "").replace("\r", "") + "\"");
			} else {
				sb.append("            comment=\"\"");
			}
			sb.append("\n");
			sb.append("            default=\"" + getNullSaveString(c.getDefaultValue()) + "\"");
			sb.append("\n");
			sb.append("            key=\"" + c.isKey() + "\"");
			sb.append("\n");
			sb.append("            label=\"" + c.getName() + "\"");
			sb.append("\n");
			sb.append("            length=\"" + (c.getLength() != null ? c.getLength() : "") + "\"");
			sb.append("\n");
			sb.append("            nullable=\"" + c.isNullable() + "\"");
			sb.append("\n");
			sb.append("            originalDbColumnName=\"" + getNullSaveString(c.getName()) + "\"");
			sb.append("\n");
			sb.append("            talendType=\"id_" + (c.getDataType() != null ? c.getDataType() : "Object") + "\"");
			sb.append("\n");
			if ("Date".equals(c.getDataType())) {
				if (c.getPattern() == null || c.getPattern().isEmpty()) {
					sb.append("            pattern=\"&quot;dd-MM-yyyy&quot;\"");
				} else {
					sb.append("            pattern=\"&quot;" + c.getPattern() + "&quot;\"");
				}
			} else {
				sb.append("            pattern=\"\"");
			}
			sb.append("\n");
			sb.append("            type=\"\"");
			sb.append("\n");
			if (c.getPrecision() != null) {
				sb.append("            precision=\"" + (c.getPrecision() != null ? c.getPrecision() : "") + "\"/>");
			} else {
				sb.append("            precision=\"\"/>");
			}
			sb.append("\n");
		}
		sb.append("</schema>");
		return sb.toString();
	}

	private String getNullSaveString(String s) {
		if (s == null) {
			return "";
		} else {
			return s.trim();
		}
	}
	
	public String getJavaClassForType(FieldDescription field) {
		if (field.getBasicTypeId() == BasicDataType.BOOLEAN.getId()) {
			return "Boolean";
		} else if (field.getBasicTypeId() == BasicDataType.CHARACTER.getId()) {
			return "String";
		} else if (field.getBasicTypeId() == BasicDataType.CLOB.getId()) {
			return "String";
		} else if (field.getBasicTypeId() == BasicDataType.DATE.getId()) {
			return "Date";
		} else if (field.getBasicTypeId() == BasicDataType.DOUBLE.getId()) {
			return "Double";
		} else if (field.getBasicTypeId() == BasicDataType.INTEGER.getId()) {
			return "Integer";
		} else if (field.getBasicTypeId() == BasicDataType.LONG.getId()) {
			return "Long";
		} else {
			return "Object";
		}
	}
	
	public String getJavaClassForDbType(SQLField field) {
		String dbType = field.getTypeName().toLowerCase();
		if ("int2".equals(dbType)) {
			return "Short";
		} else if ("smallint".equals(dbType)) {
			return "Short";
		} else if ("int4".equals(dbType)) {
			return "Integer";
		} else if ("int8".equals(dbType)) {
			return "Long";
		} else if ("bigint".equals(dbType)) {
			return "Long";
		} else if ("integer".equals(dbType)) {
			return "Integer";
		} else if (dbType.contains("double")) {
			return "Double";
		} else if (dbType.contains("number") || dbType.contains("decimal") || dbType.contains("numeric")) {
			if (field.getDecimalDigits() > 0) {
				return "BigDecimal";
			} else if (field.getLength() < 10) {
				return "Integer";
			} else if (field.getLength() == 22) {
				return "BigDecimal";
			} else {
				return "Long";
			}
		} else if (dbType.contains("float")) {
			return "Double";
		} else if (dbType.contains("char")) {
			return "String";
		} else if (dbType.contains("text")) {
			return "String";
		} else if (dbType.contains("long")) {
			return "String";
		} else if (dbType.contains("clob")) {
			return "String";
		} else if (dbType.contains("bool")) {
			return "Boolean";
		} else if (dbType.contains("bit")) {
			return "Boolean";
		} else if (dbType.contains("enum")) {
			return "String";
		} else if (dbType.contains("time")) {
			return "Date";
		} else if (dbType.contains("date")) {
			return "Date";
		} else if (dbType.contains("blob")) {
			return "byte[]";
		} else {
			return "Object";
		}
	}
	
	public void writeSchemaFile(File f, SQLTable table) throws IOException {
		String xmlText = getSchemaXMLFromTable(table, null);
        BufferedWriter bwFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
        try {
        	bwFile.write(xmlText);
        } finally {
        	if (bwFile != null) {
        		bwFile.close();
        	}
        }
	}
	
	public void writeSchemaFile(File f, List<Column> list) throws IOException {
		String xmlText = getSchemaXMLFromColumns(list);
        BufferedWriter bwFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
        try {
        	bwFile.write(xmlText);
        } finally {
        	if (bwFile != null) {
        		bwFile.close();
        	}
        }
	}

}
