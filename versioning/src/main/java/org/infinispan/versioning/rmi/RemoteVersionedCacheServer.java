package org.infinispan.versioning.rmi;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.infinispan.versioning.VersionedCache;
import org.infinispan.versioning.VersionedCacheFactory;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

import static java.lang.System.getProperties;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */

public class RemoteVersionedCacheServer {

    private Logger logger;

    class UnbindServiceHook extends Thread {
        String name;
        UnbindServiceHook(String name) {
            this.name = name;
        }
        public void run() {
            logger.info("Cleaning up ...");
            try {
                Naming.unbind(name);
            } catch (Exception e) {
                logger.error("UnbindServiceHook error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // TODO: generalise getProperty() and configLog() in separate class

    private String getProperty(String s) {
        Properties properties = getProperties();
        String configProperties="src/main/resources/config.properties"; // TODO: fix this path
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(configProperties);

        if (is != null) {
            try{
                properties.load(is);
                logger.info("Found correct " + configProperties + " file.");
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("File " + configProperties + " is corrupted.");
            }
        }

        return getProperties().getProperty(s);
    }

    void configLog() {
        String log4jConfigFile = getProperty("log4jConfigFile");
        if (log4jConfigFile == null)
            BasicConfigurator.configure();
        logger = Logger.getLogger(this.getClass());
    }

    private void run() {
        configLog();
        try {
            LocateRegistry.createRegistry(1099);
            VersionedCacheFactory fac = new VersionedCacheFactory();
            VersionGenerator generator = new VersionScalarGenerator();
            VersionedCache<String,String> vCache = fac.newVersionedCache(VersionedCacheFactory.VersioningTechnique.TREEMAP, generator, "default");
            RemoteVersionedCache<String,String> service = new RemoteVersionedCacheImpl<String, String>(vCache);

            Naming.rebind("RemoteVersionedCacheServer", service);
            logger.info("RemoteVersionedCacheServer bound in registry.");
            Runtime.getRuntime().addShutdownHook(new UnbindServiceHook("TestRemoteCache"));
        } catch (Exception e) {
            logger.error("RemoteVersionedCacheServer error: " + e.getMessage());
            e.printStackTrace();
        }
        logger.info("RemoteVersionedCacheServer running forever. Send interrupt the stop it properly.");
    }

    public static void main(String args[]) {
        new RemoteVersionedCacheServer().run();
    }
}
