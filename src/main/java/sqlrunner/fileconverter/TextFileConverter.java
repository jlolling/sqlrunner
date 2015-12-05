package sqlrunner.fileconverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * File converter which can change the encoding and the line separator
 * @author jan
 *
 */
public class TextFileConverter {
	
	private long currentInputLineNumber = 0;
	private long maxLinesPerFile = 0;
	private long currentOutputLineNumber = 0;
	
	public static enum LineSeparator {
		
		UNIX("UNIX", "\n"), WINDOWS("Windows", "\r\n");
		
		private String name;
		private String sep;
		
		LineSeparator(String name, String sep) {
			this.name = name;
			this.sep = sep;
		}
		
		public String getName() {
			return name;
		}
		
		public String getSeparator() {
			return sep;
		}
		
		public String toString() {
			return getName();
		}
		
	}
	
	public void reset() {
		currentInputLineNumber = 0;
		maxLinesPerFile = 0;
		currentOutputLineNumber = 0;
	}

	public void setMaxLinesPerFile(long maxLinesPerFile) {
		this.maxLinesPerFile = maxLinesPerFile;
	}
	
	public long getMaxLinesPerFile() {
		return maxLinesPerFile;
	}
	
	public void convert(File source, String sourceEncoding, final File target, String targetEncoding, String targetLineSeparator) throws IOException {
		if (source.equals(target)) {
			throw new IllegalArgumentException("source cannot equal to target file");
		}
		File currentTargetFile = target;
		final BufferedReader in = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(source), sourceEncoding));
		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(currentTargetFile), targetEncoding));
		String line = null;
		int fileIndex = 0;
		while ((line = in.readLine()) != null) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
			currentInputLineNumber++;
			if (maxLinesPerFile > 0 && currentOutputLineNumber == maxLinesPerFile) {
				currentOutputLineNumber = 0;
				out.close();
				currentTargetFile = createNextFile(target, ++fileIndex);
				out = new BufferedWriter(
						new OutputStreamWriter(
								new FileOutputStream(currentTargetFile), targetEncoding));
			}
			out.write(line);
			out.write(targetLineSeparator);
			currentOutputLineNumber++;
		}
		out.close();
		in.close();
	}
	
	public long getCurrentLineNumber() {
		return currentInputLineNumber;
	}
	
    private File createNextFile(File originalTargetFile, int index) {
        String path = originalTargetFile.getParent();
        final String originalName = originalTargetFile.getName();
        final int p0 = originalName.lastIndexOf(".");
        String newName;
        if (p0 != -1) {
            newName = originalName.substring(0, p0) 
            	+ "_"
                + String.valueOf(index) 
                + originalName.substring(p0, originalName.length());
        } else {
            newName = originalName 
            	+ "_" 
            	+ String.valueOf(index);
        }
        if (path != null) {
            return new File(path, newName);
        } else {
            return new File(newName);
        }
    }
	
}
