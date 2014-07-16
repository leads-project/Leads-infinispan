package org.infinispan.versioning;

import org.hibernate.search.cfg.SearchMapping;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.versioning.impl.VersionedCacheHibernateImpl;
import org.infinispan.versioning.utils.hibernate.HibernateProxy;
import org.infinispan.versioning.utils.version.VersionGenerator;
import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.util.Properties;

/**
 * @author marcelo pasin, pierre sutra
 * @since 7.0
 */

@Test(groups = "functional", testName = "VersionedCacheHibernateTest")
public class VersionedCacheHibernateTest extends VersionedCacheAbstractTest {

    @Override
    protected void setBuilder(ConfigurationBuilder builder) {
        builder.persistence().passivation(true);
        SearchMapping mapping = new SearchMapping();
        mapping.entity(HibernateProxy.class).indexed().providedId()
                .property("k", ElementType.METHOD).field()
                .property("v", ElementType.METHOD).field()
                .property("version", ElementType.METHOD).field();

        Properties properties = new Properties();
        properties.put(org.hibernate.search.Environment.MODEL_MAPPING, mapping);
        properties.put("default.directory_provider", "ram");
        builder.indexing()
                .enable()
                .indexLocalOnly(true)
                .withProperties(properties);
    }

    @Override
    protected <K, V> VersionedCache<K, V> getCache(Cache cache, VersionGenerator generator, String name) {
        return new VersionedCacheHibernateImpl<K,V>(cache,generator,name);
    }

}
