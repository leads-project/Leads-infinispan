package org.infinispan.atomic.object;

import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallRetrieve extends Call {

   private UUID openCallID;
   
   public CallRetrieve(UUID callerID, UUID openCallID) {
      super(callerID);
      this.openCallID = openCallID;
   }
   
   public UUID getOpenCallID(){
      return openCallID;
   }

   @Override
   public String toString() {
      return super.toString()+"-RET";
   }

}
