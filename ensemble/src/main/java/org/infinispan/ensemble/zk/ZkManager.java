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
    private final int TIMEOUT = 2000;
    Watcher watcher = new Watcher(){
        @Override
        public void process(WatchedEvent event) {
            System.out.println("[Watcher] Event: " + event.getType());
        }
    };

    public ZkManager(String host, String port) throws IOException {
        zk =  new ZooKeeper(host+":"+port,TIMEOUT,watcher);
    }

    public <K,V> ConcurrentMap<K,V> newConcurrentMap(String name){
        try {
            return new ZkConcurrentMap<K,V>(
                    new KeptConcurrentMap(zk, name, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
