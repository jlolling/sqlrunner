import java.io.File;

import sqlrunner.flatfileimport.CSVFileDatasetProvider;
import sqlrunner.flatfileimport.ImportAttributes;


public class CSVDatasetProviderTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testEnclosedFile2();
	}
	
	public static void testEnclosedFile() {
		CSVFileDatasetProvider p = new CSVFileDatasetProvider();
		File file = new File("/home/jlolling/test/text/enclosed_text.csv");
		ImportAttributes attr = new ImportAttributes();
		attr.setCharsetName("UTF-8");
		attr.setEnclosure("\"");
		attr.setIgnoreLineBreakInEnclosedValues(true);
		try {
			p.setupDatasetProvider(file, false, attr);
			String line = null;
			while ((line = (String) p.getNextDataset()) != null) {
				System.out.println(line);
				System.out.println("----------------");
			}
			p.closeDatasetProvider();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void testEnclosedFile2() {  
		CSVFileDatasetProvider p = new CSVFileDatasetProvider();
		File file = new File("/home/jlolling/test/text/enclosed_text.csv");
		ImportAttributes attr = new ImportAttributes();
		attr.setCharsetName("UTF-8");
		attr.setEnclosure("\"");
		attr.setIgnoreLineBreakInEnclosedValues(true);
		try {
			p.setupDatasetProvider(file, true, attr);
			String line = null;
			int i = 0;
			while ((line = (String) p.getDatasetAtRowInTestMode(i)) != null) {
				System.out.println(line);
				System.out.println("----------------");
				i++;
			}
			i--;
			System.out.println("##########################");
			while ((line = (String) p.getDatasetAtRowInTestMode(i)) != null) {
				System.out.println(line);
				System.out.println("----------------");
				i--;
			}
			p.closeDatasetProvider();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
