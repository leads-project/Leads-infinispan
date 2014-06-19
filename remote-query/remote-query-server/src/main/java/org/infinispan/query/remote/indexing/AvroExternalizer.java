package org.infinispan.query.remote.indexing;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.query.remote.ExternalizerIds;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * // TODO: Document this
 *
 * @author otrack
 * @since 4.0
 */
public class AvroExternalizer extends AbstractExternalizer<GenericData.Record> {

    public AvroExternalizer(){
    }

    @Override
    public Set<Class<? extends GenericData.Record>> getTypeClasses() {
        Set<Class<? extends GenericData.Record>> set = new HashSet<>();
        set.add(GenericData.Record.class);
        return set;
    }

    @Override
    public void writeObject(ObjectOutput output, GenericData.Record record) throws IOException {
        DatumWriter<GenericData.Record> datumWriter = new GenericDatumWriter<>(record.getSchema());
        DataFileWriter<GenericData.Record> writer = new DataFileWriter<>(datumWriter);
        writer.create(record.getSchema(),(ObjectOutputStream)output);
        writer.append(record);
    }

    @Override
    public GenericData.Record readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        DatumReader<GenericData.Record> reader = new GenericDatumReader<>();
        DataFileStream<GenericData.Record> stream = new DataFileStream<>((ObjectInputStream)input,reader);
        if(!stream.hasNext())
            throw new IOException("Stream is empty.");
        return stream.next();
    }

    @Override
    public Integer getId() {
        return ExternalizerIds.AVRO_VALUE_WRAPPER;
    }

}
