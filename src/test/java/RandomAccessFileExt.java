/**
 * created on 14.05.2007
 * created by lolling.jan
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * this class adds the ability of using different charsets in import files
 * @author jan
 */
public class RandomAccessFileExt extends RandomAccessFile {

    private String charSet;

    public RandomAccessFileExt(String name, String mode) throws FileNotFoundException {
        super(name != null ? new File(name) : null, mode);
    }

    public RandomAccessFileExt(File file, String mode) throws FileNotFoundException {
        super(file, mode);
    }

    public RandomAccessFileExt(String name, String mode, String charSet) throws FileNotFoundException {
        super(name != null ? new File(name) : null, mode);
        this.charSet = charSet;
    }

    public RandomAccessFileExt(File file, String mode, String charSet) throws FileNotFoundException {
        super(file, mode);
        this.charSet = charSet;
    }

    /**
     * Reads the next line of text from this file.  This method successively
     * reads bytes from the file, starting at the current file pointer, 
     * until it reaches a line terminator or the end
     * of the file.  Each byte is converted into a character by taking the
     * byte's value for the lower eight bits of the character and setting the
     * high eight bits of the character to zero.  This method does not,
     * therefore, support the full Unicode character set.
     *
     * <p> A line of text is terminated by a carriage-return character
     * (<code>'&#92;r'</code>), a newline character (<code>'&#92;n'</code>), a
     * carriage-return character immediately followed by a newline character,
     * or the end of the file.  Line-terminating characters are discarded and
     * are not included as part of the string returned.
     *
     * <p> This method blocks until a newline character is read, a carriage
     * return and the byte following it are read (to see if it is a newline),
     * the end of the file is reached, or an exception is thrown.
     *
     * @return     the next line of text from this file, or null if end
     *             of file is encountered before even one byte is read.
     * @exception  IOException  if an I/O error occurs.
     */
    public final String readLineWithCharSet() throws IOException {
    	if (charSet == null || charSet.length() == 0) {
    		throw new IOException("charset not set");
    	}
        final ByteBuffer input = new ByteBuffer();
        int c = -1;
        boolean eol = false;
        while (!eol) {
            switch (c = read()) {
                case -1:
                case '\n':
                    eol = true;
                    break;
                case '\r':
                    eol = true;
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
        return new String(input.getBytes(), charSet);
    }
    
    public final void seekToNextLineStart() throws IOException {
        boolean eol = false;
        while (!eol) {
            switch (read()) {
                case -1:
                case '\n':
                    eol = true;
                    break;
                case '\r':
                    eol = true;
                    final long cur = getFilePointer();
                    if ((read()) != '\n') {
                        seek(cur);
                    }
                    break;
            }
        }
    }

    public int readBackwards() throws IOException {
        long cur = getFilePointer();
    	if (cur == 0) {
    		return -1;
    	} else {
    		seek(--cur);
        	int c = read();
    		seek(cur);
        	return c;
    	}
    }
    
    public void seekToPrevLineStart() throws IOException {
        boolean eol = false;
        boolean lastEol = true;
        while (eol == false) {
            int c = readBackwards();
        	switch (c) {
                case -1:
                	lastEol = false;
                    eol = true;
                    break;
                case '\r':
                    eol = true;
                    break;
                case '\n':
                    eol = true;
                    final long cur = getFilePointer();
                    if ((readBackwards()) != '\r') {
                        seek(cur);
                    }
                    break;
                default:
                	lastEol = false;
            }
            if (lastEol && eol) {
            	lastEol = false;
            	eol = false;
            } else {
            	if (c == '\r') {
            		c = read();
            	}
            	if (c == '\n') {
            		c = read();
            	}
            }
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

}
