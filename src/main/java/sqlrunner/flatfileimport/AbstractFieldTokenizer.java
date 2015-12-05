package sqlrunner.flatfileimport;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sqlrunner.text.GenericDateUtil;

/**
 *
 * @author jan
 */
public abstract class AbstractFieldTokenizer implements FieldTokenizer {

    private final ArrayList<Object> fieldDataList = new ArrayList<Object>();
    private boolean testMode = false;
    private List<FieldDescription> descriptions;
    private static String nullValue = "\\N";

    protected int getListDataSize() {
        return fieldDataList.size();
    }
    
    protected void clearListData() {
        fieldDataList.clear();
    }
    
    protected boolean addDataValue(Object value) {
        fieldDataList.add(value);
        return value != null;
    }
    
	/* (non-Javadoc)
	 * @see sqlrunner.flatfileimport.FieldTokenizer#getData(int)
	 */
	public Object getData(int fieldDescriptionIndex) {
		if ((fieldDataList != null) && (fieldDescriptionIndex < fieldDataList.size())) {
			return fieldDataList.get(fieldDescriptionIndex);
		} else {
			return null;
		}
	}

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public boolean isTestMode() {
        return testMode;
    }
    
    protected boolean hasFieldDescriptions() {
        return descriptions != null && descriptions.isEmpty() == false;
    }

    protected int getCountFieldDescriptions() {
        if (hasFieldDescriptions() == false) {
            return 0;
        } else {
            return descriptions.size();
        }
    }
    
    protected FieldDescription getFieldDescriptionAt(int index) {
        return descriptions.get(index);
    }

   	public void setFieldDescriptions(List<FieldDescription> listOfDescriptions) {
		if (listOfDescriptions == null || listOfDescriptions.isEmpty()) {
			throw new IllegalArgumentException("listOfDescriptions cannot be empty or null");
		}
		this.descriptions = listOfDescriptions;
	}
    
    /**
     * regex filter of a content
     * @param pattern
     * @param content
     * @return filtered text
     */
	protected static String filter(Pattern pattern, String content) {
		if (pattern != null) {
			if (content != null) {
				final StringBuffer sb = new StringBuffer();
		        Matcher matcher = pattern.matcher(content);
		        while (matcher.find()) {
		            if (matcher.start() < matcher.end()) {
		                sb.append(matcher.group());
		            }
		        }
		        content = sb.toString();
			}
		}
		if (nullValue.equals(content)) {
			content = null;
		}
		return content;
	}

    protected Object convertStringValue(FieldDescription fd, String strValue) throws ParserException {
        Object value = null;
        if (strValue != null && strValue.length() > 0) {
            if (BasicDataType.isNumberType(fd.getBasicTypeId())) {
                try {
                    NumberFormat nf = fd.getNumberFormat();
                    Number n = nf.parse(strValue);
                    value = n;
                } catch (Exception e) {
                    String message = "value=" + strValue + " parse as number failed: " + e.getMessage();
                    if (isTestMode()) {
                        value = "ERROR:" + message;
                    } else {
                        throw new ParserException(message);
                    }
                }
            } else if (fd.getBasicTypeId() == BasicDataType.DATE.getId()) {
                if (fd.getFieldFormat() == null || fd.getFieldFormat().length() == 0) {
                    throw new ParserException("date format not defined");
                }
                SimpleDateFormat sdf = new SimpleDateFormat(fd.getFieldFormat(), fd.getLocale());
                try {
                	Date date = null;
                	try {
                    	date = sdf.parse(strValue);
                	} catch (ParseException e0) {
                		date = GenericDateUtil.parseDate(strValue);
                	}
                    Timestamp timestamp = new Timestamp(date.getTime());
                    value = timestamp;
                } catch (ParseException e) {
                    String message = "value=" + strValue + " parse as date failed: " + e.getMessage();
                    if (isTestMode()) {
                        value = "ERROR:" + message;
                    } else {
                        throw new ParserException(message);
                    }
                }
            } else if (fd.getBasicTypeId() == BasicDataType.BOOLEAN.getId()) {
            	value = Boolean.parseBoolean(strValue);
            } else {
                value = strValue;
            }
        } else {
            if (fd.isNullEnabled() == false) {
                String message = "value for primary key field " + fd + " cannot be null";
                if (isTestMode()) {
                    value = "ERROR:" + message;
                } else {
                    throw new ParserException(message);
                }
            }
        }
        return value;
    }

}
