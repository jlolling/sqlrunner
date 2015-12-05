package sqlrunner.flatfileimport;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author Lolling.Jan
 *
 */
class CSVFileFilter extends FileFilter {

    public boolean accept(File f) {
        return (f.isDirectory() || !f.getName().toLowerCase().endsWith(".cvs"));
    }

    public String getDescription() {
        return "CSV Daten";
    }

}

