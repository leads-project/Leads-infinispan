package org.infinispan.versioning;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.versioning.impl.VersionedCacheAtomicMapImpl;
import org.infinispan.versioning.impl.VersionedCacheAtomicShardedTreeMapImpl;
import org.infinispan.versioning.impl.VersionedCacheAtomicTreeMapImpl;
import org.infinispan.versioning.impl.VersionedCacheDummyImpl;
import org.infinispan.versioning.impl.VersionedCacheNaiveImpl;
import org.infinispan.versioning.utils.IncrediblePropertyLoader;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.infinispan.versioning.utils.version.VersionScalarGenerator;
import org.jboss.logging.Logger;

import java.io.IOException;

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
    	DUMMY,
        NAIVE,
        ATOMICMAP,
        TREEMAP,
        SHARDED_TREE
    }

    public VersionedCacheFactory(){
        IncrediblePropertyLoader.load(System.getProperties());
        logger = Logger.getLogger(this.getClass());
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
		case DUMMY: {
			return new VersionedCacheDummyImpl<K,V>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case NAIVE: {
			return new VersionedCacheNaiveImpl<K,V>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case ATOMICMAP: {
		       return new VersionedCacheAtomicMapImpl<K,V>(cacheManager.getCache(cacheName),generator,cacheName);
		}
		case TREEMAP:{
			 return new VersionedCacheAtomicTreeMapImpl<K,V>(cacheManager.getCache(cacheName), generator, cacheName);
		}
		case SHARDED_TREE:{
            return new VersionedCacheAtomicShardedTreeMapImpl<K, V>(cacheManager.getCache(cacheName), generator, cacheName);
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
	
	public  void startManager() {

		if (cacheManager != null && cacheManager.getStatus() == ComponentStatus.RUNNING) {
			logger.info("CacheManager already started, nothing to do here");
			return;
		}

        String infinispanConfig = System.getProperties().getProperty("infinispanConfigFile");
        if (infinispanConfig != null) {
            try {
            	this.cacheManager= new DefaultCacheManager(infinispanConfig);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("File " + infinispanConfig + " is corrupted.");
            }
        }

        if (this.cacheManager == null) {
        	GlobalConfiguration globalConfig = new GlobalConfigurationBuilder()
        	  .transport().defaultTransport()
        	  .build();        	
        	ConfigurationBuilder cb = new ConfigurationBuilder();
        	Configuration c = cb.        						
        						transaction().transactionMode(TransactionMode.TRANSACTIONAL).
        						clustering().cacheMode(CacheMode.REPL_SYNC).
        						build();
            this.cacheManager = new DefaultCacheManager(globalConfig, c);
            logger.warn("Using DefaultCacheManager with no configuration.");            
        }

        cacheManager.start();
        logger.info("Cache manager started.");
    }

    public void stopManager(){
		cacheManager.stop();
	    logger.info("Cache manager stopped.");	    	
	}

}
