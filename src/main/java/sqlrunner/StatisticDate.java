package sqlrunner;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Years;

public class StatisticDate implements Statistic {
	
	private long minValue = 0l;
	private long maxValue = 0l;
	private int countValues = 0;
	private long sumValue = 0l;
	private int countRows = 0;
	private SimpleDateFormat sdf = new SimpleDateFormat(MainFrame.getDateFormatMask());
	
	@Override
	public int getCount() {
		return countValues;
	}
	
	@Override
	public String render() {
		StringBuilder sb = new StringBuilder();
		sb.append("Count rows:");
		sb.append(countRows);
		sb.append(", Count values:");
		sb.append(countValues);
		sb.append(", Min: ");
		sb.append(sdf.format(new Date(minValue)));
		sb.append(", Max: ");
		sb.append(sdf.format(new Date(maxValue)));
		sb.append(", Dist: ");
		sb.append(renderDiff(minValue, maxValue));
		return sb.toString();
	}
	
	public long getDist() {
		return maxValue - minValue;
	}
	
	public void addValue(Object o) {
		countRows++;
		if (o instanceof Date) {
			try {
				long value = ((Date) o).getTime();
				if (countValues == 0) {
					minValue = value;
				}
				countValues++;
				if (minValue == 0 || minValue > value) {
					minValue = value;
				}
				if (maxValue < value) {
					maxValue = value;
				}
				sumValue = sumValue + value;
			} catch (Exception e) {}
		}
	}
	
	private String renderDiff(long min, long max) {
		DateTime start = new DateTime(min);
		DateTime end = new DateTime(max);
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		Years yearsDiff = Years.yearsBetween(start, end);
		Days daysDiff = Days.daysBetween(start.withTimeAtStartOfDay(), end.withTimeAtStartOfDay());
		Hours hoursDiff = Hours.hoursBetween(start.withMinuteOfHour(0), end.withMinuteOfHour(0));
		Minutes minutesDiff = Minutes.minutesBetween(start, end);
		if (yearsDiff.getYears() > 0) {
			sb.append(" Years: ");
			sb.append(yearsDiff.getYears());
		}
		if (daysDiff.getDays() > 0) {
			sb.append(" Days: ");
			sb.append(daysDiff.getDays());
		}
		if (hoursDiff.getHours() > 0) {
			sb.append(" Hours: ");
			sb.append(hoursDiff.getHours());
		}
		if (minutesDiff.getMinutes() > 0) {
			sb.append(" Minutes: ");
			sb.append(minutesDiff.getMinutes());
		}
		sb.append("]");
		return sb.toString();
	}


}
