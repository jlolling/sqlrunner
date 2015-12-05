package sqlrunner.text;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility class to parse a String into a Date 
 * by testing a number of common pattern
 * This class is thread save.
 * 
 * @author jan.lolling@gmail.com
 */
public class GenericDateUtil {
	
	private static ThreadLocal<DateParser> threadLocal = new ThreadLocal<DateParser>();
	
	public static Date parseDate(String source) throws ParseException {
		return parseDate(source, null);
	}

	public static Date parseDate(String source, String pattern) throws ParseException {
		DateParser p = threadLocal.get();
		if (p == null) {
			p = new DateParser();
			threadLocal.set(p);
		}
		return p.parseDate(source, pattern);
	}
	
	static class DateParser {
		
		private List<String> datePatternList = null;
		private List<String> timePatternList = null;

		DateParser() {
			datePatternList = new ArrayList<String>();
			datePatternList.add("yyyy-MM-dd");
			datePatternList.add("dd.MM.yyyy");
			datePatternList.add("d.MM.yyyy");
			datePatternList.add("d.M.yy");
			datePatternList.add("dd.MM.yy");
			datePatternList.add("dd.MMM.yyyy");
			datePatternList.add("yyyyMMdd");
			datePatternList.add("dd/MM/yyyy");
			datePatternList.add("dd/MM/yy");
			datePatternList.add("dd/MMM/yyyy");
			datePatternList.add("d/M/yy");
			datePatternList.add("MM/dd/yyyy");
			datePatternList.add("MM/dd/yy");
			datePatternList.add("dd/MMM/yyyy");
			datePatternList.add("M/d/yy");
			datePatternList.add("dd-MM-yyyy");
			datePatternList.add("dd-MM-yy");
			datePatternList.add("dd-MMM-yyyy");
			datePatternList.add("d-M-yy");
			timePatternList = new ArrayList<String>();
			timePatternList.add("HHmmss");
			timePatternList.add("HH:mm:ss");
			timePatternList.add("HH:mm:ss.SSS");
			timePatternList.add("'T'HH:mm:ss.SSSZ");
		}

		public Date parseDate(String text, String userPattern) throws ParseException {
			if (text != null) {
				SimpleDateFormat sdf = new SimpleDateFormat();
				Date dateValue = null;
				if (userPattern != null && userPattern.isEmpty() == false) {
					if (datePatternList.contains(userPattern) == false) {
						datePatternList.add(0, userPattern);
					}
				}
				for (String pattern : datePatternList) {
					sdf.applyPattern(pattern);
					try {
						dateValue = sdf.parse(text);
						// if we continue here the pattern fits
						// set this pattern at the top of the list
						int pos = datePatternList.indexOf(pattern);
						if (pos > 0) {
							datePatternList.remove(pos);
							datePatternList.add(0, pattern);
						}
						return dateValue;
					} catch (Exception e) {
						// the pattern obviously does not work
						continue;
					}
				}
				throw new ParseException("The value: " + text + " could not parsed to a Date.", 0);
			} else {
				return null;
			}
		}

	}
	
}