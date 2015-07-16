package org.infinispan.query.remote.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.lucene.document.*;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.infinispan.query.remote.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class ValueWrapperFieldBridge implements TwoWayFieldBridge{

   public static final String NULL="__null__";
   public static final String DELIMITER = "::";
   private static final Log log = LogFactory.getLog(ValueWrapperFieldBridge.class, Log.class);

   @Override
   public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
      GenericData.Record record = (GenericData.Record) value;
      Schema schema = record.getSchema();
      Collection<Field> fieldCollection = new ArrayList<>();
      for(Schema.Field field : schema.getFields()){
         if (record.get(field.name())!=null){
            Schema.Type type = field.schema().getType();
            computeLuceneFields(fieldCollection, type, field.name(),record.get(field.name()),field.schema());
         }
      }
      addFields(fieldCollection,document);
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

   private void addFields(Collection<Field> fieldCollection, Document document) {
      for(Field field : fieldCollection)
         addField(field,document);
   }
   
   /**
    * By default field whose value is greater than 1000 are not indexed.
    * @param field
    * @param document
    */
   private void addField(Field field, Document document){
      if (field instanceof StringField 
            && field.stringValue().length()>1000) {
         log.debug("field "+field.name()+" too long");
         return;
      }
      document.add(field);
   }
   
   private void computeLuceneFields(Collection<Field> fieldCollection, Schema.Type type , String name, Object value, Schema schema){
      switch (type){
      case MAP:
         Map<?,?> map = (Map) value;
         for(Object k: map.keySet()) {
            fieldCollection.add(
                  new StringField(
                        name,
                        k.toString()+DELIMITER+(map.get(k)!=null ? map.get(k).toString() : NULL),
                        Field.Store.NO)); // the field is *still* searchable
         }
         break;
      case INT:
         fieldCollection.add(
               new IntField(
                     name,
                     Integer.valueOf(value.toString()),
                     Field.Store.NO));
         break;
      case LONG:
         fieldCollection.add(
               new LongField(
                     name,
                     Long.valueOf(value.toString()),
                     Field.Store.NO));
         break;
      case FLOAT:
         fieldCollection.add(
               new FloatField(
                     name,
                     Float.valueOf(value.toString()),
                     Field.Store.NO));
         break;
      case UNION:
         for (Schema subSchema : schema.getTypes()) 
            computeLuceneFields(fieldCollection,subSchema.getType(),name,value,schema);
         break;
      case NULL:
         break;
      default:
         fieldCollection.add(
               new StringField(
                     name,
                     value.toString(),
                     Field.Store.NO));
      }
   }
}
