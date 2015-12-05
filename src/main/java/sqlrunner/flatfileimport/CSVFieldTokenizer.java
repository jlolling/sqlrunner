package sqlrunner.flatfileimport;

import java.util.List;

import org.apache.log4j.Logger;

/**
 * parser for a String-line
 */
public class CSVFieldTokenizer extends AbstractFieldTokenizer {

	private static final Logger logger = Logger.getLogger(CSVFieldTokenizer.class);
	private String rowData;
    private char[] data;
	private int lastPos;
	private String fieldDelimiter;
    private char[] delimiterChars;
	private String enclosure;
    private char[] enclosureChars;
    String combinedDelimiter = null;
    
	public CSVFieldTokenizer() {
        setRowData(null);
        setDelimiter(null);
        setEnclosure(null);
	}

    /**
     * sets the data of a dataset (row)
     * @param rowData_loc
     */
	public void setRowData(Object rowData) {
		if (logger.isDebugEnabled()) {
			logger.debug("setRowData(<" + rowData + ">)");
		}
        if (rowData instanceof String) {
            this.rowData = (String) rowData;
            data = getChars(this.rowData);
        } else if (rowData != null) {
            throw new IllegalArgumentException("rowData must be a String");
        } else {
            this.rowData = null;
            data = getChars(null);
        }
	}

	/**
     * retrieves a value between delimiters
     * delimiter is defined by field descriptions (in the common part)
     * @param fieldNum (starts with 0)
	 * @return content without delimiter
	 */
	public String extractDataBetweenDelimiters(int fieldNum) throws ParserException {
		return extractDataAtDelimiter(fieldNum, 0);
	}
    
    private char[] getChars(String s) {
        if (s == null) {
            return new char[0];
        } else {
            return s.toCharArray();
        }
    }

	private String extractDataAtDelimiter(int fieldNum, int length) throws ParserException {
        String value = null;
        int countDelimiters = 0;
        boolean inField = false;
        boolean atEnclosureStart = false;
        boolean atEnclosureStop = false;
        boolean atDelimiter = false;
        boolean useEnclosure = enclosureChars.length > 0;
        boolean fieldStartsWithEnclosure = false;
        int currPos = 0;
        while (currPos < data.length && countDelimiters <= fieldNum) {
            if (atEnclosureStart) {
                atEnclosureStart = false;
                fieldStartsWithEnclosure = true;
                currPos = currPos + enclosureChars.length;
                atEnclosureStop = startsWith(data, enclosureChars, currPos);
                if (atEnclosureStop == false) {
                    // don't check delimiter here because these chars are part of value
                    inField = true;
                }
            } else if (atEnclosureStop) {
                atEnclosureStop = false;
                currPos = currPos + enclosureChars.length;
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atDelimiter == false && currPos < data.length) {
                    throw new ParserException("delimiter after enclosure stop missing");
                }
            } else if (atDelimiter) {
                countDelimiters++;
                fieldStartsWithEnclosure = false;
                currPos = currPos + delimiterChars.length;
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atDelimiter == false) {
                    if (useEnclosure && currPos < data.length) {
                        atEnclosureStart = startsWith(data, enclosureChars, currPos);
                        if (atEnclosureStart == false) {
                            inField = true;
                        }
                    } else {
                        inField = true;
                    }
                }
            } else if (inField) {
                StringBuilder sb = null;
                if (countDelimiters == fieldNum) {
                    sb = new StringBuilder();
                }
                while (currPos < data.length) {
                    if (sb != null) {
                        sb.append(data[currPos]);
                    }
                    currPos++;
                    if (fieldStartsWithEnclosure) {
                        atEnclosureStop = startsWith(data, enclosureChars, currPos);
                        if (atEnclosureStop) {
                            break;
                        }
                    } else {
                        atDelimiter = startsWith(data, delimiterChars, currPos);
                        if (atEnclosureStart || atDelimiter) {
                            break;
                        }
                    }
                }
                inField = false;
                if (sb != null) {
                    value = sb.toString();
                }
            } else {
                if (useEnclosure) {
                    atEnclosureStart = startsWith(data, enclosureChars, currPos);
                }
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atEnclosureStart == false && atDelimiter == false) {
                    inField = true;
                }
            }
        }
        if (length > 0 && value != null && value.length() > length) {
            value = value.substring(0, length);
        }
        return value;
    }
    
    public static boolean startsWith(char[] data, char[] search, int startPos) {
        if (search.length == 0 || data.length == 0) {
            return false;
        }
        if (startPos < 0 || startPos > (data.length - search.length)) {
            return false;
        }
        int searchPos = 0;
        int count = search.length;
        int dataPos = startPos;
        while (--count >= 0) {
            if (data[dataPos++] != search[searchPos++]) {
                return false;
            }
        }
        return true;
    }
    
    public int countDelimitedFields() throws ParserException {
        int countFields = data.length > 0 ? 1 : 0;
        boolean inField = false;
        boolean atEnclosureStart = false;
        boolean atEnclosureStop = false;
        boolean atDelimiter = false;
        boolean useEnclosure = enclosureChars.length > 0;
        boolean fieldStartsWithEnclosure = false;
        int currPos = 0;
        while (currPos < data.length) {
            if (atEnclosureStart) {
                atEnclosureStart = false;
                fieldStartsWithEnclosure = true;
                currPos = currPos + enclosureChars.length;
                atEnclosureStop = startsWith(data, enclosureChars, currPos);
                if (atEnclosureStop == false) {
                    // don't check delimiter here because these chars are part of value
                    inField = true;
                }
            } else if (atEnclosureStop) {
                atEnclosureStop = false;
                currPos = currPos + enclosureChars.length;
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atDelimiter == false && currPos < data.length) {
                    throw new ParserException("delimiter after enclosure stop missing");
                }
            } else if (atDelimiter) {
                countFields++;
                fieldStartsWithEnclosure = false;
                currPos = currPos + delimiterChars.length;
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atDelimiter == false) {
                    if (useEnclosure && currPos < data.length) {
                        atEnclosureStart = startsWith(data, enclosureChars, currPos);
                        if (atEnclosureStart == false) {
                            inField = true;
                        }
                    } else {
                        inField = true;
                    }
                }
            } else if (inField) {
                while (currPos < data.length) {
                    currPos++;
                    if (fieldStartsWithEnclosure) {
                        atEnclosureStop = startsWith(data, enclosureChars, currPos);
                        if (atEnclosureStop) {
                            break;
                        }
                    } else {
                        atDelimiter = startsWith(data, delimiterChars, currPos);
                        if (atEnclosureStart || atDelimiter) {
                            break;
                        }
                    }
                }
                inField = false;
            } else {
                if (useEnclosure) {
                    atEnclosureStart = startsWith(data, enclosureChars, currPos);
                }
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atEnclosureStart == false && atDelimiter == false) {
                    inField = true;
                }
            }
        }
        return countFields;
    }
    

	private String extractDataAtAbsPos(int position, int length) {
		lastPos = position + length;
		if (lastPos > rowData.length()) {
			lastPos = rowData.length();
		}
		return rowData.substring(position, lastPos);
	}

	private String extractDataAtLastPos(int length) {
		int beginIndex;
		if (length == 0) {
			return null;
		} else if (length < 0) {
			beginIndex = lastPos + length;
			if (beginIndex < 0) {
				beginIndex = 0;
			}
			return rowData.substring(beginIndex, lastPos);
		} else {
			beginIndex = lastPos;
			lastPos = beginIndex + length;
			if (lastPos > rowData.length()) {
				lastPos = rowData.length();
			}
			return rowData.substring(beginIndex, lastPos);
		}
	}

    /**
     * use the given rowData (see Constructor)
     * @param field which contains the extraction description
     * @return String data of appropriated field
     */
	private String extractData(FieldDescription field) throws ParserException {
		if (logger.isDebugEnabled()) {
			logger.debug("extractData(field="+field.getName()+") rawdata=<"+rowData+">");
		}
		String value = null;
		if (field.getPositionType() == FieldDescription.ABSOLUTE_POSITION) {
			value = extractDataAtAbsPos(field.getAbsPos(), field.getLength());
		} else if (field.getPositionType() == FieldDescription.RELATIVE_POSITION) {
			value = extractDataAtLastPos(field.getLength());
		} else if (field.getPositionType() == FieldDescription.DELIMITER_POSITION) {
			value = extractDataBetweenDelimiters(field.getDelimPos());
		} else if (field.getPositionType() == FieldDescription.DELIMITER_POSITION_WITH_LENGTH) {
			value = extractDataAtDelimiter(field.getDelimPos(), field.getLength());
		} else if (field.getPositionType() == FieldDescription.AUTO_GENERATED) {
			value = field.getNextAutoGeneratedValue();
		} else if (field.getPositionType() == FieldDescription.FIX_VALUE) {
			value = field.getDefaultValue();
		}
        if (value != null && field.isTrimRequired()) {
            value = value.trim();
        }
		if (value == null || value.trim().length() == 0) {
            if (field.getAlternativeFieldDescription() != null) {
                // check if there is an alternative field
                value = extractData(field.getAlternativeFieldDescription());
            }
            if (value == null || value.trim().length() == 0) {
       			value = field.getDefaultValue();
            }
		} else {
			value = filter(field.getFilterPattern(), value);
		}
        if (BasicDataType.isNumberType(field.getBasicTypeId())) {
            if (value != null && value.trim().endsWith("-")) {
                value = "-" + value.replace('-', ' ').trim();
            }
        }
		if (logger.isDebugEnabled()) {
			logger.debug("getData returns <" + value + ">");
		}
		return value;
	}
    
    public boolean parseRawData(List<FieldDescription> fieldDescriptionList) throws ParserException {
        return parseRawData(fieldDescriptionList, false);
    }

	/**
	 * uses the given rowData (see Constructor)
	 * 
	 * @param fieldDescriptionList List of FieldDescriptions
     * these field descriptions replaces all other previously given descriptions
     * @param skipConverting if true parser don't try to convert the text value into the target data type
     * this is necessary to show column names in the first rows e.g.
	 */
	public boolean parseRawData(List<FieldDescription> fieldDescriptionList, boolean skipConverting) throws ParserException {
        if (logger.isDebugEnabled()) {
            logger.debug("parseRawData fieldList.size=" + fieldDescriptionList.size() + " skipConverting=" + skipConverting);
        }
		setFieldDescriptions(fieldDescriptionList);
		clearListData();
		if (rowData != null) {
			// das Array durchgehen und die Felddaten einlesen
            FieldDescription fd = null;
			for (int i = 0; i < getCountFieldDescriptions(); i++) {
                fd = getFieldDescriptionAt(i);
                try {
                    if (skipConverting) {
        				addDataValue(extractData(fd));
                    } else {
            			addDataValue(convertStringValue(fd, extractData(fd)));
                    }
                } catch (ParserException e) {
                	if (fd.isIgnoreDatasetIfInvalid()) {
                		return false;
                	} else {
                		throw e;
                	}
                }
			}
			return true;
		} else {
			return false;
		}
	}
    
    /**
     * parse rawdata (one row) and uses the given list of field descriptions
     * @param rawdata to be analysed
     */
	public boolean parseRawData(Object rawdata) throws ParserException {
		if (logger.isDebugEnabled()) {
			logger.debug("parseRowData rawdata=<"+rawdata+">");
		}
		if (hasFieldDescriptions() == false) {
			throw new IllegalStateException("descriptions cannot be empty or null");
		}
		setRowData(rawdata);
		clearListData();
		if (rowData != null) {
			// das Array durchgehen und die Felddaten einlesen
            FieldDescription fd = null;
			for (int i = 0; i < getCountFieldDescriptions(); i++) {
                fd = getFieldDescriptionAt(i);
                try {
       				addDataValue(convertStringValue(fd, extractData(fd)));
                } catch (ParserException e) {
                	if (fd.isIgnoreDatasetIfInvalid()) {
                		return false;
                	} else {
                		throw e;
                	}
                }
			}
			return true;
		} else {
			return false;
		}
	}
    
    /**
     * delimiter to be used for field separations
     * @param delimiter
     */
	public void setDelimiter(String delimiter) {
		if (logger.isDebugEnabled()) {
			logger.debug("setDelimiter(<" + delimiter + ">)");
		}
		this.fieldDelimiter = delimiter;
        delimiterChars = getChars(fieldDelimiter);
	}

	public String getDelimiter() {
		return fieldDelimiter;
	}

    /**
     * sets the text enclosure (e.g. " or ') 
     * @param enclosure
     */
	public void setEnclosure(String enclosure) {
		this.enclosure = enclosure;
        enclosureChars = getChars(enclosure);
	}
    
    public String getEnclosure() {
        return enclosure;
    }     

}
