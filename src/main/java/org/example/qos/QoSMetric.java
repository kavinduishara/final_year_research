package org.example.qos;

/**
 * Quality-of-service snapshot for a single cut candidate.
 *
 * <p>Example Q1 cut@38 (shallow, more intermediate rows):
 * <pre>
 *   QoSMetric(estimatedRows=45000, transferCostBytes=4_500_000)
 * </pre>
 *
 * @param estimatedRows      Calcite row count at the cut node
 * @param transferCostBytes  {@link TransferCostEstimator#estimate(double)} of those rows
 */
public record QoSMetric(
        double estimatedRows,
        double transferCostBytes
) {
}
