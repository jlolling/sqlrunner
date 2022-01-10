package sqlrunner.flatfileimport.generator;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;


public class NumberValueGenerator implements AutoValueGenerator {

	private static final Logger logger = LogManager.getLogger(NumberValueGenerator.class);
	private long currentValue = 0;
	private long startLongValue = 0;
	private boolean descending = false;
	
	public synchronized String getNext() {
		if (descending) {
			return String.valueOf(currentValue--);
		} else {
			return String.valueOf(currentValue++);
		}
	}

	public synchronized void reset() {
		currentValue = 0;
	}

	public void setDirectionToDescending(boolean descending) {
		this.descending = descending;
	}
	
	public boolean isDirectionDescending() {
		return descending;
	}

	public synchronized void setStartValue(String startValue) {
		try {
			startLongValue = Long.parseLong(startValue);
			currentValue = startLongValue;
		} catch (Exception e) {
			logger.error("setStartValue invalid value:" + startValue);
		}
	}

}
