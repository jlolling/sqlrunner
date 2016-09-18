package sqlrunner.talend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SQLCodeContextHelper {
	
	private Properties contextVars = new Properties();
	private String contextFilePath = null;
	private String regex = "\"[\\s]*\\+[\\s]*context.([a-z0-9\\_]*)[\\s]*\\+[\\s]*\"";

	public void initContextVars(String contextFilePath) throws IOException {
		this.contextFilePath = contextFilePath;
		File contextFile = new File(contextFilePath);
		if (contextFile.exists() == false) {
			throw new IOException("Context file: " + contextFile.getAbsolutePath() + " does not exists!");
		}
		InputStream in = new FileInputStream(contextFile);
		contextVars.load(in);
	}
	
	public String replaceContextVars(String sqlWithContextVars) {
		StringBuilder sql = new StringBuilder();
		
		return sql.toString();
	}
	
}
