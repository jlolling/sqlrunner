package sqlrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.Logger;

import dbtools.SQLStatement;

public class AdminStatement extends SQLStatement {

    private static final long serialVersionUID = 1L;
    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(AdminStatement.class);
    private String comment;
    private int dbType = -1;
    private char command;    // select editable or run
    static Vector<AdminStatement> adminSQLs = null;

    public AdminStatement(int dbType, char command, String sql, String comment) {
        super(sql);
        this.dbType = dbType;
        this.hidden = true;
        this.comment = comment;
        this.command = command;
    }

    public int getDbType() {
        return dbType;
    }

    public String getComment() {
        return comment;
    }

    public char getCommand() {
        return command;
    }

    /**
     * read Admin-SQLs from File
     * @param File contains the SQLs
     */
    static public synchronized void readAdminSQLs(File f) {
        adminSQLs = new Vector<AdminStatement>();
        adminSQLs.removeAllElements();
        String line = null;
        String comment_loc = null;
        final StringBuffer sql_loc = new StringBuffer();
        int type = -1;
        char command_loc = ' ';
        int pos0;
        int pos1;
        boolean inSQL = false;
        AdminStatement adminStat = null;
        try {
            final BufferedReader br = new BufferedReader(new FileReader(f));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if ((!line.startsWith("#")) && (line.length() > 4)) {
                    if (line.startsWith("[")) { // Beginn einer Admin-SQL gefunden
                        if (inSQL) { // einen vorhergehenden Lesevorgang einer SQL beenden
                            adminStat = new AdminStatement(type, command_loc, sql_loc.toString(), comment_loc);
                            if (logger.isDebugEnabled()) {
                                logger.debug(adminStat);
                            }
                            adminSQLs.addElement(adminStat);
                            inSQL = false;
                        }
                        pos0 = line.indexOf('|');
                        type = Integer.parseInt((line.substring(1, pos0)).trim());
                        command_loc = line.charAt(pos0 + 1);
                        pos0 = pos0 + 3;
                        pos1 = line.indexOf(']');
                        comment_loc = (line.substring(pos0, pos1)).trim();
                        inSQL = true; // ab jetzt neue SQL-Anweisung lesen
                        sql_loc.setLength(0); // StringBuffer löschen
                    } else {
                        sql_loc.append(line);
                        sql_loc.append('\n');
                    }
                }
            }
            br.close();
            // Am Ende den Rest einsammeln
            if (sql_loc.length() > 2) {
                adminStat = new AdminStatement(type, command_loc, sql_loc.toString(), comment_loc);
                if (logger.isDebugEnabled()) {
                    logger.debug(adminStat);
                }
                adminSQLs.addElement(adminStat);
            }
        } catch (IOException ioe) {
            System.err.println("SQLAdminStatement.readAdminSQLs:" + ioe.toString());
        }
    }

    static AdminStatement getSQLAdminStatement(String name) {
        AdminStatement as = null;
        for (int i = 0; i < adminSQLs.size(); i++) {
            as = adminSQLs.elementAt(i);
            if (name.equals(as.getComment())) {
                break;
            } else {
                as = null;
            }
        }
        return as;
    }

    @Override
    public String toString() {
        return comment;
    }
    
    public int hashCode() {
    	return adminSQLs.hashCode();
    }

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
    
}
