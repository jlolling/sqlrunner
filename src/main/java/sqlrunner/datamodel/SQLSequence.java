package sqlrunner.datamodel;

public class SQLSequence extends SQLObject {
	
	private int startsWith;
	private int endsWith;
	private int stepWith;

	public SQLSequence(SQLDataModel model, String name) {
		super(model, name);
	}

	public int getStartsWith() {
		return startsWith;
	}

	public void setStartsWith(int startsWith) {
		this.startsWith = startsWith;
	}

	public int getEndsWith() {
		return endsWith;
	}

	public void setEndsWith(int endsWith) {
		this.endsWith = endsWith;
	}

	public int getStepWith() {
		return stepWith;
	}

	public void setStepWith(int stepWith) {
		this.stepWith = stepWith;
	}
	
}
