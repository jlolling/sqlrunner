package sqlrunner;

public class StatisticDouble implements Statistic {
	
	private double minValue = 0;
	private double maxValue = 0;
	private double sumValue = 0;
	private int countValues = 0;
	private int countRows = 0;
	
	@Override
	public int getCount() {
		return countRows;
	}
	
	@Override
	public String render() {
		StringBuilder sb = new StringBuilder();
		sb.append("Count rows:");
		sb.append(countRows);
		sb.append(", Count values:");
		sb.append(countValues);
		sb.append(", Sum: ");
		sb.append(sumValue);
		sb.append(", Min: ");
		sb.append(minValue);
		sb.append(", Max: ");
		sb.append(maxValue);
		sb.append(", Avg: ");
		sb.append(getAvg());
		sb.append(", Dist: ");
		sb.append(getDist());
		return sb.toString();
	}
	
	private double getAvg() {
		if (countValues > 0) {
			return sumValue / (double) countValues;
		} else {
			return 0d;
		}
	}
	
	private double getDist() {
		return maxValue - minValue;
	}
	
	public void addValue(Object o) {
		countRows++;
		if (o != null) {
			double value = 0;
			boolean ok = false;
			try {
				value = Double.parseDouble(o.toString());
				ok = true;
			} catch (NumberFormatException nfe) {
				value = 0;
			}
			if (ok) {
				if (countValues == 0) {
					minValue = value;
				}
				countValues++;
				if (minValue > value) {
					minValue = value;
				}
				if (maxValue < value) {
					maxValue = value;
				}
				sumValue = sumValue + value;
			}
		}
	}
	
}
