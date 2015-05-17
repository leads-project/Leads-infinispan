package org.infinispan.atomic.filter;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.object.*;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.AbstractCacheEventFilterConverter;
import org.infinispan.notifications.cachelistener.filter.CacheAware;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinispan.atomic.object.Reference.unreference;
import static org.infinispan.atomic.object.Utils.marshall;
import static org.infinispan.atomic.object.Utils.unmarshall;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class ObjectFilterConverter extends AbstractCacheEventFilterConverter<Object,Object,Object>
      implements CacheAware<Object,Object>, Externalizable{

   // Class fields & methods
   private static Log log = LogFactory.getLog(ObjectFilterConverter.class);
   private static ObjectFilterConverter instance;
   public static synchronized ObjectFilterConverter getInstance() {
      if (instance == null)
         instance = new ObjectFilterConverter();
      return instance;
   }
   
   // Object fields
   private Cache<Object,Object> cache;   
   private ConcurrentMap<Object,Object> objects;
   private ConcurrentMap<Object,CallClose> pendingCloseCalls;
   private ConcurrentHashMap<Object,AtomicInteger> openCallsCounters;

   public ObjectFilterConverter(){
      this.openCallsCounters = new ConcurrentHashMap<>();
      this.objects = new ConcurrentHashMap<>();
      this.pendingCloseCalls = new ConcurrentHashMap<>();
   }

   @Override
   public void setCache(Cache <Object,Object> cache) {
      this.cache = cache;
   }

   @Override
   public Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {

      assert (cache!=null);

      try {

         Call call = (Call) newValue;

         CallFuture future = new CallFuture(call.getCallID());

         if (call instanceof CallInvoke) {
            
            CallInvoke invocation = (CallInvoke) call;
            Object responseValue = 
                  Utils.callObject(
                        objects.get(key), 
                        invocation.method, 
                        unreference(
                              invocation.arguments,
                              AtomicObjectFactory.forCache(cache)));
            future.set(responseValue);
            if (log.isTraceEnabled()) 
               log.trace(this+"- Called " + invocation+" (="+(responseValue==null ? "null" : responseValue.toString())+")");

         } else if (call instanceof CallPersist) {

            if (log.isTraceEnabled()) log.trace(this + "- Retrieved CallPersist ["+key+"]");
            
            assert (pendingCloseCalls.get(key)!=null);
            future = new CallFuture(pendingCloseCalls.get(key).getCallID());
            future.set(null);
            pendingCloseCalls.remove(key);
            if (openCallsCounters.get(key).get()==0) {
               pendingCloseCalls.remove(key);
               openCallsCounters.remove(key);
               objects.remove(key);
            }

         } else if (call instanceof CallOpen) {

            if (!openCallsCounters.containsKey(key))
               openCallsCounters.put(key,new AtomicInteger(0));
            
            openCallsCounters.get(key).incrementAndGet();
            
            CallOpen callOpen = (CallOpen) call;

            if (callOpen.getForceNew()) {

               if (log.isTraceEnabled())
                  log.trace(this + "- Forcing new object [" + key + "]");
               objects.put(
                     key, 
                     Utils.initObject(
                           callOpen.getClazz(), 
                           unreference(
                                 callOpen.getInitArgs(),
                                 AtomicObjectFactory.forCache(cache))));

            }else if (objects.get(key)==null) {

               if (oldValue == null) {

                  if (log.isTraceEnabled()) 
                     log.trace(this + "- Creating new object ["+key+"]");
                  objects.put(
                        key, 
                        Utils.initObject(
                              callOpen.getClazz(),
                              unreference(
                                    callOpen.getInitArgs(),
                                    AtomicObjectFactory.forCache(cache))));

               } else {

                  if (oldValue instanceof CallPersist) {

                     if (log.isTraceEnabled())
                        log.trace(this + "- Retrieving object from persistent state ["+key+"]");
                     objects.put(key,unmarshall(((CallPersist) oldValue).getBytes()));

                  } else {

                     throw new IllegalStateException("Cannot rebuild object ["+key+"]");

                  }

               }
               
            }

            future.set(null);

         } else if (call instanceof CallClose) {

            openCallsCounters.get(key).decrementAndGet();

            if (openCallsCounters.get(key).get()==0 && pendingCloseCalls.get(key)==null) {

               assert (objects.get(key)!=null);
               if (log.isTraceEnabled()) 
                  log.trace(this + "- Persisting object ["+key+"]");
               pendingCloseCalls.put(key,(CallClose)call);
               cache.putAsync(
                     key,
                     new CallPersist(call.getListenerID(), marshall(objects.get(key))));

            } else {

               future.set(null);
               
            }

         }

         if (log.isTraceEnabled()) 
            log.trace(this + "- Future (" + future.getCallID() +", "+key+") -> "+future.isDone());

         if (future.isDone())
            return future;

      } catch (Exception e) {
         e.printStackTrace();
      }

      return null;

   }

   @Override
   public String toString(){
      return "ObjectFilterConverter";
   }

   @Override
   public synchronized void writeExternal(ObjectOutput objectOutput) throws IOException {
      // nothing to do
   }

   @Override
   public synchronized void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      // nothing to do
   }

   public Object readResolve(){
      return getInstance();
   }

}
