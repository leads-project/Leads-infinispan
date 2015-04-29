package org.infinispan.atomic.object;

import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallPersist extends Call {

   private byte[] bytes;
   private UUID initialCallID;
   private int nclients;

   public CallPersist(UUID callerID, UUID initialCallId, int nclients, byte[] bytes) {
      super(callerID);
      this.bytes = bytes;
      this.initialCallID = initialCallId;
      this.nclients = nclients;
   }
   
   public byte[] getBytes(){
      return bytes;
   }

   public UUID getInitialCallID(){
      return initialCallID;
   }
   
   public int getNclients(){
      return nclients;
   }

   @Override
   public String toString() {
      return super.toString()+"-PER";
   }
}
