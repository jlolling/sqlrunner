package sqlrunner.flatfileimport;

import java.io.File;
import java.io.FileInputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XLSXFileDatasetProvider extends XLSFileDatasetProvider {

	@Override
	protected Workbook createWorkbook(File file) throws Exception {
		Workbook book = new XSSFWorkbook(new FileInputStream(file));
		return book;
	}
	
}
