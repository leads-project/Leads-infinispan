package org.infinispan.versioning;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import java.io.IOException;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class DummyFieldBridge implements TwoWayFieldBridge {

    @Override
    public Object get(String name, Document document) {
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        try {
            marshaller.objectFromByteBuffer(document.get(name).getBytes());
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return null;
    }

    @Override
    public String objectToString(Object object) {
        GenericJBossMarshaller marshaller = new GenericJBossMarshaller();
        try {
            return new String(marshaller.objectToByteBuffer(object));
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (InterruptedException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return null;
    }

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        String fieldValue = objectToString(value);
        luceneOptions.addFieldToDocument(name, fieldValue, document);
    }
}
