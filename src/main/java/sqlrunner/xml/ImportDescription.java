package sqlrunner.xml;

import java.io.File;

import sqlrunner.datamodel.SQLTable;

/**
 * @author lolling.jan
 */
class ImportDescription {

    private SQLTable table;
    private File xmlFile;

    public SQLTable getTable() {
        return table;
    }

    public void setTable(SQLTable table_loc) {
        this.table = table_loc;
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public void setXmlFile(File xmlFile_loc) {
        this.xmlFile = xmlFile_loc;
    }
    
    @Override
    public String toString() {
        return "table="+table+" xmlfile="+xmlFile;
    }
}
