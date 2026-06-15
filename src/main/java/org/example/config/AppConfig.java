package org.example.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads key/value settings from a classpath resource (typically application.properties).
 *
 * <p>Example:
 * <pre>
 *   AppConfig cfg = new AppConfig("/application.properties");
 *   String url = cfg.get("meta.db.url");
 *   // returns "jdbc:postgresql://localhost:5432/public"
 * </pre>
 */
public class AppConfig {
    /** Parsed properties from the resource file. */
    private final Properties props;

    /**
     * Reads and loads the given classpath resource.
     *
     * @param resourceName e.g. {@code "/application.properties"}
     * @throws IllegalStateException if the resource file is missing
     * @throws RuntimeException if IO fails
     */
    public AppConfig(String resourceName) {
        try (InputStream in = AppConfig.class.getResourceAsStream(resourceName)) {
            if (in == null) throw new IllegalStateException("Missing config: " + resourceName);
            props = new Properties();
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a trimmed, non-blank property value.
     *
     * @param key e.g. {@code "worker1.tables"} → {@code "customer,orders"}
     * @return the property string (never null or blank)
     * @throws IllegalArgumentException if key is missing or empty
     */
    public String get(String key) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing property: " + key);
        return v.trim();
    }
}
