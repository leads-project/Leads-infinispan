package org.infinispan.atomic.object;

import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallPersist extends Call {

   private Object object;
   private UUID initialCallID;
   private int nclients;

   public CallPersist(UUID callerID, UUID initialCallId, int nclients, Object object) {
      super(callerID);
      this.object = object;
      this.initialCallID = initialCallId;
      this.nclients = nclients;
   }
   
   public Object getObject(){
      return  object;
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
