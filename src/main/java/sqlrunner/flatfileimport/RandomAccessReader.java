package sqlrunner.flatfileimport;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * <code>RandomAccessReader</code> extends <code>Reader</code> to provide a
 * means to create buffered <code>Reader</code>s from
 * <code>RandomAccessFile</code>s.
 * 
 * @author <a href="mailto:kdj@sanger.ac.uk">Keith James</a>
 * @since 1.2
 */
public class RandomAccessReader extends Reader {

	private static final int DEFAULT_BUFFER_SIZE = 1 << 13;

	private RandomAccessFile raf;

	private char[] buffer;
	private byte[] bytes;

	private int bufferPos = 0;
	private int bufferEnd = 0;
	private long raPtrPos = 0;
	private String charSet = null;
	private boolean useEnclosure = false;
	private char enclosure = ' ';

	/**
	 * Creates a new <code>RandomAccessReader</code> wrapping the
	 * <code>RandomAccessFile</code> and using a default-sized buffer (8192
	 * bytes).
	 * 
	 * @param raf
	 *            a <code>RandomAccessFile</code> to wrap.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	public RandomAccessReader(RandomAccessFile raf, String charSet)	throws IOException {
		this(raf, DEFAULT_BUFFER_SIZE);
		this.charSet = charSet;
	}

	/**
	 * Creates a new <code>RandomAccessReader</code> wrapping the
	 * <code>RandomAccessFile</code> and using a buffer of the specified size.
	 * 
	 * @param raf
	 *            a <code>RandomAccessFile</code> to wrap..
	 * 
	 * @param sz
	 *            an <code>int</code> buffer size.
	 * @throws IOException
	 */
	public RandomAccessReader(RandomAccessFile raf, int sz) throws IOException {
		super();
		this.raf = raf;

		buffer = new char[sz];
		bytes = new byte[sz];

		resetBuffer();
	}

	/**
	 * <code>close</code> closes the underlying <code>RandomAccessFile</code>.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	public void close() throws IOException {
		raf.close();
		raf = null;
	}

	/**
	 * <code>length</code> returns the length of the underlying
	 * <code>RandomAccessFile</code>.
	 * 
	 * @return a <code>long</code>.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	public long length() throws IOException {
		return raf.length();
	}

	/**
	 * <code>read</code> reads one byte from the underlying
	 * <code>RandomAccessFile</code>.
	 * 
	 * @return an <code>int</code>number of chars read, or -1 if the end of the
	 *         stream has been reached.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	public final int read() throws IOException {
		if (bufferPos >= bufferEnd)
			if (fill() < 0) {
				return -1;
			}
		if (bufferEnd == 0) {
			return -1;
		} else {
			return buffer[bufferPos++];
		}
	}

	/**
	 * <code>read</code> reads from the underlying <code>RandomAccessFile</code>
	 * into an array.
	 * 
	 * @param cbuf
	 *            a <code>char []</code> array to read into.
	 * @param off
	 *            an <code>int</code> offset in the array at which to start
	 *            storing chars.
	 * @param len
	 *            an <code>int</code> maximum number of char to read.
	 * 
	 * @return an <code>int</code> number of chars read, or -1 if the end of the
	 *         stream has been reached.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		int remainder = bufferEnd - bufferPos;

		// If there are enough chars in the buffer to handle this
		// call, use those
		if (len <= remainder) {
			System.arraycopy(buffer, bufferPos, cbuf, off, len);
			bufferPos += len;

			return len;
		}

		// Otherwise start getting more chars from the delegate
		for (int i = 0; i < len; i++) {
			// Read from out own method which checks the buffer
			// first
			int c = read();

			if (c != -1) {
				cbuf[off + i] = (char) c;
			} else {
				return i;
			}
		}
		return len;
	}

	/**
	 * <code>getFilePointer</code> returns the effective position of the pointer
	 * in the underlying <code>RandomAccessFile</code>.
	 * 
	 * @return a <code>long</code> offset.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	public long getFilePointer() throws IOException {
		return raPtrPos - bufferEnd + bufferPos;
	}

	public void seek(long pos) throws IOException {
		int p = (int) (raPtrPos - pos);

		// Check if we can seek within the buffer
		if (p >= 0 && p <= bufferEnd) {
			bufferPos = bufferEnd - p;
		} else {
			raf.seek(pos);
			resetBuffer();
		}
	}

	/**
	 * <code>fill</code> fills the buffer from the <code>RandomAccessFile</code>
	 * .
	 * 
	 * @return an <code>int</code>.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	private int fill() throws IOException {
		if (raf == null) {
			throw new IOException("Random access file closed");
		}

		// Read bytes from random access delegate
		int b = raf.read(bytes, 0, DEFAULT_BUFFER_SIZE);

		// Copy and cast bytes read to char buffer
		for (int i = b; --i >= 0; ) {
			buffer[i] = (char) bytes[i];
		}

		// If read any bytes
		if (b >= 0) {
			raPtrPos += b;
			bufferPos = 0;
			bufferEnd = b;
		}

		// Return number bytes read
		return b;
	}

	/**
	 * <code>resetBuffer</code> resets the buffer when the pointer leaves its
	 * boundaries.
	 * 
	 * @exception IOException
	 *                if an error occurs.
	 */
	private void resetBuffer() throws IOException {
		bufferPos = 0;
		bufferEnd = 0;
		raPtrPos = raf.getFilePointer();
	}

	/**
	 * Reads the next line of text from this file. This method successively
	 * reads bytes from the file, starting at the current file pointer, until it
	 * reaches a line terminator or the end of the file. Each byte is converted
	 * into a character by taking the byte's value for the lower eight bits of
	 * the character and setting the high eight bits of the character to zero.
	 * This method does not, therefore, support the full Unicode character set.
	 * 
	 * <p>
	 * A line of text is terminated by a carriage-return character (<code>'&#92;r'</code>), a
	 * newline character (<code>'&#92;n'</code>), a carriage-return character immediately
	 * followed by a newline character, or the end of the file. Line-terminating
	 * characters are discarded and are not included as part of the string
	 * returned.
	 * 
	 * <p>
	 * This method blocks until a newline character is read, a carriage return
	 * and the byte following it are read (to see if it is a newline), the end
	 * of the file is reached, or an exception is thrown.
	 * 
	 * @return the next line of text from this file, or null if end of file is
	 *         encountered before even one byte is read.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final String readLineWithCharSet() throws IOException {
		if (charSet == null || charSet.length() == 0) {
			throw new IOException("charset not set");
		}
		final ByteBuffer input = new ByteBuffer();
		int c = -1;
		boolean eol = false;
		boolean inEnclosure = false;
		while (!eol) {
			c = read();
			if (useEnclosure) {
				if (c == enclosure) {
					if (inEnclosure) {
						inEnclosure = false;
					} else {
						inEnclosure = true;
					}
				}
			}
			switch (c) {
			case -1:
			case '\n':
				if (inEnclosure == false) {
					eol = true;
				}
				break;
			case '\r':
				if (inEnclosure == false) {
					eol = true;
				}
				final long cur = getFilePointer();
				if ((read()) != '\n') {
					seek(cur);
				}
				break;
			default:
				input.add((byte) c);
				break;
			}
		}
		if ((c == -1) && (input.length() == 0)) {
			return null;
		}
		String line = "";
		try {
			line = new String(input.getBytes(), charSet);
		} catch (UnsupportedEncodingException usee) {
			throw new IOException(usee);
		}
		return line;
	}

	public final boolean seekToNextLineStart() throws IOException {
		boolean inEnclosure = false;
		while (true) {
			int c = read();
			if (useEnclosure) {
				if (c == enclosure) {
					if (inEnclosure) {
						inEnclosure = false;
					} else {
						inEnclosure = true;
					}
				}
			}
			switch (c) {
			case -1:
				return false;
			case '\n':
				if (inEnclosure == false) {
					return true;
				}
			case '\r':
				final long cur = getFilePointer();
				c = read();
				if (c != '\n' && c != -1) {
					seek(cur);
				}
				if (c == -1) {
					return false;
				} else {
					if (inEnclosure == false) {
						return true;
					}
				}
			}
		}
	}

	public boolean seekToPrevLineStart() throws IOException {
		boolean inEnclosure = false;
		boolean lastEol = true;
		while (true) {
			int c = readBackwards();
			if (useEnclosure) {
				if (c == enclosure) {
					if (inEnclosure) {
						inEnclosure = false;
					} else {
						inEnclosure = true;
					}
				}
			}
			switch (c) {
			case -1:
				if (lastEol) {
					return false;
				} else {
					return true;
				}
			case '\r':
			case '\n':
				if (inEnclosure == false) {
					if (lastEol) {
						lastEol = false;
						final long cur = getFilePointer();
						if ((readBackwards()) != '\r') {
							seek(cur);
						}
					} else {
						read(); // to go after line separator
						return true;
					}
					break;
				}
			}
		}
	}

	public int readBackwards() throws IOException {
		long cur = getFilePointer();
		if (cur == 0) {
			return -1;
		} else {
			raf.seek(--cur);
			int c = raf.read();
			raf.seek(cur);
			resetBuffer();
			return c;
		}
	}

	private static final class ByteBuffer {

		private byte[] value = new byte[256];

		/**
		 * The count is the number of characters used.
		 */
		private int count = 0;

		public void add(byte b) {
			int newCount = count + 1;
			if (newCount > value.length) {
				expandCapacity(newCount);
			}
			value[count++] = b;
		}

		public byte[] getBytes() {
			byte[] byteArray = new byte[count];
			for (int i = 0; i < count; i++) {
				byteArray[i] = value[i];
			}
			return byteArray;
		}

		public int length() {
			return count;
		}

		private void expandCapacity(int minimumCapacity) {
			int newCapacity = (value.length + 1) * 2;
			if (newCapacity < 0) {
				newCapacity = Integer.MAX_VALUE;
			} else if (minimumCapacity > newCapacity) {
				newCapacity = minimumCapacity;
			}
			byte newValue[] = new byte[newCapacity];
			System.arraycopy(value, 0, newValue, 0, count);
			value = newValue;
		}

	}

	public char getEnclosure() {
		return enclosure;
	}

	public void setEnclosure(char enclosure) {
		this.useEnclosure = true;
		this.enclosure = enclosure;
	}
	
	public void disableEnclosure() {
		this.useEnclosure = false;
	}

}
