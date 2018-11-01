package sqlrunner;

public interface Statistic {
	
	void addValue(Object o);
	
	int getCount();
	
	String render();

}
