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
   private Class clazz;
   private Object[] initArgs;
   
   @Deprecated
   public CallOpen(){}

   public CallOpen(UUID callerID, boolean forceNew, Class clazz, Object[] initargs) {
      super(callerID);
      this.forceNew = forceNew;
      this.clazz = clazz;
      this.initArgs = initargs;
   }

   @Override
   public String toString() {
      return super.toString()+"-OPEN";
   }

   public boolean getForceNew() {
      return forceNew;
   }
   
   public Class getClazz(){
      return clazz;
   }
   
   public Object[] getInitArgs(){
      return initArgs;
   }

   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      super.writeExternal(objectOutput);
      objectOutput.writeBoolean(forceNew);
      objectOutput.writeObject(clazz);
      objectOutput.writeObject(initArgs);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      super.readExternal(objectInput);
      forceNew = objectInput.readBoolean();
      clazz = (Class) objectInput.readObject();
      initArgs = (Object[]) objectInput.readObject();
   }

}
