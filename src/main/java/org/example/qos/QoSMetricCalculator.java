package org.example.qos;

import org.example.plan.CutCandidate;

/**
 * Builds a simple QoS snapshot from one cut candidate's row estimate.
 *
 * <p>Example Q1 cut@42 with estimatedRows≈12000:
 * <pre>
 *   calculate(candidate) → QoSMetric(
 *     estimatedRows=12000,
 *     transferCostBytes=12000 × 100 = 1_200_000
 *   )
 * </pre>
 */
public class QoSMetricCalculator {

    /**
     * @param candidate enriched cut with row estimate from Calcite metadata
     * @return QoS metric with byte transfer cost via {@link TransferCostEstimator}
     */
    public static QoSMetric calculate(CutCandidate candidate) {

        double transferCost =
                TransferCostEstimator.estimate(
                        candidate.estimatedRows()
                );

        return new QoSMetric(
                candidate.estimatedRows(),
                transferCost
        );
    }
}
