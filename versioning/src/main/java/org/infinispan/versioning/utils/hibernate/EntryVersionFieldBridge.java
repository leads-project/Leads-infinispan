package org.infinispan.versioning.utils.hibernate;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.LongNumericFieldBridge;
import org.infinispan.versioning.utils.version.VersionScalar;

/**
 * // TODO: Document this
 *
 * @author Pierre Sutra
 * @since 6.0
 */

public class EntryVersionFieldBridge implements TwoWayFieldBridge{

    private static LongNumericFieldBridge bridge = new LongNumericFieldBridge();

    @Override
    public Object get(String name, Document document) {
        return bridge.get(name,document);
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
        bridge.set(name,value,document,luceneOptions);
    }

}
