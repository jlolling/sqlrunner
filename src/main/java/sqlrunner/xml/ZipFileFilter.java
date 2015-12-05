package sqlrunner.xml;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author Lolling.Jan
 *
 */
class ZipFileFilter extends FileFilter {

    public boolean accept(File f) {
        return (f.isDirectory() || f.getName().toLowerCase().endsWith(".zip"));
    }

    public String getDescription() {
        return "ZIP archive";
    }

}
