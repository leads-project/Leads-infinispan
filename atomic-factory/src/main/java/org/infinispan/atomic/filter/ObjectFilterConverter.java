package org.infinispan.atomic.filter;

import org.infinispan.Cache;
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
import java.util.*;

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
   private Map<Object,Object> objects;
   private Map<Object,CallClose> pendingCloseCalls;
   private int openCallsCounter;

   public ObjectFilterConverter(){
      this.openCallsCounter = 0;
      this.objects = new HashMap<>();
      this.pendingCloseCalls = new HashMap<>();
   }

   @Override
   public void setCache(Cache <Object,Object> cache) {
      this.cache = cache;
   }

   @Override
   public synchronized Object filterAndConvert(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
         Metadata newMetadata, EventType eventType) {

      assert (cache!=null);

      try {

         Call call = (Call) newValue;

         CallFuture future = new CallFuture(call.getCallID());

         if (call instanceof CallInvoke) {
            
            CallInvoke invocation = (CallInvoke) call;
            Object responseValue = Utils.callObject(objects.get(key), invocation.method, invocation.arguments);
            future.set(responseValue);
            if (log.isDebugEnabled()) 
               log.debug(this+"- Called " + invocation+" (="+(responseValue==null ? "null" : responseValue.toString())+")");

         } else if (call instanceof CallPersist) {

            if (log.isDebugEnabled()) log.debug(this + "- Retrieved CallPersist ["+key+"]");
            
            assert (pendingCloseCalls.get(key)!=null);
            future = new CallFuture(pendingCloseCalls.get(key).getCallID());
            future.set(null);
            pendingCloseCalls.remove(key);
            if (openCallsCounter==0)
               objects.remove(key);

         } else if (call instanceof CallOpen) {

            openCallsCounter++;
            CallOpen callOpen = (CallOpen) call;

            if (callOpen.getForceNew()) {

               if (log.isDebugEnabled())
                  log.debug(this + "- Forcing new object [" + key + "]");
               objects.put(key, Utils.initObject(callOpen.getClazz(), callOpen.getInitArgs()));

            }else if (objects.get(key)==null) {

               if (oldValue == null) {

                  if (log.isDebugEnabled()) log.debug(this + "- Creating new object ["+key+"]");
                  objects.put(key, Utils.initObject(callOpen.getClazz(), callOpen.getInitArgs()));

               } else {

                  if (oldValue instanceof CallPersist) {

                     if (log.isDebugEnabled())
                        log.debug(this + "- Retrieving object from persistent state ["+key+"]");
                     objects.put(key,unmarshall(((CallPersist) oldValue).getBytes()));

                  } else {

                     throw new IllegalStateException("Cannot rebuild object ["+key+"]");

                  }

               }
               
            }

            future.set(null);

         } else if (call instanceof CallClose) {

            openCallsCounter--;

            if (openCallsCounter==0 && pendingCloseCalls.get(key)==null) {

               assert (objects.get(key)!=null);
               if (log.isDebugEnabled()) log.debug(this + "- Persisting object ["+key+"]");
               pendingCloseCalls.put(key,(CallClose)call);
               cache.putAsync(
                     key,
                     new CallPersist(call.getListenerID(), marshall(objects.get(key))));

            } else {

               future.set(null);
               
            }

         }

         if (log.isDebugEnabled()) log.debug(this + "- Future (" + future.getCallID() +", "+key+") -> "+future.isDone());

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
