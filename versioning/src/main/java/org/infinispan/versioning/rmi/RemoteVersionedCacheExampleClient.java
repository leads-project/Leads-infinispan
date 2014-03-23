package org.infinispan.versioning.rmi;

import org.infinispan.versioning.utils.IncrediblePropertyLoader;
import org.infinispan.versioning.utils.version.Version;

import java.rmi.Naming;
import java.util.Properties;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */

public class RemoteVersionedCacheExampleClient {


    void run(String serviceURL) {
        RemoteVersionedCache<String,String> cache;
        String key = "Albert";
        String value = "Einstein";
        String key2 = "Marie";
        String value2 = "Curie";

        try {
            cache = (RemoteVersionedCache<String,String>) Naming.lookup(serviceURL);
            cache.put(key, value);
            assert value == cache.get(key);
            System.out.println("v equals get(put(k,v)).");
            Version v1 = cache.getLatestVersion(key);
            cache.put(key, "Second");
            Version v2 = cache.getLatestVersion(key);
            cache.put(key, "Third");
            Version v3 = cache.getLatestVersion(key);
            assert v2.compareTo(v1) > 0;
            assert v2.compareTo(v3) < 0;
            System.out.println("Versions increase.");
            cache.put(key2, value2);
            assert value2 == cache.get(key2);
            System.out.println("v2 equals get(put(k2,v2)).");
            assert cache.get(key, v1) == value;
            System.out.println("First version still exists.");
        } catch (Exception e) {
            System.out.println("RemoteVersionedCacheExampleClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        RemoteVersionedCacheExampleClient client = new RemoteVersionedCacheExampleClient();
        Properties prop = new Properties();
        IncrediblePropertyLoader.load(prop, "rvc-rmi-client.properties");
        String servers = prop.getProperty("servers");
        for (String server : servers.split(";")) {
            String serviceURL = "//" + server + "/" + RemoteVersionedCacheImpl.SERVICE_NAME;
            System.out.println("Connecting to " + serviceURL + " ...");
            client.run(serviceURL);
        }
    }
}
