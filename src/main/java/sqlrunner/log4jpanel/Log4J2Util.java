package sqlrunner.log4jpanel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public final class Log4J2Util {

    public static FileAppender createFileAppender(String fileName) throws IOException {
        final PatternLayout layout = PatternLayout.newBuilder().withPattern("%d %-5p %m%n").build();
        FileAppender appender = FileAppender.newBuilder()
        							.withFileName(fileName)
        							.withBufferSize(8000)
        							.withAppend(false)
        							.withImmediateFlush(true)
        							.setLayout(layout)
        							.setName(fileName)
        							.build();
        appender.start();
        return appender;
    }

    public static void clearAppenders(Logger logger) {
    	if (logger != null) {
        	LoggerContext loggerContext = (LoggerContext) LogManager.getContext(true);
        	Configuration configuration = loggerContext.getConfiguration();
        	LoggerConfig loggerConfig = configuration.getLoggerConfig(logger.getName());
        	loggerConfig.getAppenders().forEach((key, value) -> loggerConfig.removeAppender(value.getName()));    
    	}
    }

    public static void clearAppenders(Logger logger, String appenderName) {
    	if (logger != null) {
        	LoggerContext loggerContext = (LoggerContext) LogManager.getContext(true);
        	Configuration configuration = loggerContext.getConfiguration();
        	LoggerConfig loggerConfig = configuration.getLoggerConfig(logger.getName());
        	loggerConfig.removeAppender(appenderName);    
    	}
    }

    public static void setLogLevel(Logger logger, Level level) {
    	Configurator.setLevel(logger.getName(), level);
    }
    
    public static void addAppender(Logger logger, Appender appender) {
    	LoggerContext loggerContext = (LoggerContext) LogManager.getContext(true);
    	Configuration configuration = loggerContext.getConfiguration();
    	configuration.getLoggerConfig(logger.getName()).addAppender(appender, Level.INFO, null);
    }
    
    public static List<org.apache.logging.log4j.Logger> getAllLoggers() {
    	List<org.apache.logging.log4j.Logger> loggerList = new ArrayList<>();
        Logger rootLogger = LogManager.getRootLogger();
        LoggerContext logContext = (LoggerContext) LogManager.getContext(true);
        Collection<org.apache.logging.log4j.core.Logger> allLoggers = logContext.getLoggers();        
        for (org.apache.logging.log4j.core.Logger l :  allLoggers) {
            loggerList.add(l);
        }
        Collections.sort(loggerList, new Comparator<Logger>() {

			public int compare(Logger o1, Logger o2) {
				return o1.getName().compareTo(o2.getName());
			}
        	
		});
        loggerList.add(0, rootLogger);
    	return loggerList;
    }
    
    public static Logger getLogger(String name) {
    	Logger logger = LogManager.getLogger(name);
    	return logger;
    }

}
