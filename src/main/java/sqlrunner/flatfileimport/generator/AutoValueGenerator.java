package sqlrunner.flatfileimport.generator;

public interface AutoValueGenerator {

	/** sets the start value for generated values */
	void setStartValue(String startValue);
	
	/** set direction: ascending or descending values will be generated */
	void setDirectionToDescending(boolean descending);
	
	/** resets the generator to his start value */
	void reset();
	
	/** creates the next value */
	String getNext();
	
}
