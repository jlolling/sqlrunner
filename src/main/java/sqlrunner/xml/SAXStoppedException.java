package sqlrunner.xml;

import org.xml.sax.SAXException;

/**
 * @author lolling.jan
 * Diese Exception ist allein dafür da um einen geordnetes Ende des Parsens zu ermöglichen
 * und im aufrufenden Thread dieses Ereignis zu erkennen
 */
public class SAXStoppedException extends SAXException {

    private static final long serialVersionUID = 1L;

    public SAXStoppedException(String message) {
        super(message);
    }

}
