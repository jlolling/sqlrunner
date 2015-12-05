package dbtools;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author lolling.jan
 *
 * Diese Klasse enthält Informationen über eine DatabaseSession 
 * die aufrufende URL muss der URL in der static constant entsprechen. 
 */
public class DatabaseSessionPoolInfoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String URL = "dbPoolInfo";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        final PrintWriter out=response.getWriter();
        final String command=request.getParameter("command");
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        if (command == null) {
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n\t\"http://www.w3.org/TR/html4/transitional.dtd\">");
            out.println("<html>");
            out.println("<head>");
            out.println("  <title>Info zum DatabaseSessionPool</title>");
            out.println("  <style type='text/css'>");
            out.println("    th");
            out.println("    {");
            out.println("      font-family:Verdana,Arial;");
            out.println("      font-size: 9px;");
            out.println("      color: #ffffff;");
            out.println("      background-color : #808285;");
            out.println("      vertical-align: middle;");
            out.println("    }");
            out.println("    td");
            out.println("    {");
            out.println("      font-family:Verdana,Arial;");
            out.println("      font-size: 9px;");
            out.println("      color: #000000;");
            out.println("      background-color : #ffffff;");
            out.println("      vertical-align: middle;");
            out.println("    }");
            out.println("  </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h4>DatabaseSessionPool Information</h4>");
            Vector<DatabaseSession> pool = DatabaseSessionPool.getPool();
            out.println("<table border='1' cellpadding='1' cellspacing='0'>");
            out.println("  <tr>");
            out.println("    <th>Anzahl Sessions im Pool</th>");
            out.println("    <td>" + pool.size() + "</td>");
            out.println("  </tr>");
            out.println("  <tr>");
            out.println("    <th>max idle time</th>");
            out.println("    <td>" + DatabaseSessionPool.getMaxIdleTime() + "s</td>");
            out.println("  </tr>");
            if (DatabaseSessionPool.getCheckPoolCyclusTime() > 0) {
                out.println("  <tr>");
                out.println("    <th>Automatischer Test alle</th>");
                out.println("    <td>" + DatabaseSessionPool.getCheckPoolCyclusTime() + "s</td>");
                out.println("  </tr>");
            } else {
                out.println("  <tr>");
                out.println("    <th>Automatischer Test</th>");
                out.println("    <td>AUS</td>");
                out.println("  </tr>");
            }
            if (DatabaseSessionPool.getPoolCheckScheduledExecutionTime() > 0) {
                out.println("  <tr>");
                out.println("    <th>letzter Test gestartet</th>");
                out.println("    <td>"
                        + sdf.format(new Date(DatabaseSessionPool.getPoolCheckScheduledExecutionTime()))
                        + "</td>");
                out.println("  </tr>");
            } else {
                out.println("  <tr>");
                out.println("    <th>letzter Test gestartet</th>");
                out.println("    <td> -- </td>");
                out.println("  </tr>");
            }
            if (DatabaseSessionPool.getLastOccuranceOfPoolCheck() > 0) {
                if (DatabaseSessionPool.getPoolCheckScheduledExecutionTime() > DatabaseSessionPool.getLastOccuranceOfPoolCheck()) {
                    out.println("  <tr>");
                    out.println("    <th>letzter Test beendet</th>");
                    out.println("    <td>Test läuft</td>");
                    out.println("  </tr>");
                } else {
                    out.println("  <tr>");
                    out.println("    <th>letzter Test beendet</th>");
                    out.println("    <td>"
                            + sdf.format(new Date(DatabaseSessionPool.getLastOccuranceOfPoolCheck()))
                            + "</td>");
                    out.println("  </tr>");
                }
            } else {
                out.println("  <tr>");
                out.println("    <th>letzter Test beendet</th>");
                out.println("    <td> -- </td>");
                out.println("  </tr>");
            }
            if (DatabaseSessionPool.getLastOccuranceOfPoolCheckMessage() > 0) {
                out.println("  <tr>");
                out.println("    <th>letzte Änderung</th>");
                out.println("    <td>"
                        + sdf.format(new Date(DatabaseSessionPool.getLastOccuranceOfPoolCheckMessage()))
                        + "</td>");
                out.println("  </tr>");
                out.println("  <tr>");
                out.println("    <th>Info</th>");
                out.println("    <td>" + DatabaseSessionPool.getLastPoolCheckMessage() + "</td>");
                out.println("  </tr>");
            }
            out.println("</table>");
            out.println("<hr>");
            out.println("<table border='1' cellpadding='1' cellspacing='0' width='100%'>");
            out.println("  <tr>");
            out.println("    <th>ID</th>");
            out.println("    <th>Klasse</th>");
            out.println("    <th>wann verbunden</th>");
            out.println("    <th>zuletzt benutzt</th>");
            out.println("    <th>wie oft benutzt</th>");
            out.println("    <th>Idle time (s)</th>");
            out.println("    <th>bereit ?</th>");
            out.println("    <th>letzte Fehlermeldung</th>");
            out.println("    <th>wann aufgetreten</th>");
            out.println("    <th>URL/JNDI</th>");
            out.println("    <th>DB Schema</th>");
            out.println("  </tr>");
            DatabaseSession session;
            for (int i = 0; i < pool.size(); i++) {
                session = pool.elementAt(i);
                if (session != null) {
                    out.println("  <tr>");
                    out.println("    <td><a href='"
                            + URL
                            + "?command=details&sessionId="
                            + session.getSessionID()
                            + "'>"
                            + session.getSessionID()
                            + "</a></td>");
                    out.println("    <td>" + session.getClass().getName() + "</td>");
                    out.println("    <td>" + sdf.format(new Date(session.getConnectedTimestamp())) + "</td>");
                    if (session.getLastOccupiedTimestamp() > 0) {
                        out.println("    <td>" + sdf.format(new Date(session.getLastOccupiedTimestamp())) + "</td>");
                    } else {
                        out.println("    <td>unbenutzt</td>");
                    }
                    out.println("    <td>" + session.getCountUsage() + "</td>");
                    out.println("    <td>" + session.getIdleTime() + "</td>");
                    out.println("    <td>" + session.isReady() + "</td>");
                    if (session.getLastErrorMessage() != null) {
                        out.println("    <td>" + session.getLastErrorMessage() + "</td>");
                        out.println("    <td>" + sdf.format(new Date(session.getLastErrorTimestamp())) + "</td>");
                    } else {
                        out.println("    <td> OK </td>");
                        out.println("    <td> -- </td>");
                    }
                    if (session.getConnectionDescription().getJndiDataSourceName() != null) {
                        out.println("    <td colspan=2>" + session.getConnectionDescription().getJndiDataSourceName() + "</td>");
                        out.println("  </tr>");
                    } else {
                        out.println("    <td>" + session.getUrl() + "</td>");
                        out.println("    <td>" + session.getUser() + "</td>");
                        out.println("  </tr>");
                    }
                }
            }
            out.println("</table>");
            out.println("</body>");
            out.println("</html>");
            pool = null;
        } else if (command.equals("details")) {
            final int sessionId=Integer.parseInt(request.getParameter("sessionId"));
            final DatabaseSession session = DatabaseSessionPool.getDatabaseSession(sessionId);
            if (session != null) {
                out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n\t\"http://www.w3.org/TR/html4/transitional.dtd\">");
                out.println("<html>");
                out.println("<head>");
                out.println("  <title>Info zur DatabaseSession ID = " + sessionId + "</title>");
                out.println("  <style type='text/css'>");
                out.println("    th");
                out.println("    {");
                out.println("      font-family:Verdana,Arial;");
                out.println("      font-size: 9px;");
                out.println("      color: #ffffff;");
                out.println("      background-color : #808285;");
                out.println("      vertical-align: middle;");
                out.println("    }");
                out.println("    td");
                out.println("    {");
                out.println("      font-family:Verdana,Arial;");
                out.println("      font-size: 9px;");
                out.println("      color: #000000;");
                out.println("      background-color : #ffffff;");
                out.println("      vertical-align: middle;");
                out.println("    }");
                out.println("  </style>");
                out.println("</head>");
                out.println("<body>");
                out.println("<h4>Info zur DatabaseSession ID = " + sessionId + "</h4>");
                out.println("<p>letzte Action = ");
                out.println(session.getLastSQL());
                out.println("</p>");
                out.println("<p>Transactionssteuerung: ");
                out.println(((session.isAutoCommit()) ? "autocommit=true" : "autocommit=false"));
                out.println("</p>");
                out.println("<input type='button' value='KILL session' onclick=\"self.location.href='"
                        + URL
                        + "?command=kill&sessionId="
                        + sessionId
                        + "'\" />");
                out.println("<input type='button' value='COMMIT session' onclick=\"self.location.href='"
                        + URL
                        + "?command=commit&sessionId="
                        + sessionId
                        + "'\" />");
                out.println("<input type='button' value='RESET session' onclick=\"self.location.href='"
                        + URL
                        + "?command=reset&sessionId="
                        + sessionId
                        + "'\" />");
                out.println("</body>");
                out.println("</html>");
            } else {
                out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n\t\"http://www.w3.org/TR/html4/transitional.dtd\">");
                out.println("<html>");
                out.println("<head>");
                out.println("  <title>Info zur DatabaseSession ID = " + sessionId + "</title>");
                out.println("  <style type='text/css'>");
                out.println("    th");
                out.println("    {");
                out.println("      font-family:Verdana,Arial;");
                out.println("      font-size: 9px;");
                out.println("      color: #ffffff;");
                out.println("      background-color : #808285;");
                out.println("      vertical-align: middle;");
                out.println("    }");
                out.println("    td");
                out.println("    {");
                out.println("      font-family:Verdana,Arial;");
                out.println("      font-size: 9px;");
                out.println("      color: #000000;");
                out.println("      background-color : #ffffff;");
                out.println("      vertical-align: middle;");
                out.println("    }");
                out.println("  </style>");
                out.println("</head>");
                out.println("<body>");
                out.println("<h4>keine DatabaseSession mit ID = " + sessionId + " gefunden !</h4>");
                out.println("</body>");
                out.println("</html>");
            }
        } else if (command.equals("kill")) {
            final int sessionId=Integer.parseInt(request.getParameter("sessionId"));
            DatabaseSessionPool.killSession(sessionId);
            response.sendRedirect(URL);
        } else if (command.equals("commit")) {
            final int sessionId=Integer.parseInt(request.getParameter("sessionId"));
            final DatabaseSession session=DatabaseSessionPool.getDatabaseSession(sessionId);
            if (session != null) {
                session.commitUnchecked();
            }
            response.sendRedirect(URL);
        } else if (command.equals("reset")) {
            final int sessionId=Integer.parseInt(request.getParameter("sessionId"));
            final DatabaseSession session=DatabaseSessionPool.getDatabaseSession(sessionId);
            if (session != null) {
                session.resetErrorStatus();
            }
            response.sendRedirect(URL);
        }
    }

}
