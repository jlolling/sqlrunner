package sqlrunner.datamodel;

import java.util.ArrayList;
import java.util.List;

public class SQLCatalog extends SQLObject {
	
	private List<SQLSchema> schemas = new ArrayList<SQLSchema>();
	
	public SQLCatalog(SQLDataModel model, String name) {
    	super(model, name);
    }
	
	public List<SQLSchema> getSchemas() {
		return schemas;
	}

	public int getCountSchemas() {
		return schemas.size();
	}
	
	public SQLSchema getSchemaAt(int index) {
		return schemas.get(index);
	}
	
	public void addSQLSchema(SQLSchema schema) {
		if (schemas.contains(schema) == false) {
			schemas.add(schema);
			schema.setCatalog(this);
		}
	}
	
	public SQLSchema getSQLSchema(String name) {
		if (name == null) return null;
		for (SQLSchema s : schemas) {
			if (name.equalsIgnoreCase(s.getName())) {
				return s;
			}
		}
		return null;
	}
	
}
