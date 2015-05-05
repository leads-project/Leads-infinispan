package org.infinispan.atomic.object;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallPersist extends Call implements Externalizable{

   private byte[] bytes;
   private UUID initialCallID;
   private int nclients;

   @Deprecated
   public CallPersist(){}

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


   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeObject(bytes);
      objectOutput.writeObject(initialCallID);
      objectOutput.writeInt(nclients);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      bytes = (byte[]) objectInput.readObject();
      initialCallID = (UUID) objectInput.readObject();
      nclients = (int) objectInput.readObject();
   }
}
