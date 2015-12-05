package sqlrunner;

import java.io.File;
import java.util.List;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import com.apple.eawt.SystemSleepListener;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.AppReOpenedEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.AppEvent.SystemSleepEvent;

public class MacOSXAdapter implements MacOSXAdapterInterface {

	private static final Logger logger = Logger.getLogger(MacOSXAdapter.class);
	
    public void setup() {
		if (logger.isDebugEnabled()) {
			logger.debug("setup()");
		}
    	try {
        	final com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
        	application.setAboutHandler(new AboutHandler() {
				
				public void handleAbout(AboutEvent ae) {
					if (Main.startupFinished()) {
						final MainFrame mf = Main.getActiveMainFrame();
						if (mf != null) {
							mf.openAboutDialog();
						}
					}
				}
			});
        	application.addAppEventListener(new SystemSleepListener() {
				
				public void systemAwoke(SystemSleepEvent e) {

				}
				
				public void systemAboutToSleep(SystemSleepEvent e) {
					logger.info("System is about to sleep, start closing all connections");
					Main.disconnect();
				}

        	});
        	application.addAppEventListener(new AppReOpenedListener() {
				
				public void appReOpened(AppReOpenedEvent e) {
					if (Main.startupFinished()) {
						final MainFrame currActiveMainFrame = Main.getActiveMainFrame();
						currActiveMainFrame.setState(JFrame.NORMAL);
						currActiveMainFrame.toFront();
					}
				}
			});
        	application.setPreferencesHandler(new PreferencesHandler() {
				
				public void handlePreferences(PreferencesEvent ae) {
					final MainFrame mf = Main.getActiveMainFrame();
					if (Main.startupFinished() && mf != null) {
						mf.openPreferencesDialog();
					}
				}
			});
        	application.setOpenFileHandler(new OpenFilesHandler() {
				
				public void openFiles(OpenFilesEvent e) {
					List<File> listFiles = e.getFiles();
					handleOpenFiles(listFiles);
				}

                private void handleOpenFiles(List<File> listFiles) {
                	boolean firstFile = true;
					for (File file : listFiles) {
						logger.info("handleOpenFile: " + file);
						if (Main.startupFinished()) {
							logger.debug("handleOpenFile call handleFile: " + file);
                            final MainFrame currActiveMainFrame = Main.getActiveMainFrame();
                            if (file.getName().toLowerCase().endsWith(".sql")) {
                            	logger.debug("create a new window");
                                final MainFrame newMainFrame = Main.createInstance(currActiveMainFrame);
                            	logger.debug("call handle file: " + file);
                                newMainFrame.handleFile(file);
                            } else {
                                currActiveMainFrame.handleFile(file);
                            }
						} else {
							logger.debug("handleOpenFile set startup file: " + file);
							if (firstFile) {
								firstFile = false;
								Main.setStartupFile(file.getAbsolutePath());
							}
						}
					}
                }
                
        	});
        	application.setQuitHandler(new QuitHandler() {
				
				public void handleQuitRequestWith(QuitEvent e, QuitResponse resp) {
					if (Main.startupFinished()) {
						if (Main.canShutdown()) {
							Main.shutdown();
							resp.performQuit();
						} else {
							resp.cancelQuit();
						}
					}
					
				}
			});
    	} catch (Throwable e) {
    		logger.warn("setup failed: " + e.getMessage(), e);
    	}
    }

}
