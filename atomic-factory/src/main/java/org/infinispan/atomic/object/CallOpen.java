package org.infinispan.atomic.object;

import java.util.UUID;

/**
 *
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallOpen extends Call{
   
   public CallOpen(UUID callerID) {
      super(callerID);
   }

   @Override
   public String toString() {
      return super.toString()+"-OPEN";
   }

}
