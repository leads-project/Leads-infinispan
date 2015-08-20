package org.infinispan.ensemble.indexing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pierre Sutra
 */
public abstract class Indexable implements Externalizable {

    protected static ConcurrentMap<Primary,Indexable> index;

    public static Indexable resolve(Primary primary){
        return index.get(primary);
    }

    public Indexable(){
    }

    @SuppressWarnings("unchecked")
    public Object readResolve() {
        try {
            return index.putIfAbsent(primary(this),this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        for(Field field : this.getClass().getFields()){
            if (field.isAnnotationPresent(Stored.class))
                objectOutput.writeObject(this);
        }

    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        for(Field field : this.getClass().getFields()){
            if (field.isAnnotationPresent(Stored.class))
                try {
                    field.set(this,objectInput.readObject());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
        }
    }

    private Primary primary(Indexable indexable) throws IllegalAccessException {
        for(Field field : this.getClass().getFields()){
            if (field.isAnnotationPresent(Primary.class))
                return (Primary) field.get(indexable);
        }
        return null;
    }

}
