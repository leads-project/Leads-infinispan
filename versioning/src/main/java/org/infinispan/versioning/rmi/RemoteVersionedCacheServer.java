package org.infinispan.versioning.rmi;


import org.infinispan.versioning.VersionedCache;
import org.infinispan.versioning.VersionedCacheFactory;
import org.infinispan.versioning.utils.IncrediblePropertyLoader;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;
import org.jboss.logging.Logger;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */

public class RemoteVersionedCacheServer {

    private Logger logger;

/* MP: This doesn't seem to work
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
*/

    private void run() {
        Properties props = System.getProperties();
        IncrediblePropertyLoader.load(props, "config.properties");
        props.setProperty("log4j.configuration", "versioning-log4j.xml");
        logger = Logger.getLogger(this.getClass());
        int port = 1099;
        String overridePort = props.getProperty("rmiRegistryPort");
        if (overridePort != null)
            port = Integer.valueOf(overridePort);

        try {
            LocateRegistry.createRegistry(port);
            VersionGenerator generator = new VersionScalarGenerator();
            String versioningTechnique = props.getProperty("versioningTechnique");
            VersionedCacheFactory.VersioningTechnique tech = (versioningTechnique != null) ?
                    VersionedCacheFactory.VersioningTechnique.valueOf(versioningTechnique) :
                    VersionedCacheFactory.VersioningTechnique.TREEMAP;
            logger.info("RemoteVersionedCacheServer using " + tech.toString());

            VersionedCacheFactory fac = new VersionedCacheFactory();
            VersionedCache<String,String> vCache = fac.newVersionedCache(tech, generator, "default");
            RemoteVersionedCache<String,String> service = new RemoteVersionedCacheImpl<String, String>(vCache);

            String serviceName = "//" + InetAddress.getLocalHost().getHostAddress().toString() + ":" + port +
                    "/" + RemoteVersionedCacheImpl.SERVICE_NAME;

            Naming.rebind(serviceName, service);
            logger.info("RemoteVersionedCacheServer bound in registry: " + serviceName);

//            Runtime.getRuntime().addShutdownHook(new UnbindServiceHook("RemoteVersionedCacheServer"));
        } catch (Exception e) {
            logger.error("RemoteVersionedCacheServer error: " + e.getMessage());
            e.printStackTrace();
        }
        logger.info("RemoteVersionedCacheServer running forever. Send interrupt to stop it properly.");
    }

    public static void main(String args[]) {
        new RemoteVersionedCacheServer().run();
    }
}
