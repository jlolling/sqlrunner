package dbtools;

/**
 *
 * @author jan
 */
public interface DatabaseSessionPoolInfoMBean {

    public boolean getHasConfigurations();

    public String getErrorMessage();

    public String getLastPoolCheckMessage();

    public String[] getAllAliases();

    public String[] getSessionStatus();

    public void stopCheckPoolThread();

    public void setPoolCheckCyclusTime(int secondsBetweenChecks);

    public int getPoolCheckCyclusTime();

    public boolean getPoolCheckThreadIsRunning();

    public void startPoolCheckThread();

    public int getPoolSize();
}
