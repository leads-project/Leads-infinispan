package org.infinispan.versioning;

import org.apache.log4j.BasicConfigurator;
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
	
	private Logger logger;
	private EmbeddedCacheManager cacheManager;

    public static enum VersioningTechnique {
        NAIVE,
        ATOMICMAP,
        SHARDED_TREE,
        HIBERNATE
    }

    void configLog() {
        String log4jConfigFile = getConfig("log4jConfigFile");
        if (log4jConfigFile == null)
            BasicConfigurator.configure();
        logger = Logger.getLogger(VersionedCacheFactory.class.getClass());
    }

    public VersionedCacheFactory(){
        configLog();
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
			logger.info("CacheManager already started, nothing to do here");
			return;
		}

        String infinispanConfig = getConfig("infinispanConfigFile");
        if (infinispanConfig != null) {
            try {
            	this.cacheManager= new DefaultCacheManager(infinispanConfig);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("File " + infinispanConfig + " is corrupted.");
            }
        }

        if (this.cacheManager == null) {
            this.cacheManager = new DefaultCacheManager();
            logger.info("Using DefaultCacheManager with no configuration.");
        }

        cacheManager.start();
        logger.info("Cache manager started.");
    }

    private String getConfig(String s) {
        Properties properties = getProperties();
        ClassLoader cl = VersionedCacheFactory.class.getClassLoader();
        String configProperties = "config.properties";

        if (cl.getResource(configProperties) != null) {
            try{
                properties.load(cl.getResourceAsStream(configProperties));
                logger.info("Found correct " + configProperties + " file.");
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("File " + configProperties + " is corrupted.");
            }
        }

        return getProperties().getProperty(s);
    }

    public void stopManager(){
		cacheManager.stop();
	    logger.info("Cache manager stopped.");	    	
	}

}
