package sqlrunner;

import java.io.File;
import java.io.Serializable;

import javax.swing.filechooser.FileFilter;

/**
 * @author Lolling.Jan
 *
 */
public class XmlFileFilter extends FileFilter implements Serializable {

	private static final long serialVersionUID = 1L;

    public boolean accept(File f) {
        return (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
    }

    public String getDescription() {
        return "XML";
    }

}

