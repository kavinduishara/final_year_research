package org.example.qos;

public class RewardCalculator {

    public static double reward(
            double transferCostBytes,
            double estimatedCost) {

        return -(transferCostBytes + estimatedCost);
    }
}