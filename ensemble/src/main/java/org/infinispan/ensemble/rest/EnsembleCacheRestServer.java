package org.infinispan.ensemble.rest;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.ensemble.Site;
import org.jboss.logging.Logger;

import org.infinispan.ensemble.EnsembleCacheManager;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * REST server to access Infinispan Ensembles
 *
 * @author Marcelo Pasin
 * @since 6.0
 */

public class EnsembleCacheRestServer {

    private List<RemoteCacheManager> remoteServers = new ArrayList<RemoteCacheManager>();

    private class HotRodConfigurationBuilder
            extends org.infinispan.client.hotrod.configuration.ConfigurationBuilder {
        HotRodConfigurationBuilder(String server) {
            String spl[] = server.split(":");
            String host = spl[0];
            int port = Integer.parseInt(spl[1]);
            this.addServer()
                    .host(host)
                    .port(port)
                    .pingOnStartup(false);
        }
    }



    private void run() {
        Properties sysProps = System.getProperties();
        IncrediblePropertyLoader.load(sysProps);
        Logger logger = Logger.getLogger(this.getClass());

        String overridePort = sysProps.getProperty("ECrest.port", "11021");
        int port = Integer.valueOf(overridePort);

/*
        String hotrodServerString = sysProps.getProperty("restHotrodServers", "localhost:11222");
        String hotrodServer[] = hotrodServerString.split("|");


        manager = new EnsembleCacheManager();

        for (String site: hotrodServer) {
            HotRodConfigurationBuilder builder = new HotRodConfigurationBuilder(site);
            RemoteCacheManager rcm = new RemoteCacheManager(builder.build());
            manager.addSite(new Site(site, rcm, false));
            logger.info("Adding site: " + site);
        }

*/

        EnsembleCacheRestContext ctx = new EnsembleCacheRestContext();
        ctx.setEnsembleManager(new EnsembleCacheManager());

        EnsembleCacheManagerRestService.setContext(ctx);
        EnsembleCacheRestService.setContext(ctx);

        TJWSEmbeddedJaxrsServer tjws = new TJWSEmbeddedJaxrsServer();
        tjws.setPort(port);
        tjws.setRootResourcePath("/");
        tjws.getDeployment().getActualResourceClasses().add(EnsembleCacheManagerRestService.class);
        tjws.getDeployment().getActualResourceClasses().add(EnsembleCacheRestService.class);
        tjws.getDeployment().getActualResourceClasses().add(RootRestService.class);

        logger.info("Listening to port: " + port);

        tjws.start();

    }

    public static void main(String args[]) {
        new EnsembleCacheRestServer().run();
    }
}
