package org.infinispan.atomic.container.local;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.container.BaseContainer;
import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.atomic.object.CallFuture;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Pierre Sutra
 */
public class LocalContainer extends BaseContainer {

   private static Map<BasicCache,Listener> listeners = new ConcurrentHashMap<>();
   private static synchronized UUID installListener(BasicCache cache){
      if (!listeners.containsKey(cache)) {
         Listener listener = new Listener();
         FilterConverterFactory factory = new FilterConverterFactory();
         ((AdvancedCache) cache).addListener(
               listener,
               factory.getFilterConverter(new Object[] { listener.getId() }),
               null);
         log.info("Local listener "+listener.getId()+" installed");
         listeners.put(cache, listener);
      }
      return listeners.get(cache).getId();
   }
   
   private UUID listenerID;
      
   public LocalContainer(BasicCache c, Class cl, Object k,
         boolean readOptimization, boolean forceNew, List<String> methods,
         Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException,
         ExecutionException, NoSuchMethodException, InvocationTargetException, TimeoutException {
      super(c, cl, k, readOptimization, forceNew, methods, initArgs);
      listenerID = installListener(cache);
   }

   @Override 
   public UUID listenerID() {
      return listenerID;
   }

   @org.infinispan.notifications.Listener(sync = true, clustered = true)
   private static class Listener{
      
      private UUID id;
      
      public Listener(){
         id = UUID.randomUUID();
      }
      
      public UUID getId(){
         return id;
      }
      
      @Deprecated
      @CacheEntryModified
      @CacheEntryCreated
      public void onCacheModification(CacheEntryEvent event){
         log.trace(this + "Event " + event.getType()+" received");
         CallFuture ret = (CallFuture) event.getValue();
         handleFuture(ret);
      }
      
      
   }
   
}
