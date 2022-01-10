/*
 * created on 09.11.2006
 * created by lolling.jan
 */
package sqlrunner.command;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import dbtools.DatabaseSession;

public class Main {
    
    private static String url = null;
    private static String user = null;
    private static String passwd = null;
    private static String driver = null;
    private static String delimiter = null;
    private static boolean silent = false;

    public static void printUsage() {
        System.out.println("  can be used to execute any SQL code per script");
        System.out.println("  command:");
        System.out.println("    java -cp sqlrunner-<version>.jar:log4j.jar:ojdbc14.jar sqlrunner.command.Main [silent] <driver> <url> <user> <password> <delimiter> <[\"]SQL-code[\"]>");
        System.out.println("  You can set your SQL-code in double qoutas or not, if not probably you have problems with shell scripts.");
        System.out.println("  All message appears in the err (2>) channel, all result sets appeards in the out (1>) channel.");
        System.out.println("  Here an example of an Oracle-URL: jdbc:oracle:thin:@//q4dee1co076.ffm.t-com.de:1521/orapbis");
        System.out.println("  The driver can be abbreviated as \"oracle\" (without qoutas), in all other cases you have to specify the driver class name !");
        System.out.println("  Have a lot of fun...bye");
    }
    
    private static void resultOut(String text) {
        System.out.println(text);
    }
    
    private static void messageOut(String text) {
        if (silent == false) {
            System.err.println(text);
        }
    }
    
    public static void main(String[] args) {
        // 1. Argument ist die URL die user+password enthalten muss
        // 2. user@password
        // 3. driver class name
        // 4. Argument ist Trenner
        // 5. SQL in doppelten Hochkomma
        if (args.length == 0) {
        	messageOut("SQLRunner command interface");
            printUsage();
            System.exit(0);
        } else {
            StringBuffer sql = new StringBuffer();
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (i == 0) {
                    if (args[i].equalsIgnoreCase("silent")) {
                        silent = true;
                        continue; 
                    }
                    messageOut("SQLRunner command interface");
                    messageOut("  analyse parameters....");
                }
                if (index == 0) {
                    driver = args[i];
                    if (driver.equalsIgnoreCase("oracle")) {
                        driver = "oracle.jdbc.driver.OracleDriver";
                    }
                    messageOut("  * DRIVER=" + driver);
                } else if (index == 1) {
                    url = args[i];
                    messageOut("  * URL=" + url);
                } else if (index == 2) {
                    user = args[i];
                    messageOut("  * USER=" + url);
                } else if (index == 3) {
                    passwd = args[i];
                    messageOut("  * PASSWD=" + passwd);
                } else if (index == 4) {
                    delimiter = args[i];
                } else {
                    // assemble sql code
                    sql.append(args[i]);
                    sql.append(' ');
                }
                index++;
            }
            File f = null;
            if (sql.length() == 0) {
                resultOut("  * connect and disconnect without proceeding sql code");
            } else {
            	String code = sql.toString();
            	f = new File(code);
            	if (f.exists()) {
            		
            	}
            	messageOut("  * SQL=" + sql.toString());
            }
            DatabaseSession session = new DatabaseSession();
            session.setUrl(url);
            session.setDriverClassName(driver);
            session.setUser(user);
            session.setPasswd(passwd);
            session.setAutoCommit(true);
            session.setFetchSize(10000);
            messageOut("  load driver...");
            if (session.loadDriver()) {
            	messageOut("  connect...");
                if (session.connect()) {
                	messageOut("  proceed sql...");
                    if (session.execute(sql.toString())) {
                        if (session.lastStatementWasAQuery()) {
                            ResultSet rs = session.getCurrentResultSet();
                            try {
                                ResultSetMetaData rsmd = rs.getMetaData();
                                int columnCount = rsmd.getColumnCount();
                                String content = null;
                                while (rs.next()) {
                                    for (int i = 0; i < columnCount; i++) {
                                        if (i > 0) {
                                            resultOut(delimiter);
                                        }
                                        content = rs.getString(i + 1);
                                        if (content != null) {
                                        	resultOut(content.replace('\n', ' '));
                                        }
                                    }
                                    System.out.println();
                                }
                            } catch (SQLException sqle) {
                            	messageOut(sqle.toString());
                            }
                        }
                    }
                    session.close();
                } else {
                    System.exit(2);
                }
            } else {
                System.exit(2);
            }
            if (session.isSuccessful()) {
            	messageOut("end succesful");
                System.exit(0);
            } else {
            	messageOut("end with errors");
                System.exit(1);
            }
        }
    }
    
}
