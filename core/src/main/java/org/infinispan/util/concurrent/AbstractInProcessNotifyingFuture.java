package org.infinispan.util.concurrent;

import org.infinispan.commons.util.concurrent.AbstractInProcessFuture;

/**
 * An abstract NotifyingFuture that has "completed"
 *
 * @author Manik Surtani
 * @version 4.1
 */
public abstract class AbstractInProcessNotifyingFuture<V> extends AbstractInProcessFuture<V> implements NotifyingFuture<V> {
   @Override
   public NotifyingFuture<V> attachListener(FutureListener<V> futureListener) {
      futureListener.futureDone(this);
      return this;
   }

   @Override
   public org.infinispan.commons.util.concurrent.NotifyingFuture<V> attachListener(org.infinispan.commons.util.concurrent.FutureListener<V> futureListener) {
      futureListener.futureDone(this);
      return this;
   }


}
