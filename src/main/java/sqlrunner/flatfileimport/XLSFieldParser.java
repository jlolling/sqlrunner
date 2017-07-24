package sqlrunner.flatfileimport;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
/**
 *
 * @author jan
 */
public class XLSFieldParser extends AbstractFieldTokenizer {

   	private Row row = null;
    private NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
    private final Logger logger = Logger.getLogger(XLSFieldParser.class);

    public XLSFieldParser() {
        nf.setGroupingUsed(false);
    }

    @Override
	public int countDelimitedFields() throws ParserException {
        if (row != null) {
            return row.getLastCellNum();
        } else {
            return 0;
        }
    }

    @Override
	public boolean parseRawData(List<FieldDescription> fieldDescriptionList) throws ParserException {
    	return parseRawData(fieldDescriptionList, false);
    }

    @Override
	public boolean parseRawData(List<FieldDescription> fieldDescriptionList, boolean skipConverting) throws ParserException {
        setFieldDescriptions(fieldDescriptionList);
    	return parseRowData(skipConverting);
    }
    
    @Override
	public boolean parseRawData(Object rowData) throws ParserException {
    	setRowData(rowData);
    	return parseRowData(false);
    }

    @Override
	public void setRowData(Object rowData) {
        if (rowData instanceof Row) {
        	row = (Row) rowData;
        } else if (rowData != null) {
        	throw new IllegalArgumentException("rowData must be an instanceof HSSFRow");
        } else {
        	row = null;
        }
    }

    private boolean parseRowData(boolean skipConverting) throws ParserException {
		clearListData();
    	if (row != null) {
    		boolean valuesAdded = false;
            for (int i = 0; i < getCountFieldDescriptions(); i++) {
            	FieldDescription fd = getFieldDescriptionAt(i);
                try {
                    if (addDataValue(extractData(row, fd, skipConverting))) {
                    	valuesAdded = true;
                    }
                } catch (Exception e) {
                    if (isTestMode()) {
                        String message = e.getClass().getName();
                        if (e.getMessage() != null) {
                            message = " " + e.getMessage();
                        }
                        addDataValue("ERROR: " + message);
                        valuesAdded = true;
                        if (e instanceof RuntimeException) {
                            logger.error("parseRowData failed: + " + e.getMessage(), e);
                        }
                    } else if (fd.isIgnoreDatasetIfInvalid()) {
                    	return false;
                    } else {
                        throw new ParserException(e);
                    }
                }
            }
            return valuesAdded;
    	} else {
    		return false;
    	}
    }
    
    @SuppressWarnings("deprecation")
	private Object getCellValue(Cell cell, FieldDescription fd, boolean skipConverting) throws Exception {
        Object value = null;
        if (cell == null) {
            return null;
        }
    	if (logger.isDebugEnabled()) {
    		logger.debug("getCellValue cell:" + cell.toString() + " field:" + fd.toString());
    	}
        if (fd.getBasicTypeId() == BasicDataType.CHARACTER.getId()) {
            String s = "";
            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                s = cell.getRichStringCellValue().getString();
            } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                s = nf.format(cell.getNumericCellValue());
            } else {
                try {
                    java.util.Date d = cell.getDateCellValue();
                    s = d.toString();
                } catch (Exception e) {
                }
            }
            if (fd.isTrimRequired()) {
                s = s.trim();
            }
            if (fd.getPositionType() == FieldDescription.DELIMITER_POSITION_WITH_LENGTH) {
                if (fd.getLength() > 0 && s != null && s.length() > fd.getLength()) {
                    s = s.substring(0, fd.getLength());
                }
            }
            if (s != null && s.length() > 0) {
                value = filter(fd.getFilterPattern(), s);
            }
        } else if (fd.getBasicTypeId() == BasicDataType.DATE.getId()) {
            if (skipConverting) {
                value = cell.getRichStringCellValue().getString();
            } else {
                value = cell.getDateCellValue();
            }
        } else if (fd.getBasicTypeId() == BasicDataType.BOOLEAN.getId()) {
            if (skipConverting) {
                value = cell.getRichStringCellValue().getString();
            } else if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
                value = cell.getBooleanCellValue();
            } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                value = "true".equals(cell.getRichStringCellValue().getString());
            } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            	value = cell.getNumericCellValue() > 0.000001d;
            }
        } else if (BasicDataType.isNumberType(fd.getBasicTypeId())) {
            if (skipConverting) {
                value = cell.getRichStringCellValue().getString();
            } else {
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    value = cell.getNumericCellValue();
                } else if (cell.getCellType() == Cell.CELL_TYPE_STRING || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                    String s = cell.getRichStringCellValue().getString();
                    if (s == null || s.length() == 0) {
                        value = 0d;
                    } else {
                        try {
                        	if (fd.getBasicTypeId() == BasicDataType.DOUBLE.getId()) {
                                value = Double.parseDouble(cell.getRichStringCellValue().getString());
                        	} else if (fd.getBasicTypeId() == BasicDataType.INTEGER.getId()) {
                                value = Integer.parseInt(cell.getRichStringCellValue().getString());
                        	} else if (fd.getBasicTypeId() == BasicDataType.LONG.getId()) {
                                value = Long.parseLong(cell.getRichStringCellValue().getString());
                        	}
                        } catch (NumberFormatException e) {
                            throw new ParserException(e);
                        }
                    }
                } else if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
                    value = cell.getBooleanCellValue() ? "true" : "false";
                } else {
                    throw new Exception("in field " + fd + " cell type cannot be used to convert into number");
                }
            }
        }
        return value;
    }
    
    /**
     * use the given rowData (see Constructor)
     * @param field which contains the extraction description
     * @return String data of appropriated field
     */
	private Object extractData(Row row, FieldDescription field, boolean skipConverting) throws Exception {
        Object value = null;
        if (field.getPositionType() == FieldDescription.DELIMITER_POSITION || field.getPositionType() == FieldDescription.DELIMITER_POSITION_WITH_LENGTH) {
            Cell cell = row.getCell(field.getDelimPos());
            value = getCellValue(cell, field, skipConverting);
        }
		if (value == null) {
            if (field.getAlternativeFieldDescription() != null) {
                // check if there is an alternative field
                value = extractData(row, field.getAlternativeFieldDescription(), skipConverting);
            }
            if (value == null) {
       			value = convertStringValue(field, field.getDefaultValue());
            }
        }
		return value;
	}
	
}
