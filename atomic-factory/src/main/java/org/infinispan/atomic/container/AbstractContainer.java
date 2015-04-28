package org.infinispan.atomic.container;

import org.infinispan.commons.api.BasicCache;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Pierre Sutra
 */
public abstract class AbstractContainer {

   protected BasicCache cache;
   protected Class clazz;
   protected Object key;
   protected boolean readOptimization;
   protected List<String> methods;
   protected Object proxy;
   protected boolean forceNew;
   protected Object[] initArgs;
   protected UUID listenerID;

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
      listenerID = UUID.randomUUID();
   }

   public final Object getProxy(){
      return proxy;
   }

   public final Class getClazz(){
      return clazz;
   }

   public abstract void open()
         throws InterruptedException, ExecutionException, TimeoutException, IOException;

   public abstract void close(boolean keepPersistent)
         throws InterruptedException, ExecutionException, TimeoutException, IOException;
   
   public abstract void dispose();
}
