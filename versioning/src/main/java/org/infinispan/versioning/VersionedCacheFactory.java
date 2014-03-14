package org.infinispan.versioning;

import static java.lang.System.getProperties;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.infinispan.container.versioning.NumericVersionGenerator;
import org.infinispan.container.versioning.VersionGenerator;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.versioning.impl.VersionedCacheAtomicMapImpl;
import org.infinispan.versioning.impl.VersionedCacheNaiveImpl;

/**
 * A factory of {@link VersionedCache} instances.
 * 
 * @author valerio.schiavoni@gmail.com
 * 
 */
public class VersionedCacheFactory {
	
	Logger logger = Logger.getLogger(VersionedCacheFactory.class.getClass());
	
	private EmbeddedCacheManager cacheManager;
	
	public VersionedCacheFactory(){
		startManager();
	}
	
	
	public VersionedCache newVersionedCache(CacheType cacheType, VersionGenerator generator ,String cacheName) {
		switch (cacheType) {
		case NAIVE: {
			return new VersionedCacheNaiveImpl<>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case NAIVEPP: {
			return null;
		}
		case ATOMICMAP: {
		       return new VersionedCacheAtomicMapImpl<>(cacheManager.getCache(cacheName),generator,cacheName);
		}
		case HIBERNATE: {
			return null;
		}
		case SHARDED_TREE:{
			return null;
		}
		default:
			logger.info("Returning default versioned cache of type "+VersionedCacheNaiveImpl.class.getCanonicalName());
			return new VersionedCacheNaiveImpl<>(cacheManager.getCache(cacheName), generator, cacheName);			
		}		
	}
	
	private  void startManager(){  
        try{
            Properties properties = getProperties();
            properties.load(VersionedCacheFactory.class.getClassLoader().getResourceAsStream("config.properties"));
            logger.info("Found properties file.");
        } catch (IOException e) {
            logger.info("Found no config.properties file; defaulting.");
        }
        String infinispanConfig = getProperties().getProperty("infinispanConfigFile");
        
        if(infinispanConfig != null){
            try {
            	this.cacheManager= new DefaultCacheManager(infinispanConfig);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Incorrect Infinispan configuration file");
            }
        }
        cacheManager.start();
        logger.info("Cache manager started.");
    }
	
}
