package sqlrunner.log4jpanel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 *
 * @author jan
 */
public class Log4JModel extends AbstractTableModel {

	private static final Logger logger = LogManager.getLogger(Log4JModel.class);
	private static final long serialVersionUID = 1L;
	private List<Logger> loggerList = new ArrayList<Logger>();
    
	public void init() {
    	logger.debug("init");
    	loggerList = Log4J2Util.getAllLoggers();
        fireTableDataChanged();
    }

    public int getRowCount() {
        return loggerList.size();
    }

    public int getColumnCount() {
        return 2;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }
    
    @Override
    public Class<String> getColumnClass(int columnIndex) {
        return String.class;
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0: return "LoggerName";
            case 1: return "Level";
            default: return "unknown";
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Logger logger = loggerList.get(rowIndex);
        if (columnIndex == 0) {
            return logger.getName();
        } else if (columnIndex == 1) {
            if (logger.getLevel() != null) {
                return logger.getLevel().toString();
            } else {
                return null;
            }
        } else {
            return "unknown columnIndex=" + columnIndex;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Logger logger = loggerList.get(rowIndex);
        if (Level.ALL.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.ALL);
        } else if (Level.TRACE.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.TRACE);
        } else if (Level.DEBUG.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.DEBUG);
        } else if (Level.ERROR.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.ERROR);
        } else if (Level.FATAL.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.FATAL);
        } else if (Level.INFO.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.INFO);
        } else if (Level.OFF.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.OFF);
        } else if (Level.WARN.toString().equals(value)) {
        	Configurator.setLevel(logger.getName(), Level.WARN);
        } 
    }
    
    
  
}
