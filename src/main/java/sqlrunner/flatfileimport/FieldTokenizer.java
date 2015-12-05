package sqlrunner.flatfileimport;

import java.util.List;

public interface FieldTokenizer {

    public int countDelimitedFields() throws ParserException;
    
    public boolean parseRawData(List<FieldDescription> fieldDescriptionList) throws ParserException;

    public boolean parseRawData(List<FieldDescription> fieldDescriptionList, boolean skipConverting) throws ParserException;
    
    /**
     * parse the raw data by the given field descriptions
     * @param rawdata
     * @return true if the parser has data which should be stored in the database
     * @throws ParserException will be thrown if the parser found any failures in the rawdata
     */
    public boolean parseRawData(Object rawdata) throws ParserException;
    
    public void setFieldDescriptions(List<FieldDescription> listOfDescriptions);
        
    public void setRowData(Object rowData);
    
    /**
	 * returns the field related to the field description with the same index
	 * 
	 * @param index
	 * @return field data
	 */
	public Object getData(int index);

    public void setTestMode(boolean testMode);
    
    public boolean isTestMode();

}
