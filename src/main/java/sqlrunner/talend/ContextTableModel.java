package sqlrunner.talend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.table.AbstractTableModel;

public class ContextTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private List<Entry<Object, Object>> model = new ArrayList<Map.Entry<Object, Object>>(); 

	@Override
	public int getRowCount() {
		return model.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			return model.get(rowIndex).getKey();
		} else {
			return model.get(rowIndex).getValue();
		}
	}
	
	public void remove(Map.Entry<Object, Object> entry) {
		model.remove(entry);
	}

	public void remove(List<Map.Entry<Object, Object>> listEntries) {
		model.removeAll(listEntries);
	}

	public void setData(Properties contextProperties, boolean replaceAll) {
		if (replaceAll) {
			model.clear();
			fireTableDataChanged();
		}
		for (Map.Entry<Object, Object> entry : contextProperties.entrySet()) {
			if (model.contains(entry)) {
				model.remove(entry);
			}
			model.add(entry);
		}
		Collections.sort(model, new Comparator<Map.Entry<Object, Object>>() {

			@Override
			public int compare(Entry<Object, Object> o1, Entry<Object, Object> o2) {
				return ((String) o1.getKey()).compareToIgnoreCase((String) o2.getKey());
			}
			
		});
		fireTableDataChanged();
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == 1) {
			Map.Entry<Object, Object> entry = model.get(rowIndex);
			entry.setValue(aValue);
		}
	}

	@Override
	public String getColumnName(int column) {
		if (column == 0) {
			return "Variable";
		} else {
			return "Value";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 1;
	}
	
}
