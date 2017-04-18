import java.util.Map.Entry;

import sqlrunner.talend.ContextVarResolver;

public class TestSQLCodeContextHelper {

	public static void main(String[] args) {
		String test = "select * \n" +
					"from \" + context.B17_STAGING_DB_Schema + \".t1\n" +
					"join \" + context.var2 + \".t1\n" +
					"join \" + context.var2 + \".t3";
		ContextVarResolver r = new ContextVarResolver();
		r.addContextVar("B17_STAGING_DB_Schema", "b17_core");
		r.addContextVar("var2", "navi"); 
		try {
			r.readContextVars(test);
			for (Entry<Object, Object> entry : r.getContextVars().entrySet()) {
				System.out.println(entry.getKey());
			}
			System.out.println("------------------");
			System.out.println(r.replaceContextVars(test));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
