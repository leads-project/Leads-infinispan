package org.infinispan.atomic.container;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.0
 */
abstract class Call implements Serializable {

   UUID callID;

   public Call(UUID id){
      callID = id;
   }

   @Override
   public String toString(){
      return callID.toString();
   }

   @Override
   public boolean equals(Object o){
      return o instanceof Call && ((Call) o).callID.equals(this.callID);
   }

   @Override
   public int hashCode(){
      return callID.hashCode();
   }

}
