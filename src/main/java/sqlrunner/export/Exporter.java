package sqlrunner.export;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import dbtools.ConnectionDescription;

public interface Exporter {

    public static final int CONNECTING = 1;
    public static final int PARSING = 2;
    public static final int SELECTING = 3;
    public static final int FETCHING = 4;
    public static final int CLOSING = 5;
    public static final int FINISHED = 6;
    public static final int ABORTED = 7;

	public abstract void setLogger(Logger logger);

	public abstract Logger getLogger();

	/**
	 * set connection description
	 * @param connDesc defines the database
	 */
	public void setConnectionDescription(ConnectionDescription connDesc);

	public void setDateFormat(String pattern);
	/**
	 * set the query to select the datasets
	 * @param sql
	 */
	public void setQuery(String sql);

	public abstract void resetToStaticLogger();

	void setOutputFile(File outputFile) throws Exception;
	
	void exportData() throws IOException, SQLException;

	void abort();

	int getCurrentRowNum();

	String getCurrentAction();

	int getStatus();
	
	public boolean connect() throws Exception;

}