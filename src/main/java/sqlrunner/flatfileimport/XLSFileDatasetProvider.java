package sqlrunner.flatfileimport;

import java.io.File;
import java.io.FileInputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class XLSFileDatasetProvider implements DatasetProvider {
	
	protected Sheet workSheet = null;
	private Workbook book;
	private FileInputStream fileInputStream;
	protected int currentRowNum = -1;
	
	@Override
	public long retrieveDatasetCount() throws Exception {
		if (workSheet == null) {
			throw new IllegalStateException("setupDatasetProvider not performed");
		}
		return workSheet.getLastRowNum() + 1;
	}
	
	protected Workbook createWorkbook(File file) throws Exception {
		fileInputStream = new FileInputStream(file);
		book = new HSSFWorkbook(fileInputStream);
		return book;
	}
	
	@Override
	public void closeDatasetProvider() {
		book = null;
		try {
			fileInputStream.close();
		} catch (Exception e) {
			// nothing to do
		}
		workSheet = null;
	}

	@Override
	public FieldTokenizer createParser() {
		XLSFieldParser parser = new XLSFieldParser();
		return parser;
	}

	@Override
	public Object getNextDataset() throws Exception {
		currentRowNum++;
		Row row = workSheet.getRow(currentRowNum);
		return row;
	}

	@Override
	public void setupDatasetProvider(File file, boolean testMode, ImportAttributes properties) throws Exception {
		book = createWorkbook(file);
		if (properties.getSheetName() != null) {
			workSheet = book.getSheet(properties.getSheetName());
			if (workSheet == null) {
				throw new Exception("No sheet with name=" + properties.getSheetName() + " available");
			}
		} else {
			workSheet = book.getSheetAt(0);
			if (workSheet == null) {
				throw new Exception("No sheet at index 0 available");
			}
		}
		currentRowNum = -1;
	}

	@Override
	public long getCurrentRowNum() {
		return currentRowNum;
	}

	@Override
	public Object getDatasetAtRowInTestMode(long rowNumber) throws Exception {
        if (rowNumber < 0) {
            throw new IllegalArgumentException("rowNumber cannot be less zero");
        }
		currentRowNum = (int) rowNumber;
		if (workSheet != null) {
			return workSheet.getRow(currentRowNum);
		} else {
			return null;
		}
	}

}
