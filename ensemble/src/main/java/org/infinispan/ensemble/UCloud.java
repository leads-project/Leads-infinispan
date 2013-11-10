package org.infinispan.ensemble;

import org.infinispan.manager.CacheContainer;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Pierre Sutra
 * @since 4.0
 */

@XmlRootElement
public class UCloud {

    private static ConcurrentMap<String, UCloud> uclouds;
    static{
        uclouds = ZkManager.getInstance().newConcurrentMap("uclouds");
    }

    @XmlAttribute
    private String name;
    private CacheContainer container;

    public UCloud(String name, CacheContainer container) {
        this.name = name;
        this.container= container;
        if(uclouds.putIfAbsent(name, this)!=null)
            throw new IllegalAccessError();

    }

    public String getName(){
        return name;
    }

    public CacheContainer getContainer(){
        return container;
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof UCloud)) return false;
        return ((UCloud)o).getName().equals(this.getName());
    }

}
