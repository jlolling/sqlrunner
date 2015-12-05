/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dbtools;

/**
 *
 * @author jan
 */
public class DatabaseSessionPoolInfo implements DatabaseSessionPoolInfoMBean {

	@Override
    public boolean getHasConfigurations() {
        return DatabaseSessionPool.hasConfigurations();
    }

	@Override
    public String getErrorMessage() {
        return DatabaseSessionPool.getErrorMessage();
    }

	@Override
    public String getLastPoolCheckMessage() {
        return DatabaseSessionPool.getLastPoolCheckMessage();
    }

	@Override
    public String[] getAllAliases() {
        return DatabaseSessionPool.getAllAliases();
    }

	@Override
    public void stopCheckPoolThread() {
        DatabaseSessionPool.stopCheckPoolThread();
    }

	@Override
    public void setPoolCheckCyclusTime(int secondsBetweenChecks) {
        DatabaseSessionPool.setCheckPoolCyclusTime(secondsBetweenChecks);
    }
    
    @Override
    public int getPoolCheckCyclusTime() {
        return DatabaseSessionPool.getCheckPoolCyclusTime();
    }

    @Override
    public void startPoolCheckThread() {
        DatabaseSessionPool.startCheckPoolThread();
    }
    
    @Override
    public int getPoolSize() {
        return DatabaseSessionPool.getPoolSize();
    }

    @Override
    public boolean getPoolCheckThreadIsRunning() {
        return DatabaseSessionPool.isPoolCheckThreadRunning();
    }
    
    @Override
    public String[] getSessionStatus() {
        return DatabaseSessionPool.getSessionsStatus();
    }
}
