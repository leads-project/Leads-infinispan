package org.infinispan.versioning;

import static java.lang.System.getProperties;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.infinispan.container.versioning.VersionGenerator;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.versioning.impl.VersionedCacheAtomicMapImpl;
import org.infinispan.versioning.impl.VersionedCacheAtomicTreeMapImpl;
import org.infinispan.versioning.impl.VersionedCacheHibernateImpl;
import org.infinispan.versioning.impl.VersionedCacheNaiveImpl;
import org.jboss.shrinkwrap.api.IllegalArchivePathException;

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
	
	/**
	 * Instantiate a {@link VersionedCache} of the specified type.
	 * 
	 * @param cacheType one among the available {@link VersioningTechnique} types.
	 * @param generator the {@link VersionGenerator} to use
	 * @param cacheName the name of the cache
	 * @return an instance of {@link VersionedCache}
	 */
	public VersionedCache newVersionedCache(VersioningTechnique cacheType, VersionGenerator generator ,String cacheName) {
		
		if( generator == null){
			throw new IllegalArchivePathException("Invalid generator");
		} else if(cacheName == null) {
			throw new IllegalArchivePathException("Cache");				
		}
		
		switch (cacheType) {
		case NAIVE: {
			return new VersionedCacheNaiveImpl<>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case ATOMICMAP: {
		       return new VersionedCacheAtomicMapImpl<>(cacheManager.getCache(cacheName),generator,cacheName);
		}
		case HIBERNATE: {
			 return new VersionedCacheHibernateImpl<>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case SHARDED_TREE:{
			 return new VersionedCacheAtomicTreeMapImpl<>(cacheManager.getCache(cacheName), generator, cacheName);
		       
		}
		default:
			logger.info("Creating default versioned cache of type "+VersionedCacheNaiveImpl.class.getCanonicalName());
			return new VersionedCacheNaiveImpl<>(cacheManager.getCache(cacheName), generator, cacheName);			
		}		
	}
	
	public  void startManager(){  
		
		if (cacheManager!=null && cacheManager.getStatus() == ComponentStatus.RUNNING) {
			logger.info("CacheMAnager already started, nothing to do here");
			return;
		}
		
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
	
	public void stopManager(){
		cacheManager.stop();
	    logger.info("Cache manager stopped.");	    	
	}
	
}
