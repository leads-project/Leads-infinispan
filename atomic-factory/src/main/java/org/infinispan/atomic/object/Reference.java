package org.infinispan.atomic.object;

import org.infinispan.atomic.AtomicObjectFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pierre Sutra
 * @since 7.2
 *
 */
public class Reference<T> implements Externalizable{

   private Class<T> clazz;
   private Object key;

   public Reference(){}
   
   public Reference(Class<T> c, Object key){
      clazz = c;
      this.key = key;
   }

   @Override
   public int hashCode(){
      return clazz.hashCode() + key.hashCode();
   }

   @Override
   public boolean equals(Object o){
      if (!(o instanceof Reference))
         return false;
      return ((Reference)o).clazz.equals(this.clazz)
            && ((Reference)o).key.equals(this.key);
   }

   @Override
   public String toString(){
      return getClass().toString()+"#"+getKey().toString();
   }

   public Object getKey() {
      return key;
   }
   
   public Class getClazz(){ return clazz;}

   @Override public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeObject(clazz);
      objectOutput.writeObject(key);
   }

   @Override 
   public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      clazz = (Class) objectInput.readObject();
      key = objectInput.readObject();
   }
   
   public static Object unreference(Reference reference, AtomicObjectFactory factory) {
      return factory.getInstanceOf(reference);
   }
   
   public static Object[] unreference(Object[] args, AtomicObjectFactory factory) {
      List<Object> ret = new ArrayList<>();
      boolean added = false;
      for(Object arg : args) {
         ret.add( (arg instanceof Reference) ? unreference((Reference)arg, factory) : arg);
      }
      return ret.toArray();
   }
   
}
