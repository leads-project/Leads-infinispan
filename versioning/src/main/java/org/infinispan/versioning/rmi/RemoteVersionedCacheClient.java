package org.infinispan.versioning.rmi;

import org.infinispan.versioning.utils.version.Version;

import java.rmi.Naming;

/**
 * Created by pasin on 21/03/14.
 */
public class RemoteVersionedCacheClient {

    public static void main(String args[]) {
        RemoteVersionedCache<String,String> cache;
        String serviceURL = "//localhost/RemoteVersionedCacheServer";

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
}
