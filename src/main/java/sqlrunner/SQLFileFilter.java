package sqlrunner;

import java.io.File;
import java.io.Serializable;

import javax.swing.filechooser.FileFilter;

/**
 * @author Lolling.Jan
 *
 */
public class SQLFileFilter extends FileFilter implements Serializable {

	private static final long serialVersionUID = 1L;

	public boolean accept(File f) {
        return (f.isDirectory() || f.getName().toLowerCase().endsWith(".sql"));
    }

    public String getDescription() {
        return "SQL Script";
    }

}

