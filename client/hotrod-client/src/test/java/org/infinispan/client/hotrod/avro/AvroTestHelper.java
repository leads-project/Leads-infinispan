package org.infinispan.client.hotrod.avro;

import example.avro.Employee;
import example.avro.WebPage;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Pierre Sutra
 * @since 7.0
 */
public class AvroTestHelper {

   private static Random rand = new Random(System.currentTimeMillis());

   public static WebPage somePage(){
      WebPage.Builder builder= WebPage.newBuilder();
      WebPage page = builder.build();
      page.setKey("http://" + Long.toString(rand.nextLong()) + ".org/index.html");
      page.setContent(ByteBuffer.allocate(100));
      return page;
   }


   public static Employee createEmployee1() {
      Employee Employee = new Employee();
      Employee.setName("Tom");
      Employee.setSsn("12357");
      Employee.setSalary(10000);
      Employee.setDateOfBirth((long) 110280);
      return Employee;
   }

   public static Employee createEmployee2() {
      Employee Employee = new Employee();
      Employee.setName("Adrian");
      Employee.setSalary(5000);
      Employee.setSsn("12478");
      Employee.setDateOfBirth((long) 200991);
      return Employee;
   }

   public static void assertEmployee(Employee Employee) {
      assertNotNull(Employee);
      assertEquals("Tom", Employee.getName().toString());
   }

   public static void assertEmployee2(Employee Employee) {
      assertNotNull(Employee);
      assertEquals("Adrian", Employee.getName().toString());
   }

}
