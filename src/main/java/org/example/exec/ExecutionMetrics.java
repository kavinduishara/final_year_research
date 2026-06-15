package org.example.exec;

/**
 * Timing breakdown from one distributed query execution (one chosen cut).
 *
 * <p>Example (Q1 cut at JOIN orders⋈lineitem, fragment1 on worker2, fragment2 on worker1):
 * <pre>
 *   fragment1TimeMs           = 1200   (CREATE TABLE AS on worker2)
 *   baseTableShipTimeMs       = 800    (ship customer+orders to workers)
 *   intermediateTransferTimeMs= 500    (copy inter_single_n42 worker2→worker1)
 *   fragment2TimeMs           = 900    (final SELECT on worker1)
 *
 *   runtimeMs()    = 1200 + 900  = 2100
 *   transferTimeMs()= 800 + 500  = 1300
 *   totalTimeMs()  = 2100 + 1300 = 3400
 * </pre>
 * Reward = -(runtimeMs + transferTimeMs) = -3400
 */
public record ExecutionMetrics(
        /** Time to DROP + CREATE intermediate table (fragment1) on worker1. */
        long fragment1TimeMs,
        /** Time to ship remote base tables (customer, orders, lineitem copies). */
        long baseTableShipTimeMs,
        /** Time to copy intermediate table between workers (0 if same worker). */
        long intermediateTransferTimeMs,
        /** Time to run fragment2 SELECT on worker2. */
        long fragment2TimeMs
) {
    /** @return fragment1 + fragment2 compute time (excludes transfer). */
    public long runtimeMs() {
        return fragment1TimeMs + fragment2TimeMs;
    }

    /** @return base table shipping + intermediate table transfer. */
    public long transferTimeMs() {
        return baseTableShipTimeMs
                + intermediateTransferTimeMs;
    }

    /** @return full wall-clock cost used for benchmarking: runtime + all transfers. */
    public long totalTimeMs() {
        return fragment1TimeMs
                + transferTimeMs()
                + fragment2TimeMs;
    }
}
