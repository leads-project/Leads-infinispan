package org.infinispan.atomic.container;

import javassist.util.proxy.MethodFilter;
import org.infinispan.atomic.object.Call;
import org.infinispan.atomic.object.CallFuture;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * Uninstalling a listener is not necessary, as when all clients disconnect, 
 * it is automatically removed. 
 * 
 * @author Pierre Sutra
 */
public abstract class AbstractContainer {

   // class fields
   public static final int TTIMEOUT_TIME = 5000;
   protected static final Map<UUID, CallFuture> registeredCalls = new ConcurrentHashMap<>();
   protected static final Log log = LogFactory.getLog(BaseContainer.class);
   protected static final MethodFilter methodFilter = new MethodFilter() {
      @Override
      public boolean isHandled(Method m) {
         // ignore finalize() and externalization related methods.
         return !m.getName().equals("finalize")
               && !m.getName().equals("readExternal")
               && !m.getName().equals("writeExternal");
      }
   };


   protected BasicCache cache;
   protected Class clazz;
   protected Object key;
   protected boolean readOptimization;
   protected List<String> methods;
   protected Object proxy;
   protected boolean forceNew;
   protected Object[] initArgs;

   public AbstractContainer(
         final BasicCache cache,
         final Class clazz,
         final Object key,
         final boolean readOptimization,
         final boolean forceNew,
         final List<String> methods,
         final Object... initArgs){
      this.cache = cache;
      this.clazz = clazz;
      this.key = clazz.getCanonicalName()+"#"+key.toString(); // to avoid collisions
      this.readOptimization = readOptimization;
      this.methods = methods;
      this.forceNew = forceNew;
      this.initArgs = initArgs;
   }

   public final Object getProxy(){
      return proxy;
   }

   public final Class getClazz(){
      return clazz;
   }

   public abstract void open()
         throws InterruptedException, ExecutionException, TimeoutException, IOException;

   public abstract void close()
         throws InterruptedException, ExecutionException, TimeoutException, IOException;

   public abstract UUID listenerID();

   protected Object execute(Call call)
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (log.isDebugEnabled()) log.debug(this + "Executing " + call);

      CallFuture future = new CallFuture(call.getCallID());
      registeredCalls.put(call.getCallID(), future);

      cache.put(key, call);

      Object ret = future.get(TTIMEOUT_TIME, TimeUnit.MILLISECONDS);
      registeredCalls.remove(call.getCallID());
      if(!future.isDone()){
         throw new org.infinispan.util.concurrent.TimeoutException("Unable to execute "+call);
      }

      if (log.isDebugEnabled()) log.debug(this + "Returning " + ret);
      return ret;

   }


   protected static void handleFuture(CallFuture future){
      try {
         assert (future.isDone());
         if (!registeredCalls.containsKey(future.getCallID())) {
            log.warn("Future " + future.getCallID() + " trashed");
            return; // duplicate ?
         }
         CallFuture clientFuture = registeredCalls.get(future.getCallID());
         assert (clientFuture!=null);
         registeredCalls.remove(future.getCallID());
         clientFuture.set(future.get());
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

}
