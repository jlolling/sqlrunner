package sqlrunner.text;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class ClipboardUtil implements ClipboardOwner {

	private static ClipboardUtil util = null;
	
	private ClipboardUtil() {}
	
	public static ClipboardUtil getInstance() {
		if (util == null) {
			util = new ClipboardUtil();
		}
		return util;
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// do nothing
	}
	
	public void copy(String text) {
		if (text != null && text.isEmpty() == false) {
			StringSelection stringSelection = new StringSelection(text);
		    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		    clipboard.setContents(stringSelection, this);
		}
	}
	
}
