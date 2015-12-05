package sqlrunner.xml;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class TableCountImportHandler extends DefaultHandler {

    private static final Logger staticLogger = Logger.getLogger(TableImportHandler.class);
    private Logger logger = staticLogger;
    private ImportDescription impDesc;
    private int countCurrDatasets;

    TableCountImportHandler(ImportDescription impDesc) {
        this.impDesc = impDesc;
    }

    public void setLogger(Logger instanceLogger) {
        if (instanceLogger == null) {
            throw new IllegalArgumentException("instanceLogger cannot be null");
        }
        this.logger = instanceLogger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void resetToStaticLogger() {
        this.logger = staticLogger;
    }

    @Override
    public void startDocument() throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new SAXStoppedException("stopped");
        }
        countCurrDatasets = 0;
        if (logger.isDebugEnabled()) {
            logger.debug("startDocument() - import file " + (impDesc.getXmlFile()).getAbsolutePath());
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (logger.isDebugEnabled()) {
            logger.debug("endDocument() - ..." + countCurrDatasets);
        }
    }

    @Override
    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName)
            throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new SAXStoppedException("stopped");
        }
        if (qName.equals("row")) {
            countCurrDatasets++;
        }
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        logger.error("xml error in line=" + e.getLineNumber() + " column=" + e.getColumnNumber());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        logger.error("xml fatalError in line=" + e.getLineNumber() + " column=" + e.getColumnNumber());
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        logger.warn("xml warning in line=" + e.getLineNumber() + " column=" + e.getColumnNumber());
    }

    public int getCountCurrDatasets() {
        return countCurrDatasets;
    }
    
}
