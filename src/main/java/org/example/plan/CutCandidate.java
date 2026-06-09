package org.example.plan;

public record CutCandidate(
        int nodeId,
        double estimatedRows,
        int depth,
        double estimatedCost
) {}