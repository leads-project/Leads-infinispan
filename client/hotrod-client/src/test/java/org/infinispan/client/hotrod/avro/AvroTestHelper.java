package org.infinispan.client.hotrod.avro;

import example.avro.Employee;
import example.avro.WebPage;

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
        page.setUrl("http://"+Long.toString(rand.nextLong())+".org/index.html");
        return page;
    }


    public static Employee createEmployee1() {
        Employee Employee = new Employee();
        Employee.setName("Tom");
        Employee.setSsn("12357");
        Employee.setDateOfBirth((long) 110280);
        return Employee;
    }

    public static Employee createEmployee2() {
        Employee Employee = new Employee();
        Employee.setName("Adrian");
        Employee.setSsn("12478");
        Employee.setDateOfBirth((long) 200991);
        return Employee;
    }

    public static void assertEmployee(Employee Employee) {
        assertNotNull(Employee);
        assertEquals("Tom", Employee.getName().toString());
    }

}
