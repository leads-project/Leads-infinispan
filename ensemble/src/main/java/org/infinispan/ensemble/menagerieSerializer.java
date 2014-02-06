package org.infinispan.ensemble;

import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import java.io.IOException;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class menagerieSerializer<T> implements org.menagerie.Serializer<T>{

    public menagerieSerializer(){}

    public byte[] serialize(T obj) {
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        try {
            return marshaller.objectToByteBuffer(obj);
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (InterruptedException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return null;
    }

    public T deserialize(byte[] bytes) {
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        try {
            return (T) marshaller.objectFromByteBuffer(bytes);
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return null;

    }
}
