package org.example.qos;

import org.example.exec.ExecutionMetrics;

/**
 * Converts execution cost into RL reward (higher reward = lower cost).
 *
 * <p>Example:
 * <pre>
 *   metrics.runtimeMs()=2100, metrics.transferTimeMs()=1300
 *   executionReward(metrics) = -(2100 + 1300) = -3400.0
 * </pre>
 * LinUCB receives reward/1000 = -3.4 in CutPolicyEngine.observe().
 */
public class RewardCalculator {

    /**
     * Estimate-based reward (legacy / planner QoS).
     *
     * @param transferCostBytes e.g. 120000.0
     * @param estimatedCost     e.g. 45000.0
     * @return negative sum, e.g. -165000.0
     */
    public static double reward(
            double transferCostBytes,
            double estimatedCost) {

        return -(transferCostBytes + estimatedCost);
    }

    /**
     * Execution-based reward from real distributed runs (primary signal).
     *
     * @param metrics e.g. ExecutionMetrics(f1=1200, ship=800, transfer=500, f2=900)
     * @return -(runtimeMs + transferTimeMs), e.g. -3400.0
     */
    public static double executionReward(
            ExecutionMetrics metrics) {

        return -(
                metrics.runtimeMs()
                        + metrics.transferTimeMs()
        );
    }
}
