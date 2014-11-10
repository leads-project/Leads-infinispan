package org.infinispan.versioning.utils.hibernate;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.infinispan.versioning.utils.version.VersionScalar;

/**
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class EntryVersionFieldBridge implements TwoWayFieldBridge{

    private static LongBridge bridge = new LongBridge();

    @Override
    public Object get(String name, Document document) {
        return bridge.stringToObject(document.get(name));
    }

    @Override
    public String objectToString(final Object object) {
        assert object!=null;
        if(object instanceof VersionScalar){
            VersionScalar version = (VersionScalar) object;
            return bridge.objectToString(version.version());
        }
        throw new IllegalArgumentException("not a numeric version  "+object.getClass().toString());
    }

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        String fieldValue = objectToString(value);
        luceneOptions.addFieldToDocument(name, fieldValue, document);
    }

}
