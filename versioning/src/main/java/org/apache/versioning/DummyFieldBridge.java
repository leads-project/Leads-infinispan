package org.apache.versioning;

import org.hibernate.search.bridge.StringBridge;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import java.io.IOException;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class DummyFieldBridge implements StringBridge {
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
}
