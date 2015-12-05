package sqlrunner.flatfileimport;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

/**
 * 
 * @author Jan Lolling
 */
public class CSVFileDatasetProvider implements DatasetProvider {

	private transient RandomAccessReader randomReader = null;
	private transient BufferedReader bufferedReader = null;
	private boolean testMode = false;
	private File currentFile;
	private boolean useEnclosure = false;
	private char enclosure = ' ';
	
	/**
	 * this setups the delimiter and enclosure to support 
	 * @param encloser
	 * @param fieldDelimiter
	 */
	private void setEncloser(String enclosure) {
		if (enclosure != null && enclosure.isEmpty() == false) {
			if (enclosure.length() > 1) {
				throw new IllegalArgumentException("Enclosure can only be one char!");
			}
			this.enclosure = enclosure.charAt(0);
			useEnclosure = true;
		} else {
			useEnclosure = false;
		}
	}

	@Override
	public long retrieveDatasetCount() throws IOException {
		if (currentFile == null) {
			throw new IllegalStateException("setupDatasetProvider not performed");
		}
		long count = 0;
		BufferedInputStream in = null;
		try {
			byte[] buffer = new byte[1024];
			int len = 0;
			in = new BufferedInputStream(new FileInputStream(currentFile));
			char c = ' ';
			while ((len = in.read(buffer, 0, buffer.length)) != -1) {
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
				for (int i = 0; i < len; i++) {
					c = (char) buffer[i];
					if (c == '\n') {
						count++;
					}
				}
			}
			if (c != '\n') {
				// to detect last line
				count++;
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
		return count;
	}

	@Override
	public FieldTokenizer createParser() {
		CSVFieldTokenizer parser = new CSVFieldTokenizer();
		return parser;
	}

	@Override
	public void setupDatasetProvider(File file, boolean testMode, ImportAttributes properties) throws Exception {
		if (properties.isIgnoreLineBreakInEnclosedValues()) {
			setEncloser(properties.getEnclosure());
		}
		this.testMode = testMode;
		if (testMode) {
			randomReader = new RandomAccessReader(new RandomAccessFile(file, "r"), properties.getCharsetName());
			if (useEnclosure) {
				randomReader.setEnclosure(enclosure);
			} else {
				randomReader.disableEnclosure();
			}
		} else {
			bufferedReader = new BufferedReader(
					new InputStreamReader(
					new FileInputStream(
							file), 
							properties.getCharsetName()));
		}
		currentFile = file;
		currentRowNum = 0;
		currentLine = null;
	}

	@Override
	public void closeDatasetProvider() {
		if (randomReader != null) {
			try {
				randomReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (bufferedReader != null) {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public Object getNextDataset() throws Exception {
		if (testMode) {
			throw new Exception("getNextDataset not supported in test mode");
		} else {
			currentLine = readLine();
			currentRowNum++;
		}
		return currentLine;
	}
	
	private String readLine() throws IOException {
		if (useEnclosure) {
			return readLineWithEnclosure();
		} else {
			return bufferedReader.readLine();
		}
	}
	
	private String readLineWithEnclosure() throws IOException {
		StringBuilder line = new StringBuilder();
		boolean inEclosuredField = false;
		char last_c = (char) 0;
		while (true) {
			int r = bufferedReader.read();
			if (r == -1) {
				if (line.length() > 0) {
					return line.toString();
				} else {
					return null;
				}
			}
			char c = (char) r;
			if (c == enclosure) {
				if (inEclosuredField || last_c == enclosure) {
					// end of enclosed field found
					inEclosuredField = false;
				} else {
					// start of enclosed field found
					inEclosuredField = true;
				}
				line.append(c); // add the enclosure to the line
			} else if (c == '\n') {
				if (inEclosuredField) {
					// we have to add to line
					line.append(c);
				} else {
					break;
				}
			} else if (c == '\r') {
				// ignore that
				continue;
			} else {
				// all content chars
				line.append(c);
				if (last_c == enclosure) {
					inEclosuredField = true;
				}
			}
			last_c = c;
		}
		return line.toString();
	}

	private String getCurrentLine() {
		return currentLine;
	}

	@Override
	public long getCurrentRowNum() {
		return currentRowNum;
	}

	private long currentRowNum = 0;
	private String currentLine = null;

	@Override
	public Object getDatasetAtRowInTestMode(long rowNumber) throws Exception {
		if (testMode == false) {
			throw new IllegalStateException("method getDatasetAtRowInTestMode can only be used in test mode");
		}
		readDatasetInTestModeAt(rowNumber);
		return getCurrentLine();
	}
	
	private void readDatasetInTestModeAt(long rowNumber) throws Exception {
		if (rowNumber < 0) {
			randomReader.seek(0);
			currentLine = null;
			randomReader.seek(0);
			currentRowNum = 0;
		} else if (rowNumber == 0) {
			randomReader.seek(0);
			currentLine = randomReader.readLineWithCharSet();
			randomReader.seek(0);
			currentRowNum = 0;
		} else {
			if (rowNumber == currentRowNum - 1) {
				// step only one line back
				if (randomReader.seekToPrevLineStart()) {
					currentRowNum--;
					long pos = randomReader.getFilePointer();
					currentLine = randomReader.readLineWithCharSet();
					randomReader.seek(pos);
				}
			} else {
				// workaround because stepping backwards is to slow
				if (rowNumber < currentRowNum) {
					currentRowNum = 0;
					randomReader.seek(0);
				}
				if (rowNumber > currentRowNum) {
					boolean jumped = false;
					long pos = randomReader.getFilePointer();
					while (rowNumber > currentRowNum) {
						if (randomReader.seekToNextLineStart()) {
							pos = randomReader.getFilePointer();
							currentRowNum++;
							jumped = true;
						} else {
							break;
						}
					}
					if (jumped) {
						currentLine = randomReader.readLineWithCharSet();
						randomReader.seek(pos);
					}
				}
			}
		}
	}

}