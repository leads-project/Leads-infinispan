package org.infinispan.versioning;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.asserts.*;

public class VersionedCacheFactoryTest {
	
	VersionedCacheFactory factory;
	
	@BeforeTest
	public void init(){
		this.factory = new VersionedCacheFactory();
	}
	
	@Test
	public void newInstanceNaive() {
		//TODO how do I instantiate here a fake cacheManager ? 
		VersionedCache c = factory.newVersionedCache(VersioningTechnique.NAIVE, null, "test");
		Assert.assertNotNull(c);
	}
	
	@AfterTest
	public void tearDown(){
		factory = null;
	}
	
}
