package sqlrunner.editor;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import sqlrunner.swinghelper.WindowHelper;

/**
 *  Suchen/Ersetzendialog
 */
public class SearchReplaceDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JTextField jTextFieldSearch = null;
    private JTextField jTextFieldReplace = null;
    private JButton jButtonSuche = null;
    private JButton jButtonCancel = null;
    private JCheckBox jCheckBoxCaseSensitiv = null;
    private int aktCaret;
    private JCheckBox jCheckBoxWithoutAsk = null;
    private JCheckBox jCheckBoxAbAnfang = null;
    private JLabel jLabelInfo = null;
    private JButton jButtonReplace = null;
    private boolean replaceIsSelected;
    private JTextComponent editor;
    private JCheckBox jCheckBoxWholeWords = null;
    private transient SearchReplaceThread searchReplaceThread = null;
    private JPanel jContentPane = null;

    public SearchReplaceDialog(JFrame frame, boolean modal, JTextComponent editor) {
        super(frame, Messages.getString("SearchReplaceDialog.title"), modal); //$NON-NLS-1$
        this.editor = editor;
        try {
            getRootPane().putClientProperty("Window.style", "small");
            initComponents();
            pack();
            setResizable(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        switch (e.getID()) {
            case WindowEvent.WINDOW_CLOSING:
                cancel();
                break;
        }
        super.processWindowEvent(e);
    }

    public void setSearchText(String text) {
        jTextFieldSearch.setText(editor.getSelectedText());
        jTextFieldSearch.requestFocus();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("suche")) { //$NON-NLS-1$
            replaceIsSelected = false;
            startSearch();
        }
        if (e.getActionCommand().equals("ersetze")) { //$NON-NLS-1$
            if (editor.getSelectionStart() != editor.getSelectionEnd()) {
                editor.replaceSelection(jTextFieldReplace.getText());
            }
            replaceIsSelected = true; // wenn nun noch jCheckBoxWithoutAsk=selected
            startSearch(); // dann erfolgt komplette Ersetzung im Text
        }
        if (e.getActionCommand().equals("abbrechen")) { //$NON-NLS-1$
            // verlaesst Dialog
            cancel();
        }
    }

    private void cancel() {
        if (searchReplaceThread != null && searchReplaceThread.isAlive()) {
            searchReplaceThread.interrupt();
            searchReplaceThread = null;
        }
        setVisible(false);
    }

    @Override
    public void setVisible(boolean visible) {
        if (isShowing() == false) {
            try {
                this.setLocationByPlatform(!WindowHelper.isWindowPositioningEnabled());
            } catch (NoSuchMethodError e) {
            }
        }
        super.setVisible(visible);
    }

    private void initComponents() throws Exception {
        this.setContentPane(getJContentPane());
        this.getRootPane().setDefaultButton(jButtonSuche);
        this.addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                this_keyPressed(e);
            }
        });

    }

    /**
     * This method initializes jContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            // und nicht über einen Event festgestellt
            // und nicht über einen Event festgestellt
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 3;
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(10, 2, 2, 10);
                jTextFieldSearch = new JTextField();
                jTextFieldSearch.setText(""); //$NON-NLS-1$
                jTextFieldSearch.addKeyListener(new java.awt.event.KeyAdapter() {

                    @Override
                    public void keyPressed(KeyEvent e) {
                        this_keyPressed(e);
                    }
                });
                jContentPane.add(jTextFieldSearch, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridwidth = 3;
                gbc.gridx = 0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridy = 1;
                jCheckBoxCaseSensitiv = new JCheckBox();
                jCheckBoxCaseSensitiv.setText(Messages.getString("SearchReplaceDialog.casesensitive")); //$NON-NLS-1$
                jContentPane.add(jCheckBoxCaseSensitiv, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 10);
                gbc.gridx = 2;
                gbc.gridy = 1;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridwidth = 2;
                jCheckBoxWholeWords = new JCheckBox();
                jCheckBoxWholeWords.setText(Messages.getString("SearchReplaceDialog.wholewords")); //$NON-NLS-1$
                jContentPane.add(jCheckBoxWholeWords, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = 3;
                gbc.gridx = 1;
                gbc.gridy = 2;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(5, 2, 2, 10);
                jTextFieldReplace = new JTextField();
                jTextFieldReplace.setText(""); //$NON-NLS-1$
                jTextFieldReplace.addKeyListener(new java.awt.event.KeyAdapter() {

                    @Override
                    public void keyPressed(KeyEvent e) {
                        this_keyPressed(e);
                    }
                });
                jContentPane.add(jTextFieldReplace, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 3;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 4;
                jCheckBoxWithoutAsk = new JCheckBox();
                jCheckBoxWithoutAsk.setText(Messages.getString("SearchReplaceDialog.replacewithoutasking")); //$NON-NLS-1$
                jCheckBoxWithoutAsk.addChangeListener(new javax.swing.event.ChangeListener() {

                    public void stateChanged(ChangeEvent e) {
                        jCheckBoxWithoutAsk_stateChanged(e);
                    }
                });
                jContentPane.add(jCheckBoxWithoutAsk, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 2, 2);
                gbc.gridx = 0;
                gbc.gridy = 4;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 4;
                jCheckBoxAbAnfang = new JCheckBox();
                jCheckBoxAbAnfang.setText(Messages.getString("SearchReplaceDialog.startattextbegin")); //$NON-NLS-1$
                jContentPane.add(jCheckBoxAbAnfang, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(10, 5, 2, 2);
                gbc.gridx = 0;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridy = 0;
                JLabel label = new JLabel();
                label.setText(Messages.getString("SearchReplaceDialog.searchtext")); //$NON-NLS-1$
                jContentPane.add(label, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 10, 5, 2);
                gbc.gridx = 0;
                gbc.gridy = 5;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 2;
                jButtonSuche = new JButton();
                jButtonSuche.setText(Messages.getString("SearchReplaceDialog.Search")); //$NON-NLS-1$
                jButtonSuche.setToolTipText(Messages.getString("SearchReplaceDialog.tooltipsearchbutton")); //$NON-NLS-1$
                jButtonSuche.setActionCommand("suche"); //$NON-NLS-1$
                jButtonSuche.addActionListener(this);
                jContentPane.add(jButtonSuche, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 5, 2);
                gbc.gridx = 2;
                gbc.gridy = 5;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridwidth = 1;
                jButtonReplace = new JButton();
                jButtonReplace.setText(Messages.getString("SearchReplaceDialog.replace")); //$NON-NLS-1$
                jButtonReplace.setToolTipText(Messages.getString("SearchReplaceDialog.replacetext")); //$NON-NLS-1$
                jButtonReplace.setActionCommand("ersetze"); //$NON-NLS-1$
                jButtonReplace.addActionListener(this);
                jContentPane.add(jButtonReplace, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 5, 10);
                gbc.gridy = 5;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridx = 3;
                jButtonCancel = new JButton();
                jButtonCancel.setText(Messages.getString("SearchReplaceDialog.cancel")); //$NON-NLS-1$
                jButtonCancel.setActionCommand("abbrechen"); //$NON-NLS-1$
                jButtonCancel.addActionListener(this);
                jContentPane.add(jButtonCancel, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 5, 2, 2);
                gbc.gridx = 0;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.gridy = 2;
                JLabel label = new JLabel();
                label.setText(Messages.getString("SearchReplaceDialog.replacetextfield")); //$NON-NLS-1$
                jContentPane.add(label, gbc);
            }
            {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 6;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.SOUTH;
                gbc.gridwidth = 4;
                jLabelInfo = new JLabel();
                jLabelInfo.setBorder(BorderFactory.createLoweredBevelBorder());
                jLabelInfo.setText(Messages.getString("SearchReplaceDialog.status")); //$NON-NLS-1$
                jLabelInfo.setToolTipText(Messages.getString("SearchReplaceDialog.5")); //$NON-NLS-1$
                jContentPane.add(jLabelInfo, gbc);
            }
        }
        return jContentPane;
    }

    private void startSearch() {
        int startPos;
        // aktuell CurosrPosition im editor merken
        aktCaret = editor.getCaretPosition();
        // ist bereits Text im editor markiert, dann diesen als
        // Suchtext vorschlagen
        if (jCheckBoxAbAnfang.isSelected()) {
            startPos = 0;
        } else {
            startPos = aktCaret;
        }
        if (jTextFieldSearch.getText().equals("")) { //$NON-NLS-1$
            jLabelInfo.setForeground(Color.red);
            jLabelInfo.setText(Messages.getString("SearchReplaceDialog.plaesentertext")); //$NON-NLS-1$
        } else {
            // "sucht" oder "sucht und ersetzt alles" im Text
            // Funktion wird umgeschaltet durch 6. Parameter
            if (searchReplaceThread != null) {
                searchReplaceThread.interrupt();
                searchReplaceThread = null;
            }
            searchReplaceThread = new SearchReplaceThread(
                editor,
                jTextFieldSearch.getText(),
                jCheckBoxCaseSensitiv.isSelected(),
                jCheckBoxWholeWords.isSelected(),
                jTextFieldReplace.getText(),
                replaceIsSelected && jCheckBoxWithoutAsk.isSelected(),
                startPos);
            searchReplaceThread.start();
        }
    }

    private void jCheckBoxWithoutAsk_stateChanged(ChangeEvent e) {
        // den Button ersetze in ersetze alle umbennen
        if (jCheckBoxWithoutAsk.isSelected()) {
            jButtonReplace.setText(Messages.getString("SearchReplaceDialog.replaceall")); //$NON-NLS-1$
        } else {
            jButtonReplace.setText(Messages.getString("SearchReplaceDialog.replace")); //$NON-NLS-1$
        }
        pack();
    }

    private void this_keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                cancel();
        }
    }

    private class SearchReplaceThread extends Thread {

        Document doc;
        JTextComponent editor;
        String searchString;
        boolean caseSensitiv;
        boolean wholeWord;
        String replaceString;
        boolean replaceAll;   // ersetzte alle Suchmuster mit Ersatzmuster !!
        int startPos;

        // Konstruktor
        SearchReplaceThread(JTextComponent editor,
            String searchString,
            boolean caseSensitiv,
            boolean wholeWord,
            String replaceString,
            boolean replaceAll,
            int startPos) {
            this.doc = editor.getDocument();
            this.editor = editor;
            this.searchString = searchString;
            this.caseSensitiv = caseSensitiv;
            this.wholeWord = wholeWord;
            this.replaceString = replaceString;
            this.replaceAll = replaceAll;
            this.startPos = startPos;
        }

        private boolean checkIsLetter(char c) {
            boolean isLetter = false;
            if (((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || (c == '_') || (c == 'ä') || (c == 'ö') || (c == 'ü') || (c == 'Ä') || (c == 'Ö') || (c == 'Ü') || ((c >= '0') && (c <= '9'))) {
                isLetter = true;
            }
            return isLetter;
        }

        private int indexOfWholeWord(String text, String searchText, int start) {
            int i = -1;
            int pos = start;
            boolean ready = false;
            if ((text.length() > 0) && (text.length() >= searchText.length())) {
                while (!ready) {
                    i = text.indexOf(searchText, pos);
                    // ist was gefunden worden ?
                    if (i != -1) {
                        // Fundort für weitere Suche merken
                        pos = i;
                        // was gefunden dann nachsehen, ob am Anfang kein Letter
                        // nur wenn am Anfang auch was zum testen vorhanden !
                        if (i > 0) {
                            if (checkIsLetter(text.charAt(i - 1))) {
                                // Suchergebnis löschen, da nicht separates Wort
                                i = -1;
                            }
                        }
                        // am Ende des Suchtextes nachsehen
                        if ((i + searchText.length()) < text.length()) {
                            if (checkIsLetter(text.charAt(i + searchText.length()))) {
                                i = -1;
                            }
                        }
                        // wenn i hier grösser als -1 dann erfolgreiche Suche
                        if (i != -1) {
                            ready = true;
                        } else {
                            // neue Startposition für Suche
                            pos++;
                            // liegt neue SuchPosition noch im Bereich
                            // und kann Suchmuster noch im textrest enthalten sein (Platz) ?
                            if (pos >= text.length()) {
                                ready = true;
                                i = -1;
                            }
                        }
                    } else {
                        // wenn Suchmuster garnicht erst auftaucht dann Ende
                        ready = true;
                    }
                }
            }
            return i;
        }

        private int indexOfWholeWord(StringBuilder text, String searchText, int start) {
            int i = -1;
            int pos = start;
            boolean ready = false;
            if ((text.length() > 0) && (text.length() >= searchText.length())) {
                while (!ready) {
                    i = text.indexOf(searchText, pos);
                    // ist was gefunden worden ?
                    if (i != -1) {
                        // Fundort für weitere Suche merken
                        pos = i;
                        // was gefunden dann nachsehen, ob am Anfang kein Letter
                        // nur wenn am Anfang auch was zum testen vorhanden !
                        if (i > 0) {
                            if (checkIsLetter(text.charAt(i - 1))) {
                                // Suchergebnis löschen, da nicht separates Wort
                                i = -1;
                            }
                        }
                        // am Ende des Suchtextes nachsehen
                        if ((i + searchText.length()) < text.length()) {
                            if (checkIsLetter(text.charAt(i + searchText.length()))) {
                                i = -1;
                            }
                        }
                        // wenn i hier grösser als -1 dann erfolgreiche Suche
                        if (i != -1) {
                            ready = true;
                        } else {
                            // neue Startposition für Suche
                            pos++;
                            // liegt neue SuchPosition noch im Bereich
                            // und kann Suchmuster noch im textrest enthalten sein (Platz) ?
                            if (pos >= text.length()) {
                                ready = true;
                                i = -1;
                            }
                        }
                    } else {
                        // wenn Suchmuster garnicht erst auftaucht dann Ende
                        ready = true;
                    }
                }
            }
            return i;
        }

        private int findNextOccurence(int start, String completeText) {
            int i = -1;
            if (caseSensitiv) {
                // wenn GROSS/klein beachten - einfache Stringsuche Möglich
                if (wholeWord) {
                    i = indexOfWholeWord(completeText, searchString, start);
                } else {
                    i = completeText.indexOf(searchString, start);
                }
            } else {
                // GROSS/klein nicht berücksichtigen
                // durchsuchenden Text und Suchtext in Kleinbuchstaben wandeln
                // einfache Realisierung, nichts für ganz grosse Dokumente!
                String lowerText = completeText;
                String lowerSuchtext = searchString;
                lowerText = lowerText.toLowerCase();
                lowerSuchtext = lowerSuchtext.toLowerCase();
                if (wholeWord) {
                    i = indexOfWholeWord(lowerText, lowerSuchtext, start);
                } else {
                    i = lowerText.indexOf(lowerSuchtext, start);
                }
            }
            return i;
        }

        private int findNextOccurence(int start, StringBuilder completeText) {
            int i = -1;
            if (caseSensitiv) {
                // wenn GROSS/klein beachten - einfache Stringsuche Möglich
                if (wholeWord) {
                    i = indexOfWholeWord(completeText, searchString, start);
                } else {
                    i = completeText.indexOf(searchString, start);
                }
            } else {
                // GROSS/klein nicht berücksichtigen
                // durchsuchenden Text und Suchtext in Kleinbuchstaben wandeln
                // einfache Realisierung, nichts für ganz grosse Dokumente!
                String lowerText = completeText.toString().toLowerCase();
                String lowerSuchtext = searchString;
                lowerSuchtext = lowerSuchtext.toLowerCase();
                if (wholeWord) {
                    i = indexOfWholeWord(lowerText, lowerSuchtext, start);
                } else {
                    i = lowerText.indexOf(lowerSuchtext, start);
                }
            }
            return i;
        }

        @Override
        public void run() {
            jLabelInfo.setForeground(Color.black);
            jLabelInfo.setText(Messages.getString("SearchReplaceDialog.searchatpos") + " " + startPos + " ... "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            // keine weiteren Aktionen im Dialog zulassen
            jButtonSuche.setEnabled(false);
            jButtonReplace.setEnabled(false);
            try {
                if (replaceAll) {
                    StringBuilder sb = new StringBuilder(editor.getText(0, doc.getLength()));
                    // hier alles ersetzen ohne nachfragen
                    int i = findNextOccurence(startPos, sb);
                    int countReplacements = 0;
                    int lastPos = 0;
                    while (i != -1) {
                        if (isInterrupted()) {
                            jButtonSuche.setEnabled(true);
                            jButtonReplace.setEnabled(true);
                            // alles loslassen, keine Aktualisierung des Documents
                            return;
                        }
                        jLabelInfo.setText(Messages.getString("SearchReplaceDialog.foundedatpos") + " " + (i + 1));
                        lastPos = i;
                        sb.replace(i, i + searchString.length(), replaceString);
                        countReplacements++;
                        startPos = i + replaceString.length();
                        i = findNextOccurence(startPos, sb);
                    }
                    editor.setText(sb.toString());
                    editor.repaint();
                    editor.setCaretPosition(lastPos);
                    jLabelInfo.setForeground(Color.black);
                    jLabelInfo.setText(jLabelInfo.getText() + countReplacements + " " + Messages.getString("SearchReplaceDialog.replacesexecuted")); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    // von Punkt zu Punkt von Hand springen
                    int i = findNextOccurence(startPos, editor.getText(0, doc.getLength()));
                    if (i == -1) {
                        jLabelInfo.setForeground(Color.blue);
                        jLabelInfo.setText(jLabelInfo.getText() + " " + Messages.getString("SearchReplaceDialog.searchtextfounded")); //$NON-NLS-1$ //$NON-NLS-2$
                    } else {
                        // zurücksetzen, da nächste Suche sicherlich nicht von Anfang
                        // gewünscht wird
                        jCheckBoxAbAnfang.setSelected(false);
                        // Meldung ausgeben über Erfolg
                        jLabelInfo.setForeground(Color.black);
                        jLabelInfo.setText(jLabelInfo.getText() + Messages.getString("SearchReplaceDialog.foundedatpos") + " " + (i + 1)); //$NON-NLS-1$
                        // Text selektieren, Cursor steht danach am Ende der Selektion
                        //                    editor.setCaretPosition(i);
                        //                    editor.moveCaretPosition(i + searchString.length());
                        editor.select(i, i + searchString.length());
                        // TextPane neuzeichnen sonst bleiben die alten
                        // Selektionen noch sichtbar
                        editor.repaint();
                    }
                }
                // nach Abschluss der Aktion Bedienung wieder zulassen
                jButtonSuche.setEnabled(true);
                jButtonReplace.setEnabled(true);
                jButtonSuche.requestFocus();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }
}
