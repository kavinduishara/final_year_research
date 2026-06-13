package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Traditional static QoS baseline: pick the executable cut with the lowest
 * estimated transfer cost (base-table shipping + intermediate transfer).
 * This models a cost-based distributed optimizer that uses statistics but
 * does not learn from execution.
 */
public class StaticQoSBaselinePolicy implements CutPolicy {

    @Override
    public Action choose(
            List<CutCandidate> candidates
    ) {
        CutCandidate best =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .min(Comparator
                                .comparingDouble(
                                        CutCandidate::totalTransferCost
                                )
                                .thenComparingDouble(
                                        CutCandidate::estimatedCost
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for static QoS baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }
}
