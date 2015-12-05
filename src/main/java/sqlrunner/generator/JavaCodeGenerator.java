package sqlrunner.generator;

import java.io.File;
import java.text.SimpleDateFormat;

import sqlrunner.datamodel.SQLField;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.text.StringReplacer;

/**
 * 
 * @author jan
 */
public class JavaCodeGenerator {

	/**
	 * because Alfabet-software has no naming conventions, we have to be shure
	 * that class names fits to java naming conventions
	 * 
	 * @param class name setted from user or imported from alfabet
	 * @return java conform identifier
	 */
	public static String formJavaClassName(String name) {
		name = name.toLowerCase();
		StringReplacer sr = new StringReplacer(name);
		sr.replace("ü", "ue");
		sr.replace("ö", "oe");
		sr.replace("ä", "ae");
		sr.replace("ß", "sz");
		sr.replace(" ", "");
		name = sr.getResultText();
		name = changeUnderScoresToCamelCase(name);
		// classes starts with capital letter
		char c = name.charAt(0);
		if (Character.isLowerCase(c)) {
			name = Character.toUpperCase(c) + name.substring(1);
		}
		return name;
	}

	public static final File createSourceFile(String sourceRootDir,
			String packageName, String tableName) {
		return new File(sourceRootDir + "/" + packageName.replace('.', '/')
				+ "/" + formJavaClassName(tableName) + ".java");
	}

	/**
	 * because Alfabet-software has no naming conventions, we have to be sure
	 * that property names fits to java naming conventions
	 * 
	 * @param property
	 *            name set from user or imported from alfabet
	 * @return java conform identifier
	 */
	private static String formJavaPropertyName(String name) {
		name = name.toLowerCase();
		StringReplacer sr = new StringReplacer(name);
		sr.replace("ü", "ue");
		sr.replace("ö", "oe");
		sr.replace("ä", "ae");
		sr.replace("ß", "sz");
		sr.replace(" ", "");
		name = sr.getResultText();
		// properties starts with lower case letter
		if (name.equals("abstract")) {
			name = "abstractObject";
		} else if (name.equals("class")) {
			name = "objectClass";
		} else if (name.equals("protected")) {
			name = "protectedObject";
		}
		return changeUnderScoresToCamelCase(name);
	}

	private static String changeUnderScoresToCamelCase(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		char c;
		char c0 = ' ';
		for (int i = 0; i < name.length(); i++) {
			c = name.charAt(i);
			if (c != '_') {
				if (c0 == '_') {
					sb.append(Character.toUpperCase(c));
				} else {
					sb.append(c);
				}
			}
			c0 = c;
		}
		return sb.toString();
	}

	/**
	 * uppercase the first letter
	 * 
	 * @param name
	 *            to be changed
	 * @return name with uppercase first letter
	 */
	private static String capitalizeName(String name) {
		char c = name.charAt(0);
		if (Character.isLowerCase(c)) {
			name = Character.toUpperCase(c) + name.substring(1);
		}
		return name;
	}

	private static String formGetterMethodeName(SQLField field) {
		return "get"
				+ capitalizeName(changeUnderScoresToCamelCase(field.getName()
						.toLowerCase()));
	}

	private static String formSetterMethodeName(SQLField field) {
		return "set"
				+ capitalizeName(changeUnderScoresToCamelCase(field.getName()
						.toLowerCase()));
	}

	public static String createJavaClassCode(SQLTable table, String packageName) {
		final StringBuffer sbClass = new StringBuffer();
		// package
		sbClass.append("/**\n");
		sbClass.append(" * build from database url=");
		sbClass.append(table.getModel().toString());
		sbClass.append('\n');
		sbClass.append(" * at: ");
		sbClass.append(new SimpleDateFormat().format(new java.util.Date()));
		sbClass.append('\n');
		sbClass.append(" */\n");
		sbClass.append("package ");
		sbClass.append(packageName);
		sbClass.append(";\n\n");
		// imports
		sbClass.append("import java.sql.Connection;\n");
		sbClass.append("import java.sql.ResultSet;\n");
		sbClass.append("import java.sql.SQLException;\n\n");
		// class
		sbClass.append("public class ");
		sbClass.append(formJavaClassName(table.getName()));
		sbClass.append(" {\n\n");
		// member variables
		for (int i = 0; i < table.getFieldCount(); i++) {
			sbClass.append(createMemberVariableCode(table.getFieldAt(i)));
		}
		sbClass.append("\n}");
		return sbClass.toString();
	}

	private static String createMemberVariableCode(SQLField field) {
		final StringBuffer sb = new StringBuffer();
		if (BasicDataType.isStringType(field.getBasicType())) {
			sb.append("    private String ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = null;\n\n");
			sb.append("    public String ");
			sb.append(formGetterMethodeName(field));
			sb.append("() {\n");
			sb.append("        return ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(";\n");
			sb.append("    }\n\n");
			sb.append("    public void ");
			sb.append(formSetterMethodeName(field));
			sb.append("(String value) {\n");
			if (field.isNullValueAllowed() == false) {
				sb.append("        if (value == null) {\n");
				sb.append("            throw new IllegalArgumentException(\"");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" cannot be null\");\n");
				sb.append("        }\n");
			}
			sb.append("        this.");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = value;\n");
			sb.append("    }\n\n");
		} else if (field.getBasicType() == BasicDataType.DATE.getId()) {
			sb.append("    private java.util.Date ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = null;\n\n");
			sb.append("    public java.util.Date ");
			sb.append(formGetterMethodeName(field));
			sb.append("() {\n");
			sb.append("        return ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(";\n");
			sb.append("    }\n\n");
			sb.append("    public void ");
			sb.append(formSetterMethodeName(field));
			sb.append("(java.util.Date value) {\n");
			if (field.isNullValueAllowed() == false) {
				sb.append("        if (value == null) {\n");
				sb.append("            throw new IllegalArgumentException(\"");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" cannot be null\");\n");
				sb.append("        }\n");
			}
			sb.append("        this.");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = value;\n");
			sb.append("    }\n\n");
		} else if (BasicDataType.isNumberType(field.getBasicType())) {
			if (field.isNullValueAllowed()) {
				sb.append("    private Double ");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" = null;\n\n");
				sb.append("    public Double ");
			} else {
				sb.append("    private double ");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" = 0;\n\n");
				sb.append("    public double ");
			}
			sb.append(formGetterMethodeName(field));
			sb.append("() {\n");
			sb.append("        return ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(";\n");
			sb.append("    }\n");
			sb.append('\n');
			if (field.isNullValueAllowed()) {
				sb.append("    public void ");
				sb.append(formSetterMethodeName(field));
				sb.append("(Double value) {\n");
				sb.append("        if (value == null) {\n");
				sb.append("            throw new IllegalArgumentException(\"");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" cannot be null\");\n");
				sb.append("        }\n");
			} else {
				sb.append("    public void ");
				sb.append(formSetterMethodeName(field));
				sb.append("(double value) {\n");
			}
			sb.append("        this.");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = value;\n");
			sb.append("    }\n\n");
		} else if (field.getBasicType() == BasicDataType.BOOLEAN.getId()) {
			if (field.isNullValueAllowed()) {
				sb.append("    private Boolean ");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" = null;\n\n");
				sb.append("    public Boolean ");
			} else {
				sb.append("    private boolean ");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" = false;\n\n");
				sb.append("    public boolean ");
			}
			sb.append(formGetterMethodeName(field));
			sb.append("() {\n");
			sb.append("        return ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(";\n");
			sb.append("    }\n");
			sb.append('\n');
			if (field.isNullValueAllowed()) {
				sb.append("    public void ");
				sb.append(formSetterMethodeName(field));
				sb.append("(Boolean value) {\n");
				sb.append("        if (value == null) {\n");
				sb.append("            throw new IllegalArgumentException(\"");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" cannot be null\");\n");
				sb.append("        }\n");
			} else {
				sb.append("    public void ");
				sb.append(formSetterMethodeName(field));
				sb.append("(boolean value) {\n");
			}
			sb.append("        this.");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = value;\n");
			sb.append("    }\n\n");
		} else {
			sb.append("    private Object ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = null;\n\n");
			sb.append("    public Object ");
			sb.append(formGetterMethodeName(field));
			sb.append("() {\n");
			sb.append("        return ");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(";\n");
			sb.append("    }\n");
			sb.append('\n');
			sb.append("    public void ");
			sb.append(formSetterMethodeName(field));
			sb.append("(Object value) {\n");
			if (field.isNullValueAllowed() == false) {
				sb.append("        if (value == null) {\n");
				sb.append("            throw new IllegalArgumentException(\"");
				sb.append(formJavaPropertyName(field.getName()));
				sb.append(" cannot be null\");\n");
				sb.append("        }\n");
			}
			sb.append("        this.");
			sb.append(formJavaPropertyName(field.getName()));
			sb.append(" = value;\n");
			sb.append("    }\n\n");
		}
		return sb.toString();
	}

}
