package org.infinispan.atomic.container;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.atomic.object.CallClose;
import org.infinispan.atomic.object.CallInvoke;
import org.infinispan.atomic.object.CallOpen;
import org.infinispan.commons.api.BasicCache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pierre Sutra
  */
public abstract class BaseContainer extends AbstractContainer {
   
   // object's fields
   private AtomicInteger pendingCalls;
   private boolean isOpen;

   public BaseContainer(final BasicCache c, final Class cl, final Object k, final boolean readOptimization,
         final boolean forceNew, final List<String> methods, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException,
         java.util.concurrent.TimeoutException {

      super(c, cl, k, readOptimization, forceNew, methods, initArgs);      
      pendingCalls = new AtomicInteger();
      isOpen = false;

      // build the proxy
      MethodHandler handler = new MethodHandler() {

         public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {
            
            open();
            
            Object ret = execute(
                  new CallInvoke(
                        listenerID(),
                        m.getName(),
                        args)
            );
            
            pendingCalls.decrementAndGet();
            
            return ret;
            
         }
         
         @Override
         public String toString(){
            return "MethodHandler ["+key+"]";
         }
         
      };

      ProxyFactory fact = new ProxyFactory();
      fact.setSuperclass(clazz);
      fact.setFilter(methodFilter);
      proxy = fact.createClass().newInstance();
      ((ProxyObject)proxy).setHandler(handler);
      
      if (log.isDebugEnabled()) log.debug(this+"Created successfully");

   }
   
   @Override
   public synchronized void open() 
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      pendingCalls.incrementAndGet();
      
      if (!isOpen) {

         if (log.isDebugEnabled()) log.debug(this + "Opening.");
         
         execute(new CallOpen(listenerID(), forceNew, clazz, initArgs));
         isOpen = true;

         if (log.isDebugEnabled()) log.debug(this+  "Opened.");
      }      
      
   }

   @Override
   public synchronized void close()
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      if (log.isDebugEnabled()) log.debug(this + "Closing.");

      while(pendingCalls.get()!=0);

      if (isOpen) {

         isOpen = false;
         execute(new CallClose(listenerID()));
         forceNew = false;

      }

      if (log.isDebugEnabled()) log.debug(this + "Closed.");

   }

   @Override
   public String toString(){
      return "Container["+listenerID()+":"+key+"]";
   }

}
