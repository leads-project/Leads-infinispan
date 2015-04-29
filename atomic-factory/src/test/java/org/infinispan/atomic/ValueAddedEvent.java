package org.infinispan.atomic;

import java.io.Serializable;

/**
* // TODO: Document this
*
* @author otrack
* @since 4.0
*/ // Needs to be Serializable or Externalizable!
public class ValueAddedEvent implements Serializable {
   final Integer key;
   final String value;
   ValueAddedEvent(Integer key, String value) {
      this.key = key;
      this.value = value;
   }
   
   @Override
   public String toString(){
      return this.key+" "+this.value;
   }
   
}
