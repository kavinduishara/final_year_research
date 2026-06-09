package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

public class SmallestIntermediatePolicy {

    public Action choose(List<CutCandidate> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("No candidates");
        }

        CutCandidate best =
                candidates.stream()
                        .min(Comparator.comparingDouble(
                                CutCandidate::estimatedRows))
                        .orElseThrow();

        return new Action(best.nodeId());
    }
}