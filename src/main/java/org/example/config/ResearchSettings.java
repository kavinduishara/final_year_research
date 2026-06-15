package org.example.config;

/**
 * Typed accessors for research settings in application.properties.
 *
 * <p>Example current config:
 * <pre>
 *   training.mode=benchmark          → isBenchmarkMode() = true
 *   learning.bandit.path=bandit.json → banditPath()
 *   worker1.tables=customer,orders   → (read by WorkerRegistry)
 *   benchmark.baseline=weighted-ship → baselineMode()
 * </pre>
 */
public class ResearchSettings {

    private static final AppConfig CONFIG =
            new AppConfig("/application.properties");

    /** @return true if shipping.enabled (default true) — allows copying base tables to workers */
    public static boolean shippingEnabled() {
        return getBoolean(
                "shipping.enabled",
                true
        );
    }

    /** @return true if shipping.skip.if.present — skip COPY when table already on target worker */
    public static boolean skipShipIfPresent() {
        return getBoolean(
                "shipping.skip.if.present",
                true
        );
    }

    /** @return true if research.cleanup.between.queries — reset DB state before each query */
    public static boolean cleanupBetweenQueries() {
        return getBoolean(
                "research.cleanup.between.queries",
                true
        );
    }

    /** @return true if research.cleanup.after.execution — drop inter_* tables after each run */
    public static boolean cleanupAfterExecution() {
        return getBoolean(
                "research.cleanup.after.execution",
                true
        );
    }

    /**
     * @return number of training episodes per query (e.g. 5)
     * Each episode executes every executable cut once.
     */
    public static int trainingEpisodes() {
        return getInt(
                "training.episodes",
                1
        );
    }

    /** @return true when training.mode=inference — load bandit.json, run chosen cut once */
    public static boolean isInferenceMode() {
        return "inference".equalsIgnoreCase(
                trainingMode()
        );
    }

    /** @return true when training.mode=benchmark — run 10-query comparison table */
    public static boolean isBenchmarkMode() {
        return "benchmark".equalsIgnoreCase(
                trainingMode()
        );
    }

    /** @return true when training.mode=workload-train — train one bandit on all 10 queries */
    public static boolean isWorkloadTrainMode() {
        String mode = trainingMode();
        return "workload-train".equalsIgnoreCase(mode)
                || "workload_train".equalsIgnoreCase(mode);
    }

    /** @return true if training.bandit.resume — continue updating existing bandit.json */
    public static boolean resumeBanditModel() {
        return getBoolean(
                "training.bandit.resume",
                false
        );
    }

    /**
     * @return training mode string: train | inference | benchmark | workload-train
     * Default "train" if property missing.
     */
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

    /** @return path to saved LinUCB model, e.g. "bandit.json" */
    public static String banditPath() {
        try {
            return CONFIG.get("learning.bandit.path")
                    .trim();
        }
        catch (Exception e) {
            return "bandit.json";
        }
    }

    /** @return LinUCB exploration parameter alpha (e.g. 0.5) */
    public static double banditAlpha() {
        return getDouble(
                "learning.bandit.alpha",
                0.5
        );
    }

    /** @return ridge regularization for bandit (e.g. 1.0) */
    public static double banditRidge() {
        return getDouble(
                "learning.bandit.ridge",
                1.0
        );
    }

    /** @return min observations before bandit trusts its model (e.g. 1) */
    public static int banditMinObservations() {
        return getInt(
                "learning.bandit.min.observations",
                1
        );
    }

    /**
     * @return true in train/benchmark modes — after training, pick cut with best observed runtime
     * false in inference — always use bandit model scores
     */
    public static boolean useExecutionBestAfterTraining() {
        return !isInferenceMode();
    }

    /**
     * Baseline policy for benchmark comparison.
     * @return e.g. "weighted-ship", "static-qos", "pessimistic-qos", "calcite-cost"
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

    /** @return weight for transfer in combined-estimate baseline (default 1.0) */
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

    /** @return ship cost multiplier for weighted-ship baseline (default 0.02) */
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

    /** @return row inflation factor for pessimistic-qos baseline (default 4.5) */
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
