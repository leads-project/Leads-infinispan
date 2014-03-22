package org.infinispan.versioning.rmi;

import org.infinispan.versioning.utils.version.Version;

import java.rmi.Naming;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */

public class RemoteVersionedCacheClient {


    void run(String serviceURL) {
        RemoteVersionedCache<String,String> cache;

        try {
            cache = (RemoteVersionedCache<String,String>) Naming.lookup(serviceURL);
            String key = "tata";
            String value = "titi";
            cache.put(key, value);
            assert value == cache.get(key);
            System.out.println("get(put(k,v)) == v.");
            Version v1 = cache.getLatestVersion(key);
            cache.put(key, "second");
            Version v2 = cache.getLatestVersion(key);
            cache.put(key, "third");
            Version v3 = cache.getLatestVersion(key);
            assert v2.compareTo(v1) > 0;
            assert v2.compareTo(v3) < 0;
            System.out.println("Versions increase.");
            assert cache.get(key, v1) == value;
            System.out.println("First version still exists.");
        } catch (Exception e) {
            System.out.println("RemoteVersionedCacheClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        String serviceURL = "//localhost/" + RemoteVersionedCacheImpl.SERVICE_NAME;
        new RemoteVersionedCacheClient().run(serviceURL);
    }
}
