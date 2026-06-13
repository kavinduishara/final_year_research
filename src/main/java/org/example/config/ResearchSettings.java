package org.example.config;

public class ResearchSettings {

    private static final AppConfig CONFIG =
            new AppConfig("/application.properties");

    public static boolean shippingEnabled() {
        return getBoolean(
                "shipping.enabled",
                true
        );
    }

    public static boolean skipShipIfPresent() {
        return getBoolean(
                "shipping.skip.if.present",
                true
        );
    }

    public static boolean cleanupBetweenQueries() {
        return getBoolean(
                "research.cleanup.between.queries",
                true
        );
    }

    public static boolean cleanupAfterExecution() {
        return getBoolean(
                "research.cleanup.after.execution",
                true
        );
    }

    public static int trainingEpisodes() {
        return getInt(
                "training.episodes",
                1
        );
    }

    public static boolean isInferenceMode() {
        return "inference".equalsIgnoreCase(
                trainingMode()
        );
    }

    public static boolean isBenchmarkMode() {
        return "benchmark".equalsIgnoreCase(
                trainingMode()
        );
    }

    public static String trainingMode() {
        try {
            return CONFIG.get("training.mode")
                    .trim()
                    .toLowerCase();
        }
        catch (Exception e) {
            return "train";
        }
    }

    public static String qTablePath() {
        try {
            return CONFIG.get("training.qtable.path")
                    .trim();
        }
        catch (Exception e) {
            return "qtable.json";
        }
    }

    private static boolean getBoolean(
            String key,
            boolean defaultValue
    ) {
        try {
            return Boolean.parseBoolean(
                    CONFIG.get(key)
            );
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    private static int getInt(
            String key,
            int defaultValue
    ) {
        try {
            return Integer.parseInt(
                    CONFIG.get(key)
            );
        }
        catch (Exception e) {
            return defaultValue;
        }
    }
}
