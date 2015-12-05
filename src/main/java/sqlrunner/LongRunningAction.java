package sqlrunner;

/**
 *
 * @author jan
 */
public interface LongRunningAction {
    
    String getName();
    void cancel();
    boolean canBeCanceled();
    
}