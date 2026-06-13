package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Baseline that over-estimates intermediate size (ignores join selectivity).
 */
public class PessimisticQoSBaselinePolicy implements CutPolicy {

    @Override
    public Action choose(
            List<CutCandidate> candidates
    ) {
        CutCandidate best =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .min(Comparator
                                .comparingDouble(
                                        CutCandidate::baselineDecisionCost
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for pessimistic QoS baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }
}
