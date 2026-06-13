package org.example.qos;

import org.example.exec.ExecutionMetrics;

public class RewardCalculator {

    public static double reward(
            double transferCostBytes,
            double estimatedCost) {

        return -(transferCostBytes + estimatedCost);
    }

    /**
     * Execution-based reward from real distributed runs.
     * reward = -(runtimeMs + transferTimeMs)
     * where runtimeMs = fragment1Time + fragment2Time
     */
    public static double executionReward(
            ExecutionMetrics metrics) {

        return -(
                metrics.runtimeMs()
                        + metrics.transferTimeMs()
        );
    }
}
