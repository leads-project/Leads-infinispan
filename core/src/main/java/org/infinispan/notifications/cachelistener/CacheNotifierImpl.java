package org.infinispan.notifications.cachelistener;

import org.infinispan.Cache;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commons.CacheListenerException;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.InfinispanCollections;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.container.InternalEntryFactory;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.distexec.DistributedExecutionCompletionService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.filter.KeyFilter;
import org.infinispan.interceptors.locking.ClusteringDependentLogic;
import org.infinispan.iteration.impl.EntryRetriever;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.*;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.cluster.ClusterCacheNotifier;
import org.infinispan.notifications.cachelistener.cluster.ClusterListenerRemoveCallable;
import org.infinispan.notifications.cachelistener.cluster.ClusterListenerReplicateCallable;
import org.infinispan.notifications.cachelistener.cluster.RemoteClusterListener;
import org.infinispan.notifications.cachelistener.event.*;
import org.infinispan.notifications.cachelistener.event.impl.EventImpl;
import org.infinispan.notifications.cachelistener.filter.*;
import org.infinispan.notifications.impl.AbstractListenerImpl;
import org.infinispan.notifications.impl.ListenerInvocation;
import org.infinispan.partionhandling.AvailabilityMode;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheTopology;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.infinispan.commons.util.InfinispanCollections.transformCollectionToMap;
import static org.infinispan.notifications.cachelistener.event.Event.Type.*;

/**
 * Helper class that handles all notifications to registered listeners.
 *
 * @author Manik Surtani (manik AT infinispan DOT org)
 * @author Mircea.Markus@jboss.com
 * @author William Burns
 * @since 4.0
 */
public final class CacheNotifierImpl<K, V> extends AbstractListenerImpl<Event<K, V>, CacheEntryListenerInvocation<K, V>>
      implements ClusterCacheNotifier<K, V> {

   private static final Log log = LogFactory.getLog(CacheNotifierImpl.class);

   private static final Map<Class<? extends Annotation>, Class<?>> allowedListeners = new HashMap<Class<? extends Annotation>, Class<?>>(16);
   private static final Map<Class<? extends Annotation>, Class<?>> clusterAllowedListeners =
         new HashMap<Class<? extends Annotation>, Class<?>>();

   static {
      allowedListeners.put(CacheEntryCreated.class, CacheEntryCreatedEvent.class);
      allowedListeners.put(CacheEntryRemoved.class, CacheEntryRemovedEvent.class);
      allowedListeners.put(CacheEntryVisited.class, CacheEntryVisitedEvent.class);
      allowedListeners.put(CacheEntryModified.class, CacheEntryModifiedEvent.class);
      allowedListeners.put(CacheEntryActivated.class, CacheEntryActivatedEvent.class);
      allowedListeners.put(CacheEntryPassivated.class, CacheEntryPassivatedEvent.class);
      allowedListeners.put(CacheEntryLoaded.class, CacheEntryLoadedEvent.class);
      allowedListeners.put(CacheEntriesEvicted.class, CacheEntriesEvictedEvent.class);
      allowedListeners.put(TransactionRegistered.class, TransactionRegisteredEvent.class);
      allowedListeners.put(TransactionCompleted.class, TransactionCompletedEvent.class);
      allowedListeners.put(CacheEntryInvalidated.class, CacheEntryInvalidatedEvent.class);
      allowedListeners.put(DataRehashed.class, DataRehashedEvent.class);
      allowedListeners.put(TopologyChanged.class, TopologyChangedEvent.class);
      allowedListeners.put(PartitionStatusChanged.class, PartitionStatusChangedEvent.class);

      // For backward compat
      allowedListeners.put(CacheEntryEvicted.class, CacheEntryEvictedEvent.class);
   }

   static {
      clusterAllowedListeners.put(CacheEntryCreated.class, CacheEntryCreatedEvent.class);
      clusterAllowedListeners.put(CacheEntryModified.class, CacheEntryModifiedEvent.class);
      clusterAllowedListeners.put(CacheEntryRemoved.class, CacheEntryRemovedEvent.class);
   }

   final List<CacheEntryListenerInvocation<K, V>> cacheEntryCreatedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryRemovedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryVisitedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryModifiedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryActivatedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryPassivatedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryLoadedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryInvalidatedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> cacheEntriesEvictedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> transactionRegisteredListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> transactionCompletedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> dataRehashedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> topologyChangedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();
   final List<CacheEntryListenerInvocation<K, V>> partitionChangedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();

   // For backward compat
   final List<CacheEntryListenerInvocation<K, V>> cacheEntryEvictedListeners = new CopyOnWriteArrayList<CacheEntryListenerInvocation<K, V>>();

   private Cache<K, V> cache;
   private ClusteringDependentLogic clusteringDependentLogic;
   private TransactionManager transactionManager;
   private DistributedExecutorService distExecutorService;
   private Configuration config;
   private DistributionManager distributionManager;
   private EntryRetriever<K, V> entryRetriever;
   private InternalEntryFactory entryFactory;

   private final Map<Object, UUID> clusterListenerIDs = new ConcurrentHashMap<Object, UUID>();

   /**
    * This map is used to store the handler used when a listener is registered which has includeCurrentState and
    * is only used for that listener during the initial state transfer
    */
   private final ConcurrentMap<UUID, QueueingSegmentListener<K, V, ? extends Event<K, V>>> segmentHandler;


   public CacheNotifierImpl() {
      this(new ConcurrentHashMap<UUID, QueueingSegmentListener<K, V, ? extends Event<K, V>>>());
   }

   CacheNotifierImpl(ConcurrentMap<UUID, QueueingSegmentListener<K, V, ? extends Event<K, V>>> handler) {
      segmentHandler = handler;

      listenersMap.put(CacheEntryCreated.class, cacheEntryCreatedListeners);
      listenersMap.put(CacheEntryRemoved.class, cacheEntryRemovedListeners);
      listenersMap.put(CacheEntryVisited.class, cacheEntryVisitedListeners);
      listenersMap.put(CacheEntryModified.class, cacheEntryModifiedListeners);
      listenersMap.put(CacheEntryActivated.class, cacheEntryActivatedListeners);
      listenersMap.put(CacheEntryPassivated.class, cacheEntryPassivatedListeners);
      listenersMap.put(CacheEntryLoaded.class, cacheEntryLoadedListeners);
      listenersMap.put(CacheEntriesEvicted.class, cacheEntriesEvictedListeners);
      listenersMap.put(TransactionRegistered.class, transactionRegisteredListeners);
      listenersMap.put(TransactionCompleted.class, transactionCompletedListeners);
      listenersMap.put(CacheEntryInvalidated.class, cacheEntryInvalidatedListeners);
      listenersMap.put(DataRehashed.class, dataRehashedListeners);
      listenersMap.put(TopologyChanged.class, topologyChangedListeners);
      listenersMap.put(PartitionStatusChanged.class, partitionChangedListeners);

      // For backward compat
      listenersMap.put(CacheEntryEvicted.class, cacheEntryEvictedListeners);
   }

   @Inject
   void injectDependencies(Cache<K, V> cache, ClusteringDependentLogic clusteringDependentLogic,
                           TransactionManager transactionManager, Configuration config,
                           DistributionManager distributionManager, EntryRetriever<K ,V> entryRetriever,
                           InternalEntryFactory entryFactory) {
      this.cache = cache;
      this.clusteringDependentLogic = clusteringDependentLogic;
      this.transactionManager = transactionManager;
      this.config = config;
      this.distributionManager = distributionManager;
      this.entryRetriever = entryRetriever;
      this.entryFactory = entryFactory;
   }

   @Override
   public void start() {
      super.start();
      this.distExecutorService = SecurityActions.getDefaultExecutorService(cache);
   }

   @Override
   protected Log getLog() {
      return log;
   }

   @Override
   protected Map<Class<? extends Annotation>, Class<?>> getAllowedMethodAnnotations(Listener l) {
      if (l.clustered()) {
         // Cluster listeners only allow a subset of types
         return clusterAllowedListeners;
      }
      return allowedListeners;
   }

   @Override
   protected final Transaction suspendIfNeeded() {
      if (transactionManager == null) {
         return null;
      }

      try {
         switch (transactionManager.getStatus()) {
            case Status.STATUS_ACTIVE:
            case Status.STATUS_NO_TRANSACTION:
               return null;
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_PREPARED:
            case Status.STATUS_COMMITTED:
            case Status.STATUS_ROLLEDBACK:
            case Status.STATUS_UNKNOWN:
            case Status.STATUS_PREPARING:
            case Status.STATUS_COMMITTING:
            case Status.STATUS_ROLLING_BACK:
            default:
               //suspend in default and in unknown status to be safer
               return transactionManager.suspend();
         }
      } catch (Exception e) {
         if (log.isTraceEnabled()) {
            log.trace("An error occurred while trying to suspend a transaction.", e);
         }
         return null;
      }
   }

   @Override
   protected final void resumeIfNeeded(Transaction transaction) {
      if (transaction == null || transactionManager == null) {
         return;
      }
      try {
         transactionManager.resume(transaction);
      } catch (Exception e) {
         if (log.isTraceEnabled()) {
            log.tracef(e, "An error occurred while trying to resume a suspended transaction. tx=%s", transaction);
         }
      }
   }

   @Override
   public void notifyCacheEntryCreated(K key, V value, boolean pre,
         InvocationContext ctx, FlagAffectedCommand command) {
      if (!cacheEntryCreatedListeners.isEmpty()) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_CREATED);
         configureEvent(e, key, value, pre, ctx, command, null, null);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryCreatedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   @Override
   public void notifyCacheEntryModified(K key, V value, V previousValue, Metadata previousMetadata, boolean pre, InvocationContext ctx,
         FlagAffectedCommand command) {
      if (!cacheEntryModifiedListeners.isEmpty()) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_MODIFIED);
         configureEvent(e, key, value, pre, ctx, command, previousValue, previousMetadata);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryModifiedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   @Override
   public void notifyCacheEntryRemoved(K key, V previousValue, Metadata previousMetadata, boolean pre,
                                       InvocationContext ctx, FlagAffectedCommand command) {
      if (isNotificationAllowed(command, cacheEntryRemovedListeners)) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_REMOVED);
         configureEvent(e, key, null, pre, ctx, command, previousValue, previousMetadata);
         setTx(ctx, e);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryRemovedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   private void configureEvent(EventImpl<K, V> e, K key, V value, boolean pre, InvocationContext ctx,
                               FlagAffectedCommand command, V previousValue, Metadata previousMetadata) {
      boolean originLocal = ctx.isOriginLocal();

      e.setOriginLocal(originLocal);
      e.setValue(pre ? previousValue : value);
      e.setPre(pre);
      e.setOldValue(previousValue);
      e.setOldMetadata(previousMetadata);
      CacheEntry entry = ctx.lookupEntry(key);
      if (entry != null) {
         e.setMetadata(entry.getMetadata());
      }
      Set<Flag> flags;
      if (command != null && (flags = command.getFlags()) != null && flags.contains(Flag.COMMAND_RETRY)) {
         e.setCommandRetried(true);
      }
      e.setKey(key);
      setTx(ctx, e);
   }

   @Override
   public void notifyCacheEntryVisited(K key, V value, boolean pre, InvocationContext ctx, FlagAffectedCommand command) {
      if (isNotificationAllowed(command, cacheEntryVisitedListeners)) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_VISITED);
         e.setPre(pre);
         e.setKey(key);
         e.setValue(value);
         setTx(ctx, e);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryVisitedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   @Override
   public void notifyCacheEntriesEvicted(Collection<InternalCacheEntry<? extends K, ? extends V>> entries, InvocationContext ctx, FlagAffectedCommand command) {
      if (!entries.isEmpty()) {
         if (isNotificationAllowed(command, cacheEntriesEvictedListeners)) {
            EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_EVICTED);
            Map<K, V> evictedKeysAndValues = transformCollectionToMap(entries,
               new InfinispanCollections.MapMakerFunction<K, V, InternalCacheEntry<? extends K, ? extends V>>() {
                  @Override
                  public Map.Entry<K, V> transform(final InternalCacheEntry<? extends K, ? extends V> input) {
                     return new Map.Entry<K, V>() {
                        @Override
                        public K getKey() {
                          return input.getKey();
                        }

                        @Override
                        public V getValue() {
                          return input.getValue();
                        }

                        @Override
                        public V setValue(V value) {
                          throw new UnsupportedOperationException();
                        }
                     };
                  }
               }
            );

            e.setEntries(evictedKeysAndValues);
            for (CacheEntryListenerInvocation<K, V> listener : cacheEntriesEvictedListeners) listener.invoke(e);
         }

         // For backward compat
         if (isNotificationAllowed(command, cacheEntryEvictedListeners)) {
            for (InternalCacheEntry<? extends K, ? extends V> ice : entries) {
               EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_EVICTED);
               e.setKey(ice.getKey());
               e.setValue(ice.getValue());
               boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(ice.getKey());
               for (CacheEntryListenerInvocation<K, V> listener : cacheEntryEvictedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
            }
         }
      }
   }

   @Override
   public void notifyCacheEntryEvicted(K key, V value,
         InvocationContext ctx, FlagAffectedCommand command) {
      boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
      if (isNotificationAllowed(command, cacheEntriesEvictedListeners)) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_EVICTED);
         Map<K, V> map = Collections.singletonMap(key, value);
         e.setEntries(map);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntriesEvictedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }

      // For backward compat
      if (isNotificationAllowed(command, cacheEntryEvictedListeners)) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_EVICTED);
         e.setKey(key);
         e.setValue(value);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryEvictedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   @Override
   public void notifyCacheEntryInvalidated(final K key, V value, final boolean pre,
         InvocationContext ctx, FlagAffectedCommand command) {
      if (isNotificationAllowed(command, cacheEntryInvalidatedListeners)) {
         final boolean originLocal = ctx.isOriginLocal();
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_INVALIDATED);
         e.setOriginLocal(originLocal);
         e.setPre(pre);
         e.setKey(key);
         e.setValue(value);
         setTx(ctx, e);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryInvalidatedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   @Override
   public void notifyCacheEntryLoaded(K key, V value, boolean pre,
         InvocationContext ctx, FlagAffectedCommand command) {
      if (isNotificationAllowed(command, cacheEntryLoadedListeners)) {
         boolean originLocal = ctx.isOriginLocal();
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_LOADED);
         e.setOriginLocal(originLocal);
         e.setPre(pre);
         e.setKey(key);
         e.setValue(value);
         setTx(ctx, e);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryLoadedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   @Override
   public void notifyCacheEntryActivated(K key, V value, boolean pre, InvocationContext ctx, FlagAffectedCommand command) {
      if (isNotificationAllowed(command, cacheEntryActivatedListeners)) {
         boolean originLocal = ctx.isOriginLocal();
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_ACTIVATED);
         e.setOriginLocal(originLocal);
         e.setPre(pre);
         e.setKey(key);
         e.setValue(value);
         setTx(ctx, e);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryActivatedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   private void setTx(InvocationContext ctx, EventImpl<K, V> e) {
      if (ctx != null && ctx.isInTxScope()) {
         GlobalTransaction tx = ((TxInvocationContext) ctx).getGlobalTransaction();
         e.setTransactionId(tx);
      }
   }

   @Override
   public void notifyCacheEntryPassivated(K key, V value, boolean pre, InvocationContext ctx, FlagAffectedCommand command) {
      if (isNotificationAllowed(command, cacheEntryPassivatedListeners)) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, CACHE_ENTRY_PASSIVATED);
         e.setPre(pre);
         e.setKey(key);
         e.setValue(value);
         boolean isLocalNodePrimaryOwner = clusteringDependentLogic.localNodeIsPrimaryOwner(key);
         for (CacheEntryListenerInvocation<K, V> listener : cacheEntryPassivatedListeners) listener.invoke(e, isLocalNodePrimaryOwner);
      }
   }

   @Override
   public void notifyTransactionCompleted(GlobalTransaction transaction, boolean successful, InvocationContext ctx) {
      if (!transactionCompletedListeners.isEmpty()) {
         boolean isOriginLocal = ctx.isOriginLocal();
         EventImpl<K, V> e = EventImpl.createEvent(cache, TRANSACTION_COMPLETED);
         e.setOriginLocal(isOriginLocal);
         e.setTransactionId(transaction);
         e.setTransactionSuccessful(successful);
         for (CacheEntryListenerInvocation<K, V> listener : transactionCompletedListeners) listener.invoke(e);
      }
   }

   @Override
   public void notifyTransactionRegistered(GlobalTransaction globalTransaction, boolean isOriginLocal) {
      if (!transactionRegisteredListeners.isEmpty()) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, TRANSACTION_REGISTERED);
         e.setOriginLocal(isOriginLocal);
         e.setTransactionId(globalTransaction);
         for (CacheEntryListenerInvocation<K, V> listener : transactionRegisteredListeners) listener.invoke(e);
      }
   }

   @Override
   public void notifyDataRehashed(ConsistentHash readCH, ConsistentHash writeCH, ConsistentHash unionCH, int newTopologyId, boolean pre) {
      if (!dataRehashedListeners.isEmpty()) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, DATA_REHASHED);
         e.setPre(pre);
         e.setConsistentHashAtStart(readCH);
         e.setConsistentHashAtEnd(writeCH);
         e.setUnionConsistentHash(unionCH);
         e.setNewTopologyId(newTopologyId);
         for (CacheEntryListenerInvocation<K, V> listener : dataRehashedListeners) listener.invoke(e);
      }
   }

   @Override
   public void notifyTopologyChanged(CacheTopology oldTopology, CacheTopology newTopology, int newTopologyId, boolean pre) {
      if (!topologyChangedListeners.isEmpty()) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, TOPOLOGY_CHANGED);
         e.setPre(pre);
         if (oldTopology != null) {
            e.setConsistentHashAtStart(oldTopology.getReadConsistentHash());
         }
         e.setConsistentHashAtEnd(newTopology.getWriteConsistentHash());
         e.setNewTopologyId(newTopologyId);
         for (CacheEntryListenerInvocation<K, V> listener : topologyChangedListeners) listener.invoke(e);
      }
   }

   @Override
   public void notifyPartitionStatusChanged(AvailabilityMode mode, boolean pre) {
      if (!partitionChangedListeners.isEmpty()) {
         EventImpl<K, V> e = EventImpl.createEvent(cache, PARTITION_STATUS_CHANGED);
         e.setPre(pre);
         e.setAvailabilityMode(mode);
         for (CacheEntryListenerInvocation<K, V> listener : partitionChangedListeners) listener.invoke(e);
      }
   }

   @Override
   public void notifyClusterListeners(Collection<? extends CacheEntryEvent<K, V>> events, UUID uuid) {
      for (CacheEntryEvent<K, V> event : events) {
         if (event.isPre()) {
            throw new IllegalArgumentException("Events for cluster listener should never be pre change");
         }
         switch (event.getType()) {
            case CACHE_ENTRY_MODIFIED:
               for (CacheEntryListenerInvocation<K, V> listener : cacheEntryModifiedListeners) {
                  if (listener.isClustered() && uuid.equals(listener.getIdentifier())) {
                     // We force invocation, since it means the owning node passed filters already and they
                     // already converted so don't run converter either
                     listener.invokeNoChecks(event, false, true);
                  }
               }
               break;
            case CACHE_ENTRY_CREATED:
               for (CacheEntryListenerInvocation<K, V> listener : cacheEntryCreatedListeners) {
                  if (listener.isClustered() && uuid.equals(listener.getIdentifier())) {
                     // We force invocation, since it means the owning node passed filters already and they
                     // already converted so don't run converter either
                     listener.invokeNoChecks(event, false, true);
                  }
               }
               break;
            case CACHE_ENTRY_REMOVED:
               for (CacheEntryListenerInvocation<K, V> listener : cacheEntryRemovedListeners) {
                  if (listener.isClustered() && uuid.equals(listener.getIdentifier())) {
                     // We force invocation, since it means the owning node passed filters already and they
                     // already converted so don't run converter either
                     listener.invokeNoChecks(event, false, true);
                  }
               }
               break;
            default:
               throw new IllegalArgumentException("Unexpected event type encountered!");
         }
      }
   }

   @Override
   public Collection<DistributedCallable> retrieveClusterListenerCallablesToInstall() {
      Set<Object> enlistedAlready = new HashSet<>();
      Set<DistributedCallable> callables = new HashSet<>();

      if (log.isTraceEnabled()) {
         log.tracef("Request received to get cluster listeners currently registered");
      }

      registerClusterListenerCallablesToInstall(enlistedAlready, callables, cacheEntryModifiedListeners);
      registerClusterListenerCallablesToInstall(enlistedAlready, callables, cacheEntryCreatedListeners);
      registerClusterListenerCallablesToInstall(enlistedAlready, callables, cacheEntryRemovedListeners);

      if (log.isTraceEnabled()) {
         log.tracef("Cluster listeners found %s", callables);
      }

      return callables;
   }

   private void registerClusterListenerCallablesToInstall(Set<Object> enlistedAlready,
                                                          Set<DistributedCallable> callables,
                                                          List<CacheEntryListenerInvocation<K, V>> listenerInvocations) {
      for (CacheEntryListenerInvocation<K, V> listener : listenerInvocations) {
         if (!enlistedAlready.contains(listener.getTarget())) {
            // If clustered means it is local - so use our address
            if (listener.isClustered()) {
               callables.add(new ClusterListenerReplicateCallable(listener.getIdentifier(),
                                                                  cache.getCacheManager().getAddress(), listener.getFilter(),
                                                                  listener.getConverter()));
               enlistedAlready.add(listener.getTarget());
            }
            else if (listener.getTarget() instanceof RemoteClusterListener) {
               RemoteClusterListener lcl = (RemoteClusterListener)listener.getTarget();
               callables.add(new ClusterListenerReplicateCallable(lcl.getId(), lcl.getOwnerAddress(), listener.getFilter(),
                                                                  listener.getConverter()));
               enlistedAlready.add(listener.getTarget());
            }
         }
      }
   }

   public boolean isNotificationAllowed(
         FlagAffectedCommand cmd, List<CacheEntryListenerInvocation<K, V>> listeners) {
      return (cmd == null || !cmd.hasFlag(Flag.SKIP_LISTENER_NOTIFICATION))
            && !listeners.isEmpty();
   }

   @Override
   public void addListener(Object listener) {
      addListener(listener, null, null, null);
   }

   @Override
   public void addListener(Object listener, ClassLoader classLoader) {
      addListener(listener, null, null, classLoader);
   }

   @Override
   public void addListener(Object listener, KeyFilter<? super K> filter, ClassLoader classLoader) {
      addListener(listener, new KeyFilterAsCacheEventFilter<K>(filter), null, classLoader);
   }

   /**
    * Adds the listener using the provided filter converter and class loader.  The provided builder is used to add
    * additional configuration including (clustered, onlyPrimary & identifier) which can be used after this method
    * is completed to see what values were used in the addition of this listener
    * @param listener
    * @param filter
    * @param converter
    * @param classLoader
    * @param <C>
    * @return
    */
   public <C> void addListener(Object listener, CacheEventFilter<? super K, ? super V> filter,
                                           CacheEventConverter<? super K, ? super V, C> converter, ClassLoader classLoader) {
      Listener l = testListenerClassValidity(listener.getClass());
      UUID generatedId = UUID.randomUUID();
      CacheMode cacheMode = config.clustering().cacheMode();
      CacheInvocationBuilder builder = new CacheInvocationBuilder();
      builder
            .setIncludeCurrentState(l.includeCurrentState())
            .setClustered(l.clustered())
            // .setOnlyPrimary(!l.clustered() ? l.primaryOnly() : (listener instanceof KeySpecificListener))
            .setOnlyPrimary(l.clustered() ? (cacheMode.isDistributed() ? true : false) : l.primaryOnly())
            .setFilter(filter)
            .setConverter(converter)
            .setIdentifier(generatedId)
            .setClassLoader(classLoader);

      if (converter instanceof CacheAware)
         ((CacheAware) converter).setCache((Cache<Object, Object>) cache);

      boolean foundMethods = validateAndAddListenerInvocation(listener, builder);

      if (foundMethods && l.clustered()) {
         if (cacheMode.isInvalidation()) {
            throw new UnsupportedOperationException("Cluster listeners cannot be used with Invalidation Caches!");
         }

         clusterListenerIDs.put(listener, generatedId);

         EmbeddedCacheManager manager = cache.getCacheManager();
         Address ourAddress = manager.getAddress();
         List<Address> members = manager.getMembers();

         // If we are the only member don't even worry about sending listeners
         if (members!=null && members.size() > 1) {

             int count = 0;
             DistributedExecutionCompletionService decs = new DistributedExecutionCompletionService(distExecutorService);
             Callable callable = new ClusterListenerReplicateCallable(generatedId, ourAddress, filter, converter);

             if(listener instanceof KeySpecificListener){
                 log.tracef("Partially replicating cluster listener with id %s",
                         generatedId);
                 Object key = ((KeySpecificListener)listener).key;
                 for (Address member : members) {
                     if ( !member.equals(ourAddress)
                          && key !=  null
                          && this.cache.getAdvancedCache().getDistributionManager().getPrimaryLocation(key).equals(member) ){
                         decs.submit(member, callable);
                         count ++;
                     }
                 }
             }else{
                log.tracef("Fully replicating cluster listener to other nodes %s for cluster listener with id %s",
                           members, generatedId);
                 count = members.size()-1;
                for (Address member : members) {
                   if (!member.equals(ourAddress)) {
                      decs.submit(member, callable);
                   }
                }

             }

             for (int i = 0; i < count; ++i) {
                 try {
                     decs.take().get();
                 } catch (InterruptedException e) {
                     throw new CacheListenerException(e);
                 } catch (ExecutionException e) {
                     throw new CacheListenerException(e);
                 }
             }
         }
      }

      // If we have a segment listener handler, it means we have to do initial state
      QueueingSegmentListener handler = segmentHandler.remove(generatedId);
      if (handler != null) {
         if (log.isTraceEnabled()) {
            log.tracef("Listener %s requests initial state for cache", generatedId);
         }
         try (CloseableIterator<CacheEntry<K, C>> iterator = entryRetriever.retrieveEntries(filter == null ? null :
               new CacheEventFilterAsKeyValueFilter(filter), converter == null ? null : new CacheEventConverterAsConverter(converter), null, handler)) {
            while (iterator.hasNext()) {
               CacheEntry<K, C> entry = iterator.next();
               // Mark the key as processed and see if we had a concurrent update
               Object value = handler.markKeyAsProcessing(entry.getKey());
               if (value == BaseQueueingSegmentListener.REMOVED) {
                  // Don't process this value if we had a concurrent remove
                  continue;
               }
               raiseEventForInitialTransfer(generatedId, entry, builder.isClustered());

               handler.notifiedKey(entry.getKey());
            }
         }

         Set<CacheEntry> entries = handler.findCreatedEntries();

         for (CacheEntry entry : entries) {
            raiseEventForInitialTransfer(generatedId, entry, builder.isClustered());
         }

         if (log.isTraceEnabled()) {
            log.tracef("Listener %s initial state for cache completed", generatedId);
         }

         handler.transferComplete();
      }
   }

   private void raiseEventForInitialTransfer(UUID identifier, CacheEntry entry, boolean clustered) {
      EventImpl preEvent;
      if (clustered) {
         // In clustered mode we only send post event
         preEvent = null;
      } else {
         preEvent = EventImpl.createEvent(cache, CACHE_ENTRY_CREATED);
         preEvent.setKey(entry.getKey());
         preEvent.setPre(true);
      }

      EventImpl postEvent = EventImpl.createEvent(cache, CACHE_ENTRY_CREATED);
      postEvent.setKey(entry.getKey());
      postEvent.setValue(entry.getValue());
      postEvent.setMetadata(entry.getMetadata());
      postEvent.setPre(false);

      for (CacheEntryListenerInvocation<K, V> invocation : cacheEntryCreatedListeners) {
         // Now notify all our methods of the creates
         if (invocation.getIdentifier() == identifier) {
            if (preEvent != null) {
               // Non clustered notifications are done twice
               invocation.invokeNoChecks(preEvent, true, true);
            }
            invocation.invokeNoChecks(postEvent, true, true);
         }
      }
   }

   @Override
   public void addListener(Object listener, KeyFilter filter) {
      addListener(listener, filter, null);
   }

   @Override
   public <C> void addListener(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
      addListener(listener, filter, converter, null);
   }

   class CacheInvocationBuilder extends AbstractInvocationBuilder {
      CacheEventFilter<? super K, ? super V> filter;
      CacheEventConverter<? super K, ? super V, ?> converter;
      boolean onlyPrimary;
      boolean clustered;
      boolean includeCurrentState;
      UUID identifier;

      public CacheEventFilter<? super K, ? super V> getFilter() {
         return filter;
      }

      public CacheInvocationBuilder setFilter(CacheEventFilter<? super K, ? super V> filter) {
         this.filter = filter;
         return this;
      }

      public CacheEventConverter<? super K, ? super V, ?> getConverter() {
         return converter;
      }

      public CacheInvocationBuilder setConverter(CacheEventConverter<? super K, ? super V, ?> converter) {
         this.converter = converter;
         return this;
      }

      public boolean isOnlyPrimary() {
         return onlyPrimary;
      }

      public CacheInvocationBuilder setOnlyPrimary(boolean onlyPrimary) {
         this.onlyPrimary = onlyPrimary;
         return this;
      }

      public boolean isClustered() {
         return clustered;
      }

      public CacheInvocationBuilder setClustered(boolean clustered) {
         this.clustered = clustered;
         return this;
      }

      public UUID getIdentifier() {
         return identifier;
      }

      public CacheInvocationBuilder setIdentifier(UUID identifier) {
         this.identifier = identifier;
         return this;
      }

      public boolean isIncludeCurrentState() {
         return includeCurrentState;
      }

      public CacheInvocationBuilder setIncludeCurrentState(boolean includeCurrentState) {
         this.includeCurrentState = includeCurrentState;
         return this;
      }

      @Override
      public CacheEntryListenerInvocation<K, V> build() {
         ListenerInvocation<Event<K, V>> invocation = new ListenerInvocationImpl(target, method, sync, classLoader,
                                                                                 subject);
         // If we are dealing with clustered events that forces the cluster listener to only use primary only else we would
         // have duplicate events
         CacheEntryListenerInvocation<K, V> returnValue;

         if (includeCurrentState) {
            // If it is a clustered listener and distributed cache we can do some extra optimizations
            if (clustered) {
               QueueingSegmentListener handler = segmentHandler.get(identifier);
               if (handler == null) {
                  if (config.clustering().cacheMode().isDistributed()) {
                     handler = new DistributedQueueingSegmentListener(entryFactory, distributionManager);
                  } else {
                     handler = new QueueingAllSegmentListener(entryFactory);
                  }
                  QueueingSegmentListener currentQueue = segmentHandler.putIfAbsent(identifier, handler);
                  if (currentQueue != null) {
                     handler = currentQueue;
                  }
               }
               returnValue = new ClusteredListenerInvocation<K, V>(invocation, handler, filter, converter, onlyPrimary, identifier);
            } else {
//               TODO: this is removed until non cluster listeners are supported
//               QueueingSegmentListener handler = segmentHandler.get(identifier);
//               if (handler == null) {
//                  handler = new QueueingAllSegmentListener();
//                  QueueingSegmentListener currentQueue = segmentHandler.putIfAbsent(identifier, handler);
//                  if (currentQueue != null) {
//                     handler = currentQueue;
//                  }
//               }
//               returnValue = new NonClusteredListenerInvocation(invocation, handler, filter, converter, onlyPrimary,
//                                                                identifier);
               returnValue = new BaseCacheEntryListenerInvocation(invocation, filter, converter, onlyPrimary, clustered,
                                                                  identifier);
            }
         } else {
            // If no includeCurrentState just use the base listener invocation which immediately passes all notifications
            // off
            returnValue = new BaseCacheEntryListenerInvocation(invocation, filter, converter, onlyPrimary, clustered,
                                                               identifier);
         }
         return returnValue;
      }
   }

   protected class NonClusteredListenerInvocation extends BaseCacheEntryListenerInvocation<K, V> {

      private final QueueingSegmentListener<K, V, Event<K, V>> handler;

      protected NonClusteredListenerInvocation(ListenerInvocation<Event<K, V>> invocation,
                                                 QueueingSegmentListener<K, V, Event<K, V>> handler,
                                                CacheEventFilter<? super K, ? super V> filter,
                                                CacheEventConverter<? super K, ? super V, ?> converter,
                                                boolean onlyPrimary, UUID identifier) {
         super(invocation, filter, converter, onlyPrimary, false, identifier);
         this.handler = handler;
      }

      @Override
      protected void doRealInvocation(Event<K, V> event) {
         if (!handler.handleEvent(event, invocation)) {
            super.doRealInvocation(event);
         }
      }
   }

   /**
    * This class is to be used with cluster listener invocations only when they have included current state.  Thus
    * we can assume all types are CacheEntryEvent, since it doesn't allow other types.
    */
   protected static class ClusteredListenerInvocation<K, V> extends BaseCacheEntryListenerInvocation<K, V> {

      private final QueueingSegmentListener<K, V, CacheEntryEvent<K, V>> handler;

      public ClusteredListenerInvocation(ListenerInvocation<Event<K, V>> invocation, QueueingSegmentListener handler,
                                       CacheEventFilter<? super K, ? super V> filter,
                                       CacheEventConverter<? super K, ? super V, ?> converter, boolean onlyPrimary,
                                       UUID identifier)  {
         super(invocation, filter, converter, onlyPrimary, true, identifier);
         this.handler = handler;
      }

      @Override
      public void invoke(Event<K, V> event) {
         throw new UnsupportedOperationException("Clustered initial transfer don't support regular events!");
      }

      @Override
      protected void doRealInvocation(Event<K, V> event) {
         // This is only used with clusters and such we can safely cast this here
         if (!handler.handleEvent((CacheEntryEvent<K,V>)event, invocation)) {
            super.doRealInvocation(event);
         }
      }
   }

   protected static class BaseCacheEntryListenerInvocation<K, V> implements CacheEntryListenerInvocation<K, V> {

      protected final ListenerInvocation<Event<K, V>> invocation;
      protected final CacheEventFilter<? super K, ? super V> filter;
      protected final CacheEventConverter<? super K, ? super V, ?> converter;
      protected final boolean onlyPrimary;
      protected final boolean clustered;
      protected final UUID identifier;


      protected BaseCacheEntryListenerInvocation(ListenerInvocation<Event<K, V>> invocation,
                                                 CacheEventFilter<? super K, ? super V> filter,
                                              CacheEventConverter<? super K, ? super V, ?> converter, boolean onlyPrimary,
                                              boolean clustered, UUID identifier)  {
         this.invocation = invocation;
         this.filter = filter;
         this.converter = converter;
         this.onlyPrimary = onlyPrimary;
         this.clustered = clustered;
         this.identifier = identifier;
      }

      @Override
      public void invoke(Event<K, V> event) {
         doRealInvocation(event);
      }

      /**
       * This is the entry point for local listeners firing events
       * @param event
       * @param isLocalNodePrimaryOwner
       */
      @Override
      public void invoke(CacheEntryEvent<K, V> event, boolean isLocalNodePrimaryOwner) {
         CacheEntryEvent<K, V> resultingEvent;
         // See if this should be filtered first before evaluating
         if ((resultingEvent = shouldInvoke(event, isLocalNodePrimaryOwner)) != null) {
            invokeNoChecks(resultingEvent, false, filter == converter && filter instanceof CacheEventFilterConverter);
         }
      }

      /**
       * This is the entry point for remote listener events being fired
       * @param event
       * @param skipQueue
       */
      @Override
      public void invokeNoChecks(CacheEntryEvent<K, V> event, boolean skipQueue, boolean skipConverter) {
         // We run the converter first, this way the converter doesn't have to run serialized when enqueued and also
         // the handler doesn't have to worry about it
         CacheEntryEvent<K, V> eventToUse;
         if (!skipConverter) {
            eventToUse = convertValue(converter, event);
         } else {
            eventToUse = event;
         }

         if (eventToUse.getValue()==null)
            return;
         
         if (skipQueue) {
            invocation.invoke(eventToUse);
         } else {
            doRealInvocation(eventToUse);
         }
      }

      protected void doRealInvocation(Event<K, V> event) {
         invocation.invoke(event);
      }

      protected CacheEntryEvent<K, V> shouldInvoke(CacheEntryEvent<K, V> event, boolean isLocalNodePrimaryOwner) {
         if (onlyPrimary && !isLocalNodePrimaryOwner) return null;
         if (event instanceof EventImpl) {
            EventImpl<K, V> eventImpl = (EventImpl<K, V>)event;
            // Cluster listeners only get post events
            if (eventImpl.isPre() && clustered) return null;
            EventType eventType;
            // Only use the filter if it was provided and we have an event that we can filter properly
            if (filter != null && (eventType = getEvent(eventImpl)) != null) {
               if (filter instanceof CacheEventFilterConverter) {
                  Object newValue = ((CacheEventFilterConverter)filter).filterAndConvert(eventImpl.getKey(),
                        eventImpl.getOldValue(), eventImpl.getOldMetadata(), eventImpl.getValue(),
                        eventImpl.getMetadata(), eventType);
                  if (newValue != null) {
                     EventImpl<K, V> clone = eventImpl.clone();
                     clone.setValue((V) newValue);
                     return clone;
                  } else {
                     return null;
                  }
               } else if (!filter.accept(eventImpl.getKey(), eventImpl.getOldValue(), eventImpl.getOldMetadata(),
                                 eventImpl.getValue(), eventImpl.getMetadata(), eventType)) {
                  return null;
               }
            }
         }
         return event;
      }

      // We can't currently filter events that don't implement CacheEntryEvent or CACHE_ENTRY_EVICTED events.  Basically
      // events that have a single key value pair only
      private EventType getEvent(EventImpl<K, V> event) {
         switch (event.getType()) {
            case CACHE_ENTRY_ACTIVATED:
            case CACHE_ENTRY_CREATED:
            case CACHE_ENTRY_INVALIDATED:
            case CACHE_ENTRY_LOADED:
            case CACHE_ENTRY_MODIFIED:
            case CACHE_ENTRY_PASSIVATED:
            case CACHE_ENTRY_REMOVED:
            case CACHE_ENTRY_VISITED:
               return new EventType(event.isPre(), event.isCommandRetried(), event.getType());
            default:
               return null;
         }
      }

      @Override
      public Object getTarget() {
         return invocation.getTarget();
      }

      @Override
      public CacheEventFilter<? super K, ? super V> getFilter() {
         return filter;
      }

      @Override
      public CacheEventConverter<? super K, ? super V, ?> getConverter() {
         return converter;
      }

      @Override
      public boolean isClustered() {
         return clustered;
      }

      @Override
      public UUID getIdentifier() {
         return identifier;
      }

      protected CacheEntryEvent<K, V> convertValue(CacheEventConverter<? super K, ? super V, ?> converter, CacheEntryEvent<K, V> event) {
         CacheEntryEvent<K, V> returnedEvent;
         if (converter != null) {
            if (event instanceof EventImpl) {
               // This is a bit hacky to let the C type be passed in for the V type
               EventImpl<K, V> eventImpl = (EventImpl<K, V>)event;
               Object newValue = converter.convert(eventImpl.getKey(), (V) eventImpl.getOldValue(),
                                 eventImpl.getOldMetadata(), (V) eventImpl.getValue(),
                                 eventImpl.getMetadata(), null);
               if (newValue != eventImpl.getValue()) {
                  EventImpl<K, V> clone = eventImpl.clone();
                  clone.setValue((V) newValue);
                  returnedEvent = clone;
               } else {
                  returnedEvent = eventImpl;
               }
            } else {
               throw new IllegalArgumentException("Provided event should be org.infinispan.notifications.cachelistener.eventEventImpl " +
                                                        "when a converter is being used!");
            }
         } else {
            returnedEvent = event;
         }
         return returnedEvent;
      }
   }

   @Override
   public void removeListener(Object listener) {
      super.removeListener(listener);

       UUID id = clusterListenerIDs.remove(listener);

       if (id != null) {

          List<Future<?>> futures;

          if(listener instanceof KeySpecificListener){

              EmbeddedCacheManager manager = cache.getCacheManager();
              Address ourAddress = manager.getAddress();
              Callable callable = new ClusterListenerRemoveCallable(id);
              Object key = ((KeySpecificListener)listener).key;
              futures = new ArrayList<Future<?>>();

              for (Address member : manager.getMembers()) {
                  if ( !member.equals(ourAddress)
                          && key !=  null
                          && this.cache.getAdvancedCache().getDistributionManager().locate(key).contains(member) ){
                      futures.add(distExecutorService.submit(member, callable));
                  }
              }
          }else{
              futures = distExecutorService.submitEverywhere(new ClusterListenerRemoveCallable(id));
          }

         for (Future<?> future : futures) {
            try {
               future.get();
            } catch (InterruptedException e) {
               throw new CacheListenerException(e);
            } catch (ExecutionException e) {
               throw new CacheListenerException(e);
            }
         }
      }
   }
}
