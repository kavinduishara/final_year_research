package org.example.plan;

import java.util.Set;

public record CutCandidate(
        int nodeId,
        double estimatedRows,
        int depth,
        double estimatedCost,
        Set<String> fragment1Tables,
        Set<String> fragment2Tables,
        String fragment1Worker,
        String fragment2Worker,
        double baseTableTransferCost,
        double intermediateTransferCost,
        double totalTransferCost,
        double baselineDecisionCost,
        String localityBucket,
        boolean executable
) {
    public CutCandidate(
            int nodeId,
            double estimatedRows,
            int depth,
            double estimatedCost
    ) {
        this(
                nodeId,
                estimatedRows,
                depth,
                estimatedCost,
                Set.of(),
                Set.of(),
                "",
                "",
                0,
                0,
                0,
                0,
                "UNKNOWN",
                false
        );
    }
}
