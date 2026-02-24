package org.example.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private final Properties props;

    public AppConfig(String resourceName) {
        try (InputStream in = AppConfig.class.getResourceAsStream(resourceName)) {
            if (in == null) throw new IllegalStateException("Missing config: " + resourceName);
            props = new Properties();
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String get(String key) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing property: " + key);
        return v.trim();
    }
}
