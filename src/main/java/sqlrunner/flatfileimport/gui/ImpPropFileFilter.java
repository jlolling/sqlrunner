package sqlrunner.flatfileimport.gui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author Lolling.Jan
 *
 */
public final class ImpPropFileFilter extends FileFilter {

    public boolean accept(File f) {
        return (f.isDirectory() || f.getName().toLowerCase().endsWith(ImportConfiguratorFrame.IMPORT_CONFIG_EXTENSION));
    }

    public String getDescription() {
        return "Importkonfiguration";
    }

}

