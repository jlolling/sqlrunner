package sqlrunner.flatfileimport.gui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author Lolling.Jan
 *
 */
public final class TalendSchemaFileFilter extends FileFilter {

    public boolean accept(File f) {
        return (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
    }

    public String getDescription() {
        return "Talend Schema";
    }

}

