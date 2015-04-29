package org.infinispan.atomic;

import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.EventType;

/**
* // TODO: Document this
*
* @author otrack
* @since 4.0
*/ // Serializable, Externalizable or marshallable with Infinispan Externalizers
// needed when running in a cluster
public class StaticCacheEventConverter extends AbstractCacheEventFilterConverter {

   @Override public Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {
      return new ValueAddedEvent(12,"POUET");
   }
}
