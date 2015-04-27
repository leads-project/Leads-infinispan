package org.infinispan.atomic.container.remote;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.container.BaseContainer;
import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.atomic.object.CallFuture;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.infinispan.atomic.object.Utils.unmarshall;

/**
 * @author Pierre Sutra
 */

@ClientListener(
      filterFactoryName = FilterConverterFactory.FACTORY_NAME,
      converterFactoryName = FilterConverterFactory.FACTORY_NAME)
public class RemoteContainer extends BaseContainer {

   private static Log log = LogFactory.getLog(RemoteContainer.class);

   public RemoteContainer(BasicCache c, Class cl, Object k,
         boolean readOptimization, boolean forceNew, List<String> methods,
         Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException,
         ExecutionException, NoSuchMethodException, InvocationTargetException, TimeoutException {
      super(c, cl, k, readOptimization, forceNew, methods, initArgs);
   }

   @Deprecated
   @ClientCacheEntryModified
   @ClientCacheEntryCreated
   public void onCacheModification(ClientCacheEntryCustomEvent event){
      CallFuture future = (CallFuture) unmarshall(event.getEventData());
      handleFuture(future);
   }

   @Override
   protected void removeListener(){
      log.debug(this + "Removing listener");
      ((RemoteCacheImpl) cache).removeClientListener(this);
      log.debug(this + "Listener removed");
   }

   @Override
   protected void installListener(){
      log.debug(this + "Installing listener ");

      Object[] params = new Object[] { listenerID, key, clazz, forceNew, initArgs };

      if (cache instanceof RemoteCacheImpl) {

         ((RemoteCacheImpl)cache).addClientListener(
               this,
               params,
               new Object[] { listenerID });

      }else {

         FilterConverterFactory factory = new FilterConverterFactory();
         ((AdvancedCache) cache).addListener(
               this,
               factory.getFilter(params),null);

      }

      log.debug(this + "Listener installed");
   }



}
