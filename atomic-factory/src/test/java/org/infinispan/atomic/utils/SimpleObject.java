package org.infinispan.atomic.utils;

import org.infinispan.atomic.Distributed;
import org.infinispan.atomic.Key;

import java.io.Serializable;

/**
* @author Pierre Sutra
*/
@Distributed
public class SimpleObject implements Serializable {

   @Key
   public String field1;

   public SimpleObject(){
      field1 = "test";
   }

   public SimpleObject(String f){
      field1 = f;
   }

   public String getField1(){ return field1;}
   
   public String toString(){
      return "SimpleObject["+field1+"]";
   }


}
