package org.example.benchmark;

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
