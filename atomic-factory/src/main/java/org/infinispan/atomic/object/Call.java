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
public class Call implements Externalizable {

   private UUID callID;
   private UUID callerID;

   @Deprecated
   public Call(){}

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

   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeObject(callID);
      objectOutput.writeObject(callerID);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      callID = (UUID) objectInput.readObject();
      callerID = (UUID) objectInput.readObject();
   }


}
