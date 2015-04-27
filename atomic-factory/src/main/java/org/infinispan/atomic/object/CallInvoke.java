package org.infinispan.atomic.object;

import java.util.UUID;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class CallInvoke extends Call {

   public String method;
   public Object[] arguments;

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
}
