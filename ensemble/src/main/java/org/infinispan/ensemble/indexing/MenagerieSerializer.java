package org.infinispan.ensemble.indexing;

import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import java.io.IOException;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class MenagerieSerializer<T> { // implements org.menagerie.Serializer<T>{

    public MenagerieSerializer(){}

    // @Override
    public byte[] serialize(T obj) {
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        try {
            return marshaller.objectToByteBuffer(obj);
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Customise this generated block
        } catch (InterruptedException e) {
            e.printStackTrace(); // TODO: Customise this generated block
        }
        return null;
    }

    // @Override
    public T deserialize(byte[] bytes) {
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        try {
            return (T) marshaller.objectFromByteBuffer(bytes);
        } catch (IOException e) {
            e.printStackTrace(); // TODO: Customise this generated block
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // TODO: Customise this generated block
        }
        return null;
    }
}
