package org.example.qos;

public record QoSMetric(
        double estimatedRows,
        double transferCostBytes
) {
}