package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

public class CalciteCostBaselinePolicy implements CutPolicy {

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
                                .thenComparingDouble(
                                        CutCandidate::totalTransferCost
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for calcite-cost baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }
}
