package org.infinispan.atomic.object;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 *
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallOpen extends Call{

   private boolean forceNew;
   
   @Deprecated
   public CallOpen(){}

   public CallOpen(UUID callerID, boolean forceNew) {
      super(callerID);
   }

   @Override
   public String toString() {
      return super.toString()+"-OPEN";
   }

   public boolean getForceNew() {
      return forceNew;
   }

   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeBoolean(forceNew);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      forceNew = objectInput.readBoolean();
   }



}
