/*
 * created on 26.09.2005
 * created by lolling.jan
 */
package sqlrunner.flatfileimport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class StringInputStream extends InputStream {
    
    private byte[] bytes = null;
    private int pointer  = 0;
    
    public StringInputStream(String string) throws IOException {
        if (string == null) {
            throw new IOException("null string not supported");
        }
        bytes = string.getBytes();
    }

    public StringInputStream(String string, String charSet) throws UnsupportedEncodingException, IOException {
        if (string == null) {
            throw new IOException("null string not supported");
        }
        bytes = string.getBytes(charSet);
    }

    public int available() throws IOException {
        if (bytes == null) {
            throw new IOException("stream is closed");
        }
        return bytes.length - pointer;
    }

    public void close() throws IOException {
        bytes = null;
        pointer = 0;
    }

    public boolean markSupported() {
        return false;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (bytes == null) {
            throw new IOException("stream is closed");
        }
        int readedBytes = 0;
        if (pointer >= bytes.length) {
            readedBytes = -1;
        } else {
            for (int i = 0; i < len; i++) {
                if (pointer >= bytes.length) {
                    break;
                } else {
                    b[i + off] = bytes[pointer++];
                    readedBytes++;
                }
            }
        }
        return readedBytes;
    }

    public int read(byte[] b) throws IOException {
        if (bytes == null) {
            throw new IOException("stream is closed");
        }
        int readedBytes = 0;
        if (pointer >= bytes.length) {
            readedBytes = -1;
        } else {
            for (int i = 0; i < b.length; i++) {
                if (pointer >= bytes.length) {
                    break;
                } else {
                    b[i] = bytes[pointer++];
                    readedBytes++;
                }
            }
        }
        return readedBytes;
    }

    public synchronized void reset() throws IOException {
        if (bytes == null) {
            throw new IOException("stream is closed");
        }
        pointer = 0;
    }

    public long skip(long n) throws IOException {
        if (bytes == null) {
            throw new IOException("stream is closed");
        }
        if (n > bytes.length - pointer - 1) {
            n = bytes.length - pointer - 1;
        }
        pointer = pointer + (int) n;
        return n;
    }

    public int read() throws IOException {
        if (bytes == null) {
            throw new IOException("stream is closed");
        }
        if (pointer >= bytes.length) {
            return -1;
        } else {
            return (int) bytes[pointer++];
        }
    }

}
