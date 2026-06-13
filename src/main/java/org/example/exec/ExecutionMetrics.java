package org.example.exec;

public record ExecutionMetrics(
        long fragment1TimeMs,
        long baseTableShipTimeMs,
        long intermediateTransferTimeMs,
        long fragment2TimeMs
) {
    public long runtimeMs() {
        return fragment1TimeMs + fragment2TimeMs;
    }

    public long transferTimeMs() {
        return baseTableShipTimeMs
                + intermediateTransferTimeMs;
    }

    public long totalTimeMs() {
        return fragment1TimeMs
                + transferTimeMs()
                + fragment2TimeMs;
    }
}
