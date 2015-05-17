package org.infinispan.atomic.utils;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
@Distributed
public class SimpleShardedObject implements Serializable{
   
   @Key
   public UUID id;
   
   public SimpleObject shard;
   
   public SimpleShardedObject(SimpleObject shard) {
      id = UUID.randomUUID();
      this.shard = shard;
   }
   
   public SimpleObject getShard(){
      return shard;
   }
   
   @Override
   public String toString(){
      return "SimpleShardedObject["+shard.toString()+"]";
   }

}
