package org.infinispan.ensemble.test;

import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.indexing.LocalIndexBuilder;
import org.infinispan.query.remote.client.avro.AvroMarshaller;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;


/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public abstract class EnsembleAbstractTest<K,T> extends MultipleSitesAbstractTest {

    protected EnsembleCacheManager manager;
    protected abstract int numberOfSites();
    protected abstract Class<? extends T> valueClass();
    protected abstract Class<? extends K> keyClass();
    protected abstract EnsembleCache<K,T> cache();

    @Override
    protected void createCacheManagers() throws Throwable {
        super.createCacheManagers();
        manager = new EnsembleCacheManager(sites(),new AvroMarshaller<>(valueClass()),new LocalIndexBuilder());
    }

    @AfterMethod(alwaysRun = true)
    protected void clearContent() throws Throwable {
        cache().clear();
    }

    @AfterClass(alwaysRun = true)
    @Override
    public void destroy(){
        super.destroy();
        manager.stop();
    }

}
