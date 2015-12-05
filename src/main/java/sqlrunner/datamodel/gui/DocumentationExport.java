package sqlrunner.datamodel.gui;

import sqlrunner.datamodel.SQLField;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.generator.SQLCodeGenerator;

public class DocumentationExport {
	
	public static String getHTMLTableFor(SQLTable table) {
		StringBuilder sb = new StringBuilder(255);
		sb.append("<html><body>\n");
		sb.append("<p>");
		sb.append(table.getName());
		sb.append("</p>\n");
		sb.append("<table>\n");
		sb.append("<tr>\n");
		sb.append("<th>");
		sb.append("column name");
		sb.append("</th>\n");
		sb.append("<th>");
		sb.append("column type");
		sb.append("</th>\n");
		sb.append("<th>");
		sb.append("description");
		sb.append("</th>\n");
		sb.append("</tr>\n");
		for (int i = 0; i < table.getFieldCount(); i++) {
			sb.append("<tr>\n");
			SQLField f = table.getFieldAt(i);
			sb.append("<td>");
			sb.append(f.getName());
			sb.append("</td>\n");
			sb.append("<td>");
			sb.append(SQLCodeGenerator.getInstance().getFieldType(f));
			sb.append("</td>\n");
			sb.append("<td>");
			if (f.getComment() != null) {
				sb.append(f.getComment());
			}
			sb.append("</td>\n");
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");
		sb.append("</body></html>");
		return sb.toString();
	}
	
	public static String getWikiTableFor(SQLTable table) {
		StringBuilder sb = new StringBuilder(255);
		sb.append("<html><body>\n");
		sb.append("__");
		sb.append(table.getName());
		sb.append("__\n");
		sb.append("\n||column name||column type||description");
		for (int i = 0; i < table.getFieldCount(); i++) {
			sb.append("\n|");
			SQLField f = table.getFieldAt(i);
			sb.append(f.getName());
			sb.append("|");
			sb.append(SQLCodeGenerator.getInstance().getFieldType(f));
			sb.append("|");
			if (f.getComment() != null) {
				sb.append(f.getComment());
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}