package org.infinispan.ensemble.rest;

import org.infinispan.ensemble.EnsembleCacheManager;


public class EnsembleCacheRestContext {
    private EnsembleCacheManager manager;

    public EnsembleCacheManager getManager() {
        return manager;
    }

    public void setManager(EnsembleCacheManager manager) {
        this.manager = manager;
    }
}
