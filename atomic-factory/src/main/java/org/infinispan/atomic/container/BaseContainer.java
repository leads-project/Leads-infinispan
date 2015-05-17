package org.infinispan.atomic.container;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.object.CallClose;
import org.infinispan.atomic.object.CallInvoke;
import org.infinispan.atomic.object.CallOpen;
import org.infinispan.atomic.object.Reference;
import org.infinispan.commons.api.BasicCache;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinispan.atomic.object.Reference.unreference;
import static org.infinispan.atomic.object.Utils.initObject;

/**
 * @author Pierre Sutra
  */
public abstract class BaseContainer extends AbstractContainer {
   
   // object's fields
   private AtomicInteger pendingCalls;
   private boolean isOpen;
   
   public BaseContainer(final BasicCache c, Reference reference, final boolean readOptimization,
         final boolean forceNew, final List<String> methods, final Object... initArgs)
         throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
         InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException,
         java.util.concurrent.TimeoutException {

      super(c, reference, readOptimization, forceNew, methods, initArgs);
      pendingCalls = new AtomicInteger();
      isOpen = false;

      // build the proxy
      MethodHandler handler = new MyMethodHandler();
      ProxyFactory fact = new ProxyFactory();
      fact.setSuperclass(getClazz());
      fact.setFilter(methodFilter);
      fact.setInterfaces(new Class[]{WriteReplace.class});
      fact.setUseWriteReplace(false);
      proxy = initObject(fact.createClass(), initArgs);
      ((ProxyObject)proxy).setHandler(handler);

      if (log.isDebugEnabled()) log.debug(this+"Created successfully");

   }
   
   @Override
   public synchronized void open() 
         throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

      pendingCalls.incrementAndGet();
      
      if (!isOpen) {

         if (log.isDebugEnabled()) log.debug(this + "Opening.");
         
         execute(new CallOpen(listenerID(), forceNew, getClazz(), initArgs));
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
      return "Container["+listenerID()+":"+getKey()+"]";
   }

   private class MyMethodHandler implements MethodHandler, Serializable{

      public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {

         if (m.getName().equals("equals") && args[0] == proxy)
            return true;

         if (m.getName().equals("writeReplace")) {
            return reference;
         }
         
         open();

         Object ret = execute(
               new CallInvoke(
                     listenerID(),
                     m.getName(),
                     args)
         );

         pendingCalls.decrementAndGet();

         return (ret instanceof Reference) ? unreference((Reference)ret, AtomicObjectFactory.forCache(cache)) : ret;

      }

      @Override
      public String toString(){
         return "MethodHandler ["+getKey()+"]";
      }

   }
   
   public static interface WriteReplace {
      public Object writeReplace() throws java.io.ObjectStreamException;
   }
   
}
