package org.infinispan.versioning.utils.hibernate;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.LongNumericFieldBridge;
import org.infinispan.container.versioning.NumericVersion;

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
        if(object instanceof NumericVersion){
            NumericVersion version = (NumericVersion) object;
            return bridge.objectToString(version.getVersion());
        }
        throw new IllegalArgumentException("not a numeric version  "+object.getClass().toString());
    }

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        bridge.set(name,value,document,luceneOptions);
    }

}
