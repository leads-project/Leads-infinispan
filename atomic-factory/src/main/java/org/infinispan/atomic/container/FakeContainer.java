package org.infinispan.atomic.container;

import org.infinispan.atomic.object.Utils;
import org.infinispan.commons.api.BasicCache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Pierre Sutra
 */
public class FakeContainer extends AbstractContainer {
   
   private static ConcurrentMap<Object,Object> objects = new ConcurrentHashMap<>();

   public FakeContainer(BasicCache cache, Class clazz,
         Object key, boolean readOptimization, boolean forceNew,
         List<String> methods, Object... initArgs) {
      super(cache, clazz, key, readOptimization, forceNew, methods, initArgs);
      
      try {
         Object o = Utils.initObject(this.clazz, this.initArgs);
         objects.putIfAbsent(this.key,o);
         proxy = objects.get(this.key);
      } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
         e.printStackTrace(); 
      } 
      
   }

   @Override 
   public void open() throws InterruptedException, ExecutionException, TimeoutException, IOException {
      // nothing to do
   }

   @Override 
   public void close(boolean keepPersistent)
         throws InterruptedException, ExecutionException, TimeoutException, IOException {
      // nothing to do
   }

   @Override 
   public void dispose() {
      // nothing to do
   }
}
