package org.infinispan.atomic.object;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.marshall.core.JBossMarshaller;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Pierre Sutra
 * @since 7.2
 */
public class Utils {

   public static Object callObject(Object obj, String method, Object[] args)
         throws InvocationTargetException, IllegalAccessException {

      boolean isFound = false;
      Object ret = null;

      for (Method m : obj .getClass().getMethods()) { // only public methods (inherited and not)
         if (method.equals(m.getName())) {
            boolean isAssignable = true;
            Class[] argsTypes = m.getParameterTypes();
            if(argsTypes.length == args.length){
               for(int i=0; i<argsTypes.length; i++){
                  if( !argsTypes[i].isAssignableFrom(args[i].getClass()) ){
                     isAssignable = false;
                     break;
                  }
               }
            }else{
               isAssignable = false;
            }
            if(!isAssignable)
               continue;

            ret = m.invoke(obj, args);
            isFound = true;
            break;
         }
      }

      if(!isFound)
         throw new IllegalStateException("Method "+method+" not found.");

      return ret;
   }

   public static Object initObject(Class clazz, Object... initArgs)
         throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

      boolean found = false;
      Constructor[] allConstructors = clazz.getDeclaredConstructors();
      for (Constructor ctor : allConstructors) {
         Class<?>[] pType  = ctor.getParameterTypes();
         if(pType.length==initArgs.length){
            found=true;
            for (int i = 0; i < pType.length; i++) {
               if(!pType[i].isAssignableFrom(initArgs[i].getClass())){
                  found=false;
                  break;
               }
            }
            if(found){
               return ctor.newInstance(initArgs);
            }
         }
      }

      throw new IllegalArgumentException("Unable to find constructor for "+clazz.toString()+" with "+initArgs);

   }

   public static byte[] marshall(Object object) {
      Marshaller marshaller = new JBossMarshaller();
      try {
         if (object instanceof byte[])
            return (byte[]) object;
         return marshaller.objectToByteBuffer(object);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
   }

   public static Object unmarshall(Object object) {
      Marshaller marshaller = new JBossMarshaller();
      try {
         if (object instanceof byte[])
            return marshaller.objectFromByteBuffer((byte[]) object);
         return object;
      } catch (Exception e){
         e.printStackTrace();
      }
      return null;
   }

   public static <K,V> Map<K,V> lruCache(final int maxSize) {
      return new LinkedHashMap<K,V>(maxSize*4/3, 0.75f, true) {
         @Override
         protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > maxSize;
         }
      };
   }

}
