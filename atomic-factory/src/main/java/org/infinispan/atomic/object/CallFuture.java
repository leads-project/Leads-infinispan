package org.infinispan.atomic.object;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Idempotent future object.
 * @author Pierre Sutra
 * @since 6.0
 */
public class CallFuture implements Future<Object>, Externalizable {

   private static Log log = LogFactory.getLog(CallFuture.class);

   private Object ret;
   private UUID callID;
   private int state; // 0 => init, 1 => done, -1 => cancelled

   @Deprecated
   public CallFuture(){}

   public CallFuture(UUID callID){
      this.callID = callID;
      this.ret = null;
      this.state = 0;
   }

   public void set(Object r){

      synchronized (this) {

         if (state != 0) {
            return;
         }

         ret = r;
         state = 1;
         this.notifyAll();

      }

   }

   public UUID getCallID(){
      return callID;
   }

   public UUID setCallID(UUID id){
      return callID = id;
   }

   @Override
   public boolean equals(Object o){
      return o instanceof CallFuture&& ((CallFuture) o).callID.equals(this.callID);
   }

   @Override
   public boolean cancel(boolean mayInterruptIfRunning) {
      synchronized (this) {
         if (state != 0)
            return false;
         state = -1;
         if (mayInterruptIfRunning)
            this.notifyAll();
      }
      return true;
   }

   @Override
   public Object get() throws InterruptedException, ExecutionException {
      synchronized (this) {
         if (state == 0)
            this.wait();
      }
      return (state == -1) ? null : ret;
   }

   @Override
   public Object get(long timeout, TimeUnit unit)
         throws InterruptedException, ExecutionException, TimeoutException {
      synchronized (this) {
         if (state == 0)
            this.wait(timeout);
      }
      if (state==0) throw new TimeoutException(this + " failed");
      return (state == -1) ? null : ret;
   }

   @Override
   public boolean isCancelled() {
      return state == -1;
   }

   @Override
   public boolean isDone() {
      return state == 1;
   }

   @Override
   public String toString() {
      return "Future["+callID+"]";
   }

   @Override
   public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeObject(ret);
      objectOutput.writeObject(callID);
      objectOutput.writeInt(state);
   }

   @Override
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      ret = objectInput.readObject();
      callID = (UUID) objectInput.readObject();
      state = objectInput.readInt();
   }
}
