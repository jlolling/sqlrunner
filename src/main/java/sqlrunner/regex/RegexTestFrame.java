package sqlrunner.regex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import sqlrunner.text.StringReplacer;

public class RegexTestFrame extends JFrame {

	private static final Logger logger = LogManager.getLogger(RegexTestFrame.class);
	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JScrollPane jScrollPaneTestText = null;
	private JTextField jTextFieldRegex = null;
	private JTextField jTextFieldJava = null;
	private JSplitPane jSplitPane = null;
	private JPanel jPanelTop = null;
	private JPanel jPanelButtom = null;
	private JTextArea jTextAreaTest = null;
	private JScrollPane jScrollPaneMessage = null;
	private JTextArea jTextAreaMessage = null;
    private DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.lightGray);
	private JCheckBox jCheckBoxShowOnlyByGroupFound = null;
	private JCheckBox jCheckBoxCaseSensitive = null;
	
	/**
	 * This is the default constructor
	 */
	public RegexTestFrame() {
		super();
		initialize();
		setVisible(true);
		jSplitPane.setDividerLocation(this.getHeight() / 3);
	    final DocumentListener docListener = new DocumentListener() {

	        @Override
			public void insertUpdate(DocumentEvent e) {
	            showRegexMatches();
	        }

	        @Override
			public void removeUpdate(DocumentEvent e) {
	            showRegexMatches();
	        }

	        @Override
			public void changedUpdate(DocumentEvent e) {
	            showRegexMatches();
	        }
	    };
	    jTextAreaTest.getDocument().addDocumentListener(docListener);
	    jTextFieldRegex.getDocument().addDocumentListener(docListener);
	    jTextFieldRegex.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				copyToJava();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				copyToJava();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				copyToJava();
			}
		});
	    jTextFieldJava.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				copyToRegex();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				copyToRegex();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				copyToRegex();
			}
		});
	}

	private boolean currentlyCopyingToRegex = false;
	private boolean currentlyCopyingToJava = false;
	
	private void copyToJava() {
		if (currentlyCopyingToRegex == false) {
			currentlyCopyingToJava = true;
			try {
				String regex = jTextFieldRegex.getText();
				if (regex != null) {
					regex = regex.trim();
					String javaCode = regexToJava(regex);
					jTextFieldJava.setText(javaCode);
					jTextFieldJava.setSelectionStart(0);
					jTextFieldJava.setSelectionEnd(javaCode.length() - 1);
				}
			} finally {
				currentlyCopyingToJava = false;
			}
		}
	}
	
	private void copyToRegex() {
		if (currentlyCopyingToJava == false) {
			currentlyCopyingToRegex = true;
			try {
				String javaCode = jTextFieldJava.getText();
				if (javaCode != null) {
					String regex = javaToRegex(javaCode);
					jTextFieldRegex.setText(regex);
				}
			} finally {
				currentlyCopyingToRegex = false;
			}
		}
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setContentPane(getJContentPane());
		this.setTitle(Messages.getString("RegexTestFrame.0")); //$NON-NLS-1$
		this.setSize(new Dimension(400, 400));
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weighty = 1.0;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1.0;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getJSplitPane(), gbc);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jScrollPaneTestText	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPaneTestText() {
		if (jScrollPaneTestText == null) {
			jScrollPaneTestText = new JScrollPane();
			jScrollPaneTestText.setViewportView(getJTextAreaTest());
		}
		return jScrollPaneTestText;
	}

	/**
	 * This method initializes jTextFieldRegex	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTextFieldRegex() {
		if (jTextFieldRegex == null) {
			jTextFieldRegex = new JTextField();
		}
		return jTextFieldRegex;
	}

	private JTextField getJTextFieldJava() {
		if (jTextFieldJava == null) {
			jTextFieldJava = new JTextField();
		}
		return jTextFieldJava;
	}
	
	/**
	 * This method initializes jSplitPane	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getJSplitPane() {
		if (jSplitPane == null) {
			jSplitPane = new JSplitPane();
			jSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			jSplitPane.setTopComponent(getJPanelTop());
			jSplitPane.setBottomComponent(getJPanelButtom());
		}
		return jSplitPane;
	}

	/**
	 * This method initializes jPanelTop	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelTop() {
		if (jPanelTop == null) {
			jPanelTop = new JPanel();
			jPanelTop.setLayout(new GridBagLayout());
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.insets = new Insets(2, 2, 2, 2);
				JLabel jLabel = new JLabel();
				jLabel.setText(Messages.getString("RegexTestFrame.2")); //$NON-NLS-1$
				jPanelTop.add(jLabel, gbc);
			}
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = 1;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.weightx = 1.0;
				gbc.weighty = 1.0;
				gbc.insets = new Insets(2, 2, 2, 2);
				jPanelTop.add(getJScrollPaneTestText(), gbc);
			}
		}
		return jPanelTop;
	}

	/**
	 * This method initializes jPanelButtom	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanelButtom() {
		if (jPanelButtom == null) {
			jPanelButtom = new JPanel();
			jPanelButtom.setLayout(new GridBagLayout());
			int y = 0;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridy = y;
				jPanelButtom.add(getJCheckBoxCheckBoxCaseSensitive(), gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridy = y;
				JLabel jLabel1 = new JLabel();
				jLabel1.setText(Messages.getString("RegexTestFrame.3")); //$NON-NLS-1$
				jPanelButtom.add(jLabel1, gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridy = y;
				gbc.weightx = 1.0;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridwidth = 2;
				gbc.gridx = 0;
				jPanelButtom.add(getJTextFieldRegex(), gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridy = y;
				gbc.gridwidth = 2;
				JLabel jLabel1 = new JLabel();
				jLabel1.setText("Escaped String");
				jPanelButtom.add(jLabel1, gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridy = y;
				gbc.weightx = 1.0;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridwidth = 2;
				gbc.gridx = 0;
				jPanelButtom.add(getJTextFieldJava(), gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridy = y;
				jPanelButtom.add(getJCheckBoxShowOnlyByGroupFound(), gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridy = y;
				JLabel jLabel2 = new JLabel();
				jLabel2.setText(Messages.getString("RegexTestFrame.1")); //$NON-NLS-1$
				jPanelButtom.add(jLabel2, gbc);
			}
			y++;
			{
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridy = y;
				gbc.weightx = 1.0;
				gbc.weighty = 1.0;
				gbc.insets = new Insets(2, 2, 2, 2);
				gbc.gridwidth = 2;
				gbc.gridx = 0;
				jPanelButtom.add(getJScrollPaneMessage(), gbc);
			}
		}
		return jPanelButtom;
	}

	/**
	 * This method initializes jTextAreaTest	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJTextAreaTest() {
		if (jTextAreaTest == null) {
			jTextAreaTest = new JTextArea();
			jTextAreaTest.setWrapStyleWord(true);
			jTextAreaTest.setLineWrap(true);
		}
		return jTextAreaTest;
	}

	/**
	 * This method initializes jScrollPaneMessage	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPaneMessage() {
		if (jScrollPaneMessage == null) {
			jScrollPaneMessage = new JScrollPane();
			jScrollPaneMessage.setViewportView(getJTextAreaMessage());
		}
		return jScrollPaneMessage;
	}

	/**
	 * This method initializes jTextAreaMessage	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJTextAreaMessage() {
		if (jTextAreaMessage == null) {
			jTextAreaMessage = new JTextArea();
			jTextAreaMessage.setSize(new Dimension(300, 30));
			jTextAreaMessage.setEditable(false);
		}
		return jTextAreaMessage;
	}

    private void showRegexMatches() {
    	String regex = jTextFieldRegex.getText();
    	if (regex != null) {
            Pattern pattern = null;
            StringBuffer message = new StringBuffer();
            try {
            	if (jCheckBoxCaseSensitive.isSelected()) {
                    pattern = Pattern.compile(jTextFieldRegex.getText());
            	} else {
                    pattern = Pattern.compile(jTextFieldRegex.getText(), Pattern.CASE_INSENSITIVE);
            	}
                jTextFieldRegex.setBackground(new Color(200, 255, 200));
                message.append(Messages.getString("RegexTestFrame.4")); 
            } catch (PatternSyntaxException pse) {
                jTextFieldRegex.setBackground(new Color(255, 200, 200));
                message.append(pse.getMessage());
            }
            jTextAreaTest.getHighlighter().removeAllHighlights();
            jTextAreaTest.repaint();
            if (pattern != null) {
                Matcher matcher = pattern.matcher(jTextAreaTest.getText());
                int countSequences = 0;
                while (matcher.find()) {
                	countSequences++;
                	message.append('\n');
                	message.append("sequence ("); 
                	message.append(countSequences);
                	message.append(") <"); 
                	message.append(matcher.group());
                	message.append(">"); 
                	if (jCheckBoxShowOnlyByGroupFound.isSelected()) {
                		for (int i = 1; i <= matcher.groupCount(); i++) {
                			int start = matcher.start(i);
                			int end = matcher.end(i);
                            if (start < end) {
                            	message.append('\n');
                            	message.append("  start:");
                            	message.append(start);
                            	message.append("  end:");
                            	message.append(end);
                            	message.append("  group:"); 
                            	message.append(i);
                            	message.append(" <"); 
                            	message.append(matcher.group(i));
                            	message.append(">"); 
                                try {
                                    jTextAreaTest.getHighlighter().addHighlight(matcher.start(i), matcher.end(i), highlightPainter);
                                } catch (BadLocationException ex) {
                                	logger.error("showRegexMatches failed: " + ex.getMessage(), ex); 
                                }
                            }
                		}
                	} else {
                        if (matcher.start() < matcher.end()) {
                            try {
                                jTextAreaTest.getHighlighter().addHighlight(matcher.start(), matcher.end(), highlightPainter);
                            } catch (BadLocationException ex) {
                            	logger.error("showRegexMatches failed: " + ex.getMessage(), ex); 
                            }
                        }
                	}
                }
            }
            jTextAreaMessage.setText(message.toString());
    	}
    }

	/**
	 * This method initializes jCheckBoxShowOnlyByGroupFound	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJCheckBoxShowOnlyByGroupFound() {
		if (jCheckBoxShowOnlyByGroupFound == null) {
			jCheckBoxShowOnlyByGroupFound = new JCheckBox();
			jCheckBoxShowOnlyByGroupFound.setText(Messages.getString("RegexTestFrame.11")); //$NON-NLS-1$
			jCheckBoxShowOnlyByGroupFound.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					showRegexMatches();
				}
				
			});
		}
		return jCheckBoxShowOnlyByGroupFound;
	}
    
	private JCheckBox getJCheckBoxCheckBoxCaseSensitive() {
		if (jCheckBoxCaseSensitive == null) {
			jCheckBoxCaseSensitive = new JCheckBox();
			jCheckBoxCaseSensitive.setSelected(true);
			jCheckBoxCaseSensitive.setText(Messages.getString("RegexTestFrame.caseSensitive"));
			jCheckBoxCaseSensitive.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					showRegexMatches();
				}
				
			});
		}
		return jCheckBoxCaseSensitive;
	}
    

	private static String regexToJava(String aRegexFragment) {
		final StringBuilder result = new StringBuilder();
		result.append("\"");
		final StringCharacterIterator iterator = new StringCharacterIterator(aRegexFragment);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			if (character == '"') {
				result.append('\\');
				result.append(character);
			} else if (character == '\\') {
				result.append('\\');
				result.append(character);
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		result.append("\"");
		return result.toString();
	}

	private String javaToRegex(String javaEscapedText) {
		if (javaEscapedText.startsWith("\"")) {
			javaEscapedText = javaEscapedText.substring(0, 1);
		}
		if (javaEscapedText.endsWith("\"") && javaEscapedText.endsWith("\\\"") == false) {
			int lenght = javaEscapedText.length();
			javaEscapedText = javaEscapedText.substring(lenght - 1, lenght);
		}
		StringReplacer sr = new StringReplacer(javaEscapedText);
		sr.replace("\\\"", "\"");
		sr.replace("\\\\", "\\");
		return sr.getResultText();
	}

}
