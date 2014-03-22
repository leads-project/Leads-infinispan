package org.infinispan.versioning.rmi;


import org.infinispan.versioning.VersionedCache;
import org.infinispan.versioning.VersionedCacheFactory;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Created by pasin on 21/03/14.
 */
public class RemoteVersionedCacheServer {

    static class UnbindServiceHook extends Thread {
        String name;
        UnbindServiceHook(String name) {
            this.name = name;
        }
        public void run() {
            System.out.println("Cleaning up...");
            try {
                Naming.unbind(name);
            } catch (Exception e) {
                System.out.println("UnbindServiceHook error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        try {
            LocateRegistry.createRegistry(1099);
            VersionedCacheFactory fac = new VersionedCacheFactory();
            VersionGenerator generator = new VersionScalarGenerator();
            VersionedCache<String,String> vCache = fac.newVersionedCache(VersionedCacheFactory.VersioningTechnique.TREEMAP, generator, "default");
            RemoteVersionedCache<String,String> service = new RemoteVersionedCacheImpl<String, String>(vCache);

            Naming.rebind("RemoteVersionedCacheServer", service);

            System.out.println("RemoteVersionedCacheServer bound in registry");
            Runtime.getRuntime().addShutdownHook(new UnbindServiceHook("TestRemoteCache"));
        } catch (Exception e) {
            System.out.println("RemoteVersionedCacheServer error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("RemoteVersionedCacheServer running forever. Hit Ctrl-C to exit.");
    }
}
