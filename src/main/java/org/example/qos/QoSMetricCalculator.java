package org.example.qos;

import org.example.plan.CutCandidate;

public class QoSMetricCalculator {

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