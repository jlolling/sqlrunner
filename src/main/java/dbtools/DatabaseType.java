package dbtools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Klasse nimmt eine Datenbank-Typ-Beschreibung auf
 */
public class DatabaseType implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id;                   // zur Identifizierung
    private String name;                 // zur sinnvollen Anzeige für Auswahlboxen
    private String driverClassName;      // Treiber-Klassenname
    private String urlTemplate;          // Vorlage für URL
    private int adminOptionID = -1;
    private List<URLElement> urlElements;
    private boolean userDataInUrl = false;

    public DatabaseType(int id, String name, String driverClassName, String urlTemplate) {
        this.id = id;
        this.name = name;
        this.driverClassName = driverClassName;
        this.urlTemplate = urlTemplate;
        urlElements = getURLElements(urlTemplate);
    }

    public DatabaseType(String paramStr) {
        parseParamStr(paramStr);
        urlElements = getURLElements(urlTemplate);
    }

    private void parseParamStr(String param) {
        final int i1=param.indexOf('|');
        id = Integer.parseInt((param.substring(0, i1)).trim());
        final int i2=param.indexOf('|',i1 + 1);
        name = param.substring(i1 + 1, i2);
        final int i3=param.indexOf('|',i2 + 1);
        driverClassName = (param.substring(i2 + 1, i3)).trim();
        final int i4=param.indexOf('|',i3 + 1);
        if (i4 > i3) {
            urlTemplate = (param.substring(i3 + 1, i4)).trim();
            adminOptionID = Integer.parseInt((param.substring(i4 + 1, param.length())).trim());
        } else {
            urlTemplate = param.substring(i3 + 1, param.length()).trim();
            adminOptionID = 0;	
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public List<URLElement> getURLElements() {
        return urlElements;
    }

    public List<URLElement> cloneURLElementList() {
    	List<URLElement> list = new ArrayList<URLElement>(urlElements.size());
    	for (URLElement elem : urlElements) {
    		list.add(elem);
    	}
    	return list;
    }
    
    public URLElement getURLElementAt(int index) {
        if (index < urlElements.size()) {
            return (URLElement) urlElements.get(index);
        } else {
            return null;
        }
    }

    public int getURLElementeCount() {
        return urlElements.size();
    }

    public int getAdminOptionID() {
        return adminOptionID;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isUserDataInUrl() {
        return userDataInUrl;
    }

    /**
     * das Template enthält eingefasst in %-Zeichen die zu konfigurierenden
     * Bestandteile der URL, wobei innerhalb des Bestandteiles
     * Default-werte enthalten sind die nach einem ggf vorhandenen =-Zeichen beginnen
     */
    private List<URLElement> getURLElements(String urlTemplate_loc) {
        boolean fertig = false;
        URLElement element;
        String temp;
        final Vector<URLElement> elements = new Vector<URLElement>();
        // immer USER und PASSWORD hinzufügen
        int p0 = -1;
        int p1 = -1;
        int p2 = -1;
        while(!fertig) {
            p0 = urlTemplate_loc.indexOf('%', p1 + 1);
            p1 = urlTemplate_loc.indexOf('%', p0 + 1);
            if ((p0 != -1) && (p1 != -1)) {
                element = new URLElement();
                // den Teil nun untersuchen nach =
                temp = urlTemplate_loc.substring(p0 + 1, p1);
                if (temp.equalsIgnoreCase("USER") || temp.equalsIgnoreCase("PASSWORD")) {
                    userDataInUrl = true;
                }
                p2 = temp.indexOf('=');
                if (p2 != -1) {
                    element.setName(temp.substring(0, p2));
                    element.setValue(temp.substring(p2 + 1, temp.length()));
                } else {
                    element.setName(temp);
                }
                elements.addElement(element);
            } else {
                fertig = true;
            }
        }
        if(!userDataInUrl) {
            elements.addElement(new URLElement(URLElement.USER_NAME, null));
            elements.addElement(new URLElement(URLElement.PASSWORD_NAME, null));
        }
        return elements;
    }

}
