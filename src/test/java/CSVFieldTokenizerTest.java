import java.util.ArrayList;
import java.util.List;

import sqlrunner.flatfileimport.BasicDataType;
import sqlrunner.flatfileimport.CSVFieldTokenizer;
import sqlrunner.flatfileimport.FieldDescription;
import sqlrunner.flatfileimport.ParserException;


public class CSVFieldTokenizerTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CSVFieldTokenizer t = new CSVFieldTokenizer();
		t.setDelimiter("|");
		t.setEnclosure("\"");
		FieldDescription f1 = new FieldDescription();
		f1.setPositionType(FieldDescription.DELIMITER_POSITION);
		f1.setDelimPos(0);
		f1.setBasicTypeId(BasicDataType.CHARACTER.getId());
		FieldDescription f2 = new FieldDescription();
		f2.setPositionType(FieldDescription.DELIMITER_POSITION);
		f2.setDelimPos(1);
		f2.setBasicTypeId(BasicDataType.CHARACTER.getId());
		FieldDescription f3 = new FieldDescription();
		f3.setPositionType(FieldDescription.DELIMITER_POSITION);
		f3.setDelimPos(2);
		f3.setBasicTypeId(BasicDataType.CHARACTER.getId());
		FieldDescription f4 = new FieldDescription();
		f4.setPositionType(FieldDescription.DELIMITER_POSITION);
		f4.setDelimPos(3);
		f4.setBasicTypeId(BasicDataType.CHARACTER.getId());
		List<FieldDescription> list = new ArrayList<FieldDescription>();
		list.add(f1);
		list.add(f2);
		list.add(f3);
		list.add(f4);
		t.setFieldDescriptions(list);
		try {
			t.parseRawData("f1|f2|\"\"f3\"\"|f4");
			for (int i = 0; i < t.countDelimitedFields(); i++) {
				System.out.println(t.getData(i));
			}
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
