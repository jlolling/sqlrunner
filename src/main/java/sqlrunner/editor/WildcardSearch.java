package sqlrunner.editor;

import java.io.File;
import java.io.FileFilter;

import sqlrunner.Main;

/**
 *  spezielle Version eines FilenameFilters
 *  kann Wildcards verarbeiten
 */
public class WildcardSearch implements FileFilter {

    private String filter;
    private String exclExt;              // Datei-Erweiterung, die nicht mit angezeigt werden soll
    private boolean caseSensitive = false;

    WildcardSearch(String init, String exclExt) {
        this.filter = init;
        this.exclExt = exclExt;
    }

    WildcardSearch(String init, String exclExt, boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        if (caseSensitive) {
            this.filter = init;
        } else {
            this.filter = init.toLowerCase();
        }
        this.exclExt = exclExt;
    }

    public WildcardSearch() {}

    public boolean patternSearch(String muster, String test) {
        boolean passt = true;
        String region;
        int musterPos = 0;
        int testPos = 0;
        int testPosNeu = 0;
        int sternPos;
        int fragePos;
        int regionEndePos;
        boolean fixeSuchstelle = false;
        // f ohne Bedeutung, da Verzeichnis kein Filterkriterium
        // sein soll, aber das Interface erwartet diesen Parameter
        // erstes Zeichen im filter testen
        while (passt && (musterPos < muster.length())) {
            switch (muster.charAt(musterPos)){
                case '*': {
                    musterPos++;
                    fixeSuchstelle = false;
                    break;
                }
                case '?': {
                    musterPos++;
                    testPos++;
                    fixeSuchstelle = true;
                    break;
                }
                default: {
                    // kein Wildcard gelesen
                    // dann ab aktueller position suchString ermitteln
                    if (musterPos == 0) {
                        fixeSuchstelle = true;
                    }
                    // SuchTeilString extrahieren ->region
                    sternPos = muster.indexOf('*', musterPos);
                    fragePos = muster.indexOf('?', musterPos);
                    if ((sternPos != -1) && (fragePos != -1)) {
                        // wenn beide vorhanden, den nächst liegenden nehmen
                        regionEndePos = Math.min(sternPos, fragePos); // nächstliegendes Ende finden
                    } else {
                        // einer von beider ist -1
                        // dann den anderen nehmen
                        regionEndePos = Math.max(sternPos, fragePos);
                    }
                    // beide -1 ?
                    if (regionEndePos == -1) {
                        region = muster.substring(musterPos, muster.length());
                        musterPos = muster.length(); // den restlichen String nehmen
                    } else {
                        region = muster.substring(musterPos, regionEndePos);
                        musterPos = regionEndePos; // nächste Stringregion separieren
                    }
                    // nur suchen wenn was zum suchen vorhanden
                    if (region.length() != 0) {
                        // position der region innerhalb test ermitteln
                        testPosNeu = test.indexOf(region, testPos);
                        // Ergebnis des Mustervergleichs auswerten
                        // wenn fixeSuchstelle, dann muss ...
                        if (fixeSuchstelle) {
                            // ...die gefundene position der region mit vorgegebener Pos
                            // übereinstimmen
                            if (testPosNeu != testPos) {
                                // passt nicht
                                passt = false;
                            } else {
                                testPos = testPosNeu + region.length();
                            }
                        } else {
                            // keine fixSuchstelle, dann muss neue position nur
                            // nach der alten liegen und groesser als -1 sein
                            if (testPosNeu < testPos) {
                                passt = false;
                            } else {
                                testPos = testPosNeu + region.length();
                            }
                        }
                        // nun testen ob region am Ende von muster lag,
                        // dann muss das auch bei test der Fall sein
                        if (musterPos == muster.length()) {
                            if (testPos < test.length()) {
                                passt = false;
                            }
                        }
                    }
                }}
        }
        // wenn fixeSuchstelle, dann muessen beide Zeiger auf Ende zeigen
        // sonst nicht gefunden setzen
        if (fixeSuchstelle && ((musterPos != muster.length()) || (testPos != test.length()))) {
            passt = false;
        }
        return passt;
    }

    public boolean accept(File f) {
        if (f.isFile()) {
            boolean ok;
            if ((exclExt.length() != 0) && ((f.getAbsolutePath()).toLowerCase()).endsWith(exclExt)) {
                return false;
            } else {
                if (caseSensitive) {
                    ok = patternSearch(filter, Main.getName(f));
                } else {
                    ok = patternSearch(filter, (Main.getName(f)).toLowerCase());
                }
                return ok;
            }
        } else {
            return true;
        }
    }
}
