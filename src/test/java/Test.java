import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import sqlrunner.StatisticDate;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			testStatisticDate();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testStatisticDate() throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d1 = sdf.parse("2015-01-11 14:22:00");
		Date d2 = sdf.parse("2015-02-28 13:44:00");
		StatisticDate sd = new StatisticDate();
		sd.addValue(d1);
		sd.addValue(d2);
		System.out.println(sd.toString());
	}
	
	
	public static void test1() {
		byte[] ba = new byte[] {0x23, 0x34, 0x66};
		StringBuilder sb = new StringBuilder();
		for (byte b : ba) {
			sb.append(Integer.toHexString(b));
		}
		System.out.println(sb.toString());
	}
	
	public static String toHexString(byte[] bin) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bin) {
			sb.append(Integer.toHexString(b));
		}
		return sb.toString();
	}
	
}
