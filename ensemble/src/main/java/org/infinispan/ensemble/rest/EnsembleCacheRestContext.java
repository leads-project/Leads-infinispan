package org.infinispan.ensemble.rest;

import org.infinispan.ensemble.EnsembleCacheManager;


public class EnsembleCacheRestContext {
    private EnsembleCacheManager manager;

    public void setEnsembleManager(EnsembleCacheManager manager) {
        this.manager = manager;
    }

    public EnsembleCacheManager getEnsembleManager() {
        return manager;
    }

}
