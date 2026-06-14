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

    public static boolean isWorkloadTrainMode() {
        String mode = trainingMode();
        return "workload-train".equalsIgnoreCase(mode)
                || "workload_train".equalsIgnoreCase(mode);
    }

    public static boolean resumeQTable() {
        return getBoolean(
                "training.qtable.resume",
                false
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

    /**
     * qtable = tabular Q-learning (default)
     * linucb = contextual bandit with LinUCB
     * thompson = contextual bandit with Thompson sampling
     */
    public static String learningAlgorithm() {
        try {
            return CONFIG.get("learning.algorithm")
                    .trim()
                    .toLowerCase();
        }
        catch (Exception e) {
            return "qtable";
        }
    }

    public static boolean usesBandit() {
        String algorithm = learningAlgorithm();
        return "linucb".equals(algorithm)
                || "thompson".equals(algorithm)
                || "ts".equals(algorithm);
    }

    public static org.example.rl.bandit.BanditAlgorithm banditAlgorithm() {
        return org.example.rl.bandit.BanditAlgorithm
                .fromConfig(learningAlgorithm());
    }

    public static String banditPath() {
        try {
            return CONFIG.get("learning.bandit.path")
                    .trim();
        }
        catch (Exception e) {
            return "bandit.json";
        }
    }

    public static double banditAlpha() {
        return getDouble(
                "learning.bandit.alpha",
                0.5
        );
    }

    public static double banditRidge() {
        return getDouble(
                "learning.bandit.ridge",
                1.0
        );
    }

    public static int banditMinObservations() {
        return getInt(
                "learning.bandit.min.observations",
                1
        );
    }

    public static boolean useExecutionBestAfterTraining() {
        return !isInferenceMode();
    }

    /**
     * static-qos = min estimated transfer (default, realistic traditional baseline)
     * combined-estimate = calcite cost + weighted transfer
     * deep-join = deepest join cut (weak ablation baseline)
     */
    public static String baselineMode() {
        try {
            return CONFIG.get("benchmark.baseline")
                    .trim()
                    .toLowerCase();
        }
        catch (Exception e) {
            return "static-qos";
        }
    }

    public static double baselineTransferWeight() {
        try {
            return Double.parseDouble(
                    CONFIG.get(
                            "benchmark.baseline.transfer.weight"
                    )
            );
        }
        catch (Exception e) {
            return 1.0;
        }
    }

    public static double baselineShipWeight() {
        try {
            return Double.parseDouble(
                    CONFIG.get(
                            "benchmark.baseline.ship.weight"
                    )
            );
        }
        catch (Exception e) {
            return 0.02;
        }
    }

    public static double baselinePessimismFactor() {
        try {
            return Double.parseDouble(
                    CONFIG.get(
                            "benchmark.baseline.pessimism.factor"
                    )
            );
        }
        catch (Exception e) {
            return 4.5;
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

    private static double getDouble(
            String key,
            double defaultValue
    ) {
        try {
            return Double.parseDouble(
                    CONFIG.get(key)
            );
        }
        catch (Exception e) {
            return defaultValue;
        }
    }
}
