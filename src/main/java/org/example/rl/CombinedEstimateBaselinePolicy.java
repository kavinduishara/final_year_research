package org.example.rl;

import org.example.config.ResearchSettings;
import org.example.plan.CutCandidate;

import java.util.Comparator;
import java.util.List;

/**
 * Combined estimate baseline: transfer cost plus Calcite plan cost.
 *
 * <p>Models optimizers that balance I/O/network estimates with planner cost.
 * Score = {@code estimatedCost + transferWeight × totalTransferCost}.
 *
 * <p>Q1 example (transferWeight=0.001):
 * <pre>
 *   cut@38: cost=12500 + 0.001×4.5e8 ≈ 462500
 *   cut@42: cost=8200  + 0.001×6e8   ≈ 608200
 *   choose() → Action(38)
 * </pre>
 */
public class CombinedEstimateBaselinePolicy implements CutPolicy {

    /**
     * @param candidates enriched Q1 cut list
     * @return action with lowest combined planner + transfer score
     */
    @Override
    public Action choose(
            List<CutCandidate> candidates
    ) {
        double transferWeight =
                ResearchSettings.baselineTransferWeight();

        CutCandidate best =
                candidates.stream()
                        .filter(CutCandidate::executable)
                        .min(Comparator
                                .<CutCandidate>comparingDouble(
                                        candidate ->
                                                combinedScore(
                                                        candidate,
                                                        transferWeight
                                                )
                                )
                                .thenComparingInt(
                                        CutCandidate::nodeId
                                ))
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No executable cut for combined baseline"
                                ));

        return new Action(
                best.nodeId()
        );
    }

    /**
     * @param candidate       one cut, e.g. node 38
     * @param transferWeight  from {@link ResearchSettings#baselineTransferWeight()}
     * @return combined scalar cost for comparison
     */
    private static double combinedScore(
            CutCandidate candidate,
            double transferWeight
    ) {
        return candidate.estimatedCost()
                + transferWeight
                        * candidate.totalTransferCost();
    }
}
