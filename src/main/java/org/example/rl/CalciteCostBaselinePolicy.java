package org.example.rl;

import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Baseline that trusts Calcite planner self-cost at each cut node.
 *
 * <p>Uses {@link CutCandidate#baselineDecisionCost()} in calcite-cost mode
 * (falls back to total transfer when planner cost is unavailable).
 *
 * <p>Q1 example:
 * <pre>
 *   cut@38 estimatedCost=12500, totalTransfer=4.5e8
 *   cut@42 estimatedCost=8200,  totalTransfer=0
 *   choose() → Action(42) if Calcite ranks deeper join cheaper
 * </pre>
 */
public class CalciteCostBaselinePolicy implements CutPolicy {

    /**
     * @param candidates enriched cuts with estimatedCost from Calcite metadata
     * @return action minimizing baseline decision cost, then total transfer
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
                                        "No executable cut for calcite-cost baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }
}
