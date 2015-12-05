package sqlrunner.flatfileimport.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import sqlrunner.flatfileimport.Importer;

public class ImportProgressPanel extends JPanel {

    private static final Logger logger = Logger.getLogger(ImportProgressPanel.class);
    private static final long serialVersionUID = 1L;
    private JLabel jLabel = null;
    private JLabel jLabel1 = null;
    private JLabel jLabel2 = null;
    private JProgressBar jProgressBarLines = null;
    private JLabel jLabel3 = null;
    private JLabel jLabel4 = null;
    private JLabel jLabel5 = null;
    private JProgressBar jProgressBarInserted = null;
    private JProgressBar jProgressBarUpdated = null;
    private JProgressBar jProgressBarIgnored = null;
    private JLabel jLabelLines = null;
    private JLabel jLabelInserted = null;
    private JLabel jLabelUpdated = null;
    private JLabel jLabelIgnored = null;
    private JLabel jLabelStartedAt = null;
    private JLabel jLabelCalculatedTimeConsumption = null;
    private JLabel jLabel7 = null;
    private JTextField jTextFieldErrorLogFileName = null;
    private JLabel jLabel8 = null;
    private JLabel jLabel9 = null;
    private JLabel jLabelLinesPerSecond = null;
    private JLabel jLabelFinishedAt = null;
    private transient Importer importer = null;  //  @jve:decl-index=0:
    private javax.swing.Timer timer = null;  //  @jve:decl-index=0:
    private static int REFRESH_TIME = 1000;
    private JLabel jLabel6 = null;
    private JTextField jTextFieldCurrentAction = null;
    private JButton jButtonCancel = null;
    private long lastCountLines = 0;

    /**
     * This is the default constructor
     */
    public ImportProgressPanel() {
        super();
        initialize();
    }

    public void setImporter(Importer importer) {
        this.importer = importer;
    }

    public Importer getImporter() {
        return importer;
    }

    public boolean isMonitorRunning() {
        return timer != null && timer.isRunning();
    }

    public void startMonitoring() {
        //logger.setLevel(Level.DEBUG);
        if (logger.isDebugEnabled()) {
            logger.debug("startMonitoring()"); //$NON-NLS-1$
        }
        reset();
        if (importer != null) {
            timer = new javax.swing.Timer(REFRESH_TIME, new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final long startTime = importer.getStartTime();
                    final long currTime = System.currentTimeMillis();
                    final long countMaximum = importer.getCountMaxInput();
                    final long countLines = importer.getCountCurrInput();
                    final long countDiff = countLines - lastCountLines;
                    lastCountLines = countLines;
                    final long countInserts = importer.getCountInserts();
                    final long countUpdates = importer.getCountUpdates();
                    final long countIgnores = importer.getCountIgnored();
                    jTextFieldCurrentAction.setText(importer.getCurrentAction());
                    final long expectedTime = calculateExpectedTimeConsumption(
                            startTime,
                            currTime,
                            countMaximum,
                            countLines);
                    jLabelCalculatedTimeConsumption.setText(getFormatedTime(expectedTime));
                    setStartTime(startTime);
                    setMaximum((int) countMaximum);
                    final long stopTime = importer.getStopTime();
                    if (startTime > 1 && stopTime == 0) {
                        jButtonCancel.setEnabled(true);
                    } else {
                        jButtonCancel.setEnabled(false);
                    }
                    if (countMaximum > 0) {
                        jProgressBarLines.setValue((int) countLines);
                        jProgressBarInserted.setValue((int) countInserts);
                        jProgressBarUpdated.setValue((int) countUpdates);
                        jProgressBarIgnored.setValue((int) countIgnores);
                    } else {
                        jProgressBarLines.setValue(0);
                        jProgressBarInserted.setValue(0);
                        jProgressBarUpdated.setValue(0);
                        jProgressBarIgnored.setValue(0);
                    }
                    jLabelLines.setText(String.valueOf(countLines) + " / " + countMaximum); //$NON-NLS-1$
                    jLabelInserted.setText(String.valueOf(countInserts));
                    jLabelUpdated.setText(String.valueOf(countUpdates));
                    jLabelIgnored.setText(String.valueOf(countIgnores));
                    jLabelLinesPerSecond.setText(String.valueOf(countDiff));
                    setStopTime(stopTime);
                    setLogFileName(importer.getLogFileName());
                    if (stopTime > 0) {
                        stopMonitoring();
                    }
                }
            });
            timer.setRepeats(true); // immer wieder
            timer.start(); // Timer starten
        }
    }

    private void setLogFileName(String name) {
        jTextFieldErrorLogFileName.setText(name);
    }

    private void stopMonitoring() {
        if (logger.isDebugEnabled()) {
            logger.debug("stopMonitoring()");
        }
        if (timer == null) {
            throw new IllegalStateException("timer is null"); //$NON-NLS-1$
        }
        timer.stop();
    }

    private static long calculateExpectedTimeConsumption(
            final long startTime,
            final long currTime,
            final long maxCountDatasets,
            final long currCountDatasets) {
        if (startTime == 0 || currTime == 0 || maxCountDatasets == 0 || currCountDatasets == 0) {
            return -1;
        }
        final long timeDiff = (currTime - startTime) / 1000;
        final long quantityLeft = maxCountDatasets - currCountDatasets;
        final long expectedTime = (quantityLeft * timeDiff) / currCountDatasets;
        return expectedTime;
    }

    private static String getFormatedTime(long time) {
        if (time < 0) {
            return Messages.getString("ImportProgressPanel.calculating"); //$NON-NLS-1$
        }
        final StringBuffer sb = new StringBuffer();
        if (time < 60) {
            // less than one minute
            sb.append(String.valueOf(time));
            sb.append('s'); //$NON-NLS-1$
        } else if (time < (60 * 60)) {
            // less than one hour
            sb.append(String.valueOf(time / 60));
            sb.append("min "); //$NON-NLS-1$
            sb.append(String.valueOf((time % 60)));
            sb.append('s'); //$NON-NLS-1$
        } else {
            sb.append(time / 3600);
            sb.append("h "); //$NON-NLS-1$
            sb.append((time / 60) % 60);
            sb.append("min"); //$NON-NLS-1$
        }
        return sb.toString();
    }

    private void setMaximum(int lineCount) {
        if (lineCount == 0) {
            lineCount = 100;
        }
        jProgressBarLines.setMaximum(lineCount);
        jProgressBarInserted.setMaximum(lineCount);
        jProgressBarUpdated.setMaximum(lineCount);
        jProgressBarIgnored.setMaximum(lineCount);
    }

    private void setStartTime(long startTime) {
        if (startTime > 0) {
            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); //$NON-NLS-1$
            jLabelStartedAt.setText(sdf.format(new Date(startTime)));
        } else {
            jLabelFinishedAt.setText(Messages.getString("ImportProgressPanel.notstarted")); //$NON-NLS-1$
        }
    }

    private void setStopTime(long stopTime) {
        if (stopTime > 0) {
            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); //$NON-NLS-1$
            jLabelFinishedAt.setText(sdf.format(new Date(stopTime)));
        } else {
            jLabelFinishedAt.setText(Messages.getString("ImportProgressPanel.notfinished")); //$NON-NLS-1$
        }
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints gb = new GridBagConstraints();
        jLabel = new JLabel();
        jLabel.setText(Messages.getString("ImportProgressPanel.startedat")); //$NON-NLS-1$
        gb.gridy = 0;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel, gb);
        jLabelStartedAt = new JLabel();
        jLabelStartedAt.setText(Messages.getString("ImportProgressPanel.notstarted")); //$NON-NLS-1$
        gb.gridy = 0;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.insets = new Insets(2, 2, 2, 2);
        gb.anchor = GridBagConstraints.WEST;
        this.add(jLabelStartedAt, gb);
        jLabel1 = new JLabel();
        jLabel1.setText(Messages.getString("ImportProgressPanel.expectedtime")); //$NON-NLS-1$
        gb.gridy = 1;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel1, gb);
        jLabelCalculatedTimeConsumption = new JLabel();
        jLabelCalculatedTimeConsumption.setText(Messages.getString("ImportProgressPanel.willbecalculated")); //$NON-NLS-1$
        gb.gridy = 1;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.anchor = GridBagConstraints.WEST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabelCalculatedTimeConsumption, gb);
        jLabel9 = new JLabel();
        jLabel9.setText(Messages.getString("ImportProgressPanel.progressPerSecond"));
        gb.gridy = 2;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel9, gb);
        jLabelLinesPerSecond = new JLabel();
        jLabel = new JLabel();
        gb.gridy = 2;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.WEST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabelLinesPerSecond, gb);
        jLabel6 = new JLabel();
        jLabel6.setText(Messages.getString("ImportProgressPanel.runningaction")); //$NON-NLS-1$
        gb.gridy = 3;
        gb.gridx = 0;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel6, gb);
        gb.gridy = 3;
        gb.gridx = 1;
        gb.gridwidth = 2;
        gb.weightx = 1.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.anchor = GridBagConstraints.CENTER;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(getJTextFieldCurrentAction(), gb);
        jLabel2 = new JLabel();
        jLabel2.setText(Messages.getString("ImportProgressPanel.lines")); //$NON-NLS-1$
        gb.gridy = 4;
        gb.gridx = 0;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel2, gb);
        gb.gridy = 4;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 1.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.anchor = GridBagConstraints.CENTER;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(getJProgressBarLines(), gb);
        jLabelLines = new JLabel();
        jLabelLines.setText("0"); //$NON-NLS-1$
        gb.gridy = 4;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.WEST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabelLines, gb);
        jLabel3 = new JLabel();
        jLabel3.setText(Messages.getString("ImportProgressPanel.inserted")); //$NON-NLS-1$
        gb.gridy = 5;
        gb.gridx = 0;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel3, gb);
        gb.gridy = 5;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 1.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(getJProgressBarInserted(), gb);
        jLabelInserted = new JLabel();
        jLabelInserted.setText("0"); //$NON-NLS-1$
        gb.gridy = 5;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.WEST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabelInserted, gb);
        jLabel4 = new JLabel();
        jLabel4.setText(Messages.getString("ImportProgressPanel.updated")); //$NON-NLS-1$
        gb.gridy = 6;
        gb.gridx = 0;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel4, gb);
        gb.gridy = 6;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 1.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.anchor = GridBagConstraints.CENTER;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(getJProgressBarUpdated(), gb);
        jLabelUpdated = new JLabel();
        jLabelUpdated.setText("0"); //$NON-NLS-1$
        gb.gridy = 6;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.WEST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabelUpdated, gb);
        jLabel5 = new JLabel();
        jLabel5.setText(Messages.getString("ImportProgressPanel.ignored")); //$NON-NLS-1$
        gb.gridy = 7;
        gb.gridx = 0;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel5, gb);
        gb.gridy = 7;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 1.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.anchor = GridBagConstraints.CENTER;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(getJProgressBarIgnored(), gb);
        jLabelIgnored = new JLabel();
        jLabelIgnored.setText("0"); //$NON-NLS-1$
        gb.gridy = 7;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.WEST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabelIgnored, gb);
        jLabel7 = new JLabel();
        jLabel7.setText(Messages.getString("ImportProgressPanel.errorlog")); //$NON-NLS-1$
        gb.gridy = 8;
        gb.gridx = 0;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.EAST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabel7, gb);
        gb.gridy = 8;
        gb.gridx = 1;
        gb.gridwidth = 2;
        gb.weightx = 1.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.anchor = GridBagConstraints.CENTER;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(getJTextFieldErrorLogFileName(), gb);
        jLabel8 = new JLabel();
        jLabel8.setText(Messages.getString("ImportProgressPanel.finishedat")); //$NON-NLS-1$
        gb.gridy = 9;
        gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.insets = new Insets(2, 2, 2, 2);
        gb.anchor = GridBagConstraints.EAST;
        this.add(jLabel8, gb);
        jLabelFinishedAt = new JLabel();
        jLabelFinishedAt.setText(Messages.getString("ImportProgressPanel.notfinished")); //$NON-NLS-1$
        gb.gridy = 9;
        gb.gridx = 2;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.HORIZONTAL;
        gb.anchor = GridBagConstraints.WEST;
        gb.insets = new Insets(2, 2, 2, 2);
        this.add(jLabelFinishedAt, gb);
		gb.gridy = 10;
		gb.gridx = 1;
        gb.gridwidth = 1;
        gb.weightx = 0.0;
        gb.fill = GridBagConstraints.NONE;
        gb.anchor = GridBagConstraints.CENTER;
		gb.insets = new Insets(2, 2, 2, 2);
        this.add(getJButtonCancel(), gb);
    }

    public void reset() {
        jTextFieldCurrentAction.setText(null);
        jLabelCalculatedTimeConsumption.setText(null);
        jLabelFinishedAt.setText(null);
        jLabelIgnored.setText("0"); //$NON-NLS-1$
        jLabelInserted.setText("0"); //$NON-NLS-1$
        jLabelUpdated.setText("0"); //$NON-NLS-1$
        jLabelLines.setText("0"); //$NON-NLS-1$
        jLabelLinesPerSecond.setText(null);
        jProgressBarIgnored.setValue(0);
        jProgressBarInserted.setValue(0);
        jProgressBarLines.setValue(0);
        jProgressBarUpdated.setValue(0);
        jProgressBarIgnored.setMaximum(100);
        jProgressBarInserted.setMaximum(100);
        jProgressBarLines.setMaximum(100);
        jProgressBarUpdated.setMaximum(100);
        jTextFieldErrorLogFileName.setText(null);
        jButtonCancel.setEnabled(false);
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    /**
     * This method initializes jProgressBarLines	
     * 	
     * @return javax.swing.JProgressBar	
     */
    private JProgressBar getJProgressBarLines() {
        if (jProgressBarLines == null) {
            jProgressBarLines = new JProgressBar();
            jProgressBarLines.setStringPainted(true);
        }
        return jProgressBarLines;
    }

    /**
     * This method initializes jProgressBarInserted	
     * 	
     * @return javax.swing.JProgressBar	
     */
    private JProgressBar getJProgressBarInserted() {
        if (jProgressBarInserted == null) {
            jProgressBarInserted = new JProgressBar();
            jProgressBarInserted.setStringPainted(true);
        }
        return jProgressBarInserted;
    }

    /**
     * This method initializes jProgressBarUpdated	
     * 	
     * @return javax.swing.JProgressBar	
     */
    private JProgressBar getJProgressBarUpdated() {
        if (jProgressBarUpdated == null) {
            jProgressBarUpdated = new JProgressBar();
            jProgressBarUpdated.setStringPainted(true);
        }
        return jProgressBarUpdated;
    }

    /**
     * This method initializes jProgressBarIgnored	
     * 	
     * @return javax.swing.JProgressBar	
     */
    private JProgressBar getJProgressBarIgnored() {
        if (jProgressBarIgnored == null) {
            jProgressBarIgnored = new JProgressBar();
            jProgressBarIgnored.setStringPainted(true);
        }
        return jProgressBarIgnored;
    }

    /**
     * This method initializes jTextFieldErrorLogFileName	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getJTextFieldErrorLogFileName() {
        if (jTextFieldErrorLogFileName == null) {
            jTextFieldErrorLogFileName = new JTextField();
            jTextFieldErrorLogFileName.setEditable(false);
            jTextFieldErrorLogFileName.setEnabled(true);
        }
        return jTextFieldErrorLogFileName;
    }

    /**
     * This method initializes jTextFieldCurrentAction	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getJTextFieldCurrentAction() {
        if (jTextFieldCurrentAction == null) {
            jTextFieldCurrentAction = new JTextField();
            jTextFieldCurrentAction.setEditable(false);
        }
        return jTextFieldCurrentAction;
    }

    /**
     * This method initializes jButtonCancel	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButtonCancel() {
        if (jButtonCancel == null) {
            jButtonCancel = new JButton();
            jButtonCancel.setText("Import Abbrechen");
            jButtonCancel.setEnabled(false);
            jButtonCancel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    importer.abort();
                }
            });
        }
        return jButtonCancel;
    }
    
}  //  @jve:decl-index=0:visual-constraint="10,10"

