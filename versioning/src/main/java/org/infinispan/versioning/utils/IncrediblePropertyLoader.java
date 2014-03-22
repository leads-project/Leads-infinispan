package org.infinispan.versioning.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Marcelo Pasin (pasin)
 * @since 7.0
 */
public class IncrediblePropertyLoader {

    public static int load(Properties prop, String fileName) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        if (is == null) {
            try { is = new FileInputStream(fileName); }
            catch (FileNotFoundException e) { }
        }
        if (is != null) {
            try {
                prop.load(is);
                return 1;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }
        return 0;
    }
}
