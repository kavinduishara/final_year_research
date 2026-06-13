package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Traditional baseline that under-weights base-table shipping cost.
 * Often prefers deep cuts (zero intermediate transfer in the estimate)
 * even when real execution favors a shallow cut — giving RL room to win
 * after measuring actual runtime and transfer.
 */
public class WeightedShipBaselinePolicy implements CutPolicy {

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
                                        "No executable cut for weighted-ship baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }
}
