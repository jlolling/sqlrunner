package dbtools;

/**
 * @author jan
 * This Exception will be thrown if a instance will use in a not proper way inside a pool.
 */
public class HandlingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HandlingException(String message) {
        super(message);
    }

}
