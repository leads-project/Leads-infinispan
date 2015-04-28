package org.infinispan.atomic.container;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.atomic.object.*;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.util.concurrent.TimeoutException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jgroups.util.Util.assertNotNull;
import static org.jgroups.util.Util.assertTrue;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public abstract class BaseContainer extends AbstractContainer {

   protected static Log log = LogFactory.getLog(BaseContainer.class);
   public static final int TTIMEOUT_TIME = 5000;
   protected static final MethodFilter methodFilter = new MethodFilter() {
      @Override
      public boolean isHandled(Method m) {
         // ignore finalize() and externalization related methods.
         return !m.getName().equals("finalize")
               && !m.getName().equals("readExternal")
               && !m.getName().equals("writeExternal");
      }
   };

   private Map<UUID, CallFuture> registeredCalls;
   private AtomicInteger pendingCalls;
   private boolean isOpen;
   private boolean isDisposed;
   
   public BaseContainer(final BasicCache c, final Class cl, final Object k, final boolean readOptimization,
         final boolean forceNew, final List<String> methods, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException,
         java.util.concurrent.TimeoutException {

      super(c, cl, k, readOptimization, forceNew, methods, initArgs);
      
      registeredCalls = new ConcurrentHashMap<>();
      pendingCalls = new AtomicInteger();
      isOpen = false;
      isDisposed = false;

      // build the proxy
      MethodHandler handler = new MethodHandler() {

         public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {
            
            open();
            
            Object ret = execute(
                  new CallInvoke(
                        listenerID,
                        m.getName(),
                        args)
            );
            
            pendingCalls.decrementAndGet();
            
            if (isDisposed) close(true);
            
            return ret;
            
         }
         
         @Override
         public String toString(){
            return "MethodHandler ["+listenerID.toString()+"]";
         }
         
      };

      ProxyFactory fact = new ProxyFactory();
      fact.setSuperclass(clazz);
      fact.setFilter(methodFilter);
      proxy = fact.createClass().newInstance();
      ((ProxyObject)proxy).setHandler(handler);
      
      log.debug(this+"Created successfully");

   }
   
   @Override
   public synchronized void open() 
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      pendingCalls.incrementAndGet();
      
      if (!isOpen) {
   
         log.debug(this + "Opening.");
   
         installListener();
         execute(new CallOpen(listenerID));
         isOpen = true;
   
         log.debug(this+  "Opened.");
      }      
      
   }

   @Override
   public synchronized void close(boolean keepPersistent)
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      log.debug(this + "Closing.");

      while(pendingCalls.get()!=0);

      if (isOpen) {

         isOpen = false;
         isDisposed = true;
         if (keepPersistent) {
            execute(new CallClose(listenerID));
            forceNew = false;
         }
         removeListener();

      }
         
      log.debug(this + "Closed.");

   }
   
   @Override
   public void dispose(){
      isDisposed = true;
      log.debug(this + "Disposed");
   }

   @Override
   public String toString(){
      return "Container["+listenerID.toString()+"]";
   }

   protected abstract void removeListener();

   protected abstract void installListener();
   
   protected Object execute(Call call)
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
      
      log.debug(this + "Executing " + call);
      
      CallFuture future = new CallFuture(call.getCallID());
      registeredCalls.put(call.getCallID(), future);
      cache.put(key, call);
      
      Object ret = future.get(TTIMEOUT_TIME,TimeUnit.MILLISECONDS);
      registeredCalls.remove(call.getCallID());
      if(!future.isDone()){
         throw new TimeoutException("Unable to execute "+call);
      }
      
      log.debug(this + "Returning " + ret);
      return ret;
      
   }

   protected void handleFuture(CallFuture future){
      try {
         assertTrue(future.isDone());
         if (!registeredCalls.containsKey(future.getCallID())) {
            log.debug(this + "Future " + future.getCallID() + " trashed");
            return; // duplicate
         }
         CallFuture clientFuture = registeredCalls.get(future.getCallID());
         assertNotNull(clientFuture);
         registeredCalls.remove(future.getCallID());
         clientFuture.set(future.get());
      } catch (Exception e) {
         e.printStackTrace();
      }

   }


}
