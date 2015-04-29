package org.infinispan.atomic;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;

/**
* // TODO: Document this
*
* @author otrack
* @since 4.0
*/
@ClientListener(filterFactoryName = "static-converter", converterFactoryName = "static-converter")
public class Simplelistener {

   public Simplelistener() {

   }

   @ClientCacheEntryCreated
   @ClientCacheEntryModified
   @ClientCacheEntryRemoved
   public void handleCustomEvent(ClientCacheEntryCustomEvent e) {
      System.out.println(e.getEventData());
   }

}
