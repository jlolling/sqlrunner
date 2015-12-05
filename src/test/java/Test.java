import java.text.SimpleDateFormat;
import java.util.Date;

import sqlrunner.generator.SQLCodeGenerator;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
