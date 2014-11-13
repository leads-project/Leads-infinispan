package org.infinispan.query.backend;

import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;

/**
 * Defines for which events the Query Interceptor will generate indexing events.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 * @since 7.0
 */
public enum IndexModificationStrategy {

   /**
    * Indexing events will not be triggered automatically, still the indexing service
    * will be available.
    * Suited for example if index updates are controlled explicitly.
    */
   MANUAL {
      @Override
      public boolean shouldModifyIndexes(FlagAffectedCommand command, InvocationContext ctx, boolean isOwner) {
         return false;
      }
   },

   /**
    * Any event intercepted by the current node will trigger an indexing event
    * (excepting those flagged with {@code Flag.SKIP_INDEXING}.
    */
   ALL {
      @Override
      public boolean shouldModifyIndexes(FlagAffectedCommand command, InvocationContext ctx, boolean isOwner) {
         return !command.hasFlag(Flag.SKIP_INDEXING);
      }
   },

   /**
    * Any event intercepted by the current node will trigger an indexing event
    * (excepting those flagged with {@code Flag.SKIP_INDEXING}.
    */
   OWNER {
      @Override
      public boolean shouldModifyIndexes(FlagAffectedCommand command, InvocationContext ctx, boolean isOwner) {
         return isOwner && !command.hasFlag(Flag.SKIP_INDEXING);
      }
   },


   /**
    * Only events generated by the current node will trigger indexing events
    * (excepting those flagged with {@code Flag.SKIP_INDEXING}.
    */
   LOCAL_ONLY {
      @Override
      public boolean shouldModifyIndexes(FlagAffectedCommand command, InvocationContext ctx, boolean isOwner) {
         // will index only local updates that were not flagged with SKIP_INDEXING,
         // are not caused internally by state transfer and indexing strategy is not configured to 'manual'
         return ctx.isOriginLocal() && !command.hasFlag(Flag.PUT_FOR_STATE_TRANSFER) && !command.hasFlag(Flag.SKIP_INDEXING);
      }
   };

   public abstract boolean shouldModifyIndexes(FlagAffectedCommand command, InvocationContext ctx, boolean isOwner);

   /**
    * For a given configuration, define which IndexModificationStrategy is going to be used.
    * @param searchFactory
    * @param cfg
    * @return the appropriate IndexModificationStrategy
    */
   public static IndexModificationStrategy configuredStrategy(SearchFactoryIntegrator searchFactory, Configuration cfg) {
      if ("manual".equals(((SearchFactoryImplementor) searchFactory).getIndexingStrategy())) {
         return MANUAL;
      }
      else {
         if (cfg.indexing().index().isOwner()) {
            return OWNER;
         }
         else if (cfg.indexing().index().isLocalOnly()) {
            return LOCAL_ONLY;
         }
         else {
            return ALL;
         }
      }
   }

}
