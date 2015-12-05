package sqlrunner.flatfileimport.gui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author Lolling.Jan
 *
 */
public final class ImpDataFileFilter extends FileFilter {

    public boolean accept(File f) {
    	if (f.isDirectory()) {
    		return true;
    	} else {
        	String fileName = f.getName().toLowerCase();
            return fileName.endsWith(".csv")
                || fileName.endsWith(".tsv")
                || fileName.endsWith(".txt")
                || fileName.endsWith(".xlsx")
                || fileName.endsWith(".xls");
    	}
    }

    public String getDescription() {
        return "Importdaten";
    }

}

