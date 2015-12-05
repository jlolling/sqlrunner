package sqlrunner.export;

public interface ExportListener {
	
	void exportStarted(String messages);
	
	void progressMade(long countAll, long countCurrent, String messages);
	
	void exportCancelled(long count, String messages);
	
	void exportFinished(long count, String messages);
	
	void problemOccured(String messages, Exception e);

}
