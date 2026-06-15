package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Traditional baseline that under-weights base-table shipping cost.
 *
 * <p>Often prefers deep cuts (zero intermediate transfer in the estimate)
 * even when real execution favors a shallow cut — giving RL room to win
 * after measuring actual runtime and transfer on Q1.
 *
 * <p>Example (worker1/worker2, shipping enabled):
 * <pre>
 *   cut@38: baseTableTransfer=0, intermediateTransfer≈4.5e6 → weighted cost low
 *   cut@42: baseTableTransfer≈6e8 (ship lineitem), intermediate=0 → may look worse
 *   But weighted ship factor 0.1 makes cut@42 appear cheaper → Action(42)
 * </pre>
 */
public class WeightedShipBaselinePolicy implements CutPolicy {

    /**
     * @param candidates Q1 cuts with baselineDecisionCost in weighted-ship mode
     * @return action with lowest weighted decision cost
     */
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
