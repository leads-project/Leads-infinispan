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
public class CallInvoke extends Call implements Externalizable{

   public String method;
   public Object[] arguments;

   @Deprecated
   public CallInvoke(){}

   public CallInvoke(UUID callerID, String m, Object[] args) {
      super(callerID);
      method = m;
      arguments = args;

   }

   @Override
   public String toString(){
      String args = " ";
      for(Object a : arguments){
         args+=a.toString()+" ";
      }
      return super.toString()+" -INV-"+method+ "("+args+")";
   }


   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeObject(method);
      objectOutput.writeObject(arguments);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      method = (String) objectInput.readObject();
      arguments = (Object[]) objectInput.readObject();
   }

}
