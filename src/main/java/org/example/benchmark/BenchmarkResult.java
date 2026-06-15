package org.example.benchmark;

/**
 * Timing breakdown for one policy's chosen cut on one query.
 *
 * <p>Example Q1 comparison row:
 * <pre>
 *   BenchmarkResult(
 *     policyName="LinUCB",
 *     rewardInputMs=1850,      // runtime + transfer (reward input)
 *     runtimeMs=1200,
 *     transferTimeMs=650,
 *     totalTimeMs=1850,
 *     baseTableShipTimeMs=0,   // cut@38: no base ship, intermediate only
 *     intermediateTransferTimeMs=650
 *   )
 * </pre>
 *
 * @param policyName                   baseline or "LinUCB"
 * @param rewardInputMs                runtime + transfer (used by reward function)
 * @param runtimeMs                    SQL execution time on workers
 * @param transferTimeMs               base ship + intermediate copy time
 * @param totalTimeMs                  end-to-end wall clock
 * @param baseTableShipTimeMs          time shipping base tables (e.g. lineitem)
 * @param intermediateTransferTimeMs   time copying inter_single_n42 between workers
 */
public record BenchmarkResult(
        String policyName,
        double rewardInputMs,
        long runtimeMs,
        long transferTimeMs,
        long totalTimeMs,
        long baseTableShipTimeMs,
        long intermediateTransferTimeMs
) {
}
