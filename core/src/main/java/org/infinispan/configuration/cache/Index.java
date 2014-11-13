package org.infinispan.configuration.cache;

/**
 * Used to configure indexing of entries in the cache for searching.
 *
 * @author Paul Ferraro
 */
public enum Index {
   NONE, OWNER, LOCAL, ALL;

   public boolean isEnabled() {
      return this == LOCAL || this == ALL || this == OWNER;
   }

   public boolean isLocalOnly() {
      return this == LOCAL;
   }

   public boolean isOwner() { return this==OWNER; }

}
