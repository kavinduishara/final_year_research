package org.example.qos;

public class TransferCostEstimator {

    private static final double AVG_ROW_SIZE_BYTES = 100;

    public static double estimate(double rows) {
        return rows * AVG_ROW_SIZE_BYTES;
    }
}