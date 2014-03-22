package org.infinispan.versioning.utils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */
public class IncredibleLoggerFactory {
    public static Logger getLogger(String logName) {
        String log4jConfigFile = System.getProperties().getProperty("log4jConfigFile");
        if (log4jConfigFile == null)
            BasicConfigurator.configure();
        return Logger.getLogger(logName);
    }
}
