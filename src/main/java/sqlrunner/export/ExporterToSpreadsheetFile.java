package sqlrunner.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExporterToSpreadsheetFile {
	
	private static final Logger logger = Logger.getLogger(ExporterToSpreadsheetFile.class);
	
	public static enum SpreadsheetTyp {XLS, XLSX};
	private SpreadsheetTyp currentInputType;
	private SpreadsheetTyp currentOutputType;
	private FileInputStream fin;
	private FileOutputStream fout;
	private File inputFile;
	private File outputFile;
	private Workbook workbook;
	private Sheet sheet;
	private String targetSheetName;
	private Row row;
	private int currentRowIndex = 0;
	private boolean exportDateTypeAsString = false;
	private String dateFormatPattern = "dd.MM.yyyy HH.mm.ss";
	private SimpleDateFormat sdf = new SimpleDateFormat(dateFormatPattern);
	private CreationHelper createHelper;
	private CellStyle cellDateStyle;
	
	public void setDateFormat(String pattern) {
		if (pattern != null) {
			sdf = new SimpleDateFormat(pattern);
			dateFormatPattern = pattern;
			setupDateFormatStyle();
		} else {
			exportDateTypeAsString = false;
		}
	}

	public void setExportDateAsString() {
		exportDateTypeAsString = true;
	}
	
	public void setExportDateAsDate() {
		exportDateTypeAsString = false;
	}
	
	public void setTargetSheetName(String name) throws IOException {
		this.targetSheetName = name;
		setSheet();
	}
	
	public void setOutputFile(File outputFile) throws Exception {
		this.outputFile = outputFile;
		currentOutputType = getSpreadsheetType(outputFile);
		if (currentInputType != null) {
			if (currentInputType != currentOutputType) {
				throw new Exception("Workbook cannot be saved into a different type for input");
			}
		}
	}
	
	public void setInputFile(File inputFile) throws Exception {
		this.inputFile = inputFile;
		fin = new FileInputStream(inputFile);
		currentInputType = getSpreadsheetType(inputFile);
		if (currentOutputType != null) {
			if (currentInputType != currentOutputType) {
				logger.error("Input and Output must have the same file type!");
				throw new Exception("Input and Output must have the same file type!");
			}
		} else {
			currentOutputType = currentInputType;
		}
	}
	
	private SpreadsheetTyp getSpreadsheetType(File f) {
		SpreadsheetTyp type = null;
		if (f.getName().toLowerCase().endsWith(".xls")) {
			type = SpreadsheetTyp.XLS;
		} else if (f.getName().toLowerCase().endsWith(".xlsx")) {
			type = SpreadsheetTyp.XLSX;
		}
		return type;
	}
	
	public void setWorkbook() throws IOException {
		if (fin != null) {
			if (currentOutputType == SpreadsheetTyp.XLS) {
				logger.debug("Create XLS workbook from file " + inputFile.getAbsolutePath());
				workbook = new HSSFWorkbook(fin);
			} else if (currentOutputType == SpreadsheetTyp.XLSX) {
				logger.debug("Create XLSX workbook from file " + inputFile.getAbsolutePath());
				workbook = new XSSFWorkbook(fin);
			}
		} else {
			if (currentOutputType == SpreadsheetTyp.XLS) {
				logger.debug("Create XLS workbook");
				workbook = new HSSFWorkbook();
			} else if (currentOutputType == SpreadsheetTyp.XLSX) {
				logger.debug("Create XLSX workbook");
				workbook = new XSSFWorkbook();
			}
		}
		setupDateFormatStyle();
	}
	
	private void setupDateFormatStyle() {
		if (workbook != null) {
			createHelper = workbook.getCreationHelper();
			cellDateStyle = workbook.createCellStyle();
			cellDateStyle.setDataFormat(createHelper.createDataFormat().getFormat(dateFormatPattern));
		}
	}
	
	public void setSheet() throws IOException {
		if (workbook == null) {
			setWorkbook();
		}
		if (targetSheetName != null) {
			sheet = workbook.getSheet(targetSheetName);
			if (sheet == null) {
				logger.debug("Create sheet with name: " + targetSheetName);
				workbook.createSheet(targetSheetName);
			}
		} else {
			logger.debug("Create sheet with default name");
			sheet = workbook.createSheet();
		}
	}
	
	public void writeRow(List<Object> listValues) throws IOException {
		Object[] oneRow = listValues.toArray();
		writeRow(oneRow);
	}
	
	public void writeRow(Object[] listValues) throws IOException {
		if (sheet == null) {
			setSheet();
		}
		logger.debug("Create new row with index=" + currentRowIndex);
		row = sheet.createRow(currentRowIndex++);
		int cellIndex = 0;
		for (Object value : listValues) {
			Cell cell = row.createCell(cellIndex++);
			if (value instanceof String) {
				cell.setCellValue((String) value);
				cell.setCellType(Cell.CELL_TYPE_STRING);
			} else if (value instanceof Integer) {
				cell.setCellValue((Integer) value);
				cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			} else if (value instanceof Boolean) {
				cell.setCellValue((Integer) value);
				cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
			} else if (value instanceof Double) {
				cell.setCellValue((Double) value);
				cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			} else if (value instanceof Number) {
				cell.setCellValue(Double.valueOf(((Number) value).doubleValue()));
				cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			} else if (value instanceof java.util.Date) {
				if (exportDateTypeAsString) {
					String s = sdf.format((java.util.Date) value);
					cell.setCellValue(s);
					cell.setCellType(Cell.CELL_TYPE_STRING);
				} else {
				    cell.setCellStyle(cellDateStyle);
					cell.setCellValue((java.util.Date) value);
				}
			} else if (value != null) {
				cell.setCellValue((String) value.toString());
				cell.setCellType(Cell.CELL_TYPE_STRING);
			}
		}
	}

	public void writeWorkbook() throws Exception {
		logger.debug("Write workbook");
		fout = new FileOutputStream(outputFile);
		workbook.write(fout);
		fout.flush();
	}
	
	public void close() {
		logger.debug("Close workbook and files");
		if (fin != null) {
			try {
				fin.close();
			} catch (Exception e) {
				logger.warn("closing input file failed: " + e.getMessage(), e);
			}
		}
		if (fout != null) {
			try {
				fout.close();
			} catch (Exception e) {
				logger.warn("closing output file failed: " + e.getMessage(), e);
			}
		}
	}
	
}
