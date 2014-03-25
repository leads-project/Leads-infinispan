package org.infinispan.versioning.rmi;


import org.infinispan.versioning.VersionedCache;
import org.infinispan.versioning.VersionedCacheFactory;
import org.infinispan.versioning.utils.IncrediblePropertyLoader;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */

public class RemoteVersionedCacheServer {

    private void run() {
        Properties sysProps = System.getProperties();
        IncrediblePropertyLoader.load(sysProps);
        Logger logger = Logger.getLogger(this.getClass());

        String overridePort = sysProps.getProperty("rmiRegistryPort", "1099");
        int port = Integer.valueOf(overridePort);

        String versioningTechnique = sysProps.getProperty("versioningTechnique", "ATOMICMAP");
        VersionedCacheFactory.VersioningTechnique tech = VersionedCacheFactory.VersioningTechnique.valueOf(versioningTechnique);
        logger.info("Versioning implementation used: " + tech.toString());

        try {
            VersionedCacheFactory fac = new VersionedCacheFactory();
            VersionedCache<String,String> vCache = fac.newVersionedCache(tech, new VersionScalarGenerator(), "default");
            RemoteVersionedCache<String,String> service = new RemoteVersionedCacheImpl<String, String>(vCache);

            String serviceName = "//" + InetAddress.getLocalHost().getHostAddress().toString() + ":" + port + "/" + RemoteVersionedCacheImpl.SERVICE_NAME + "-" + versioningTechnique;
            LocateRegistry.createRegistry(port);
            Naming.rebind(serviceName, service);
            logger.info("Bound in registry: " + serviceName);

//            Runtime.getRuntime().addShutdownHook(new UnbindServiceHook("RemoteVersionedCacheServer"));
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
            e.printStackTrace();
        }
        logger.info("Running forever, send interrupt to stop it properly.");
    }

    public static void main(String args[]) {
        new RemoteVersionedCacheServer().run();
    }
}
