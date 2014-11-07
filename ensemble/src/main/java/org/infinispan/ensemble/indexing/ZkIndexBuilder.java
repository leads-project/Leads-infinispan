//package org.infinispan.ensemble.indexing;
//
//import org.menagerie.DefaultZkSessionManager;
//import org.menagerie.ZkSessionManager;
//import org.menagerie.collections.ZkHashMap;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentMap;
//
///**
// *
// * @author Pierre Sutra
// * @since 7.0
// */
//public class ZkIndexBuilder implements IndexBuilder {
//
//    public static final int ZK_TO = 12000;
//    public static final String ZK_DEFAULT_CONNECT_STRING = "127.0.0.1";
//
//    public ZkSessionManager zkManager;
//
//    public ZkIndexBuilder(){
//        this(ZK_DEFAULT_CONNECT_STRING, ZK_TO);
//    }
//
//    public ZkIndexBuilder(String zkConnectString, int timeout){
//        zkManager = new DefaultZkSessionManager(zkConnectString, timeout);
//    }
//
//    @Override
//    public <K, V extends Indexable> ConcurrentMap<K, V> getIndex(Class<V> clazz) {
//        return new ZkHashMap<K, V>(clazz.getCanonicalName(), zkManager,new MenagerieSerializer<Map.Entry<K,V>>());
//    }
//}
