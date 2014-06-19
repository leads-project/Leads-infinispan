package org.infinispan.query.remote.indexing;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class AvroValueWrapperFieldBridge implements FieldBridge{
    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        GenericData.Record record = (GenericData.Record) value;
        Schema schema = record.getSchema();
        StringField stringField;
        for(Schema.Field field : schema.getFields()){
            if (record.get(field.name())!=null){
                stringField = new StringField(field.name(),record.get(field.name()).toString(), Field.Store.NO);
            }else{
                stringField = new StringField(field.name(),"", Field.Store.NO);
            }
            document.add(stringField);
        }
    }
}
