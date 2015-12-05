package sqlrunner.editor;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;

import javax.swing.JEditorPane;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;

import sqlrunner.Main;
import sqlrunner.MainFrame;

/**
 * abgeleiteter EditorPane
 * erMöglicht:
 *  die Umschaltung zwischen Zeilenumbruch und ungebrochenen Zeilen
 *  die Umschaltung des Textcursors zwischen Einfüge- und überschreibe-Cursor
 */
public class ExtEditorPane extends JEditorPane {

    private static final long serialVersionUID = 1L;
    public long prevCaretPosition;
    private MainFrame mainFrame;
    private boolean overWrite = false;

    public ExtEditorPane(MainFrame mainFrame) {
        super();
        this.mainFrame = mainFrame;
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public boolean isOverWrite() {
        return overWrite;
    }

    public void setOverWrite(boolean overWrite_loc) {
        this.overWrite = overWrite_loc;
        checkCaret();
    }

    /**
     * setzt den Editor auf nicht editierbar und erstellt einen neuen
     * Caret
     */
    @Override
    public void setEditable(boolean ok) {
        super.setEditable(ok);
        if (ok) {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        checkCaret();
    }

    public void checkCaret() {
        // die alte position merken
        int prevCaretPosition_loc=0;
        if (getCaret() != null) {
            prevCaretPosition_loc = getCaretPosition();
        }
        if (overWrite) {
            // den Overwrite-Caret setzen
            final XORCaret defaultCaret=new XORCaret();
            // die BlinkRate setzen bevor der Caret für den Editor gesetzt wird !
            defaultCaret.setBlinkRate(Main.CARET_BLINK_RATE);
            setCaret(defaultCaret);
        } else {
            // den normalen Caret setzen (für die TextKomponente der TextCaret)
            final DefaultCaret defaultCaret=new DefaultCaret();
            defaultCaret.setBlinkRate(Main.CARET_BLINK_RATE);
            setCaret(defaultCaret);
        }
        // die alte CaretPos wieder herstellen
        setCaretPosition(prevCaretPosition_loc);
    }

    // behandelt verschiedene Mausereignisse
    @Override
    public void processMouseEvent(MouseEvent me) {
        switch (me.getID()) {
            case MouseEvent.MOUSE_PRESSED: {
                // Mousetaste gedrückt? ...
                if (me.isShiftDown()) {
                    // ... und SHIFT-Taste auch ? dann ...
                    // erstmal die aktuelle Cursorposition merken
                    final int prevCaretPosition_loc=getCaretPosition();
                    // dann die default handler agieren lassen
                    super.processMouseEvent(me);
                    // Cursor auf neue Stelle setzen
                    // und Text zwischen alter und neuer position markieren
                    if (prevCaretPosition_loc < getCaretPosition()) {
                        select(prevCaretPosition_loc, getCaretPosition());
                    } else {
                        select(getCaretPosition(), prevCaretPosition_loc);
                    }
                } else {
                    // wenn nicht SHIFT gedrückt, dann Standardaktionen
                    super.processMouseEvent(me);
                }
                break;
            }
            case MouseEvent.MOUSE_ENTERED: {
                // Maus über Editorfensterbereich
                // dann Text-Mauszeiger einstellen
                if (isEditable()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                break;
            }
            default:
                super.processMouseEvent(me);}
    }
    
    public void copyTextIntoLinesSmart(String text) throws Exception {
    	// separate input text lines
    	text = text.replaceAll("\r", ""); // remove Windows line break chars
    	StringTokenizer st = new StringTokenizer(text, "\n");
    	// in current line recognize the 
    	String selectedText = getSelectedText();
    	//System.out.println("selected text=" + selectedText);
		int pos = getCaretPosition();
    	boolean atPattern = false;
    	boolean atLineEnd = false;
    	boolean atLinePos = false;
    	if (selectedText != null && selectedText.isEmpty() == false) {
    		// we have to search in the next line for this text and add the content line by line
    		select(pos, pos);
    		atPattern = true;
    	} else {
    		// are we at the end of the current line?
    		String s = getText(pos, 1);
    		if ("\n".equals(s)) {
    			// we are at the line end, we have to add the input text line by line at the end
    			atLineEnd = true;
    		} else {
    			// we have to add the text content at the same position line by line
    			atLinePos = true;
    		}
    	}
    	// iterator through none empty lines
		final Document doc = getDocument();
		final Element rootElem = doc.getDefaultRootElement();
		int lineNum = rootElem.getElementIndex(getCaretPosition());
		int maxLineNum = rootElem.getElementCount();
		if (atPattern) {
			while (st.hasMoreTokens()) {
				String content = st.nextToken();
				//System.out.println("content=" + content);
	    		int nextLineNum = lineNum;
    			while (true) {
    				Element line = rootElem.getElement(nextLineNum);
    	    		int start = line.getStartOffset();
    	    		int end = line.getEndOffset();
        			String lineText = getText(start, end - start);
        			//System.out.println("lineText=" + lineText);
        			int patternPos = lineText.indexOf(selectedText);
        			//System.out.println("patternPos=" + patternPos);
        			if (patternPos > 0) {
        				patternPos = patternPos + selectedText.length();
            			doc.insertString(start + patternPos, content, null);
            			lineNum = nextLineNum;
            			break;
        			} else {
            			nextLineNum++;
            			if (nextLineNum >= maxLineNum) {
                			lineNum = nextLineNum;
            				break;
            			}
        			}
    			}
        		lineNum++;
        		if (lineNum >= maxLineNum) {
        			break;
        		}
        	}
		} else if (atLinePos) {
			Element line = rootElem.getElement(lineNum);
    		int start = line.getStartOffset();
    		int columnPos = pos - start;
			while (st.hasMoreTokens()) {
				String content = st.nextToken();
	    		line = rootElem.getElement(lineNum);
	    		doc.insertString(line.getStartOffset() + columnPos, content, null);
        		lineNum++;
        		if (lineNum >= maxLineNum) {
        			break;
        		}
			}
		} else if (atLineEnd) {
			Element line = rootElem.getElement(lineNum);
			while (st.hasMoreTokens()) {
				String content = st.nextToken();
	    		line = rootElem.getElement(lineNum);
	    		doc.insertString(line.getEndOffset() - 1, content, null);
        		lineNum++;
        		if (lineNum >= maxLineNum) {
        			break;
        		}
			}
		}
    }

}
