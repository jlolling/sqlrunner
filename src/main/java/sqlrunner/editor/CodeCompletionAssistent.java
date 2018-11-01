package sqlrunner.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import sqlrunner.MainFrame;
import sqlrunner.datamodel.SQLField;
import sqlrunner.datamodel.SQLObject;
import sqlrunner.datamodel.SQLProcedure;
import sqlrunner.datamodel.SQLSchema;
import sqlrunner.datamodel.SQLSequence;
import sqlrunner.datamodel.SQLTable;
import sqlrunner.resources.ApplicationIcons;

public class CodeCompletionAssistent extends JWindow {

	private final Logger logger = Logger.getLogger(CodeCompletionAssistent.class);
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("rawtypes")
	private JList jList;
	private FilterModel model;
	private MainFrame mainFrame;
	private Object selectedItem;
	private String currentSearchStr;
	
	public CodeCompletionAssistent(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
		init();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void init() {
		getContentPane().setLayout(new BorderLayout());
		JScrollPane sp = new JScrollPane();
		model = new FilterModel();
		jList = new JList(model);
		jList.setCellRenderer(new ObjectRenderer());
		KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		jList.getActionMap().put("esc", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				mainFrame.closeSyntaxChooser();
			}
			
		});
		jList.getInputMap().put(esc, "esc");
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
		jList.getActionMap().put("enter", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				mainFrame.insertSyntaxChooserText();
				mainFrame.closeSyntaxChooser();
			}
			
		});
		jList.getInputMap().put(enter, "enter");
		jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				selectedItem = jList.getSelectedValue();
			}
		});
		jList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					mainFrame.insertSyntaxChooserText();
					mainFrame.closeSyntaxChooser();
				}
			}
			
		});
		sp.setViewportView(jList);
		getContentPane().add(sp, BorderLayout.CENTER);
	}
	
	public void addItems(List<? extends Object> listItems) {
		if (logger.isDebugEnabled()) {
			logger.debug("addItems: add " + listItems.size() + " keywords");
		}
		model.addElements(listItems);
	}
	
	public void setSearchTerm(String term) {
		if (logger.isDebugEnabled()) {
			logger.debug("setSearchTerm " + term);
		}
		currentSearchStr = term;
		model.refilter(currentSearchStr);
	}
	
	public void selectDown() {
		int pos = jList.getSelectedIndex();
		if (pos == -1) {
			pos = 0;
		} else {
			pos++;
		}
		if (pos < jList.getModel().getSize()) {
			jList.setSelectedIndex(pos);
			jList.ensureIndexIsVisible(pos);
		}
	}
	
	public void selectUp() {
		int pos = jList.getSelectedIndex();
		if (pos == -1) {
			pos = jList.getModel().getSize() - 1;
		} else if (pos > 0) {
			pos--;
		}
		jList.setSelectedIndex(pos);
		jList.ensureIndexIsVisible(pos);
	}
	
	public Object getSeletedItem() {
		return selectedItem;
	}
	
	public void reset() {
		model.clear();
		selectedItem = null;
	}

	@SuppressWarnings("rawtypes")
	private static final class ObjectRenderer extends JLabel implements	ListCellRenderer {

		private static final long serialVersionUID = 1L;
		public final static Color keywordBgColor = new Color(250, 250, 220);
		public final static Color tableBgColor = new Color(220, 250, 250);
		public final static Color schemaBgColor = new Color(250, 220, 250);
		public final static Color fieldBgColor = new Color(220, 255, 220);
		private static Color selectedBgColor;
		private static Color selectedFgColor;
		private static Color textColor;

		@Override
		public Component getListCellRendererComponent(
				JList list, 
				Object value,
				int index, 
				boolean isSelected, 
				boolean cellHasFocus) {
			setOpaque(true);
			if (selectedBgColor == null) {
				selectedBgColor = list.getSelectionBackground();
			}
			if (selectedFgColor == null) {
				selectedFgColor = list.getSelectionForeground();
			}
			if (textColor == null) {
				textColor = list.getForeground();
			}
			if (value instanceof String) {
				setText((String) value);
				setIcon(null);
				if (isSelected) {
					setForeground(selectedFgColor);
					setBackground(selectedBgColor);
				} else {
					setForeground(textColor);
					setBackground(keywordBgColor);
				}
			} else if (value instanceof SQLTable) {
				setText(((SQLTable) value).getName());
				setIcon(ApplicationIcons.TABLE_PNG);
				if (isSelected) {
					setForeground(selectedFgColor);
					setBackground(selectedBgColor);
				} else {
					setForeground(textColor);
					setBackground(tableBgColor);
				}
			} else if (value instanceof SQLProcedure) {
				setText(((SQLProcedure) value).getName());
				setIcon(ApplicationIcons.PROCEDURE_PNG);
				if (isSelected) {
					setForeground(selectedFgColor);
					setBackground(selectedBgColor);
				} else {
					setForeground(textColor);
					setBackground(keywordBgColor);
				}
			} else if (value instanceof SQLSchema) {
				setText(((SQLSchema) value).getName());
				setIcon(ApplicationIcons.SCHEMA_PNG);
				if (isSelected) {
					setForeground(selectedFgColor);
					setBackground(selectedBgColor);
				} else {
					setForeground(textColor);
					setBackground(schemaBgColor);
				}
			} else if (value instanceof SQLField) {
				setText(((SQLField) value).getName());
				setIcon(ApplicationIcons.FIELD_PNG);
				if (isSelected) {
					setForeground(selectedFgColor);
					setBackground(selectedBgColor);
				} else {
					setForeground(textColor);
					setBackground(fieldBgColor);
				}
			} else if (value instanceof SQLSequence) {
				setText(((SQLSequence) value).getName());
				setIcon(ApplicationIcons.SEQUENCE_PNG);
				if (isSelected) {
					setForeground(selectedFgColor);
					setBackground(selectedBgColor);
				} else {
					setForeground(textColor);
					setBackground(fieldBgColor);
				}
			}
			return this;
		}

	}

	@SuppressWarnings("rawtypes")
	private class FilterModel extends AbstractListModel {

		private static final long serialVersionUID = 1L;
		private List<Object> allItems;
		private List<Object> filteredItems;
		private boolean searchMethodStartsWith = false;

		public FilterModel() {
			super();
			allItems = new ArrayList<Object>();
			filteredItems = new ArrayList<Object>();
		}

		@Override
		public Object getElementAt(int index) {
			if (index < filteredItems.size()) {
				return filteredItems.get(index);
			} else {
				return null;
			}
		}

		@Override
		public int getSize() {
			return filteredItems.size();
		}
		
		public void addElements(List<? extends Object> listElements) {
			allItems.addAll(listElements);
			refilter(null);
		}
		
		public void clear() {
			allItems.clear();
			int prevSize = filteredItems.size();
			filteredItems.clear();
			selectedItem = null;
			if (prevSize > 0) {
				fireContentsChanged(this, 0, prevSize);
			}
		}

		public void refilter(String term) {
			filteredItems.clear();
			if (term != null) {
				term = term.toLowerCase();
			}
			for (Object item : allItems) {
				if (item instanceof String) {
					if (match((String) item, term)) {
						filteredItems.add(item);
					}
				} else if (item instanceof SQLObject) {
					if (match(((SQLObject) item).getName(), term)) {
						filteredItems.add(item);
					}
				}
			}
			fireContentsChanged(this, 0, getSize());
			if (filteredItems.size() > 0) {
				jList.setSelectedIndex(0);
				selectedItem = filteredItems.get(0);
			}
		}
		
		private boolean match(String itemStr, String searchStr) {
			if (searchStr == null || searchStr.isEmpty()) {
				return true;
			}
			if (searchMethodStartsWith) {
				return itemStr.toLowerCase().startsWith(searchStr);
			} else {
				return itemStr.toLowerCase().indexOf(searchStr) != -1;
			}
		}

		public boolean isSearchMethodStartsWith() {
			return searchMethodStartsWith;
		}

		public void setSearchMethodStartsWith(boolean searchMethodStartsWith) {
			this.searchMethodStartsWith = searchMethodStartsWith;
		}
		
	}

	@Override
	public boolean requestFocusInWindow() {
		return jList.requestFocusInWindow();
	}

}
