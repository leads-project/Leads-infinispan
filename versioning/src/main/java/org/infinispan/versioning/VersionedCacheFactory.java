package org.infinispan.versioning;

import org.apache.log4j.Logger;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.versioning.impl.VersionedCacheAtomicMapImpl;
import org.infinispan.versioning.impl.VersionedCacheAtomicTreeMapImpl;
import org.infinispan.versioning.impl.VersionedCacheHibernateImpl;
import org.infinispan.versioning.impl.VersionedCacheNaiveImpl;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;

import java.io.IOException;
import java.util.Properties;

import static java.lang.System.getProperties;

/**
 * A factory of {@link VersionedCache} instances.
 * 
 * @author valerio.schiavoni@gmail.com
 * 
 */
public class VersionedCacheFactory {
	
	private static Logger logger = Logger.getLogger(VersionedCacheFactory.class.getClass());
	private EmbeddedCacheManager cacheManager;

    public static enum VersioningTechnique {
        NAIVE,
        ATOMICMAP,
        SHARDED_TREE,
        HIBERNATE
    }


    public VersionedCacheFactory(){
		startManager();
	}
	
	/**
	 * Instantiate a {@link VersionedCache} of the specified type.
	 * 
	 * @param versioningTechnique one among the available {@link VersioningTechnique} types.
	 * @param generator the {@link VersionGenerator} to use
	 * @param cacheName the name of the cache
	 * @return an instance of {@link VersionedCache}
	 */
	public <K,V> VersionedCache<K,V> newVersionedCache(VersioningTechnique versioningTechnique, VersionGenerator generator ,String cacheName) {
		
		if( generator == null){
			throw new IllegalArgumentException("Invalid generator");
		} else if(cacheName == null) {
			throw new IllegalArgumentException("Cache");
		}
		
		switch (versioningTechnique) {
		case NAIVE: {
			return new VersionedCacheNaiveImpl<K,V>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case ATOMICMAP: {
		       return new VersionedCacheAtomicMapImpl<K,V>(cacheManager.getCache(cacheName),generator,cacheName);
		}
		case HIBERNATE: {
			 return new VersionedCacheHibernateImpl<K,V>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case SHARDED_TREE:{
			 return new VersionedCacheAtomicTreeMapImpl<K,V>(cacheManager.getCache(cacheName), generator, cacheName);
		       
		}
		default:
			logger.info("Creating default versioned cache of type "+VersionedCacheNaiveImpl.class.getCanonicalName());
			return new VersionedCacheNaiveImpl<K,V>(cacheManager.getCache(cacheName), generator, cacheName);
		}
	}
	
	/**
	 * Use the {@link VersionScalarGenerator} with the given {@link VersioningTechnique} versioned cache.
	 * Forward the call to {@link VersionedCacheFactory#newVersionedCache(VersioningTechnique, VersionGenerator, String)}.
	 * 
	 * @param versioningTechnique one among the available {@link VersioningTechnique} types.
	 * @param cacheName
	 * @return an instance of {@link VersionedCache}.
	 */
	public <K,V> VersionedCache<K,V> newVersionedCache(VersioningTechnique versioningTechnique, String cacheName){
		return newVersionedCache(versioningTechnique, new  VersionScalarGenerator(), cacheName);
	}
	
	/**
	 * Use the {@link VersionScalarGenerator} with the given {@link VersioningTechnique} versioned cache.
	 * Use the {@link VersioningTechnique#NAIVE} versioning technique.
	 * Forward the call to {@link VersionedCacheFactory#newVersionedCache(VersioningTechnique, VersionGenerator, String)}.
	 * 
	 * @param cacheName
	 * @return an instance of {@link VersionedCache}.
	 */
	public <K,V> VersionedCache<K,V> newVersionedCache( String cacheName){
		return newVersionedCache(VersioningTechnique.NAIVE, new VersionScalarGenerator(), cacheName);
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
