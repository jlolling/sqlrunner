package sqlrunner.config;

/**
 * Interface dass ein Konfigaurationspanel beschreibt.
 */
public interface ConfigurationPanel {

    /**
     * signalisiert, dass eine Änderung an der Konfiguration erfolgen soll
     * @return true wenn Konfigurationsdaten geändert werden sollen
     */
    boolean isChanged();

    /**
     * Ausführen der Konfigurationsänderungen
     * @return true wenn erfolgreich geändert.
     */
    boolean performChanges();

    /**
     * setzt die Einstellungen zurück 
     */
    void cancel();

}
