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
    /**
     * Measure insertion time, 1 key, many versions.
     * @param serviceURL
     */
    void run_t1(String serviceURL){
    	 try {
    		 
    		final int VERSIONS=10000;
			RemoteVersionedCache<String,String> cache = (RemoteVersionedCache<String,String>) Naming.lookup(serviceURL);
			
			long now = System.currentTimeMillis();
			
			for (int i = 0; i <VERSIONS ; i++) {
				cache.put("key1",new Integer(i).toString());
			}
			
			long insertionTime = System.currentTimeMillis() - now;
			System.out.println(VERSIONS+" versions inserted in:"+ insertionTime);
			//cache.clear(); //to avoid next tests to have some effects			
			
		}catch (Exception e) {
            System.out.println("RemoteVersionedCacheExampleClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Measure get time, 1 key, many versions, earliest, latest time.
     * @param serviceURL
     */
    void run_t2(String serviceURL){
   	 try {
   		 
   		 	final int VERSIONS=10000;
			RemoteVersionedCache<String,String> cache = (RemoteVersionedCache<String,String>) Naming.lookup(serviceURL);
			for (int i = 0; i <VERSIONS ; i++) {
				cache.put("key1",new Integer(i).toString());
			}
			long now = System.nanoTime();
			cache.getLatestVersion("key1");
			long insertionTime = System.nanoTime() - now;
			System.out.println("Latest version got in:"+ insertionTime);
			
			now = System.nanoTime();
			cache.getEarliestVersion("key1");
			insertionTime = System.nanoTime() - now;
			System.out.println("Earlist version got in:"+ insertionTime);
			
			//cache.clear(); //to avoid next tests to have some effects			
			
		}catch (Exception e) {
           System.out.println("RemoteVersionedCacheExampleClient exception: " + e.getMessage());
           e.printStackTrace();
       }
   }

    /**
     * Measure get time, 1 key, get all versions.
     * @param serviceURL
     */
    void run_t3(String serviceURL){
   	 try {
   		 	final String key="key1";
   		 	final int VERSIONS=100000;
			RemoteVersionedCache<String,String> cache = (RemoteVersionedCache<String,String>) Naming.lookup(serviceURL);
			long start=System.currentTimeMillis();
			for (int i = 0; i <VERSIONS ; i++) {
				cache.put(key,new Integer(i).toString());
				if (i%100==0) {
					long t1=System.currentTimeMillis();
					System.out.println("Inserted so far:"+i+ " versions. 100 versions in "+(t1-start)+" ms");
					start=t1;					
				}
			}
			
			Version earliest=cache.getEarliestVersion(key);
			Version latest = cache.getLatestVersion(key);
			
			for (int i = 0; i < 1000; i++) {
				long now = System.nanoTime();
				cache.get(key, earliest, latest);
				long time = System.nanoTime() - now;
				System.out.println( time );			
			}
			
					
			//cache.clear(); //to avoid next tests to have some effects			
			
		}catch (Exception e) {
           System.out.println("RemoteVersionedCacheExampleClient exception: " + e.getMessage());
           e.printStackTrace();
       }
   }

    

    public static void main(String args[]) {
        RemoteVersionedCacheExampleClient client = new RemoteVersionedCacheExampleClient();
        Properties sysProps = System.getProperties();
        IncrediblePropertyLoader.load(sysProps, "rvc-rmi-client.properties");
        String servers = sysProps.getProperty("servers");
        String versioningTechnique = sysProps.getProperty("versioningTechnique", "ATOMICMAP");

        for (String server : servers.split(";")) {
            String serviceURL = "//" + server + "/" + RemoteVersionedCacheImpl.SERVICE_NAME + "-" + versioningTechnique;
            System.out.println("Connecting to " + serviceURL + " ...");
            client.run_t3(serviceURL);
        }
    }
}
