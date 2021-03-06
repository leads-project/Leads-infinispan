package org.infinispan.statetransfer;

import org.infinispan.Cache;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commons.hash.MurmurHash3;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.container.DataContainer;
import org.infinispan.container.InternalEntryFactory;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.distribution.TestAddress;
import org.infinispan.distribution.ch.impl.DefaultConsistentHash;
import org.infinispan.distribution.ch.impl.DefaultConsistentHashFactory;
import org.infinispan.notifications.cachelistener.cluster.ClusterCacheNotifier;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.rpc.RpcOptionsBuilder;
import org.infinispan.remoting.transport.Address;
import org.infinispan.test.TestingUtil;
import org.infinispan.topology.CacheTopology;
import org.infinispan.transaction.impl.LocalTransaction;
import org.infinispan.transaction.impl.RemoteTransaction;
import org.infinispan.transaction.impl.TransactionTable;
import org.infinispan.util.concurrent.IsolationLevel;
import org.infinispan.commons.util.concurrent.NotifyingNotifiableFuture;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for StateProviderImpl.
 *
 * @author anistor@redhat.com
 * @since 5.2
 */
@Test(groups = "functional", testName = "statetransfer.StateProviderTest")
public class StateProviderTest {

   private static final Log log = LogFactory.getLog(StateProviderTest.class);
   private static final TestAddress A = new TestAddress(0, "A");
   private static final TestAddress B = new TestAddress(1, "B");
   private static final TestAddress C = new TestAddress(2, "C");
   private static final TestAddress D = new TestAddress(3, "D");
   private static final TestAddress E = new TestAddress(4, "E");
   private static final TestAddress F = new TestAddress(5, "F");
   private static final TestAddress G = new TestAddress(6, "G");


   private Configuration configuration;

   private ExecutorService mockExecutorService;
   private Cache cache;

   private RpcManager rpcManager;
   private CommandsFactory commandsFactory;
   private ClusterCacheNotifier cacheNotifier;
   private PersistenceManager persistenceManager;
   private DataContainer dataContainer;
   private TransactionTable transactionTable;
   private StateTransferLock stateTransferLock;
   private StateConsumer stateConsumer;
   private CacheTopology cacheTopology;
   private InternalEntryFactory ef;

   @BeforeTest
   public void setUp() {
      // create cache configuration
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().invocationBatching().enable()
            .clustering().cacheMode(CacheMode.DIST_SYNC)
            .clustering().stateTransfer().timeout(10000)
            .versioning().enable().scheme(VersioningScheme.SIMPLE)
            .locking().lockAcquisitionTimeout(200).writeSkewCheck(true).isolationLevel(IsolationLevel.REPEATABLE_READ);
      configuration = cb.build();

      ThreadFactory threadFactory = new ThreadFactory() {
         @Override
         public Thread newThread(Runnable r) {
            return new Thread(r);
         }
      };

      mockExecutorService = mock(ExecutorService.class);
      cache = mock(Cache.class);
      when(cache.getName()).thenReturn("testCache");

      rpcManager = mock(RpcManager.class);
      commandsFactory = mock(CommandsFactory.class);
      cacheNotifier = mock(ClusterCacheNotifier.class);
      persistenceManager = mock(PersistenceManager.class);
      dataContainer = mock(DataContainer.class);
      transactionTable = mock(TransactionTable.class);
      stateTransferLock = mock(StateTransferLock.class);
      stateConsumer = mock(StateConsumer.class);
      ef = mock(InternalEntryFactory.class);
      when(stateConsumer.getCacheTopology()).thenAnswer(new Answer<CacheTopology>() {
         @Override
         public CacheTopology answer(InvocationOnMock invocation) {
            return cacheTopology;
         }
      });
   }

   public void test1() throws InterruptedException {
      int numSegments = 4;

      // create list of 6 members
      List<Address> members1 = Arrays.<Address>asList(A, B, C, D, E, F);
      List<Address> members2 = new ArrayList<Address>(members1);
      members2.remove(A);
      members2.remove(F);
      members2.add(G);

      // create CHes
      DefaultConsistentHashFactory chf = new DefaultConsistentHashFactory();
      DefaultConsistentHash ch1 = chf.create(new MurmurHash3(), 2, numSegments, members1, null);
      DefaultConsistentHash ch2 = chf.updateMembers(ch1, members2, null);

      // create dependencies
      when(mockExecutorService.submit(any(Runnable.class))).thenAnswer(new Answer<Future<?>>() {
         @Override
         public Future<?> answer(InvocationOnMock invocation) {
            return null;
         }
      });

      when(rpcManager.getAddress()).thenReturn(A);
      when(rpcManager.getRpcOptionsBuilder(any(ResponseMode.class))).thenAnswer(new Answer<RpcOptionsBuilder>() {
         @Override
         public RpcOptionsBuilder answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            return new RpcOptionsBuilder(10000, TimeUnit.MILLISECONDS, (ResponseMode) args[0], true);
         }
      });

      // create state provider
      StateProviderImpl stateProvider = new StateProviderImpl();
      stateProvider.init(cache, mockExecutorService,
            configuration, rpcManager, commandsFactory, cacheNotifier, persistenceManager,
            dataContainer, transactionTable, stateTransferLock, stateConsumer, ef);

      final List<InternalCacheEntry> cacheEntries = new ArrayList<InternalCacheEntry>();
      Object key1 = new TestKey("key1", 0, ch1);
      Object key2 = new TestKey("key2", 0, ch1);
      cacheEntries.add(new ImmortalCacheEntry(key1, "value1"));
      cacheEntries.add(new ImmortalCacheEntry(key2, "value2"));
      when(dataContainer.iterator()).thenAnswer(new Answer<Iterator<InternalCacheEntry>>() {
         @Override
         public Iterator<InternalCacheEntry> answer(InvocationOnMock invocation) {
            return cacheEntries.iterator();
         }
      });
      when(transactionTable.getLocalTransactions()).thenReturn(Collections.<LocalTransaction>emptyList());
      when(transactionTable.getRemoteTransactions()).thenReturn(Collections.<RemoteTransaction>emptyList());

      cacheTopology = new CacheTopology(1, 1, ch1, ch1, ch1);
      stateProvider.onTopologyUpdate(cacheTopology, false);

      log.debug("ch1: " + ch1);
      Set<Integer> segmentsToRequest = ch1.getSegmentsForOwner(members1.get(0));
      List<TransactionInfo> transactions = stateProvider.getTransactionsForSegments(members1.get(0), 1, segmentsToRequest);
      assertEquals(0, transactions.size());

      try {
         stateProvider.getTransactionsForSegments(members1.get(0), 1, new HashSet<Integer>(Arrays.asList(2, numSegments)));
         fail("IllegalArgumentException expected");
      } catch (IllegalArgumentException e) {
         // expected
      }

      verifyNoMoreInteractions(stateTransferLock);

      stateProvider.startOutboundTransfer(F, 1, Collections.singleton(0));

      assertTrue(stateProvider.isStateTransferInProgress());

      log.debug("ch2: " + ch2);
      cacheTopology = new CacheTopology(2, 1, ch2, ch2, ch2);
      stateProvider.onTopologyUpdate(cacheTopology, true);

      assertFalse(stateProvider.isStateTransferInProgress());

      stateProvider.startOutboundTransfer(D, 1, Collections.singleton(0));

      assertTrue(stateProvider.isStateTransferInProgress());

      stateProvider.stop();

      assertFalse(stateProvider.isStateTransferInProgress());
   }

   public void test2() throws InterruptedException {
      int numSegments = 4;

      // create list of 6 members
      List<Address> members1 = Arrays.<Address>asList(A, B, C, D, E, F);
      List<Address> members2 = new ArrayList<Address>(members1);
      members2.remove(A);
      members2.remove(F);
      members2.add(G);

      // create CHes
      DefaultConsistentHashFactory chf = new DefaultConsistentHashFactory();
      DefaultConsistentHash ch1 = chf.create(new MurmurHash3(), 2, numSegments, members1, null);
      //todo [anistor] it seems that address 6 is not used for un-owned segments
      DefaultConsistentHash ch2 = chf.updateMembers(ch1, members2, null);

      when(commandsFactory.buildStateResponseCommand(any(Address.class), anyInt(), any(Collection.class))).thenAnswer(new Answer<StateResponseCommand>() {
         @Override
         public StateResponseCommand answer(InvocationOnMock invocation) {
            return new StateResponseCommand("testCache", (Address) invocation.getArguments()[0],
                  ((Integer) invocation.getArguments()[1]).intValue(),
                  (Collection<StateChunk>) invocation.getArguments()[2]);
         }
      });

      // create dependencies
      when(rpcManager.getAddress()).thenReturn(A);

      //rpcManager.invokeRemotelyInFuture(Collections.singleton(destination), cmd, false, sendFuture, timeout);
      doAnswer(new Answer<Map<Address, Response>>() {
         @Override
         public Map<Address, Response> answer(InvocationOnMock invocation) {
            Collection<Address> recipients = (Collection<Address>) invocation.getArguments()[0];
            ReplicableCommand rpcCommand = (ReplicableCommand) invocation.getArguments()[1];
            if (rpcCommand instanceof StateResponseCommand) {
               Map<Address, Response> results = new HashMap<Address, Response>();
               TestingUtil.sleepThread(10000, "RpcManager mock interrupted during invokeRemotelyInFuture(..)");
               return results;
            }
            return Collections.emptyMap();
         }
      }).when(rpcManager).invokeRemotelyInFuture(any(Collection.class), any(ReplicableCommand.class), any(RpcOptions.class),
                                                 any(NotifyingNotifiableFuture.class));

      when(rpcManager.getRpcOptionsBuilder(any(ResponseMode.class))).thenAnswer(new Answer<RpcOptionsBuilder>() {
         @Override
         public RpcOptionsBuilder answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            return new RpcOptionsBuilder(10000, TimeUnit.MILLISECONDS, (ResponseMode) args[0], true);
         }
      });


      // create state provider
      StateProviderImpl stateProvider = new StateProviderImpl();
      stateProvider.init(cache, mockExecutorService,
            configuration, rpcManager, commandsFactory, cacheNotifier, persistenceManager,
            dataContainer, transactionTable, stateTransferLock, stateConsumer, ef);

      final List<InternalCacheEntry> cacheEntries = new ArrayList<InternalCacheEntry>();
      Object key1 = new TestKey("key1", 0, ch1);
      Object key2 = new TestKey("key2", 0, ch1);
      Object key3 = new TestKey("key3", 1, ch1);
      Object key4 = new TestKey("key4", 1, ch1);
      cacheEntries.add(new ImmortalCacheEntry(key1, "value1"));
      cacheEntries.add(new ImmortalCacheEntry(key2, "value2"));
      cacheEntries.add(new ImmortalCacheEntry(key3, "value3"));
      cacheEntries.add(new ImmortalCacheEntry(key4, "value4"));
      when(dataContainer.iterator()).thenAnswer(new Answer<Iterator<InternalCacheEntry>>() {
         @Override
         public Iterator<InternalCacheEntry> answer(InvocationOnMock invocation) {
            return cacheEntries.iterator();
         }
      });
      when(transactionTable.getLocalTransactions()).thenReturn(Collections.<LocalTransaction>emptyList());
      when(transactionTable.getRemoteTransactions()).thenReturn(Collections.<RemoteTransaction>emptyList());

      cacheTopology = new CacheTopology(1, 1, ch1, ch1, ch1);
      stateProvider.onTopologyUpdate(cacheTopology, false);

      log.debug("ch1: " + ch1);
      Set<Integer> segmentsToRequest = ch1.getSegmentsForOwner(members1.get(0));
      List<TransactionInfo> transactions = stateProvider.getTransactionsForSegments(members1.get(0), 1, segmentsToRequest);
      assertEquals(0, transactions.size());

      try {
         stateProvider.getTransactionsForSegments(members1.get(0), 1, new HashSet<Integer>(Arrays.asList(2, numSegments)));
         fail("IllegalArgumentException expected");
      } catch (IllegalArgumentException e) {
         // expected
      }

      verifyNoMoreInteractions(stateTransferLock);

      stateProvider.startOutboundTransfer(F, 1, Collections.singleton(0));

      assertTrue(stateProvider.isStateTransferInProgress());

      // TestingUtil.sleepThread(15000);
      log.debug("ch2: " + ch2);
      cacheTopology = new CacheTopology(2, 1, ch2, ch2, ch2);
      stateProvider.onTopologyUpdate(cacheTopology, false);

      assertFalse(stateProvider.isStateTransferInProgress());

      stateProvider.startOutboundTransfer(E, 1, Collections.singleton(0));

      assertTrue(stateProvider.isStateTransferInProgress());

      stateProvider.stop();

      assertFalse(stateProvider.isStateTransferInProgress());
   }
}
