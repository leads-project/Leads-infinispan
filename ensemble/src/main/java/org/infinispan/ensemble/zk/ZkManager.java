package org.infinispan.ensemble.zk;

import net.killa.kept.KeptConcurrentMap;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */
public class ZkManager {

    private ZooKeeper zk;
    private final int TIMEOUT = 1200000; // TODO put a proper timer
    Watcher watcher = new Watcher(){
        @Override
        public void process(WatchedEvent event) {
        }
    };

    public ZkManager(String host, String port) throws IOException {
        zk =  new ZooKeeper(host+":"+port,TIMEOUT,watcher);
    }

    public <K,V> ConcurrentMap<K,V> newConcurrentMap(String name) throws KeeperException, InterruptedException {
        return new ZkConcurrentMap<K,V>(
                new KeptConcurrentMap(zk, name, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
    }

}
