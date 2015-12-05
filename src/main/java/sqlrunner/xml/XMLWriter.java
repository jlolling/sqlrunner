package sqlrunner.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * diese Klasse erstellt generisch XML-Dokumente Die Ausgabe erfolgt an den im
 * Konstruktor mitgegebenen BufferedWriter
 * 
 * @author jan
 * 
 */
public class XMLWriter {

	private BufferedWriter out;
	private static final char TAG_START = '<';
	private static final char TAG_END = '>';
	private static final String TAG_END_WITHOUT_CHILDREN = "/>";
	private static final String INDENT_STRING = "  "; // 2 Leerzeichen als
														// Einrückung
	private static final String CDATA_START = "<![CDATA[";
	private static final String CDATA_END = "]]>";
	private final TagStack stack = new TagStack();
	private static final int maxAttrCountPerLine = 2;
	private boolean formatOutput = true;
	public static final String DEFAULT_ENCODING = "UTF-8";
	private String encoding = DEFAULT_ENCODING;
	private boolean lastElementWasTagEnd = false;

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public boolean isFormatOutput() {
		return formatOutput;
	}

	public void setFormatOutput(boolean formatOutput) {
		this.formatOutput = formatOutput;
	}

	/**
	 * Instanziert den XMLWriter
	 * 
	 * @param out
	 *            Ausgabe-Writer
	 */
	public XMLWriter(BufferedWriter out) {
		this.out = out;
	}

	public XMLWriter(OutputStream out, String encoding) throws IOException {
		this.out = new BufferedWriter(new OutputStreamWriter(out, encoding));
	}

	public XMLWriter(OutputStream out) throws IOException {
		this.out = new BufferedWriter(new OutputStreamWriter(out,
				DEFAULT_ENCODING));
	}

	/**
	 * schreibt den XML-Header
	 * 
	 * @throws IOException
	 */
	public void writeXMLBegin() throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"" + encoding
				+ "\" standalone=\"yes\"?>");
	}

	/**
	 * schreibt ein DOCTYPE Tag
	 * 
	 * @param docTypeTag
	 * @throws IOException
	 */
	public void writeDocType(String docTypeTag) throws IOException {
		out.write(docTypeTag);
	}

	/**
	 * gibt das letzte Tag aus welches noch Kinder zulässt.
	 * 
	 * @return Name
	 */
	public String getCurrentTagName() {
		return stack.getCurrentTag();
	}

	/**
	 * schreibt ein Tag welches sinnvollerweise Kinde besitzt. Das Tag wird mit
	 * der Methode writeTagEnd geschlossen.
	 * 
	 * @param tagName
	 * @param attributes
	 *            (key=1.Dimension, value=2.Dimension)
	 * @throws IOException
	 */
	public void writeTagBegin(String tagName, String[][] attributes)
			throws IOException {
		// Child-Tag wird erzeugt
		// einrücken dem level entsprechend
		lastElementWasTagEnd = false;
		out.newLine();
		if (formatOutput) {
			for (int x = 0; x < stack.getCurrentLevel(); x++) {
				out.write(INDENT_STRING);
			}
		}
		// Tag schreiben
		out.write(TAG_START);
		out.write(tagName);
		if (attributes != null) {
			out.write(' ');
			// wenn Attribute vorhanden dann diese jetzt einbauen
			for (int i = 0; i < attributes.length; i++) {
				out.write(attributes[i][0]);
				out.write("='");
				out.write(attributes[i][1]);
				out.write("' ");
				if (formatOutput) {
					if ((maxAttrCountPerLine > 0) && (i > 0)
							&& (i < attributes.length - 1)
							&& (i % maxAttrCountPerLine == 0)) {
						out.newLine();
						for (int x = 0; x < stack.getCurrentLevel(); x++) {
							out.write(INDENT_STRING);
						}
						// soweit einrücken, bis der Startpunkt unter dem
						// Attribute der vorhergehenden Zeile steht
						for (int x = 0; x < tagName.length() + 2; x++) {
							out.write(' ');
						}
					}
				}
			}
		}
		out.write(TAG_END);
		stack.pushTag(tagName);
	}

	/**
	 * schreibt ein Tag welches keine Kinder besitzt und sofort wieder
	 * geschlossen wird
	 * 
	 * @param tagName
	 * @param attributes
	 *            (key=1.Dimension, value=2.Dimension)
	 * @throws IOException
	 */
	public void writeClosedTag(String tagName, String[][] attributes)
			throws IOException {
		// einrücken dem level entsprechend
		lastElementWasTagEnd = true;
		out.newLine();
		if (formatOutput) {
			for (int x = 0; x < stack.getCurrentLevel(); x++) {
				out.write(INDENT_STRING);
			}
		}
		// Tag schreiben
		out.write(TAG_START);
		out.write(tagName);
		if (attributes != null) {
			out.write(' ');
			// wenn Attribute vorhanden dann diese jetzt einbauen
			for (int i = 0; i < attributes.length; i++) {
				out.write(attributes[i][0]);
				out.write("='");
				out.write(attributes[i][1]);
				out.write("' ");
				if (formatOutput) {
					if ((maxAttrCountPerLine > 0) && (i > 0)
							&& (i < attributes.length - 1)
							&& (i % maxAttrCountPerLine == 0)) {
						out.newLine();
						for (int x = 0; x < stack.getCurrentLevel(); x++) {
							out.write(INDENT_STRING);
						}
						// soweit einrücken, bis der Startpunkt unter dem
						// Attribute der vorhergehenden Zeile
						for (int x = 0; x < tagName.length() + 2; x++) {
							out.write(' ');
						}
					}
				}
				if (i == attributes.length - 1) {
					out.write(TAG_END_WITHOUT_CHILDREN);
				}
			}
		}
	}

	/**
	 * schreibt beliebigen Text als Kind des vorhergehenden Tags
	 * 
	 * @param content
	 *            Inhalt
	 * @throws IOException
	 */
	public void writeTextContent(String content) throws IOException {
		if (content != null) {
			out.write(content);
		}
	}

	/**
	 * schreibt beliebigen Inhalt gekapselt in CDATA-Klammer als Kind des
	 * vorhergehenden Tags
	 * 
	 * @param content
	 *            Inhalt
	 * @throws IOException
	 */
	public void writeCDATAContent(String content) throws IOException {
		// wenn ein Inhalt mit CDATA eingeschlossen werden soll
		out.write(CDATA_START);
		out.newLine();
		out.write(content);
		out.newLine();
		if (formatOutput) {
			for (int i = 0; i < stack.getCurrentLevel(); i++) {
				out.write(INDENT_STRING);
			}
		}
		out.write(CDATA_END);
	}

	/**
	 * schreibt XML-Kommentar
	 * 
	 * @param commentText
	 * @throws IOException
	 */
	public void writeComment(String commentText) throws IOException {
		out.newLine();
		if (formatOutput) {
			for (int i = 0; i < stack.getCurrentLevel(); i++) {
				out.write(INDENT_STRING);
			}
		}
		out.write("<!--");
		out.newLine();
		out.write(commentText);
		out.newLine();
		if (formatOutput) {
			for (int i = 0; i < stack.getCurrentLevel(); i++) {
				out.write(INDENT_STRING);
			}
		}
		out.write("-->");
	}

	/**
	 * beendet das letzte geschriebene offene Tag (nur für Tags mit
	 * writeTagBegin gegonnen wurden) ist im Tag ein Child-Tag enthalten, wird
	 * das Ende auf eine neue Zeile geschrieben und eingerückt.
	 * 
	 * @throws IOException
	 */
	public void writeTagEnd() throws IOException {
		String tagName = stack.popTag();
		if (lastElementWasTagEnd) {
			out.newLine();
			if (formatOutput) {
				for (int i = 0; i < stack.getCurrentLevel(); i++) {
					out.write(INDENT_STRING);
				}
			}
		}
		out.write(TAG_START);
		out.write("/");
		out.write(tagName);
		out.write(TAG_END);
		lastElementWasTagEnd = true;
	}

	/**
	 * leert den Ausgabepuffer des Writers
	 * 
	 * @throws IOException
	 */
	public void flush() throws IOException {
		out.flush();
	}

	/**
	 * schliesst den Writer
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		stack.clear();
		out.close();
	}

	private static class TagStack {

		private List<String> stack = new ArrayList<String>();

		void pushTag(String tagName) {
			stack.add(tagName);
		}

		void clear() {
			stack.clear();
		}

		String popTag() {
			int pos = stack.size() - 1;
			String tagName = stack.get(pos);
			stack.remove(pos);
			return tagName;
		}

		String getCurrentTag() {
			int pos = stack.size() - 1;
			String tagName = stack.get(pos);
			return tagName;
		}

		int getCurrentLevel() {
			return stack.size();
		}

	}

}
