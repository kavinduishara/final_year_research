package org.example.benchmark;

public record BenchmarkResult(
        String policyName,
        double averageTransferCost
) {}