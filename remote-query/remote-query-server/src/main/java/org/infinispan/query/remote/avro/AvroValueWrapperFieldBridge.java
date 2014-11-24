package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

import java.io.IOException;
import java.util.Map;


/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroValueWrapperFieldBridge implements TwoWayFieldBridge{

   public static final String NULL="__null__";
   public static final String DELIMITER = "::";

   public GenericRecordExternalizer externalizer;

   public AvroValueWrapperFieldBridge(){
      externalizer = new GenericRecordExternalizer();
   }

   @Override
   public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
      AvroValueWrapper valueWrapper = (AvroValueWrapper) value;

      try {
         GenericData.Record record =  (GenericData.Record) externalizer.objectFromByteBuffer(valueWrapper.getBinary());
         Schema schema = record.getSchema();
         StringField stringField;
         for(Schema.Field field : schema.getFields()) {
            if (record.get(field.name()) != null) {
               switch (field.schema().getType()) {
               case MAP:
                  Map<?, ?> map = (Map) record.get(field.name());
                  for (Object k : map.keySet()) {
                     stringField = new StringField(
                           field.name(),
                           k.toString() + DELIMITER + (map.get(k) != null ? map.get(k).toString() : NULL),
                           Field.Store.YES);
                     addField(stringField, document);
                  }
                  break;
               default:
                  stringField = new StringField(
                        field.name(),
                        record.get(field.name()).toString(),
                        Field.Store.YES);
                  addField(stringField, document);
               }
            } else {
               stringField = new StringField(
                     field.name(),
                     NULL,
                     Field.Store.YES);
               addField(stringField, document);
            }
         }

      } catch (IOException | ClassNotFoundException e) {
         e.printStackTrace();
      }

   }

   @Override
   public Object get(String name, Document document) {
      if (document.get(name).equals(NULL))
         return null;
      return document.get(name);
   }

   @Override
   public String objectToString(Object object) {
      if (object==null)
         return NULL;
      return object.toString();
   }

   /**
    * By default field whose value is greater than 1000 are not indexed.
    * @param field
    * @param document
    */
   private void addField(StringField field, Document document){
      if (field.stringValue().length()>1000){
         //            // build a binary field (not indexed)
         //            // limit set to 1000 to handle the hard coded max value in Lucene (~30k) and save space
         //            document.add(new BinaryDocValuesField(
         //                    field.name(),
         //                    new BytesRef(field.stringValue().getBytes())));
      }else {
         document.add(field);
      }
   }
}
