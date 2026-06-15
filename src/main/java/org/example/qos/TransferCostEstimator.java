package org.example.qos;

/**
 * Linear transfer-cost model: bytes ≈ rows × average row size.
 *
 * <p>Used throughout Q1 planning for intermediate and base-table shipping estimates.
 * Not a precise network model — a simple QoS proxy for comparing cuts.
 *
 * <p>Example:
 * <pre>
 *   estimate(45_000)  → 4_500_000 bytes
 *   estimate(1_500_000) → 150_000_000 bytes (orders table ship)
 * </pre>
 */
public class TransferCostEstimator {

    /** Assumed bytes per row for TPC-H style wide rows. */
    private static final double AVG_ROW_SIZE_BYTES = 100;

    /**
     * @param rows estimated or actual row count, e.g. 12000 at cut@42
     * @return transfer cost in bytes (rows × 100)
     */
    public static double estimate(double rows) {
        return rows * AVG_ROW_SIZE_BYTES;
    }
}
