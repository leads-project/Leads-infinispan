package org.infinispan.ensemble.test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pierre Sutra
 */
public class RealDriver implements Driver {
   
   @Override 
   public int getNumberOfSites() {
      return 0;  // TODO: Customise this generated block
   }

   @Override 
   public void setNumberOfSites(int numberOfSites) {
      // TODO: Customise this generated block
   }

   @Override 
   public int getNumberOfNodes() {
      return 0;  // TODO: Customise this generated block
   }

   @Override 
   public void setNumberOfNodes(int numberOfNodes) {
      // TODO: Customise this generated block
   }

   @Override 
   public List<String> getCacheNames() {
      return null;  // TODO: Customise this generated block
   }

   @Override 
   public void setCacheNames(List<String> cacheNames) {
      // TODO: Customise this generated block
   }

   @Override 
   public void createSites() throws Throwable {
      // TODO: Customise this generated block
   }

   @Override 
   public List<String> sites() {
      List<String> ret = new ArrayList<>();
      // ret.add("80.156.73.93");
      ret.add("5.147.254.197");
      return ret;
   }

   @Override public void destroy() {
      // TODO: Customise this generated block
   }
}
