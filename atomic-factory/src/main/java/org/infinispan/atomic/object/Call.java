package org.infinispan.atomic.object;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public abstract class Call implements Serializable {

   private UUID callID;
   private UUID callerID;

   public Call(UUID callerID){
      this.callID = UUID.randomUUID();
      this.callerID = callerID;
   }

   public UUID getCallID(){
      return callID;
   }
   
   public UUID getCallerID(){
      return callerID;
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
