package sqlrunner.flatfileimport;

public interface Importer {
	
	final int NOT_STARTED = -1;
	final int NORMAL = 0;
	final int WARNINGS = 1;
	final int FATALS = 2;

	/**
	 * counts the maximum of to proceed input datasets (e.g. lines of the whole file)
	 * @return
	 */
	long getCountMaxInput();
	
	/**
	 * counts the currently proceeded lines
	 * @return
	 */
	long getCountCurrInput();

	/**
	 * couunts the currently inserted datasets
	 * @return
	 */
	long getCountInserts();
	
	/**
	 * counts the currently updates datasets
	 * @return
	 */
	long getCountUpdates();
	
	/**
	 * counts the currently ignored input datasets (e.g. lines of a file)
	 * @return
	 */
	long getCountIgnored();
	
	/**
	 * start date
	 * @return
	 */
	long getStartTime();
	
	/**
	 * stop date
	 * @return
	 */
	long getStopTime();
	
	/**
	 * stop status
	 * @return true if imported has stopped importprocess
	 */
	boolean isStopped();
	
	/**
	 * @return true if importer is running
	 */
	boolean isRunning();
	
	/**
	 * status of the importer
	 * @return status code
	 * @see NOT_STARTED, NORMAL, WARNINGS, ERRORS
	 */
	int getStatusCode();
	
	void abort();
	
	String getLogFileName();
	
	String getCurrentAction();
	
	/**
	 * Returns the last not empty valid value of an import column.
	 * @param columnName
	 * @return value of the column
	 */
	Object getLastValue(String columnName);
	
}
