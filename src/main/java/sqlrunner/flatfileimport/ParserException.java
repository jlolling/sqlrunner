/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sqlrunner.flatfileimport;

/**
 *
 * @author jan
 */
public class ParserException extends Exception {

	private static final long serialVersionUID = 1L;

	public ParserException(String message) {
        super(message);
    }
    
    public ParserException(Throwable t) {
        super(t);
    }

}
