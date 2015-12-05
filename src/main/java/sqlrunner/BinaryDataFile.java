package sqlrunner;

import java.sql.Blob;

/**
 * stellt einen Platzhalter fÃ¼r binï¿½re Daten in einem Datenbankfeld dar.
 * @author jan
 */
public class BinaryDataFile {
    private String name;
    private Blob blob;

    public BinaryDataFile() {}

    public BinaryDataFile(Blob blob) {
    	this.blob = blob;
    }

    BinaryDataFile(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        if (name != null) {
            return name;
        } else {
            return "binary data";
        }
    }

    public String getFilename() {
        return name;
    }

	public Blob getBlob() {
		return blob;
	}

}
