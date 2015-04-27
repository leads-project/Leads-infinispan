package org.infinispan.atomic.container.local;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.container.BaseContainer;
import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.atomic.object.CallFuture;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.infinispan.atomic.object.Utils.unmarshall;

/**
 * @author Pierre Sutra
 */
@Listener(sync = true, clustered = true)
public class LocalContainer extends BaseContainer {

   public LocalContainer(BasicCache c, Class cl, Object k,
         boolean readOptimization, boolean forceNew, List<String> methods,
         Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException,
         ExecutionException, NoSuchMethodException, InvocationTargetException, TimeoutException {
      super(c, cl, k, readOptimization, forceNew, methods, initArgs);
   }

   @Deprecated
   @CacheEntryModified
   @CacheEntryCreated
   public void onCacheModification(CacheEntryEvent event){
      log.trace(this + "Event " + event.getType()+" received");
      CallFuture ret = (CallFuture) unmarshall(event.getValue());
      handleFuture(ret);
   }

   @Override 
   protected void removeListener() {
      log.debug(this + "Removing listener");
      ((AdvancedCache)cache).removeListener(this);
      log.debug(this + "Listener removed");
   }

   @Override 
   protected void installListener() {
      log.debug(this + "Installing listener ");
      Object[] params = new Object[] { listenerID, key, clazz, forceNew, initArgs };
      FilterConverterFactory factory = new FilterConverterFactory();
      ((AdvancedCache) cache).addListener(
            this,
            factory.getFilter(params),null);
      log.debug(this + "Listener installed");
   }
}
